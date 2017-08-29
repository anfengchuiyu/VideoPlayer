package com.zhe.ijkplayersample.ijkplayer_demo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TableLayout;

import com.zhe.ijkplayersample.R;
import com.zhe.ijkplayersample.system_bar.SystemBarUtil;
import com.zhe.ijkplayersample.system_bar.controller.MySystemBarController;
import com.zhe.lib.ijkplayer.media.IjkVideoView;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by zhe on 2017/8/18.
 */

public class IjkPlayerActivity extends AppCompatActivity {

    private static final String URL = "http://221.228.226.23/9/n/s/i/b/nsibhbkffizwbmymnuogqhdbkgvota/hc.yinyuetai.com/10BF015D362A1D1BE13B06B95C41E615.mp4?sc=9e5b5dfd06cf3ab1&br=789&vid=2908926&aid=28047&area=ML&vst=0&ptp=mv&rd=yinyuetai.com";
    //private static final String URL = "http://211.162.58.42/hc.yinyuetai.com/uploads/videos/common/10BF015D362A1D1BE13B06B95C41E615.mp4?sc=11af176ffff2dbc8&br=789&vid=2908926&aid=28047&area=ML&vst=0&ptp=mv&rd=yinyuetai.com";

    private IjkVideoView videoView;
    private TableLayout mHudView;
    private IjkPlayerController mediaController;
    private boolean isPortrait;
    private float currentSpeed = 1.0f;
    private MySystemBarController systemBarController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ijk_player);

        videoView = (IjkVideoView) findViewById(R.id.videoview);
        mHudView = (TableLayout) findViewById(R.id.hud_view);

        videoView.setRender(2);
        //设置显示调试信息的View
        videoView.setHudView(mHudView);
        videoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                mediaController.show();
            }
        });

        mediaController = new IjkPlayerController(this);
        videoView.setMediaController(mediaController);
        mediaController.setVisibleListener(new IjkPlayerController.OnVisibleListener() {
            @Override
            public void onHidden() {
                if (!isPortrait) {
                    systemBarController.hide();
                }
            }

            @Override
            public void onShow() {
                if (isPortrait) {
                    mediaController.setPadding(0, 0, 0, 0);
                }else {
                    systemBarController.show();
                    int nav_width = 0;
                    if (SystemBarUtil.checkDeviceHasNavigationBar(IjkPlayerActivity.this)) {
                        nav_width = SystemBarUtil.getNavigationBarWidthLandscape(IjkPlayerActivity.this);
                    }
                    mediaController.setPadding(0, SystemBarUtil.getStatusBarHeight(IjkPlayerActivity.this),
                            nav_width, 0);
                }
            }
        });
        //横竖屏点击切换
        mediaController.setFullScreenListener(new IjkPlayerController.OnClickFullScreenListener() {
            @Override
            public void OnClickFullScreen() {
                if (isPortrait) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
        //返回按钮点击
        mediaController.setBackListener(new IjkPlayerController.OnClickBackListener() {
            @Override
            public void onClickBack() {
                if (isPortrait) {
                    onBackPressed();
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
        //倍速点击
        mediaController.setmSpeedListener(new IjkPlayerController.OnClickSpeedListener() {
            @Override
            public void onClickSpeed(float speed) {
                videoView.setSpeed(speed);
            }
        });

        videoView.setVideoPath(URL);
        videoView.start();


        /*View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_video_source, null);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("视频源地址")
                .show();

        final EditText editText = (EditText) dialogView.findViewById(R.id.edittext);
        dialogView.findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = editText.getText().toString().trim();
                if (!TextUtils.isEmpty(path)) {
                    dialog.dismiss();
                    videoView.setVideoPath(path);
                    videoView.start();
                    mediaController.show();
                }
            }
        });*/


        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            isPortrait = true;
        } else {
            isPortrait = false;
        }

        systemBarController = new MySystemBarController(isPortrait, getWindow());
    }


    @Override
    protected void onResume() {
        super.onResume();
        videoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                videoView.start();
            }
        }, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.releaseWithoutStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏
            isPortrait = true;

            systemBarController.setPortrait(isPortrait);
            systemBarController.recover();
        } else {
            //横屏
            isPortrait = false;

            systemBarController.setPortrait(isPortrait);
            systemBarController.hide();
        }

        mediaController.hide();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && !isPortrait) {

            if (hasFocus) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            /*if (systemBarController != null) {
                systemBarController.hide();
            }*/
        }
    }


}
