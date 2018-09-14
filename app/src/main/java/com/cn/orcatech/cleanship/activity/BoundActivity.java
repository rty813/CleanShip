package com.cn.orcatech.cleanship.activity;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.cn.orcatech.cleanship.R;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import lib.kingja.switchbutton.SwitchMultiButton;

public class BoundActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnEnable;
    private Button btnCancel;
    private Button btnDelete;
    private Button btnSet;
    private SwitchMultiButton swFlag;
    private ProgressBar progressBar;
    private TextView tvHint;
    private Drawable picMarkEnable;
    private Drawable picMarkDisable;
    private MapView mMapView;
    private AMap aMap;
    private int shipId;
    private boolean markEnable = false;
    private boolean hasClosed = false;

    private ArrayList<Marker> markers;
    private ArrayList<Polyline> polylines;
    private ArrayList<Polygon> polygons;
    private String TAG = "BoundActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bound);
        shipId = getIntent().getIntExtra("ship_id", 5);
        initView();
        mMapView.onCreate(savedInstanceState);
        markers = new ArrayList<>();
        polylines = new ArrayList<>();
        polygons = new ArrayList<>();
        initMap();
        loadBound();
        setResult(0);
        moveCamera();
    }

    private void moveCamera() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
                .strokeColor(Color.parseColor("#00000000"))
                .radiusFillColor(Color.parseColor("#00000000"));
        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);
        // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.setMyLocationEnabled(true);
        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(15);
        aMap.moveCamera(cameraUpdate);
    }

    private void initView() {
        mMapView = findViewById(R.id.mapView);
        btnEnable = findViewById(R.id.btn_enable);
        btnCancel = findViewById(R.id.btn_cancel);
        btnDelete = findViewById(R.id.btn_delete);
        swFlag = findViewById(R.id.sw_flag);
        btnSet = findViewById(R.id.btn_set);
        progressBar = findViewById(R.id.progressBar);
        tvHint = findViewById(R.id.tv_hint);

        btnEnable.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnSet.setOnClickListener(this);

        picMarkEnable = getResources().getDrawable(R.drawable.mark_y);
        picMarkDisable = getResources().getDrawable(R.drawable.mark_n);
        picMarkEnable.setBounds(0, 0, picMarkEnable.getMinimumWidth(), picMarkEnable.getMinimumHeight());
        picMarkDisable.setBounds(0, 0, picMarkDisable.getMinimumWidth(), picMarkDisable.getMinimumHeight());

        findViewById(R.id.ll_bound).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void initMap() {
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        aMap.getUiSettings().setCompassEnabled(false);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (!markEnable || hasClosed) {
                    return;
                }
                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
                markerOptions.title(String.valueOf(markers.size() + 1));
                markerOptions.snippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                markerOptions.anchor(0.5f, 0.5f);
                markerOptions.setFlat(true);
                markerOptions.draggable(true);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                        markers.size() == 0 ? R.drawable.mao_start : R.drawable.mao)));
                if (markers.size() > 0) {
                    LatLng lastLatlng = markers.get(markers.size() - 1).getPosition();
                    polylines.add(aMap.addPolyline(new PolylineOptions().add(latLng, lastLatlng).width(14)
                            .color(Color.parseColor("#0B76CE"))));
                }
                markers.add(aMap.addMarker(markerOptions));
            }
        });

        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                if (markEnable && !hasClosed) {
                    Marker lastMarker = markers.get(markers.size() - 1);
                    if (markers.size() > 2 && marker.getId().equals(markers.get(0).getId())) {
                        marker.hideInfoWindow();
                        polylines.add(aMap.addPolyline(new PolylineOptions()
                                .add(marker.getPosition(), lastMarker.getPosition()).width(14)
                                .color(Color.parseColor("#0B76CE"))));
                        Toasty.success(BoundActivity.this, "已闭合！请点设定", Toast.LENGTH_SHORT).show();
                        hasClosed = true;
                        btnEnable.performClick();
                    }
                }
                return true;
            }
        });

        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            private int index;

            @Override
            public void onMarkerDragStart(Marker marker) {
                for (index = 0; index < markers.size(); index++) {
                    if (markers.get(index).getId().equals(marker.getId())) {
                        break;
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                LatLng latLng = marker.getPosition();
                marker.setSnippet(String.format(Locale.getDefault(), "纬度：%.6f\n经度：%.6f", latLng.latitude, latLng.longitude));
                if (markers.size() > 1) {
                    PolylineOptions options = new PolylineOptions().width(14).color(Color.parseColor("#0B76CE"));
                    if (index == 0) {
                        options.add(latLng, markers.get(1).getPosition());
                        polylines.get(0).setOptions(options);
                        options = new PolylineOptions().width(14).color(Color.parseColor("#0B76CE"))
                                .add(latLng, markers.get(markers.size() - 1).getPosition());
                        polylines.get(polylines.size() - 1).setOptions(options);
                    } else if (index == markers.size() - 1) {
                        options.add(markers.get(markers.size() - 2).getPosition(), latLng);
                        polylines.get(polylines.size() - 2).setOptions(options);
                        options = new PolylineOptions().width(14).color(Color.parseColor("#0B76CE"))
                                .add(latLng, markers.get(0).getPosition());
                        polylines.get(polylines.size() - 1).setOptions(options);
                    } else {
                        options.add(markers.get(index - 1).getPosition(), latLng);
                        polylines.get(index - 1).setOptions(options);
                        options = new PolylineOptions().width(10).color(Color.parseColor("#0B76CE"))
                                .add(latLng, markers.get(index + 1).getPosition());
                        polylines.get(index).setOptions(options);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

            }
        });
    }

    private void loadBound() {
        StringRequest request = new StringRequest("http://orca-tech.cn/app/bound.php", RequestMethod.POST);
        request.add("ship_id", shipId).add("type", "select");
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            @Override
            public void onStart(int what) {
                super.onStart(what);
                progressBar.setVisibility(View.VISIBLE);
                tvHint.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSucceed(int what, Response<String> response) {
                super.onSucceed(what, response);
                try {
                    JSONArray array = new JSONArray(response.get());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String latlngs[] = obj.getString("latlng").split(";");
                        PolygonOptions options = initPolygonOptions(obj.getInt("flag"));
                        for (String latlng : latlngs) {
                            double lat = Double.parseDouble(latlng.split(",")[0]);
                            double lng = Double.parseDouble(latlng.split(",")[1]);
                            options.add(new LatLng(lat, lng));
                        }
                        polygons.add(aMap.addPolygon(options));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(int what, Response<String> response) {
                Toasty.warning(BoundActivity.this, "拉取失败", Toast.LENGTH_SHORT).show();
                super.onFailed(what, response);
            }

            @Override
            public void onFinish(int what) {
                super.onFinish(what);
                progressBar.setVisibility(View.INVISIBLE);
                tvHint.setVisibility(View.INVISIBLE);
            }
        });
    }

    private PolygonOptions initPolygonOptions(int flag) {
        PolygonOptions options = new PolygonOptions();
        if (flag == 0) {
            options.strokeWidth(15).strokeColor(Color.argb(150, 1, 1, 1))
                    .fillColor(Color.argb(30, 0, 0, 0));
        } else {
            options.strokeWidth(15).strokeColor(Color.argb(255, 255, 0, 0))
                    .fillColor(Color.argb(200, 255, 60, 60));
        }
        return options;
    }

    private void viewCtl(boolean enabled) {
        btnSet.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
        btnDelete.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_enable:
                markEnable = !markEnable;
                btnEnable.setCompoundDrawables(null, markEnable ? picMarkEnable : picMarkDisable, null, null);
                break;
            case R.id.btn_cancel:
                if (markers.size() > 0) {
                    if (!hasClosed) {
                        Marker marker = markers.get(markers.size() - 1);
                        marker.hideInfoWindow();
                        marker.destroy();
                        markers.remove(markers.size() - 1);
                    }
                    if (polylines.size() > 0) {
                        polylines.get(polylines.size() - 1).remove();
                        polylines.remove(polylines.size() - 1);
                    }
                    hasClosed = false;
                }
                break;
            case R.id.btn_delete:
                break;
            case R.id.btn_set:
                if (markers.size() < 3) {
                    Toasty.error(this, "必须大于三个点", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!hasClosed) {
                    Toasty.error(this, "路径必须闭合", Toast.LENGTH_SHORT).show();
                    return;
                }

                StringBuilder latlng = new StringBuilder();
                final PolygonOptions options = initPolygonOptions(swFlag.getSelectedTab());
                for (Marker marker : markers) {
                    LatLng position = marker.getPosition();
                    options.add(position);
                    latlng.append(String.format(Locale.getDefault(), "%.6f,%.6f;", position.latitude, position.longitude));
                }

                StringRequest request = new StringRequest("http://orca-tech.cn/app/bound.php", RequestMethod.POST);
                request.add("ship_id", shipId).add("flag", swFlag.getSelectedTab())
                        .add("type", "insert").add("latlng", latlng.toString());
                AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
                    @Override
                    public void onStart(int what) {
                        super.onStart(what);
                        progressBar.setVisibility(View.VISIBLE);
                        tvHint.setVisibility(View.VISIBLE);
                        tvHint.setText("正在设定");
                        viewCtl(false);
                    }

                    @Override
                    public void onSucceed(int what, Response<String> response) {
                        super.onSucceed(what, response);
                        if (response.get().contains("success")) {
                            Toasty.success(BoundActivity.this, "设定成功", Toast.LENGTH_SHORT).show();
                            polygons.add(aMap.addPolygon(options));
                            for (Marker marker : markers) {
                                marker.hideInfoWindow();
                                marker.destroy();
                            }
                            markers.clear();
                            for (Polyline polyline : polylines) {
                                polyline.remove();
                            }
                            polylines.clear();
                            hasClosed = false;
                        } else {
                            Toasty.error(BoundActivity.this, "设定失败", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onSucceed: " + response.get());
                        }
                    }

                    @Override
                    public void onFailed(int what, Response<String> response) {
                        super.onFailed(what, response);
                        Toasty.error(BoundActivity.this, "设定失败", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onFailed: " + response.get());
                    }

                    @Override
                    public void onFinish(int what) {
                        super.onFinish(what);
                        progressBar.setVisibility(View.INVISIBLE);
                        tvHint.setVisibility(View.INVISIBLE);
                        viewCtl(true);
                    }
                });
                setResult(1);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
}
