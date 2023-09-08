package com.chaoxi.map;

import static com.chaoxi.map.MyApp.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements AMapLocationListener, AMap.OnMapClickListener, View.OnKeyListener, GeocodeSearch.OnGeocodeSearchListener {
    MapView mapView = null;
    AMap aMap;
    private LatLng mMarker;
    private EditText etAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etAddress = (EditText) findViewById(R.id.et_address);
        etAddress.setOnKeyListener(this);
        initLocation();
        initMap(savedInstanceState);

        initMarker();
        requestPermission();
    }


    /**
     * 初始化定位
     * mLocationClient.startLocation();开始定位
     */
    private void initLocation() {
        // 初始化定位
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mLocationClient != null) {
            // 设置定位回调监听
            mLocationClient.setLocationListener(this);
            // 初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            // 设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 获取最近3s内精度最高的一次定位结果：
            // 设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
            mLocationOption.setOnceLocationLatest(true);
            // 设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);
            // 设置定位请求超时时间，单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
            mLocationOption.setHttpTimeOut(20000);
            // 关闭缓存机制，高精度定位会产生缓存。
            mLocationOption.setLocationCacheEnable(false);
            // 给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
        }
    }

    // 地理编码搜索
    private GeocodeSearch geocodeSearch;
    // 解析成功标识码
    private static final int PARSE_SUCCESS_CODE = 1000;
    private void initMap(Bundle savedInstanceState) {
        // 获取地图控件引用
        mapView = (MapView) findViewById(R.id.map);
        // 在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        MyLocationStyle myLocationStyle = new MyLocationStyle();// 初始化定位蓝点样式类;
        // 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        // myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        aMap.setMinZoomLevel(5.0f);
        aMap.setMaxZoomLevel(15.0f);
//        设置默认定位按钮是否显示，非必需设置。
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

        aMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        // UiSettings更改一些ui设置
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);

        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_foreground);
        myLocationStyle.myLocationIcon(bitmapDescriptor);
        myLocationStyle.showMyLocation(true);
        // 自定义精度范围的圆形边框颜色  都为0则透明
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        // 自定义精度范围的圆形边框宽度  0 无宽度
        myLocationStyle.strokeWidth(0);
        // 设置圆形的填充颜色  都为0则透明
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));

        aMap.setMyLocationStyle(myLocationStyle);// 设置定位蓝点的Style
        aMap.setOnMapClickListener(this);

        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }

    }

    private void initMarker() {
//        LatLng latLng = new LatLng(39.906901,116.397972);

    }

    // 请求权限码
    private static final int REQUEST_PERMISSIONS = 8888;

    /**
     * 动态请求权限
     */
    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    private void requestPermission() {
        String[] permissions = {
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (EasyPermissions.hasPermissions(this, permissions)) {
            // true 有权限 开始定位
            Toast.makeText(this, "已获得权限，开始定位！", Toast.LENGTH_SHORT).show();
            // mLocationClient.startLocation();
        } else {
            // false 无权限
            EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSIONS, permissions);
        }
    }

    /**
     * 请求权限结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 设置权限请求结果
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy();
        // 销毁定位客户端，同时销毁本地定位服务。
        mLocationClient.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    // 声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    // 声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    /**
     * 接收异步返回的定位结果
     *
     * @param aMapLocation
     */
    @SuppressLint("DefaultLocale")
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                // 地址
                String address = aMapLocation.getAddress();
                // 获取经度
                double latitude = aMapLocation.getLatitude();
                // 获取纬度
                double longitude = aMapLocation.getLongitude();

                Toast.makeText(this, String.format("我的地址：%s%n 经度：%f %n 纬度:%f", address, latitude, longitude), Toast.LENGTH_SHORT).show();
                Log.d(TAG, String.format("我的地址：%s%n 经度：%f %n 纬度: %f", address, latitude, longitude));
                // 停止定位本地定位服务不会被销毁
                mLocationClient.stopLocation();
                street = aMapLocation.getStreet();
            } else {
                // 定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e(TAG, "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    boolean isMarkerAdded = false;

    /**
     * 点击地图添加标记地点
     *
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "~~~~~~~~~~~");

//        mMarker = latLng;
        latLngToAddress(latLng);
       /*  if (!isMarkerAdded) {
            Marker marker = aMap.addMarker(
                    new MarkerOptions()
                            .position(latLng)
                            // 点标记是否可拖拽
                            .draggable(true)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_foreground)));
        } */
    }


    private String street;
    /**
     * 键盘点击
     *
     * @param v
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
            // 获取输入框的值
            String address = etAddress.getText().toString().trim();

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            // 隐藏软键盘
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

            // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
            GeocodeQuery query = new GeocodeQuery(address, street);
            geocodeSearch.getFromLocationNameAsyn(query);

            return true;
        }
        return false;
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
        //解析result获取地址描述信息
        if(rCode == PARSE_SUCCESS_CODE){
            RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
            //显示解析后的地址
            Toast.makeText(this, "地址："+regeocodeAddress.getFormatAddress(), Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * 通过经纬度获取地址
     * @param latLng
     */
    private void latLngToAddress(LatLng latLng) {
        //位置点  通过经纬度进行构建
        LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
        //逆编码查询  第一个参数表示一个LatLng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 20, GeocodeSearch.AMAP);
        //异步获取地址信息
        geocodeSearch.getFromLocationAsyn(query);
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
        if (rCode == PARSE_SUCCESS_CODE) {
            List<GeocodeAddress> geocodeAddressList = geocodeResult.getGeocodeAddressList();
            if(geocodeAddressList!=null && geocodeAddressList.size()>0){
                LatLonPoint latLonPoint = geocodeAddressList.get(0).getLatLonPoint();
                //显示解析后的坐标
                Toast.makeText(this, "坐标：" + latLonPoint.getLongitude()+"，"+latLonPoint.getLatitude(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}