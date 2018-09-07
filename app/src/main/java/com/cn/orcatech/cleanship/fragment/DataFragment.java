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
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataFragment extends NoFragment {
    private TextView tvData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvData = view.findViewById(R.id.tv_data);
        final LineChart chart = view.findViewById(R.id.chart);

        StringRequest request = new StringRequest("http://orca-tech.cn/app/history_select.php", RequestMethod.POST);
        request.add("ship_id", 5).add("id", 1).add("limit", "5000");
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



    public void setData(String data) {
        tvData.setText(data);
    }
}
