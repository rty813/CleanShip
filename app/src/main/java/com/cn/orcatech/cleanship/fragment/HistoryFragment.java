package com.cn.orcatech.cleanship.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.cn.orcatech.cleanship.activity.TaskDetailActivity;
import com.cn.orcatech.cleanship.adapter.TaskHistoryAdapter;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;
import com.yanzhenjie.recyclerview.swipe.SwipeItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;


public class HistoryFragment extends NoFragment implements SwipeItemClickListener {

    private ArrayList<Map<String, String>> list;
    private TaskHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SwipeMenuRecyclerView recyclerView = view.findViewById(R.id.srv_task);
        list = new ArrayList<>();
        adapter = new TaskHistoryAdapter(list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setSwipeItemClickListener(this);
        recyclerView.setAdapter(adapter);
        super.onViewCreated(view, savedInstanceState);
    }

    public void update(final MainActivity activity, int id) {
        list.clear();
        StringRequest request = new StringRequest("http://orca-tech.cn/app/taskhistory.php", RequestMethod.POST);
        request.add("ship_id", activity.userInfo.getShip_id()).add("id", id).add("type", "select");
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            @Override
            public void onSucceed(int what, Response<String> response) {
                super.onSucceed(what, response);
                try {
                    JSONArray array = new JSONArray(response.get());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject objTask = array.getJSONObject(i);
                        Map<String, String> map = new HashMap<>();
                        map.put("date", objTask.getString("start_time"));
                        map.put("length", objTask.getString("length"));
                        map.put("timeCost", objTask.getString("time_cost"));
                        map.put("ise", objTask.getString("ise"));
                        map.put("variance", objTask.getString("variance"));
                        map.put("start_time", objTask.getString("start_time"));
                        map.put("end_time", objTask.getString("end_time"));
                        map.put("route_id", objTask.getString("route_id"));
                        list.add(map);
                    }
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(int what, Response<String> response) {
                Toasty.error(activity, "拉取历史任务失败！", Toast.LENGTH_SHORT).show();
                super.onFailed(what, response);
            }
        });
    }

    @Override
    public void onItemClick(View itemView, int position) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra("task", (Serializable) list.get(position));
        intent.putExtra("id", ((MainActivity) getActivity()).selectShip);
        intent.putExtra("ship_id", ((MainActivity) getActivity()).userInfo.getShip_id());
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
