package com.xyz.rty813.cleanship;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by doufu on 2017/12/2.
 */

public class DetailRecyclerViewAdapter extends RecyclerView.Adapter{

    private ArrayList<String> list;

    public DetailRecyclerViewAdapter(ArrayList<String> list) {
        super();
        this.list = list;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_detail, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        viewHolder.getTv_point().setText(list.get(position));
        viewHolder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_point;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv_point = itemView.findViewById(R.id.tv_point);
        }

        public TextView getTv_point() {
            return tv_point;
        }
    }

}
