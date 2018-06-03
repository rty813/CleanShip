package xyz.rty813.wear;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wear.widget.drawer.WearableDrawerLayout;
import android.support.wear.widget.drawer.WearableDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.WearMapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.security.Permissions;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends WearableActivity {

    private WearMapView mapView;
    private long mLastNotifyTime;
    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;
    private AMap aMap;
    private WearableDrawerView drawerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                0);

        findViewById(R.id.btnSpeak).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displaySpeechRecognizer();
//                startActivity(new Intent(MainActivity.this, TestActivity.class));
            }
        });
        drawerView = findViewById(R.id.drawer_view);
        WearableDrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerStateCallback(new WearableDrawerLayout.DrawerStateCallback(){
            @Override
            public void onDrawerStateChanged(WearableDrawerLayout layout, int newState) {
                if (newState == 0 && drawerView.isPeeking()) {
                    drawerView.getController().closeDrawer();
                }
            }
        });
        mapView = findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        aMap.getUiSettings().setCompassEnabled(false);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        setAmbientEnabled();
        mapView.setOnDismissCallbackListener(new WearMapView.OnDismissCallback() {
            @Override
            public void onDismiss() {
                mapView.onDismiss();
                MainActivity.this.finish();
            }
            @Override
            public void onNotifySwipe() {
                if (System.currentTimeMillis() - mLastNotifyTime >= 300) {
                    //当触发滑动时,设置振动交互。开发者可在该回调中自定义滑动时的交互事件
                    Vibrator vib = (Vibrator)MainActivity.this.getSystemService(Service.VIBRATOR_SERVICE);
                    vib.vibrate(50);
                }
                else {
                    mLastNotifyTime = System.currentTimeMillis();
                }
            }
        });

        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
                .strokeColor(Color.parseColor("#00000000"))
                .radiusFillColor(Color.parseColor("#00000000"));
        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);
        // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.setMyLocationEnabled(true);
        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(14);
        aMap.moveCamera(cameraUpdate);

        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
//                if (!markEnable) {
//                    return;
//                }
//                if (AMapUtils.calculateLineDistance(latLng, limitCircle.getCenter()) > CTL_RADIUS) {
//                    Toasty.warning(NewActivity.this, "超出遥控范围", Toast.LENGTH_SHORT).show();
//                }
                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
                markerOptions.title(String.valueOf(markers.size() + 1));
                markerOptions.snippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                markerOptions.anchor(0.5f, 0.5f);
                markerOptions.setFlat(true);
                markerOptions.draggable(true);

                System.out.println(latLng.toString());
                if (markers.size() > 0) {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao)));
                    LatLng latLng1 = latLng;
                    LatLng latLng2 = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng1, latLng2).width(14)
                            .color(Color.parseColor("#0B76CE"))));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mao_start)));
//                    GeocodeSearch geocodeSearch = new GeocodeSearch(NewActivity.this);
//                    geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
//                        @Override
//                        public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
//                            if (i == 1000) {
//                                RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
//                                pos = address.getProvince();
//                                if (!address.getProvince().equals(address.getCity())) {
//                                    pos += " " + address.getCity();
//                                }
//                                pos += " " + address.getDistrict();
//                                if (address.getPois().size() != 0) {
//                                    pos = pos + " " + address.getPois().get(0);
//                                }
////                                System.out.println(pos);
//                            }
//                        }
//
//                        @Override
//                        public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
//
//                        }
//                    });
//                    RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 1000, GeocodeSearch.AMAP);
//                    geocodeSearch.getFromLocationAsyn(query);
                }
                markers.add(aMap.addMarker(markerOptions));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行map.onDestroy()，销毁地图
        mapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行map.onResume ()，重新绘制加载地图
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行map.onPause ()，暂停地图的绘制
        mapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行map.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            if (drawerView.isOpened()) {
                drawerView.getController().closeDrawer();
            }
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            if (spokenText.equals("清空")) {
                aMap.clear(false);
                markers.removeAll(markers);
                polylines.removeAll(polylines);
            } else {
                Toast.makeText(this, spokenText, Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
