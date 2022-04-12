package com.example.shellapplication;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity==========";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        mParent = null;
        findViewById(R.id.btn_to_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test03();
            }
        });

        ClassLoader classLoader = getClassLoader();
        while (classLoader != null) {
            Log.d(TAG, String.valueOf(classLoader));
            classLoader = classLoader.getParent();
        }
    }

    public void test01() {
        Intent intent = new Intent(MainActivity.this, SecoundActivity.class);
        intent.setClass(MainActivity.this, SecoundActivity.class);
        String className = SecoundActivity.class.getName();
        intent.setClassName(getPackageName(), className);
        Activity activity = getParent();
        System.out.println(activity);
        startActivity(intent);
    }

    public void test02() {
        try {
            Class<ActivityManager> clazz = ActivityManager.class;
            Method method_IActivityManager = clazz.getDeclaredMethod("getService");
            method_IActivityManager.setAccessible(true);
            Object object = method_IActivityManager.invoke(null);
            Log.d(TAG, String.valueOf(object));//android.app.IActivityManager$Stub$Proxy@6c215ac
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void test03() {
        try {
            Class clazz = Class.forName("android.app.IActivityManager");
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                Log.d(TAG, String.valueOf(method));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}