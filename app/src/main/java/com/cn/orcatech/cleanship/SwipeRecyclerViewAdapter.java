package com.cn.orcatech.cleanship;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_history, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        viewHolder.getTv_detail().setText(list.get(position).get("detail"));
        viewHolder.getTv_title().setText(list.get(position).get("title"));
        viewHolder.getIv_Top().setVisibility(Boolean.parseBoolean(list.get(position).get("top")) ? View.VISIBLE : View.INVISIBLE);
        viewHolder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_title;
        private TextView tv_detail;
        private ImageView iv_top;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_detail = itemView.findViewById(R.id.tv_detail);
            iv_top = itemView.findViewById(R.id.iv_top);
        }

        public TextView getTv_detail() {
            return tv_detail;
        }

        public TextView getTv_title() {
            return tv_title;
        }

        public ImageView getIv_Top() {
            return iv_top;
        }
    }

    

}
