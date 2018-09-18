package com.cn.orcatech.cleanship.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.yanzhenjie.fragment.NoFragment;

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
    private MainActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

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
        LineDataSet lineDataSet = new LineDataSet(new ArrayList<Entry>(), "电量");
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawCircleHole(false);
        lineDatas.add(new LineData(lineDataSet));

        lineDataSet = new LineDataSet(new ArrayList<Entry>(), "航向角");
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawCircleHole(false);
        lineDatas.add(new LineData(lineDataSet));

        LineDataSet lineDataSet1 = new LineDataSet(new ArrayList<Entry>(), "电子仓温度");
        lineDataSet1.setColor(Color.GREEN);
        lineDataSet1.setCircleColor(Color.GREEN);
        lineDataSet1.setDrawCircleHole(false);

        LineDataSet lineDataSet2 = new LineDataSet(new ArrayList<Entry>(), "电池温度");
        lineDataSet2.setColor(Color.BLUE);
        lineDataSet2.setCircleColor(Color.BLUE);
        lineDataSet2.setDrawCircleHole(false);

        LineDataSet lineDataSet3 = new LineDataSet(new ArrayList<Entry>(), "驱动盒温度");
        lineDataSet3.setColor(Color.RED);
        lineDataSet3.setCircleColor(Color.RED);
        lineDataSet3.setDrawCircleHole(false);
        lineDatas.add(new LineData(lineDataSet1, lineDataSet2, lineDataSet3));

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

    public void addEntry(int index, short temp1, short temp2, short temp3, String time) {
        if (index != CHART_TEMP) {
            return;
        }
        if (chartCount[index] == 0) {
            charts.get(index).setData(lineDatas.get(index));
        }
        this.times.get(index).add(time.split(" ")[1].substring(0, 5));
        lineDatas.get(index).addEntry(new Entry(chartCount[index], temp1), 0);
        lineDatas.get(index).addEntry(new Entry(chartCount[index], temp2), 1);
        lineDatas.get(index).addEntry(new Entry(chartCount[index]++, temp3), 2);
        if (lineDatas.get(index).getEntryCount() > 500) {
            Entry entry = lineDatas.get(index).getDataSetByIndex(0).getEntryForIndex(0);
            lineDatas.get(index).removeEntry(entry, 0);
            entry = lineDatas.get(index).getDataSetByIndex(1).getEntryForIndex(0);
            lineDatas.get(index).removeEntry(entry, 1);
            entry = lineDatas.get(index).getDataSetByIndex(2).getEntryForIndex(0);
            lineDatas.get(index).removeEntry(entry, 2);
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
