package com.cn.orcatech.cleanship.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.cn.orcatech.cleanship.R;
import com.cn.orcatech.cleanship.activity.MainActivity;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.yanzhenjie.fragment.NoFragment;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class DataFragment extends NoFragment implements View.OnClickListener {

    private TXLivePlayer mLivePlayer;
    private Button btnPause;
    private  TXCloudVideoView mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view.findViewById(R.id.video_view);
        mLivePlayer = new TXLivePlayer(getActivity());
        mLivePlayer.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
        mLivePlayer.setRenderRotation(TXLiveConstants.RENDER_ROTATION_LANDSCAPE);
        mLivePlayer.setPlayerView(mView);
        btnPause = view.findViewById(R.id.btn_videopause);
        view.findViewById(R.id.btn_videoplay).setOnClickListener(this);
        view.findViewById(R.id.btn_videopause).setOnClickListener(this);
        view.findViewById(R.id.btn_videostop).setOnClickListener(this);
        ((SeekBar)view.findViewById(R.id.sb_thrust)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                MainActivity activity = (MainActivity) getActivity();
                try {
                    activity.getMapFragment().mqttClient.publish("APP2SHIP/5/1", ("$4GCTL," + i*5 + ",0#").getBytes(), 0, false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        MainActivity activity = (MainActivity) getActivity();
        String url = "rtmp://25779.liveplay.myqcloud.com/live/25779_" + activity.userInfo.getShip_id() + "_" + activity.selectShip;
        switch (view.getId()) {
            case R.id.btn_videoplay:
                try {
                    activity.getMapFragment().publishMessage("$video;play#");
                    Thread.sleep(2000);
                    mLivePlayer.startPlay(url, TXLivePlayer.PLAY_TYPE_LIVE_RTMP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_videopause:
                if (btnPause.getText().equals("暂停")) {
                    mLivePlayer.pause();
                    btnPause.setText("继续");
                }
                else {
                    mLivePlayer.resume();
                    btnPause.setText("暂停");
                }
                break;
            case R.id.btn_videostop:
                activity.getMapFragment().publishMessage("$video;stop#");
                mLivePlayer.stopPlay(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        mLivePlayer.stopPlay(true);
        mView.onDestroy();
        super.onDestroy();
    }
}
