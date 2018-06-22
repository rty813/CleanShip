package com.cn.orcatech.cleanship.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.fragment.DataFragment;
import com.cn.orcatech.cleanship.fragment.LoginFragment;
import com.cn.orcatech.cleanship.fragment.MapFragment;
import com.cn.orcatech.cleanship.fragment.UserInfoFragment;
import com.yanzhenjie.nohttp.NoHttp;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fm;
    private ArrayList<Fragment> fragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NoHttp.initialize(this);

        BottomNavigationBar navigationBar = findViewById(R.id.bottom_navigation);

        fm = getSupportFragmentManager();
        fragmentList = new ArrayList<>();
        fragmentList.add(new MapFragment());
        fragmentList.add(new DataFragment());
        fragmentList.add(new LoginFragment());
        fragmentList.add(new UserInfoFragment());

        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fragmentList) {
            transaction.add(R.id.container, fragment).hide(fragment);
        }
        transaction.show(fragmentList.get(0));
        transaction.commit();

        navigationBar.setMode(BottomNavigationBar.MODE_FIXED);
        navigationBar.setBackgroundStyle(BottomNavigationBar.BACKGROUND_STYLE_STATIC);
        navigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                hideAllFragments();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.show(fragmentList.get(position));
                transaction.commit();
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {

            }
        });
        navigationBar.addItem(new BottomNavigationItem(R.drawable.map_plane, "地图"))
                .addItem(new BottomNavigationItem(R.drawable.bottom_data, "数据"))
                .addItem(new BottomNavigationItem(R.drawable.store, "个人"))
                .setFirstSelectedPosition(0)
                .initialise();
    }

    private void hideAllFragments() {
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fragmentList) {
            transaction.hide(fragment);
        }
        transaction.commit();
    }

    public void loginSuccess() {
        hideAllFragments();
        fm.beginTransaction()
                .show(fragmentList.get(3))
                .commit();
    }

    public void logout() {
        hideAllFragments();
        fm.beginTransaction()
                .show(fragmentList.get(2))
                .commit();
    }
}
