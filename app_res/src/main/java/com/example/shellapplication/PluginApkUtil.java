package com.example.shellapplication;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PluginApkUtil {


    private static PluginApkUtil INSTANCE;
    private Application mApplication;
    private AssetManager mAssetManager;
    private Resources mResource;
    private Resources.Theme mTheme;

    private PluginApkUtil(Application application) {
        mApplication = application;
    }

    public static void init(Application application) {
        if (INSTANCE == null) {
            INSTANCE = new PluginApkUtil(application);
        }
    }

    public static PluginApkUtil getInstance() {
        return INSTANCE;
    }

    /**
     * @param apkPluginPath
     */
    public void loadAppPluginResource(String apkPluginPath) {
        try {
            //加载插件中的资源
            Resources oldResource = mApplication.getResources();
            mAssetManager = AssetManager.class.newInstance();
            Method method_addAssetPath = mAssetManager.getClass().getMethod("addAssetPath", String.class);
            method_addAssetPath.invoke(mAssetManager, apkPluginPath);
            mResource = new Resources(mAssetManager, oldResource.getDisplayMetrics(), oldResource.getConfiguration());
            mTheme = mResource.newTheme();
            mTheme.setTo(mApplication.getTheme());
            System.out.println("===============" + mApplication.getClass());
            //获取contextImpl
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            Method method = contextImplClass.getDeclaredMethod("getImpl", Context.class);
            method.setAccessible(true);
            Object contextImplObject = method.invoke(null, mApplication);
            //获取LoadedApk
            Field field_packageInfo = contextImplClass.getDeclaredField("mPackageInfo");
            field_packageInfo.setAccessible(true);
            Object loadedApkObj = field_packageInfo.get(contextImplObject);
            Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
            //获取mResDir信息
            Field resDir = loadedApkClass.getDeclaredField("mResDir");
            resDir.setAccessible(true);
            resDir.set(loadedApkObj, apkPluginPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AssetManager getAssetManager() {
        return mAssetManager;
    }

    public Resources getResource() {
        return mResource;
    }

    public Resources.Theme getTheme() {
        return mTheme;
    }

}
