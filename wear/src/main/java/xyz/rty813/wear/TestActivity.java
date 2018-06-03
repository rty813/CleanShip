package xyz.rty813.wear;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableDrawerLayout;
import android.support.wear.widget.drawer.WearableDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import xyz.rty813.wear.fragment.MapFragment;
import xyz.rty813.wear.fragment.SpeechFragment;

public class TestActivity extends WearableActivity {

    private static final String TAG = "Test";
    private TextView mTextView;
    private WearableNavigationDrawerView mWearableNavigationDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        // Enables Always-on
        setAmbientEnabled();
        WearableDrawerLayout layout = findViewById(R.id.drawer_layout);
        final WearableDrawerView drawerView = findViewById(R.id.drawerView);
        layout.setDrawerStateCallback(new WearableDrawerLayout.DrawerStateCallback(){
            @Override
            public void onDrawerOpened(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                Log.d(TAG, "onDrawerOpened: ");
                super.onDrawerOpened(layout, drawerView);
            }

            @Override
            public void onDrawerClosed(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                Log.d(TAG, "onDrawerClosed: ");
                super.onDrawerClosed(layout, drawerView);
            }

            @Override
            public void onDrawerStateChanged(WearableDrawerLayout layout, int newState) {
                Log.d(TAG, "onDrawerStateChanged: " + newState);
                super.onDrawerStateChanged(layout, newState);
                Log.d(TAG, "onDrawerStateChanged: isPeeking:" + drawerView.isPeeking());
                if (drawerView.isPeeking() && newState == 0) {
                    drawerView.getController().closeDrawer();
                }
            }
        });
    }


}
