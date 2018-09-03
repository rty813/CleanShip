package com.cn.orcatech.cleanship.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;

public class DataFragment extends NoFragment implements View.OnClickListener {

    private TXLivePlayer mLivePlayer;
    private Button btnPause;
    private TXCloudVideoView mView;
    private TextView tvData;
    private int thrust = 0;
    private int dire = 0;
    private EditText etP;
    private EditText etI;
    private EditText etD;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view.findViewById(R.id.video_view);
        tvData = view.findViewById(R.id.tv_data);
        mLivePlayer = new TXLivePlayer(getActivity());
        mLivePlayer.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
        mLivePlayer.setRenderRotation(TXLiveConstants.RENDER_ROTATION_LANDSCAPE);
        mLivePlayer.setPlayerView(mView);
        btnPause = view.findViewById(R.id.btn_videopause);
        etP = view.findViewById(R.id.et_p);
        etI = view.findViewById(R.id.et_i);
        etD = view.findViewById(R.id.et_d);
        view.findViewById(R.id.btn_videoplay).setOnClickListener(this);
        view.findViewById(R.id.btn_videopause).setOnClickListener(this);
        view.findViewById(R.id.btn_videostop).setOnClickListener(this);
        view.findViewById(R.id.btn_setPID).setOnClickListener(this);
        ((SeekBar)view.findViewById(R.id.sb_thrust)).setProgress(10);
        ((SeekBar)view.findViewById(R.id.sb_thrust)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                thrust = (seekBar.getProgress() - 10) * 10;
                MainActivity activity = (MainActivity) getActivity();
                try {
                    activity.getMapFragment().mqttClient.publish("APP2SHIP/5/1", ("$4GCTL," + thrust + "," + dire + "#").getBytes(), 0, false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
        ((SeekBar)view.findViewById(R.id.sb_dire)).setProgress(10);
        ((SeekBar)view.findViewById(R.id.sb_dire)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dire = (i - 10) * 10;
                MainActivity activity = (MainActivity) getActivity();
                try {
                    activity.getMapFragment().mqttClient.publish("APP2SHIP/5/1", ("$4GCTL," + thrust + "," + dire + "#").getBytes(), 0, false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(10);
                dire = 0;
                MainActivity activity = (MainActivity) getActivity();
                try {
                    activity.getMapFragment().mqttClient.publish("APP2SHIP/5/1", ("$4GCTL," + thrust + "," + dire + "#").getBytes(), 0, false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
        final LineChart chart = view.findViewById(R.id.chart);

        StringRequest request = new StringRequest("http://orca-tech.cn/app/history_select.php", RequestMethod.POST);
        request.add("ship_id", 5).add("id", 1).add("start_time", "2018-08-29 00:00:00");
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            @Override
            public void onSucceed(int what, Response<String> response) {
                super.onSucceed(what, response);
                try {
                    List<String> time = new ArrayList<>();
                    List<Entry> entries = new ArrayList<>();
                    JSONArray array = new JSONArray(response.get());
                    for (int i = array.length() - 1; i >= 0; i--) {
                        JSONObject objHistory = array.getJSONObject(i);
                        time.add(objHistory.getString("time").split(" ")[1].substring(0, 5));
                        entries.add(new Entry(array.length() - i - 1, objHistory.getInt("pd_percent")));
                    }
                    LineDataSet dataSet = new LineDataSet(entries, "电量");
                    LineData lineData = new LineData(dataSet);
                    chart.setData(lineData);
                    chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    XAxis axis = chart.getXAxis();
                    axis.setValueFormatter(new MyXFormatter(time));
                    axis.setLabelCount(8);
                    chart.invalidate();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public class MyXFormatter implements IAxisValueFormatter {
        List<String> mValues;

        public MyXFormatter(List<String> values) {
            mValues = values;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return mValues.get((int)(value));
        }
    }

    @Override
    public void onClick(View view) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity.selectShip == -1) {
            Toasty.error(activity, "请先选船", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = "rtmp://25779.liveplay.myqcloud.com/live/25779_" + activity.userInfo.getShip_id() + "_" + activity.selectShip;
        switch (view.getId()) {
            case R.id.btn_videoplay:
                mView.setVisibility(View.VISIBLE);
                try {
                    activity.getMapFragment().publishMessage("$video;play#");
                    Thread.sleep(2000);
                    mLivePlayer.startPlay(url, TXLivePlayer.PLAY_TYPE_LIVE_RTMP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_videopause:
                if (btnPause.getText().equals("暂停")) {
                    mLivePlayer.pause();
                    btnPause.setText("继续");
                }
                else {
                    mLivePlayer.resume();
                    btnPause.setText("暂停");
                }
                break;
            case R.id.btn_videostop:
                mView.setVisibility(View.GONE);
                activity.getMapFragment().publishMessage("$video;stop#");
                mLivePlayer.stopPlay(false);
                break;
            case R.id.btn_setPID:
                activity.getMapFragment().publishMessageForResult(String.format(Locale.getDefault(),
                        "$ORDER,9,%f,%f,%f#",
                        Float.parseFloat(etP.getText().toString()),
                        Float.parseFloat(etI.getText().toString()),
                        Float.parseFloat(etD.getText().toString())));
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        mLivePlayer.stopPlay(true);
        mView.onDestroy();
        super.onDestroy();
    }

    public void setData(String data) {
        tvData.setText(data);
    }
}
