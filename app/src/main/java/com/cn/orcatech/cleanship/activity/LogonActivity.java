package com.cn.orcatech.cleanship.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.util.MD5Utils;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.rest.AsyncRequestExecutor;
import com.yanzhenjie.nohttp.rest.Response;
import com.yanzhenjie.nohttp.rest.SimpleResponseListener;
import com.yanzhenjie.nohttp.rest.StringRequest;

import es.dmoral.toasty.Toasty;

public class LogonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logon);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.btn_logon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialEditText etPassword = findViewById(R.id.et_password);
                MaterialEditText etUsername = findViewById(R.id.et_username);
                String username = etUsername.getText().toString();
                String password_origin = etPassword.getText().toString();
                String password = MD5Utils.stringToMD5(password_origin);

                if (username.length() < 5 || username.length() > 20 || password_origin.length() < 6) {
                    Toasty.error(LogonActivity.this, "用户名或密码长度不符", Toast.LENGTH_SHORT).show();
                    return;
                }

                StringRequest request = new StringRequest("http://orca-tech.cn/app/loginlogon.php", RequestMethod.POST);
                request.add("username", username).add("password", password).add("type", "logon");
                AsyncRequestExecutor.INSTANCE.execute(0, request, new SimpleResponseListener<String>() {
                    @Override
                    public void onSucceed(int what, Response<String> response) {
                        super.onSucceed(what, response);
                        if (response.get().equals("success")) {
                            Toasty.success(LogonActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else if (response.get().equals("fail")){
                            Toasty.error(LogonActivity.this, "用户名已存在", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailed(int what, Response<String> response) {
                        Toasty.error(LogonActivity.this, "服务器连接失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                        super.onFailed(what, response);
                    }

                    @Override
                    public void onStart(int what) {
                        super.onStart(what);
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFinish(int what) {
                        super.onFinish(what);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }
}
