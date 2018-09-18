package com.cn.orcatech.cleanship.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.UserInfo;
import com.cn.orcatech.cleanship.activity.BoundActivity;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import es.dmoral.toasty.Toasty;

public class UserInfoFragment extends NoFragment implements View.OnClickListener {
    private UserInfo userinfo;
    private Toolbar toolbar;
    private TextView tvVerify;
    private TextView tvTotalship;
    private Button btnVerify;
    private MainActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_userinfo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnVerify = view.findViewById(R.id.btn_verify);
        view.findViewById(R.id.btn_logout).setOnClickListener(this);
        btnVerify.setOnClickListener(this);
        tvVerify = view.findViewById(R.id.tv_verify);
        tvTotalship = view.findViewById(R.id.tv_totalship);
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setTitle(R.string.app_name);
        setToolbar(toolbar);
        view.findViewById(R.id.btn_bound).setOnClickListener(this);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            activity.hideToolbar(View.VISIBLE);
        }
        else {
            activity.hideToolbar(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_logout:
                activity.logout();
                break;
            case R.id.btn_verify:
                final EditText editText = new EditText(activity);
                new AlertDialog.Builder(activity)
                        .setTitle("认证")
                        .setMessage("请输入序列号")
                        .setView(editText)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    int id = Integer.parseInt(editText.getText().toString());
                                    verify(id);
                                }
                                catch (NumberFormatException e){
                                    Toasty.error(activity, "请输入正确的序列号", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setCancelable(true)
                        .show();
                break;
            case R.id.btn_bound:
                Intent intent = new Intent(activity, BoundActivity.class);
                intent.putExtra("ship_id", userinfo.getShip_id());
                startActivityForResult(intent, 0);
                break;
        }
    }

    private void verify(final int id) {
        StringRequest request = new StringRequest("http://orca-tech.cn/app/loginlogon.php", RequestMethod.POST);
        request.add("type", "verify").add("username", userinfo.getUsername()).add("id", id);
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            @Override
            public void onSucceed(int what, Response<String> response) {
                if (response.get().equals("success")) {
                    Toasty.success(activity, "认证成功！", Toast.LENGTH_SHORT).show();
                    tvVerify.setText("已认证！ID=" + String.valueOf(id));
                }
                else {
                    Toasty.error(activity, "认证失败", Toast.LENGTH_SHORT).show();
                }
                super.onSucceed(what, response);
            }

            @Override
            public void onFailed(int what, Response<String> response) {
                Toasty.error(activity, "认证失败！", Toast.LENGTH_SHORT).show();
                super.onFailed(what, response);
            }
        });
    }

    public void setUserinfo(UserInfo userinfo) {
        this.userinfo = userinfo;
        toolbar.setTitle(userinfo.getUsername());
        if (userinfo.getShip_id() == -1) {
            btnVerify.setEnabled(true);
            Toasty.warning(activity, "未认证！", Toast.LENGTH_SHORT).show();
        }
        else {
            btnVerify.setEnabled(false);
            tvTotalship.setText("拥有的船：" + userinfo.getTotalship() + "艘");
            tvVerify.setText("已认证！ID=" + String.valueOf(userinfo.getShip_id()));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == 1) {
            activity.getMapFragment().loadBound();
        }
    }
}
