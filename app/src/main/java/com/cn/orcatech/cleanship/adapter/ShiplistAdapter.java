package com.cn.orcatech.cleanship.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cn.orcatech.cleanship.R;

import java.util.ArrayList;
import java.util.Map;

public class ShiplistAdapter extends RecyclerView.Adapter {

    private ArrayList<Map<String, String>> list;

    public ShiplistAdapter(ArrayList<Map<String, String>> list) {
        super();
        this.list = list;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_shiplist, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        if (position == 0) {
            viewHolder.getTv_title().setText("显示全部");
            viewHolder.getTv_detail().setVisibility(View.GONE);
            viewHolder.getIv_status().setVisibility(View.GONE);
        }
        else {
            viewHolder.getTv_detail().setText(list.get(position - 1).get("detail"));
            viewHolder.getTv_title().setText(list.get(position - 1).get("title"));
            switch (list.get(position - 1).get("status")) {
                case "1":
                    viewHolder.getIv_status().setImageResource(R.drawable.round_green);
                    viewHolder.getTv_title().setTextColor(0xFF000000);
                    break;
                case "-1":
                    viewHolder.getIv_status().setImageResource(R.drawable.round_red);
                    viewHolder.getTv_title().setTextColor(0xFF898989);
                    break;
                case "0":
                    viewHolder.getIv_status().setImageResource(R.drawable.round_orange);
                    viewHolder.getTv_title().setTextColor(0xFF000000);
                    break;
                default:
                    break;
            }
        }
        viewHolder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return list == null ? 1 : list.size() + 1;
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_title;
        private TextView tv_detail;
        private ImageView iv_status;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_detail = itemView.findViewById(R.id.tv_detail);
            iv_status = itemView.findViewById(R.id.iv_status);
        }

        public TextView getTv_detail() {
            return tv_detail;
        }

        public TextView getTv_title() {
            return tv_title;
        }

        public ImageView getIv_status() {
            return iv_status;
        }
    }
}
