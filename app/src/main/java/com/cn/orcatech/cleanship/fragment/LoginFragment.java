package com.cn.orcatech.cleanship.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.ForgetPwActivity;
import com.cn.orcatech.cleanship.activity.LogonActivity;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.cn.orcatech.cleanship.util.MD5Utils;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import es.dmoral.toasty.Toasty;

public class LoginFragment extends NoFragment implements View.OnClickListener {

    private MaterialEditText etUsername;
    private MaterialEditText etPassword;
    private ProgressBar progressbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.tv_forget).setOnClickListener(this);
        view.findViewById(R.id.tv_logon).setOnClickListener(this);
        view.findViewById(R.id.btn_login).setOnClickListener(this);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        progressbar = view.findViewById(R.id.progressBar);
        SharedPreferences preferences = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        etUsername.setText(preferences.getString("username", ""));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            ((MainActivity)getActivity()).hideToolbar(View.VISIBLE);
        }
        else {
            ((MainActivity)getActivity()).hideToolbar(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.tv_forget:
                startActivity(new Intent(activity, ForgetPwActivity.class));
                break;
            case R.id.tv_logon:
                startActivity(new Intent(activity, LogonActivity.class));
                break;
            case R.id.btn_login:
                String username = etUsername.getText().toString();
                String password_origin = etPassword.getText().toString();
                String password = MD5Utils.stringToMD5(password_origin);
                if (username.length() < 5 || username.length() > 20 || password_origin.length() < 6) {
                    Toasty.error(activity, "用户名或密码长度不符", Toast.LENGTH_SHORT).show();
                }
                else {
                    login(activity, username, password);
                }
                break;
        }
    }

    public void login(final MainActivity activity, final String username, final String password) {
        StringRequest request = new StringRequest("http://orca-tech.cn/app/loginlogon.php", RequestMethod.POST);
        request.add("username", username).add("password", password).add("type", "login");
        AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
            @Override
            public void onSucceed(int what, Response<String> response) {
                super.onSucceed(what, response);
                String[] res = response.get().split(";");
                if (res.length == 3 && res[0].equals("success")) {
                    Toasty.success(activity, "登陆成功", Toast.LENGTH_SHORT).show();
                    SharedPreferences sharedPreferences = activity.getSharedPreferences("userinfo", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.apply();
                    activity.userInfo.setUsername(username);
                    activity.userInfo.setPassword(password);
                    activity.userInfo.setShip_id(Integer.parseInt(res[1]));
                    activity.userInfo.setTotalship(Integer.parseInt(res[2]));
                    activity.loginSuccess();
                }
                else if (response.get().equals("fail")){
                    Toasty.error(activity, "密码错误", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toasty.error(activity, "用户名不存在", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(int what, Response<String> response) {
                Toasty.error(activity, "服务器连接失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                activity.logout();
                super.onFailed(what, response);
            }

            @Override
            public void onStart(int what) {
                progressbar.setVisibility(View.VISIBLE);
                super.onStart(what);
            }

            @Override
            public void onFinish(int what) {
                progressbar.setVisibility(View.INVISIBLE);
                super.onFinish(what);
            }
        });
    }
}
