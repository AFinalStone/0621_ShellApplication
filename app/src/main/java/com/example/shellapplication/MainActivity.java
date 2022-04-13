package com.example.shellapplication;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shellapplication.media.RawUtil;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    public static String TAG = "MediaPlayActivity==========";

    private MainActivity mActivity;
    private SurfaceView mSurfaceView;
    MediaPlayer mPlayer;
    boolean mIsInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surface_view);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!mIsInit) {
            mIsInit = true;
            try {
                AssetFileDescriptor afd = RawUtil.getRawResFile(mActivity, R.raw.base_login_bg, "base_login_bg");
                mPlayer = new MediaPlayer();
                final AudioAttributes aa = new AudioAttributes.Builder().build();
                mPlayer.setAudioAttributes(aa);
                mPlayer.setAudioSessionId(0);
                mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mPlayer.setLooping(true);
                mPlayer.prepare();
                mPlayer.setDisplay(mSurfaceView.getHolder());
                mPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }
}