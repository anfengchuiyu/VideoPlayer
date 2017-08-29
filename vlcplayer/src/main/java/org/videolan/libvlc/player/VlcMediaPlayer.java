package org.videolan.libvlc.player;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by zhe on 2017/8/23.
 */

public class VlcMediaPlayer extends AbstractMediaPlayer {


    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private Media media;


    private int mVideoWidth, mVideoHeight;
    //视频宽高比采样率
    private int mVideoSarNum, mVideoSarDen;
    private String mDataSource;
    private Context context;

    public VlcMediaPlayer(Context context) {
        this.context = context.getApplicationContext();
    }


    private MediaPlayer.EventListener eventListener = new MediaPlayer.EventListener() {
        VlcMediaPlayer player = VlcMediaPlayer.this;
        int opening = 0;

        @Override
        public void onEvent(MediaPlayer.Event event) {
            if (player == null)
                return;
            switch (event.type) {
                case MediaPlayer.Event.Opening: //视频打开中
                    //opening = 1;
                    break;
                case MediaPlayer.Event.Buffering: //视频缓冲中
                    player.notifyOnBufferingUpdate((int) event.getBuffering());
                    break;
                case MediaPlayer.Event.Playing://视频播放中
                    /*if (opening == 1) {
                        player.notifyOnPrepared();
                        opening = 0;
                    }*/
                    break;
                case MediaPlayer.Event.TimeChanged:
                    player.notifyOnSeekComplete();
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                case MediaPlayer.Event.Paused: //视频暂停
                    break;
                case MediaPlayer.Event.Stopped: //视频停止
                    break;
                case MediaPlayer.Event.EndReached: //视频播放完
                    player.notifyOnCompletion();
                    break;
                case MediaPlayer.Event.EncounteredError: //遇到错误
                    player.notifyOnError(MEDIA_ERROR_UNKNOWN, 0);
                    break;
            }

        }
    };

    private IVLCVout.OnNewVideoLayoutListener layoutListener = new IVLCVout.OnNewVideoLayoutListener() {

        @Override
        public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            mVideoWidth = width;
            mVideoHeight = height;
            mVideoSarNum = sarNum;
            mVideoSarDen = sarDen;
        }
    };

    private IVLCVout.Callback callback = new IVLCVout.Callback() {

        @Override
        public void onSurfacesCreated(IVLCVout vlcVout) {

        }

        @Override
        public void onSurfacesDestroyed(IVLCVout vlcVout) {

        }
    };


    @Override
    public void setDisplay(SurfaceHolder surface) {
        if(mediaPlayer != null && !mediaPlayer.getVLCVout().areViewsAttached() && surface != null) {
            mediaPlayer.getVLCVout().setVideoSurface(surface.getSurface(), null);
            mediaPlayer.getVLCVout().attachViews(this.layoutListener);
        }
    }

    @Override
    public void setDataSource(String dataSource) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (mediaPlayer == null)
            return;
        if (TextUtils.isEmpty(dataSource))
            return;

        mDataSource = dataSource;
        if (dataSource.startsWith("/")) {
            media = new Media(libVLC, dataSource);
        } else {
            media = new Media(libVLC, Uri.parse(dataSource));
        }
        mediaPlayer.setMedia(media);
    }

    @Override
    public String getDataSource() {
        return mDataSource;
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        release();
        ArrayList<String> options = new ArrayList<String>();
        options.add("--aout=opensles");
        options.add("--audio-time-stretch"); // time stretching
        options.add("--http-reconnect");
        options.add("--avcodec-hw=vaapi");
        options.add(":network-caching=1500");//网络缓存
        options.add(":file-caching=1500");
        //options.add("--ffmpeg-hurry-up");
        options.add("-vvv"); // verbosity

        libVLC = new LibVLC(context, options);
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.setEventListener(eventListener);

        IVLCVout vout = mediaPlayer.getVLCVout();
        vout.addCallback(callback);

        this.notifyOnPrepared();


        System.out.println("vlc version is " + libVLC.version());
    }

    @Override
    public void start() throws IllegalStateException {
        if (mediaPlayer != null)
            mediaPlayer.play();
    }

    @Override
    public void stop() throws IllegalStateException {
        if (mediaPlayer != null)
            mediaPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        if (mediaPlayer != null)
            mediaPlayer.pause();
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer != null)
            return mediaPlayer.isPlaying();
        return false;
    }

    @Override
    public void seekTo(long var1) throws IllegalStateException {
        if (mediaPlayer != null)
            mediaPlayer.setTime(var1);
    }

    @Override
    public long getCurrentPosition() {
        if (mediaPlayer != null)
            return mediaPlayer.getTime();
        return 0;
    }

    @Override
    public long getDuration() {
        if (mediaPlayer != null)
            return mediaPlayer.getLength();
        return 0;
    }

    @Override
    public void release() {
        try {
            if (libVLC == null || mediaPlayer == null)
                return;
            if (callback != null)
                mediaPlayer.getVLCVout().removeCallback(callback);
            if (mediaPlayer.getVLCVout().areViewsAttached())
                mediaPlayer.getVLCVout().detachViews();

            final Media media = mediaPlayer.getMedia();
            if (media != null) {
                media.setEventListener(null);
                mediaPlayer.stop();
                mediaPlayer.setMedia(null);
                media.release();
            }

            resetListeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reset() {
        mVideoWidth = 0;
        mVideoHeight = 0;
    }


    @Override
    public int getVideoSarNum() {
        return mVideoSarNum;
    }

    @Override
    public int getVideoSarDen() {
        return mVideoSarDen;
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public void setSurface(Surface var1) {
        if (mediaPlayer != null && !mediaPlayer.getVLCVout().areViewsAttached()) {
            mediaPlayer.getVLCVout().setVideoSurface(var1, null);
            mediaPlayer.getVLCVout().attachViews(layoutListener);
        }
    }



    public void voutViewsAttach() {
        if(mediaPlayer != null && !mediaPlayer.getVLCVout().areViewsAttached())
            mediaPlayer.getVLCVout().attachViews(this.layoutListener);
    }


    public void voutViewsDetach() {
        if(mediaPlayer != null && mediaPlayer.getVLCVout().areViewsAttached())
            mediaPlayer.getVLCVout().detachViews();
    }


    public void setVoutSize(int w, int h) {
        if (mediaPlayer != null && mediaPlayer.getVLCVout() != null) {
            mediaPlayer.getVLCVout().setWindowSize(w, h);
        }
    }

    public void setSpeed(float speed) {
        if (mediaPlayer != null)
            mediaPlayer.setRate(speed);
    }

}
