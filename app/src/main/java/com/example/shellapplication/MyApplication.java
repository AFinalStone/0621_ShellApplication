package com.example.shellapplication;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
