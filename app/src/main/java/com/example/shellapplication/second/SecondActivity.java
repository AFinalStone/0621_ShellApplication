package com.example.shellapplication.second;

import android.app.ActivityManager;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shellapplication.MainActivity;
import com.example.shellapplication.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SecondActivity extends AppCompatActivity {

    public static String TAG = "MainActivity==========";
    private SurfaceView mSurfaceView;
    MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        String packageName = getPackageName();
        Log.d(TAG, "packageName = " + packageName);
        ClassLoader classLoader = getClassLoader();
        while (classLoader != null) {
            Log.d(TAG, String.valueOf(classLoader));
            classLoader = classLoader.getParent();
        }
    }

    public void test01() {
        Intent intent = new Intent(SecondActivity.this, MainActivity.class);
        intent.setClass(SecondActivity.this, MainActivity.class);
        String className = MainActivity.class.getName();
        String packageName = getPackageName();
        intent.setClassName(packageName, className);
        android.app.Activity activity = getParent();
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
        Class clazz = null;
        try {
            Log.d(TAG, "-----------------------------------------------");
            clazz = Class.forName("android.app.IActivityManager");
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                Log.d(TAG, String.valueOf(method));
            }
            Log.d(TAG, "-----------------------------------------------");
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Log.d(TAG, String.valueOf(field));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}