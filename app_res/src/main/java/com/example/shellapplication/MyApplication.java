package com.example.shellapplication;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.example.shellapplication.resload.PluginApkUtil;

import java.io.File;

public class MyApplication extends Application {
    public static String TAG = "MyApplication==========";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        System.out.println("MyApplication===============attachBaseContext");
        PluginApkUtil.init(this);
        loadApkPluginResource();
    }

    private void loadApkPluginResource() {
        File apkResFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/app_resource.zip");
        if (apkResFile.exists()) {
            PluginApkUtil.getInstance().loadAppPluginResource(apkResFile.getAbsolutePath());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("MyApplication===============onCreate");
    }
}
