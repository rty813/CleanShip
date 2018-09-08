package com.cn.orcatech.cleanship.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cn.orcatech.cleanship.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.StringRequest;
import com.yanzhenjie.nohttp.rest.SyncRequestExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataFragment extends NoFragment {
    private TextView tvData;
    public static final int CHART_BATTERY = 0;
    public static final int CHART_YAW = 1;
    public static final int CHART_TEMP = 2;
    private ArrayList<LineChart> charts;
    private ArrayList<LineData> lineDatas;
    private ArrayList<ArrayList<String>> times;
    private int chartCount[];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvData = view.findViewById(R.id.tv_data);
        charts = new ArrayList<>();
        charts.add((LineChart) view.findViewById(R.id.chart_battery));
        charts.add((LineChart) view.findViewById(R.id.chart_yaw));
        charts.add((LineChart) view.findViewById(R.id.chart_temperature));

        lineDatas = new ArrayList<>();
        lineDatas.add(new LineData(new LineDataSet(new ArrayList<Entry>(), "电量")));
        lineDatas.add(new LineData(new LineDataSet(new ArrayList<Entry>(), "航向角")));
        lineDatas.add(new LineData(new LineDataSet(new ArrayList<Entry>(), "温度")));

        chartCount = new int[charts.size()];

        times = new ArrayList<>();
        for (int i = 0; i < charts.size(); i++) {
            chartCount[i] = 0;
            times.add(new ArrayList<String>());
            XAxis axis = charts.get(i).getXAxis();
            axis.setPosition(XAxis.XAxisPosition.BOTTOM);
            axis.setValueFormatter(new MyXFormatter(times.get(i)));
            axis.setLabelCount(8);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                StringRequest request = new StringRequest("http://orca-tech.cn/app/history_select.php", RequestMethod.POST);
                request.add("ship_id", 5).add("id", 1).add("limit", "5000");
                Response<String> response = SyncRequestExecutor.INSTANCE.execute(request);
                if (response.isSucceed()) {
                    try {
                        JSONArray array = new JSONArray(response.get());
                        for (int i = array.length() - 1; i >= 0; i--) {
                            System.out.println(i);
                            final JSONObject jsonObject = array.getJSONObject(i);

                            final String time = jsonObject.getString("time");
                            final int pdPercent = jsonObject.getInt("pd_percent");
                            final float yaw = (float) jsonObject.getDouble("yaw");
                            int temp1 = jsonObject.getInt("temperature");
                            if (temp1 < 10 || temp1 > 80) {
                                temp1 = (int) lineDatas.get(CHART_TEMP).getDataSetByIndex(0)
                                        .getEntryForIndex(lineDatas.get(CHART_TEMP).getDataSetByIndex(0).getEntryCount() - 1)
                                        .getY();
                            }
                            final int temp = temp1;

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addEntry(0, pdPercent, time);
                                    addEntry(1, yaw, time);
                                    addEntry(2, temp, time);
                                }
                            });
                            Thread.sleep(100);
                        }
                    } catch (JSONException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void addEntry(int index, float data, String time) {
        if (chartCount[index] == 0) {
            charts.get(index).setData(lineDatas.get(index));
        }
        this.times.get(index).add(time.split(" ")[1].substring(0, 5));
        lineDatas.get(index).addEntry(new Entry(chartCount[index]++, data), 0);
        if (lineDatas.get(index).getEntryCount() > 500) {
            Entry entry = lineDatas.get(index).getDataSetByIndex(0).getEntryForIndex(0);
            lineDatas.get(index).removeEntry(entry, 0);
        }
        charts.get(index).notifyDataSetChanged();
        charts.get(index).invalidate();
    }

    public void clearChart(int index) {
        times.get(index).clear();
        charts.get(index).clear();
        lineDatas.set(index, new LineData(new LineDataSet(new ArrayList<Entry>(), "电量")));
        charts.get(index).setData(lineDatas.get(index));
        charts.get(index).invalidate();
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



    public void setData(String data) {
        tvData.setText(data);
    }
}
