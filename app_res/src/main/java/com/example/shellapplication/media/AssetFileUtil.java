package com.example.shellapplication.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetFileUtil {

    /**
     * 获取asset文件夹下面的文件
     *
     * @param context
     * @param fileName
     * @return
     */
    public static AssetFileDescriptor getAssetFile(Context context, String fileName) {
//        try {
//            return context.getAssets().openFd(fileName);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e("jiagu", fileName + " 读取异常!，尝试拷贝读取...");
//        }
        File cacheFile = new File(context.getCacheDir(), fileName);
        try {
            if (cacheFile.exists()) {
                return new AssetFileDescriptor(ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY), 0, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("jiagu", "缓存资源异常!");
            return null;
        }
        try {
            cacheFile.getParentFile().mkdirs();
            //把文件拷贝到缓存目录
            copyAssetFileToCacheFile(context, fileName, cacheFile);
            return new AssetFileDescriptor(ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY), 0, -1);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("jiagu", "拷贝资源文件未找到异常!");
        }
        return null;
    }

    private static void copyAssetFileToCacheFile(Context context, String assetPath, File cacheFile) throws IOException {
        final InputStream inputStream = context.getAssets().open(assetPath, AssetManager.ACCESS_BUFFER);
        FileOutputStream fos = new FileOutputStream(cacheFile, false);
        int b = 0;
        // 判断是否到文件结尾
        while ((b = inputStream.read()) != -1) {
            fos.write(b);
            fos.flush();
        }// 关闭源， 先开的后关，后开的先关
        fos.close();
        inputStream.close();
    }

    /**
     * 获取raw文件夹下面的文件
     *
     * @param context
     * @param fileResId
     * @return
     */
    public static AssetFileDescriptor getRawResFile(Context context, int fileResId, String fileResName) {
        //不存在缓存则正常获取
//        try {
//            return context.getResources().openRawResourceFd(fileResId);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e("jiagu", fileResId + " 读取异常!，尝试拷贝读取...");
//        }
//        String app_name = YRBaseBizAppLike.getInstance().getProxyApplication_app_name();
//        String app_version = YRBaseBizAppLike.getInstance().getProxyApplication_app_version();
//        File cacheFile = new File(context.getCacheDir().getAbsolutePath() + "/" + app_name + "_" + app_version + "/res/raw/" + fileResName);
//        System.out.println("ProxyApplication===============getRawResFile= " + cacheFile.getAbsolutePath());
        File cacheFile = new File(context.getCacheDir(), fileResName);
        try {
            //先判断是否存在缓存
            if (cacheFile.exists()) {
                return new AssetFileDescriptor(ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY), 0, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            cacheFile.getParentFile().mkdirs();
            //把文件拷贝到缓存目录
            copyRawFileToCacheFile(context, fileResId, cacheFile);
            return new AssetFileDescriptor(ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY), 0, -1);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("jiagu", "拷贝资源文件未找到异常!");
        }
        return null;
    }

    private static void copyRawFileToCacheFile(Context context, int resId, final File cacheFile) throws IOException {
        final InputStream inputStream = context.getResources().openRawResource(resId);
        FileOutputStream fos = new FileOutputStream(cacheFile, false);
        int b = 0;
        // 判断是否到文件结尾
        while ((b = inputStream.read()) != -1) {
            fos.write(b);
            fos.flush();
        }// 关闭源， 先开的后关，后开的先关
        fos.close();
        inputStream.close();
    }
}
