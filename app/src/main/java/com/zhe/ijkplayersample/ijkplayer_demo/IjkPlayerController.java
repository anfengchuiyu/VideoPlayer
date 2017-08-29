package com.zhe.ijkplayersample.ijkplayer_demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zhe.ijkplayersample.R;
import com.zhe.lib.ijkplayer.media.IMediaController;

import java.util.Formatter;
import java.util.Locale;


/**
 * Created by zhe on 2017/8/18.
 */

public class IjkPlayerController implements IMediaController {


    public interface OnClickFullScreenListener {
        void OnClickFullScreen();
    }

    public interface OnClickSpeedListener {
        void onClickSpeed(float speed);
    }

    public interface OnClickDefinitionListener {
        void onClickDefinition();
    }

    public interface OnClickBackListener {
        void onClickBack();
    }

    public interface OnVisibleListener {
        void onHidden();

        void onShow();
    }

    //constant
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;


    private Context mAppContext;
    private MediaController.MediaPlayerControl mPlayer;

    //views
    private View mAnchor;
    private View mRoot;
    private View mBackView;
    private View mSpeedView;
    private ImageView mPlayToggle;
    private ImageView mFullScreen;
    private SeekBar mSeekBar;
    private TextView mCurrentTimeView, mEndTimeView;

    //others variable
    private long mDuration;
    private boolean mShowing;
    private boolean mDragging;
    private boolean mInstantSeeking = true;
    private static int sDefaultTimeout = 3000;
    private static final int SEEK_TO_POST_DELAY_MILLIS = 200;
    private boolean mIsPortrait;
    private Runnable mLastSeekBarRunnable;
    private boolean mFromXml;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    //listeners
    private OnClickFullScreenListener mFullScreenListener;
    private OnVisibleListener mVisibleListener;
    private OnClickSpeedListener mSpeedListener;
    private OnClickBackListener mBackListener;


    public IjkPlayerController(@NonNull Context context) {
        mAppContext = context;
    }


    public void setFullScreenListener(OnClickFullScreenListener listener) {
        this.mFullScreenListener = listener;
    }

    public void setVisibleListener(OnVisibleListener listener) {
        this.mVisibleListener = listener;
    }

    public void setmSpeedListener(OnClickSpeedListener mSpeedListener) {
        this.mSpeedListener = mSpeedListener;
    }

    public void setBackListener(OnClickBackListener listener) {
        this.mBackListener = listener;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        if (mRoot != null) {
            mRoot.setPadding(left, top, right, bottom);
        }
    }

    private View makeControllerView() {
        mRoot = LayoutInflater.from(mAppContext).inflate(R.layout.layout_video_player_controller, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        mPlayToggle = (ImageView) v.findViewById(R.id.player_ui_play_toggle);
        mSeekBar = (SeekBar) v.findViewById(R.id.player_ui_seekbar);

        if (mSeekBar != null) {
            mSeekBar.setMax(1000);
            mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        }

        if (mPlayToggle != null) {
            mPlayToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doPauseResume();
                    show();
                }
            });
        }

        mBackView = v.findViewById(R.id.player_ui_back);
        if (mBackView != null) {
            mBackView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBackListener != null) {
                        mBackListener.onClickBack();
                    }
                }
            });
        }

        mSpeedView = v.findViewById(R.id.player_ui_speed);
        if (mSpeedView != null) {
            mSpeedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PopupMenu popupMenu = new PopupMenu(mAppContext, v);
                    popupMenu.inflate(R.menu.menu_video_speed);
                    popupMenu.show();

                    show(3600000);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (mSpeedListener == null) {
                                return false;
                            }
                            switch (item.getItemId()) {
                                case R.id.menu_speed_1:
                                    mSpeedListener.onClickSpeed(0.5f);
                                    break;
                                case R.id.menu_speed_2:
                                    mSpeedListener.onClickSpeed(1.0f);
                                    break;
                                case R.id.menu_speed_3:
                                    mSpeedListener.onClickSpeed(1.5f);
                                    break;
                                case R.id.menu_speed_4:
                                    mSpeedListener.onClickSpeed(1.75f);
                                    break;
                                case R.id.menu_speed_5:
                                    mSpeedListener.onClickSpeed(2.0f);
                                    break;
                            }
                            return true;
                        }
                    });

                    popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            hide();
                        }
                    });
                }
            });
        }

        mFullScreen = (ImageView) v.findViewById(R.id.player_ui_play_fullscreen);
        if (mFullScreen != null) {
            mFullScreen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFullScreenListener != null) {
                        mFullScreenListener.OnClickFullScreen();
                    }
                }
            });
        }

        mCurrentTimeView = (TextView) v.findViewById(R.id.player_ui_current_time);
        mEndTimeView = (TextView) v.findViewById(R.id.player_ui_end_time);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }


    @Override
    public void hide() {
        if (mShowing) {
            mRoot.setVisibility(View.GONE);
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
    public void setAnchorView(View view) {

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = ((ViewGroup) view);

            if (mRoot == null) {
                int childViewCount = viewGroup.getChildCount();
                mRoot = makeControllerView();
                initControllerView(mRoot);

                mRoot.setTag(childViewCount);
            } else {
                //viewGroup.removeViewAt((Integer) mRoot.getTag() - 1);
                viewGroup.removeAllViews();
            }

            viewGroup.addView(mRoot);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public void setMediaPlayer(MediaController.MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    @Override
    public void show(int timeout) {
        if (!mShowing) {
            mRoot.setVisibility(View.VISIBLE);
            mShowing = true;

            if (mVisibleListener != null) {
                mVisibleListener.onShow();
            }
        }
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT),
                    timeout);
        }
    }

    @Override
    public void show() {
        show(sDefaultTimeout);
    }

    @Override
    public void showOnce(View view) {

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

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private long setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();

        //update UI
        if (duration > 0) {
            long pos = 1000L * position / duration;
            mSeekBar.setProgress((int) pos);
        }
        int percent = mPlayer.getBufferPercentage();
        mSeekBar.setSecondaryProgress(percent * 10);

        if (mCurrentTimeView != null) {
            mCurrentTimeView.setText(stringForTime(position));
        }
        if (mEndTimeView != null) {
            mEndTimeView.setText(stringForTime(duration));
        }

        mDuration = duration;

        return position;
    }


    //更新暂停播放按钮UI
    private void updatePausePlay() {
        if (mPlayer == null || mPlayToggle == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayToggle.setImageResource(R.drawable.exo_controls_pause);
        } else {
            mPlayToggle.setImageResource(R.drawable.exo_controls_play);
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


    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            final long newPosition = (long) ((mDuration * progress) / 1000);
            if (mInstantSeeking) {
                mHandler.removeCallbacks(mLastSeekBarRunnable);
                mLastSeekBarRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mPlayer.seekTo((int) newPosition);
                    }
                };
                mHandler.postDelayed(mLastSeekBarRunnable, SEEK_TO_POST_DELAY_MILLIS);
            }

            if (mCurrentTimeView != null) {
                mCurrentTimeView.setText(stringForTime((int) newPosition));
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
                mPlayer.seekTo((int) (mDuration * seekBar.getProgress() / 1000));
            }

            show(sDefaultTimeout);
            mHandler.removeMessages(SHOW_PROGRESS);
            mDragging = false;
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
        }
    };


}
