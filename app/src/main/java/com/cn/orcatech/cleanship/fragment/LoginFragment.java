package com.cn.orcatech.cleanship.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.LogonActivity;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.cn.orcatech.cleanship.util.MD5Utils;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import es.dmoral.toasty.Toasty;

public class LoginFragment extends Fragment implements View.OnClickListener {

    private MaterialEditText etUsername;
    private MaterialEditText etPassword;

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
    }

    @Override
    public void onClick(View v) {
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.tv_forget:
                break;
            case R.id.tv_logon:
                startActivity(new Intent(activity, LogonActivity.class));
                break;
            case R.id.btn_login:
                String username = etUsername.getText().toString();
                String password_origin = etPassword.getText().toString();
                String password = MD5Utils.stringToMD5(password_origin);
                StringRequest request = new StringRequest("http://orca-tech.cn/app/loginlogon.php", RequestMethod.POST);
                request.add("username", username).add("password", password).add("type", "login");
                AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
                    @Override
                    public void onSucceed(int what, Response<String> response) {
                        super.onSucceed(what, response);
                        if (response.get().equals("success")) {
                            Toasty.success(activity, "登陆成功", Toast.LENGTH_SHORT).show();
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
                        super.onFailed(what, response);
                    }
                });
                break;
        }
    }
}
