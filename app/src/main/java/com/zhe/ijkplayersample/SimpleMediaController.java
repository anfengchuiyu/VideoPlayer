package com.zhe.ijkplayersample;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pili.pldroid.player.IMediaController;

/**
 * Created by zhe on 2017/8/15.
 */

public class SimpleMediaController implements IMediaController {

    public static class NavBarInfo {
        public int statusbar_height;
        public boolean have_navbar;
        public int navbar_height;
    }

    public interface OnClickFullScreenListener {
        void OnClickFullScreen();
    }

    public interface OnClickSpeedListener {
        void onClickSpeed(int speed);
    }

    public interface OnVisibleListener {
        void onHidden();

        void onShow();
    }


    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;

    private IMediaController.MediaPlayerControl mPlayer;

    private Context mContext;


    //views
    private View mRoot;
    private TextView mPlayToggle;
    private SeekBar mSeekBar;
    private TextView speedView;
    private View statusBarPHView; //状态栏占位  横屏时用
    private View navBarPHView; //navigation bar占位 横屏时用，且有虚拟按键的情况下

    //others
    private long mDuration;
    private boolean mShowing;
    private boolean mDragging;
    private boolean mInstantSeeking = true;
    private static int sDefaultTimeout = 3000;
    private static final int SEEK_TO_POST_DELAY_MILLIS = 200;
    private boolean mIsPortrait;
    private NavBarInfo mNavBarInfo;
    private AlphaAnimation showAlphaAnim = new AlphaAnimation(0.4f, 1.0f);
    private AlphaAnimation hideAlphaAnim = new AlphaAnimation(0.8f, 0f);
    private Runnable mLastSeekBarRunnable;
    private String[] sppedStrs = new String[]{"0.5x", "1.0x", "1.5x", "2.0x"};
    private int[] sppedValues = new int[]{0X00010002, 0X00010001, 0X00030002, 0X00020001};

    //listener
    private OnClickFullScreenListener mFullScreenListener;
    private OnVisibleListener mVisibleListener;
    private OnClickSpeedListener mSpeedListener;


    public SimpleMediaController(Context context) {
        this.mContext = context;
    }

    public void setOnClickFullScreenListener(OnClickFullScreenListener listener) {
        this.mFullScreenListener = listener;
    }

    public void setOnVisibleListener(OnVisibleListener listener) {
        this.mVisibleListener = listener;
    }

    public void setSpeedListener(OnClickSpeedListener listener) {
        this.mSpeedListener = listener;
    }

    public void setPortrait(boolean portrait) {
        mIsPortrait = portrait;
    }

    public void setNavBarInfo(NavBarInfo navBarInfo) {
        this.mNavBarInfo = navBarInfo;

        if (statusBarPHView != null) {
            ViewGroup.LayoutParams lp1 = statusBarPHView.getLayoutParams();
            lp1.height = mNavBarInfo.statusbar_height;

            if (mNavBarInfo.have_navbar) {
                ViewGroup.LayoutParams lp = navBarPHView.getLayoutParams();
                lp.width = mNavBarInfo.navbar_height;
            }
        }
    }

    /**
     * 设置是否可seek
     *
     * @param seekWhenDragging
     */
    public void setInstantSeeking(boolean seekWhenDragging) {
        mInstantSeeking = seekWhenDragging;
    }


    private View makeControllerView() {
        return LayoutInflater.from(mContext).inflate(R.layout.layout_simple_video_player, null);
    }

    private void initControllerView(View v) {
        mPlayToggle = (TextView) v.findViewById(R.id.player_ui_play_toggle);
        mSeekBar = (SeekBar) v.findViewById(R.id.player_ui_seekbar);
        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);

        mPlayToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPauseResume();
                show();
            }
        });

        v.findViewById(R.id.player_ui_play_fullscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFullScreenListener != null) {
                    mFullScreenListener.OnClickFullScreen();
                }
            }
        });

        speedView = (TextView) v.findViewById(R.id.player_ui_speed);
        speedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(speedView.getContext())
                        .setItems(sppedStrs, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mSpeedListener != null) {
                                    mSpeedListener.onClickSpeed(sppedValues[which]);
                                }
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });


        statusBarPHView = v.findViewById(R.id.player_ui_statusbar_placeholder);
        navBarPHView = v.findViewById(R.id.player_ui_navbar_placeholder);


        if (mNavBarInfo != null) {
            ViewGroup.LayoutParams lp1 = statusBarPHView.getLayoutParams();
            lp1.height = mNavBarInfo.statusbar_height;

            if (mNavBarInfo.have_navbar) {
                ViewGroup.LayoutParams lp = navBarPHView.getLayoutParams();
                lp.width = mNavBarInfo.navbar_height;
            }
        }

    }


    @Override
    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    @Override
    public void show() {
        show(sDefaultTimeout);
    }

    @Override
    public void show(int timeout) {
        if (!mShowing) {

            /*showAlphaAnim.setDuration(300);
            showAlphaAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mRoot.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mRoot.startAnimation(showAlphaAnim);*/

            mRoot.setVisibility(View.VISIBLE);


            showStatusBarPHView(!mIsPortrait);

            /*if (!mIsPortrait && mNavBarInfo != null) {
                if (mNavBarInfo.have_navbar) {
                    mRoot.setPadding(0, 0, mNavBarInfo.navbar_height, 0);
                }
            }else {
                mRoot.setPadding(0, 0, 0, 0);
            }*/


            mShowing = true;
        }
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT),
                    timeout);
        }

        if (mVisibleListener != null) {
            mVisibleListener.onShow();
        }
    }

    @Override
    public void hide() {
        if (mShowing) {

            /*if (mRoot.getWindowToken() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    mRoot.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }*/

            /*hideAlphaAnim.setDuration(300);
            hideAlphaAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mRoot.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mRoot.startAnimation(hideAlphaAnim);*/

            mRoot.setVisibility(View.GONE);
            showStatusBarPHView(false);


            mShowing = false;

            if (mVisibleListener != null) {
                mVisibleListener.onHidden();
            }
        }
    }

    @Override
    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public void setEnabled(boolean b) {

    }

    @Override
    public void setAnchorView(View view) {
        if (view instanceof ViewGroup) {

            ViewGroup viewGroup = ((ViewGroup) view);

            if (mRoot == null) {
                int childViewCount = viewGroup.getChildCount();
                mRoot = makeControllerView();
                initControllerView(mRoot);

                mRoot.setTag(childViewCount);
            } else {
                viewGroup.removeViewAt((Integer) mRoot.getTag() - 1);
            }

            viewGroup.addView(mRoot);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress();
                    if (!mDragging && mShowing) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                        updatePausePlay();
                    }
                    break;
            }
        }
    };


    private long setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }

        long position = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();

        //update UI
        if (duration > 0) {
            long pos = 1000L * position / duration;
            mSeekBar.setProgress((int) pos);
        }
        int percent = mPlayer.getBufferPercentage();
        mSeekBar.setSecondaryProgress(percent * 10);

        mDuration = duration;

        return position;
    }

    //更新暂停播放按钮UI
    private void updatePausePlay() {
        if (mPlayer == null || mPlayToggle == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayToggle.setText("暂停");
        } else {
            mPlayToggle.setText("播放");
        }
    }

    //暂停、继续
    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying())
            mPlayer.pause();
        else
            mPlayer.start();
        updatePausePlay();
    }


    private void showStatusBarPHView(boolean isShow) {
        if (isShow) {
            statusBarPHView.setVisibility(View.VISIBLE);
            navBarPHView.setVisibility(View.VISIBLE);
        } else {
            statusBarPHView.setVisibility(View.GONE);
            navBarPHView.setVisibility(View.GONE);
        }
    }

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            final long newPosition = (long) ((mDuration * progress) / 1000);
            if (mInstantSeeking) {
                mHandler.removeCallbacks(mLastSeekBarRunnable);
                mLastSeekBarRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mPlayer.seekTo(newPosition);
                    }
                };
                mHandler.postDelayed(mLastSeekBarRunnable, SEEK_TO_POST_DELAY_MILLIS);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            show(3600000);
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (!mInstantSeeking) {
                mPlayer.seekTo(mDuration * seekBar.getProgress() / 1000);
            }

            show(sDefaultTimeout);
            mHandler.removeMessages(SHOW_PROGRESS);
            mDragging = false;
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
        }
    };


}
