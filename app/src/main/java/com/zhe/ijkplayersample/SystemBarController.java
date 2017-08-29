package com.zhe.ijkplayersample;

import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by zhe on 2017/8/17.
 */

public class SystemBarController {

    private static final int HIDE_SYSTEM_BAR = -1014;
    public static final int DELAY_MILLS = 3000;

    private boolean isPortrait;
    private Window window;


    public SystemBarController(boolean isPortrait, Window window) {
        this.isPortrait = isPortrait;
        this.window = window;
    }

    public void setPortrait(boolean isPortrait) {
        this.isPortrait = isPortrait;
    }


    private Handler mHideSystemBarHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!isPortrait) {
                hide();
            }
        }
    };


    public void delayedHide(int delayMills) {
        mHideSystemBarHandler.removeMessages(HIDE_SYSTEM_BAR);
        mHideSystemBarHandler.sendEmptyMessageDelayed(HIDE_SYSTEM_BAR, delayMills);
    }


    public void hide() {
        if (isPortrait) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
            );
        } else {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            window.setAttributes(attrs);
        }
    }


    public void show() {
        if (isPortrait) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }


        //system bar颜色处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }

    }

    /**
     * 恢复竖屏下的systembar状态
     */
    public void revcoverProtrait() {
        if (isPortrait) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            } else {
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.setAttributes(attrs);
            }

            //system bar颜色处理
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(window.getContext().getResources().getColor(R.color.colorPrimaryDark));
                window.setNavigationBarColor(Color.BLACK);
            }

        }
    }


}
