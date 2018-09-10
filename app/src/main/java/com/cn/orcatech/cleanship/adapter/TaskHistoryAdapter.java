package com.cn.orcatech.cleanship.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cn.orcatech.cleanship.R;

import java.util.ArrayList;
import java.util.Map;

public class TaskHistoryAdapter extends RecyclerView.Adapter {

    private ArrayList<Map<String, String>> list;

    public TaskHistoryAdapter(ArrayList<Map<String, String>> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_taskhistory, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        viewHolder.getTvDate().setText(list.get(position).get("date"));
        viewHolder.getTvLength().setText("距离：" + list.get(position).get("length") + "米");
        viewHolder.getTvTimeCost().setText("耗时：" + list.get(position).get("timeCost") + "秒");
        viewHolder.getTvIse().setText("ISE：" + list.get(position).get("ise"));
        viewHolder.getTvVariance().setText("方差：" + list.get(position).get("variance"));
        viewHolder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDate;
        private TextView tvLength;
        private TextView tvTimeCost;
        private TextView tvIse;
        private TextView tvVariance;

        public MyViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvLength = itemView.findViewById(R.id.tv_length);
            tvTimeCost = itemView.findViewById(R.id.tv_timeCost);
            tvIse = itemView.findViewById(R.id.tv_ise);
            tvVariance = itemView.findViewById(R.id.tv_variance);
        }

        public TextView getTvDate() {
            return tvDate;
        }

        public TextView getTvLength() {
            return tvLength;
        }

        public TextView getTvTimeCost() {
            return tvTimeCost;
        }

        public TextView getTvIse() {
            return tvIse;
        }

        public TextView getTvVariance() {
            return tvVariance;
        }
    }
}
