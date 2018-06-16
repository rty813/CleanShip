package com.cn.orcatech.cleanship;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.cn.orcatech.cleanship.fragment.LoginFragment;
import com.cn.orcatech.cleanship.fragment.MapFragment;
import com.yanzhenjie.fragment.CompatActivity;

public class MainActivity extends CompatActivity {

    private MapFragment mapFragment;
    private LoginFragment loginFragment;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationBar navigationBar = findViewById(R.id.bottom_navigation);

        fm = getSupportFragmentManager();
        mapFragment = new MapFragment();
        loginFragment = new LoginFragment();
        fm.beginTransaction()
                .add(R.id.container, mapFragment)
                .add(R.id.container, loginFragment)
                .hide(loginFragment)
                .commit();
        navigationBar.setMode(BottomNavigationBar.MODE_FIXED);
        navigationBar.setBackgroundStyle(BottomNavigationBar.BACKGROUND_STYLE_RIPPLE);
        navigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                hideAllFragments();
                FragmentTransaction transaction = fm.beginTransaction();
                switch (position) {
                    case 0:
                        transaction.show(mapFragment);
                        break;
                    case 1:
                        transaction.show(loginFragment);
                        break;
                }
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
                .addItem(new BottomNavigationItem(R.drawable.store, "我"))
                .setFirstSelectedPosition(0)
                .initialise();
    }

    @Override
    protected int fragmentLayoutId() {
        return R.id.container;
    }

    private void hideAllFragments() {
        fm.beginTransaction()
                .hide(mapFragment).hide(loginFragment).commit();
    }
}
