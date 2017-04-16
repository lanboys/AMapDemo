package com.amap.map3d.demo.overlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.map3d.demo.R;
import com.amap.map3d.demo.util.Constants;
import com.amap.map3d.demo.util.ToastUtil;

import java.util.ArrayList;

/**
 * AMapV2地图中简单介绍一些Marker的用法.
 */
public class MapActivity extends Activity implements OnMarkerClickListener,
        OnInfoWindowClickListener,
        OnClickListener, InfoWindowAdapter {

    Bitmap lastMarkerBitMap = null;
    private TextView markerText;
    private AMap aMap;
    private MapView mapView;
    private Marker marker1; // 好好学习
    private Marker marker2;// 有跳动效果的marker对象(西安)
    private Marker marker3;// 从地上生长的marker对象(郑州)
    private LatLng latlng = new LatLng(36.061, 103.834);
    private int count = 1;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption = new AMapLocationClientOption();
    //标识，用于判断是否只显示一次定位信息和用户重新定位
    private boolean isFirstLoc = true;
    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (null != amapLocation) {
                //解析定位结果
                String result = Utils.getLocationStr(amapLocation);
                //   tvResult.setText(result);
            } else {
                //tvResult.setText("定位失败，loc is null");
            }

           // Toast.makeText(MapActivity.this, "位置改变", Toast.LENGTH_SHORT).show();

            if (isFirstLoc) {
                //设置缩放级别
                aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                //将地图移动到定位点
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude())));
                //添加图钉
                //aMap.addMarker(getMarkerOptions(amapLocation));
                addMarkersToMap(amapLocation);

                //获取定位信息
                isFirstLoc = false;
                // Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        /*
         * 设置离线地图存储目录，在下载离线地图或初始化地图设置; 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
		 * 则需要在离线地图下载和使用地图页面都进行路径设置
		 */
        // Demo中为了其他界面可以使用下载的离线地图，使用默认位置存储，屏蔽了自定义设置
        // MapsInitializer.sdcardDir =OffLineMapUtils.getSdCacheDir(this);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState); // 此方法必须重写



        init();
        initLocation();
        startLocation();
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        markerText = (TextView) findViewById(R.id.mark_listenter_text);

        Button clearMap = (Button) findViewById(R.id.clearMap);
        clearMap.setOnClickListener(this);
        Button resetMap = (Button) findViewById(R.id.resetMap);
        resetMap.setOnClickListener(this);

        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }

        //设置右上角手动定位定位按钮 并且可以点击
        UiSettings settings = aMap.getUiSettings();
        // 是否显示定位按钮
        settings.setMyLocationButtonEnabled(true);

        // 是否可触发定位并显示定位层
        aMap.setMyLocationEnabled(true);
    }

    private void setUpMap() {
        // aMap.setOnMarkerDragListener(this);// 设置marker可拖拽事件监听器
        // aMap.setOnMapLoadedListener(this);// 设置amap加载成功事件监听器

        aMap.setOnMarkerClickListener(this);// 设置点击marker事件监听器
        aMap.setOnInfoWindowClickListener(this);// 设置点击infoWindow事件监听器

        aMap.setInfoWindowAdapter(this);// 设置自定义InfoWindow样式
        //addMarkersToMap();// 往地图上添加marker
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        destroyLocation();
    }

    /**
     * 销毁定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void destroyLocation() {
        if (null != locationClient) {

            // 停止定位
            locationClient.stopLocation();
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
        }
    }

    /**
     * 在地图上添加marker
     *
     * @param amapLocation
     */
    private void addMarkersToMap(AMapLocation amapLocation) {

        // marker1 = aMap.addMarker(new MarkerOptions()
        //         .title("确认位置")
        //         .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        //         .draggable(true));

        marker1 = aMap.addMarker(getMarkerOptions(amapLocation));

        marker1.setRotateAngle(-90);// 设置marker旋转90度
        marker1.setPositionByPixels(400, 900);
        marker1.showInfoWindow();// 设置默认显示一个infowinfow
    }

    private void addMarkersToMap() {

        //郑州
        marker3 = aMap.addMarker(
                new MarkerOptions()
                        .position(Constants.ZHENGZHOU)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
    }

    /**
     * 对marker标注点点击响应事件
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        markerText.setText("你点击的是" + marker.getTitle());

        return false;
    }

    /**
     * marker点击时跳动一下
     */
    public void jumpPoint(final Marker marker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = aMap.getProjection();
        Point startPoint = proj.toScreenLocation(Constants.XIAN);
        startPoint.offset(0, -100);
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * Constants.XIAN.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * Constants.XIAN.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }



 /*   1.getInfoWindow(Marker marker)-用于个性化定制信息窗口。如果这个方法
      返回一个view，则这个view会被用来当做整个信息窗口。如果这个方法返回null，
      则会是使用默认的信息窗口风格。

      2.getInfoContents(Marker marker)-用于定制信息窗口的内容。
      该方法只有在getInfoWindow(Marker) 返回null时才会被调用。如果这个方
      法返回一个view，它将替代现有的默认的信息窗口。如果这个方法返回null，
      则会使用默认的信息窗口风格。
 */

    /*
    * 有 自定义  ------>  getInfoWindow   非 null return
    *                                     null----->  getInfoContents   非 null return
    *                                                                    null----->  系统自定义
    * */

    /**
     * marker 必须有设置图标，否则无效果
     *
     * @param marker
     */
    private void dropInto(final Marker marker) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final LatLng markerLatlng = marker.getPosition();
        Projection proj = aMap.getProjection();
        Point markerPoint = proj.toScreenLocation(markerLatlng);
        Point startPoint = new Point(markerPoint.x, 0);// 从marker的屏幕上方下落
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 800;// 动画总时长

        final Interpolator interpolator = new AccelerateInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * markerLatlng.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * markerLatlng.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    /**
     * 从地上生长效果，实现思路
     * 在较短的时间内，修改marker的图标大小，从而实现动画<br>
     * 1.保存原始的图片；
     * 2.在原始图片上缩放得到新的图片，并设置给marker；
     * 3.回收上一张缩放后的图片资源；
     * 4.重复2，3步骤到时间结束；
     * 5.回收上一张缩放后的图片资源，设置marker的图标为最原始的图片；
     * <p>
     * 其中时间变化由AccelerateInterpolator控制
     *
     * @param marker
     */
    private void growInto(final Marker marker) {
        marker.setVisible(false);
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 250;// 动画总时长
        final Bitmap bitMap = marker.getIcons().get(0).getBitmap();// BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
        final int width = bitMap.getWidth();
        final int height = bitMap.getHeight();

        final Interpolator interpolator = new AccelerateInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);

                if (t > 1) {
                    t = 1;
                }

                // 计算缩放比例
                int scaleWidth = (int) (t * width);
                int scaleHeight = (int) (t * height);
                if (scaleWidth > 0 && scaleHeight > 0) {

                    // 使用最原始的图片进行大小计算
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap
                            .createScaledBitmap(bitMap, scaleWidth,
                                    scaleHeight, true)));
                    marker.setVisible(true);

                    // 因为替换了新的图片，所以把旧的图片销毁掉，注意在设置新的图片之后再销毁
                    if (lastMarkerBitMap != null
                            && !lastMarkerBitMap.isRecycled()) {
                        lastMarkerBitMap.recycle();
                    }

                    //第一次得到的缩放图片，在第二次回收，最后一次的缩放图片，在动画结束时回收
                    ArrayList<BitmapDescriptor> list = marker.getIcons();
                    if (list != null && list.size() > 0) {
                        // 保存旧的图片
                        lastMarkerBitMap = marker.getIcons().get(0).getBitmap();
                    }
                }

                if (t < 1.0 && count < 10) {
                    handler.postDelayed(this, 16);
                } else {
                    // 动画结束回收缩放图片，并还原最原始的图片
                    if (lastMarkerBitMap != null
                            && !lastMarkerBitMap.isRecycled()) {
                        lastMarkerBitMap.recycle();
                    }
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitMap));
                    marker.setVisible(true);
                }
            }
        });
    }

    /**
     * 监听点击infowindow窗口事件回调
     */
    @Override
    public void onInfoWindowClick(Marker marker) {
        ToastUtil.show(this, "你点击了infoWindow窗口" + marker.getTitle());
        ToastUtil.show(this, "当前地图可视区域内Marker数量:"
                + aMap.getMapScreenMarkers().size());
    }

    /**
     * 监听自定义infowindow窗口的infocontents事件回调
     */
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 监听自定义infowindow窗口的infowindow事件回调
     */
    @Override
    public View getInfoWindow(Marker marker) {
        View infoWindow = getLayoutInflater().inflate(
                R.layout.map_info_window, null);
        TextView titleUi = ((TextView) infoWindow.findViewById(R.id.title));
        titleUi.setText("确认位置");
        return infoWindow;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /**
             * 清空地图上所有已经标注的marker
             */
            case R.id.clearMap:
                if (aMap != null) {
                    aMap.clear();
                }
                break;
            /**
             * 重新标注所有的marker
             */
            case R.id.resetMap:
                if (aMap != null) {
                    aMap.clear();
                   // addMarkersToMap();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 初始化定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void initLocation() {
        //初始化client
        locationClient = new AMapLocationClient(this.getApplicationContext());
        //设置定位参数
        locationClient.setLocationOption(getDefaultOption());
        // 设置定位监听
        locationClient.setLocationListener(locationListener);
    }

    /**
     * 开始定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void startLocation() {
        //根据控件的选择，重新设置定位参数
        //resetOption();
        // 设置定位参数
        //locationClient.setLocationOption(locationOption);
        // 启动定位
        locationClient.startLocation();
    }

    /**
     * 默认的定位参数
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        return mOption;
    }

    //自定义一个图钉，并且设置图标，当我们点击图钉时，显示设置的信息
    private MarkerOptions getMarkerOptions(AMapLocation amapLocation) {
        //设置图钉选项
        MarkerOptions options = new MarkerOptions();
        //图标
        //  options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding));
        //位置
        options.position(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude()));

        StringBuffer buffer = new StringBuffer();
        buffer.append(amapLocation.getCountry() + "" + amapLocation.getProvince() + "" + amapLocation.getCity() + "" + amapLocation.getDistrict() + "" + amapLocation.getStreet() + "" + amapLocation.getStreetNum());
        //标题
        options.title(buffer.toString());
        //子标题
        options.snippet("这里好火");
        //设置多少帧刷新一次图片资源
        options.period(60);

        //http://developer.amap.com/api/android-sdk/guide/create-map/mylocation#location-marker-5-0
        // 定位蓝点的图标锚点自定义：
        // 锚点是指定位蓝点图标像素与定位蓝点坐标的关联点，
        // 例如需要将图标的左下方像素点与定位蓝点的经纬度关联在一起，通过如下方法传入（0.0,1.0）。图标左上点为像素原点。
        // MyLocationStyle anchor(float u, float v);//设置定位蓝点图标的锚点方法。
        options.anchor(0.5f, 1);
        LatLng point = new LatLng(2, 2);
        options.position(point);

        return options;
    }
}
