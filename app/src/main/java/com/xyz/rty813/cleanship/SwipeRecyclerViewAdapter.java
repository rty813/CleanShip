package com.xyz.rty813.cleanship;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by doufu on 2017/11/30.
 */

public class SwipeRecyclerViewAdapter extends RecyclerView.Adapter{

    private ArrayList<Map<String, String>> list;

    public SwipeRecyclerViewAdapter(ArrayList<Map<String, String>> list) {
        super();
        this.list = list;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        viewHolder.getTv_route().setText(list.get(position).get("route"));
        viewHolder.getTv_time().setText(list.get(position).get("time"));
        viewHolder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_time;
        private TextView tv_route;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv_time = itemView.findViewById(R.id.tv_time);
            tv_route = itemView.findViewById(R.id.tv_route);
        }

        public TextView getTv_route() {
            return tv_route;
        }

        public TextView getTv_time() {
            return tv_time;
        }
    }

    

}
