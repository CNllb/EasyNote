package com.fwwb.easynote.Activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.baidu.location.*;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.*;
import com.baidu.mapapi.search.poi.*;
import com.fwwb.easynote.Adapters.PoiListAdapter;
import com.fwwb.easynote.R;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity{
    @BindView(R.id.view_map)
    MapView mapView;
    public static EditText locationEditText;
    @BindView(R.id.button_ok_location)
    Button okLocationButtton;
    @BindView(R.id.poi_listview)
    ListView poiListView;
    PoiListAdapter poiListAdapter;
    List<String> poiInfoList=new ArrayList<>();
    BaiduMap map;
    LocationClient locationClient;
    GeoCoder coder;
    PoiSearch poiSearch;

    String address=null;

    boolean firstLocate=false;


    public class MyLocationListener extends BDAbstractLocationListener{
        @Override
        public void onReceiveLocation(BDLocation location){
            //mapView 销毁后不在处理新接收的位置
            if(location==null||mapView==null){
                return;
            }
            MyLocationData locData=new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            map.setMyLocationData(locData);

            if(!firstLocate){
                firstLocate=true;
                setPosition2Center(map,location,true);
                if(location.getPoiList()!=null){
                    List<Poi> poiNames=location.getPoiList();
                    for(Poi i: poiNames){
                        poiInfoList.add(i.getName());
                    }
                }
                poiListAdapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        map=mapView.getMap();
        map.setMyLocationEnabled(true);

        //检查定位权限
        checkLocationPermission();

        //初始化地图
        initMap();

        locationEditText=findViewById(R.id.edittext_location);
        okLocationButtton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent=new Intent();
                //把返回数据存入Intent
                intent.putExtra("address",locationEditText.getText().toString().trim());
                //设置返回数据
                setResult(RESULT_OK,intent);
                //关闭Activity
                finish();
            }
        });
        poiListAdapter=new PoiListAdapter(poiInfoList,MapActivity.this);
        poiListView.setAdapter(poiListAdapter);


    }

    private void initMap(){
        //定位初始化
        locationClient=new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option=new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        locationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener=new MyLocationListener();
        locationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        locationClient.start();

        //反地理编码
        coder=GeoCoder.newInstance();
        OnGetGeoCoderResultListener listener=new OnGetGeoCoderResultListener(){
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult){

            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult){
                if(reverseGeoCodeResult==null||reverseGeoCodeResult.error!=SearchResult.ERRORNO.NO_ERROR){
                    //没有找到检索结果
                    locationEditText.setText("暂无定位");
                }else{
                    //详细地址
                    address=reverseGeoCodeResult.getAddress();
                    List<PoiInfo> poiList=reverseGeoCodeResult.getPoiList();
                    poiInfoList.clear();
                    for(PoiInfo i: poiList){
                        poiInfoList.add(i.name);
                    }
                    poiListAdapter.notifyDataSetChanged();
//                    poiListView.setVisibility(View.VISIBLE);
                    if(poiList.size()!=0){
                        locationEditText.setText(poiInfoList.get(0));
                    }else{
                        locationEditText.setText(address);
                    }
                    //行政区号
                    int adCode=reverseGeoCodeResult.getCityCode();
                }
            }
        };
        coder.setOnGetGeoCodeResultListener(listener);

        //地图状态监听
        BaiduMap.OnMapStatusChangeListener mapStatusChangeListener=new BaiduMap.OnMapStatusChangeListener(){
            /**
             * 手势操作地图，设置地图状态等操作导致地图状态开始改变。
             *
             * @param status 地图状态改变开始时的地图状态
             */
            @Override
            public void onMapStatusChangeStart(MapStatus status){

            }

            /**
             * 手势操作地图，设置地图状态等操作导致地图状态开始改变。
             *
             * @param status 地图状态改变开始时的地图状态
             *
             * @param reason 地图状态改变的原因
             */

            //用户手势触发导致的地图状态改变,比如双击、拖拽、滑动底图
            //int REASON_GESTURE = 1;
            //SDK导致的地图状态改变, 比如点击缩放控件、指南针图标
            //int REASON_API_ANIMATION = 2;
            //开发者调用,导致的地图状态改变
            //int REASON_DEVELOPER_ANIMATION = 3;
            @Override
            public void onMapStatusChangeStart(MapStatus status,int reason){

            }

            /**
             * 地图状态变化中
             *
             * @param status 当前地图状态
             */
            @Override
            public void onMapStatusChange(MapStatus status){
                poiListView.setVisibility(View.GONE);
            }

            /**
             * 地图状态改变结束
             *
             * @param status 地图状态改变结束后的地图状态
             */
            @Override
            public void onMapStatusChangeFinish(MapStatus status){

                coder.reverseGeoCode(new ReverseGeoCodeOption()
                        .location(status.target)
                        // POI召回半径，允许设置区间为0-1000米，超过1000米按1000米召回。默认值为1000
                        .radius(500));
                new Handler().postDelayed(new Runnable(){
                    public void run(){
                        poiListView.setVisibility(View.VISIBLE);
                    }
                },1000);
            }
        };
        //设置地图状态监听
        map.setOnMapStatusChangeListener(mapStatusChangeListener);

        //搜索点周边poi
        poiSearch=PoiSearch.newInstance();
        poiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener(){
            @Override
            public void onGetPoiResult(PoiResult poiResult){

            }

            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult){

            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult){

            }

            //废弃
            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult){

            }
        });

    }

    //把定位放到地图中间
    public void setPosition2Center(BaiduMap map,BDLocation bdLocation,Boolean isShowLoc){
        MyLocationData locData=new MyLocationData.Builder()
                .accuracy(bdLocation.getRadius())
                .direction(bdLocation.getRadius()).latitude(bdLocation.getLatitude())
                .longitude(bdLocation.getLongitude()).build();
        map.setMyLocationData(locData);

        if(isShowLoc){
            LatLng ll=new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
            MapStatus.Builder builder=new MapStatus.Builder();
            builder.target(ll).zoom(18.0f);
            map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    }


    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy(){
        locationClient.stop();
        map.setMyLocationEnabled(false);
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mapView.onDestroy();
        mapView=null;
        coder.destroy();
        super.onDestroy();
    }


    private void checkLocationPermission(){

        if(ContextCompat.checkSelfPermission(MapActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)
                !=PackageManager.PERMISSION_GRANTED){//未开启定位权限
            //开启定位权限,200是标识码
            ActivityCompat.requestPermissions(MapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},200);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch(requestCode){
            case 200://刚才的识别码
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){//用户同意权限,执行我们的操作

                }else{//用户拒绝之后,当然我们也可以弹出一个窗口,直接跳转到系统设置页面
                    Toast.makeText(MapActivity.this,"未开启定位权限,请手动到设置去开启权限",Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

}
