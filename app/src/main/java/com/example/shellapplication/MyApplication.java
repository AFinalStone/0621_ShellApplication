package com.example.shellapplication;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {
    public static String TAG = "MyApplication==========";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        System.out.println("MyApplication===============attachBaseContext");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("MyApplication===============onCreate");
    }
}
