package com.proxygtzbabc.core;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 代替真正的APPLication
 *
 * @author syl
 * @time 2021/12/24 11:14
 */
public class ProxyApplication extends Application {

    private String app_name;//应用名称
    private String app_package;//应用包名
    private String app_version;//应用版本号

    /**
     * ActivityThread 创建Application之后调用的第一个函数
     *
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        System.out.println("ProxyApplication===============attachBaseContext");
        String currentProcessName = getCurrentProcessName();
        System.out.println("ProxyApplication===============currentProcessName=" + currentProcessName);
        getMetaData();
        File apkFile = new File(getApplicationInfo().sourceDir);
        File versionDir = new File(getCacheDir().getAbsolutePath() + "/" + app_name + "_" + app_version);
        File dexDir = new File(versionDir, "dexDir");
        List<File> dexFilesList = new ArrayList<>();
        if (!dexDir.exists() || dexDir.list().length == 0) {
            Zip.unZip(apkFile, versionDir);
            File[] files = versionDir.listFiles();
            if (files != null && files.length > 0)
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith(".xed")) {
                        try {
                            byte[] bytes = Utils.getBytes(file);
                            byte[] decrypt = EncryptUtils.getInstance().decrypt(bytes);
                            if (!dexDir.exists()) {
                                dexDir.mkdirs();
                            }
                            File fileDex = new File(dexDir, file.getName());
                            FileOutputStream fos = new FileOutputStream(fileDex);
                            fos.write(decrypt);
                            fos.flush();
                            fos.close();
                            dexFilesList.add(fileDex);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        } else {
            //已经解密过了
            for (File file : dexDir.listFiles()) {
                dexFilesList.add(file);
            }
        }
        try {
            loadDex(dexFilesList, versionDir);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("ProxyApplication===============onCreate");
        String currentProcessName = getCurrentProcessName();
        System.out.println("ProxyApplication===============currentProcessName=" + currentProcessName);
        if (!TextUtils.isEmpty(app_package) && app_package.equals(currentProcessName)) {
            try {
                bindRealApplication();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    boolean isBindReal;
    Application delegate;

    public void bindRealApplication() throws Exception {
        if (isBindReal) {
            return;
        }
        //如果用户(使用这个库的开发者) 没有配置Application 就不用管了
        if (TextUtils.isEmpty(app_name)) {
            return;
        }
        //这个就是attachBaseContext传进来的 ContextImpl
        Context baseContext = getBaseContext();
        //反射创建出真实的 用户 配置的Application
        Class<?> delegateClass = Class.forName(app_name);
        delegate = (Application) delegateClass.newInstance();
        //反射获得 attach函数
        Method attach = Application.class.getDeclaredMethod("attach", Context.class);
        //设置允许访问
        attach.setAccessible(true);
        attach.invoke(delegate, baseContext);

        /**
         *  替换
         *  ContextImpl -> mOuterContext ProxyApplication->MyApplication
         */
        Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
        //获得 mOuterContext 属性
        Field mOuterContextField = contextImplClass.getDeclaredField("mOuterContext");
        mOuterContextField.setAccessible(true);
        mOuterContextField.set(baseContext, delegate);


        /**
         * ActivityThread  mAllApplications 与 mInitialApplication
         */
        //获得ActivityThread对象 ActivityThread 可以通过 ContextImpl 的 mMainThread 属性获得
        Field mMainThreadField = contextImplClass.getDeclaredField("mMainThread");
        mMainThreadField.setAccessible(true);
        Object mMainThread = mMainThreadField.get(baseContext);

        //替换 mInitialApplication
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Field mInitialApplicationField = activityThreadClass.getDeclaredField
                ("mInitialApplication");
        mInitialApplicationField.setAccessible(true);
        mInitialApplicationField.set(mMainThread, delegate);

        //替换 mAllApplications
        Field mAllApplicationsField = activityThreadClass.getDeclaredField
                ("mAllApplications");
        mAllApplicationsField.setAccessible(true);
        ArrayList<Application> mAllApplications = (ArrayList<Application>) mAllApplicationsField.get(mMainThread);
        mAllApplications.remove(this);
        mAllApplications.add(delegate);


        /**
         * LoadedApk -> mApplication ProxyApplication
         */
        //LoadedApk 可以通过 ContextImpl 的 mPackageInfo 属性获得
        Field mPackageInfoField = contextImplClass.getDeclaredField("mPackageInfo");
        mPackageInfoField.setAccessible(true);
        Object mPackageInfo = mPackageInfoField.get(baseContext);

        Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
        Field mApplicationField = loadedApkClass.getDeclaredField("mApplication");
        mApplicationField.setAccessible(true);
        mApplicationField.set(mPackageInfo, delegate);

        //修改ApplicationInfo className LoadedApk
        Field mApplicationInfoField = loadedApkClass.getDeclaredField("mApplicationInfo");
        mApplicationInfoField.setAccessible(true);
        ApplicationInfo mApplicationInfo = (ApplicationInfo) mApplicationInfoField.get(mPackageInfo);
        mApplicationInfo.className = app_name;

        delegate.onCreate();
        isBindReal = true;
    }

    @Override
    public String getPackageName() {
        //如果meta-data 设置了 application
        //让ContentProvider创建的时候使用的上下文 在ActivityThread中的installProvider函数
        //命中else
        if (!TextUtils.isEmpty(app_name)) {
            return "";
        }
        return super.getPackageName();
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        if (TextUtils.isEmpty(app_name)) {
            return super.createPackageContext(packageName, flags);
        }
        try {
            bindRealApplication();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return delegate;
    }


    /**
     * 加载dex文件集合
     *
     * @param dexFiles
     */
    private void loadDex(List<File> dexFiles, File optimizedDirectory) throws
            NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        /**
         * 1.获得 系统 classloader中的dexElements数组
         */
        //1.1  获得classloader中的pathList => DexPathList
        Field pathListField = Utils.findField(getClassLoader(), "pathList");
        Object pathList = pathListField.get(getClassLoader());
        //1.2 获得pathList类中的 dexElements
        Field dexElementsField = Utils.findField(pathList, "dexElements");
        Object[] dexElements = (Object[]) dexElementsField.get(pathList);
        /**
         * 2.创建新的 element 数组 -- 解密后加载dex
         */
        //需要适配
        Method makeDexElements = null;
        Object[] addElements;
        ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT <
                Build.VERSION_CODES.M) {
            //5.x
            makeDexElements = Utils.findMethod(pathList, "makeDexElements", ArrayList.class,
                    File.class, ArrayList.class);
            addElements = (Object[]) makeDexElements.invoke(pathList, dexFiles,
                    optimizedDirectory,
                    suppressedExceptions);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //6.x
            makeDexElements = Utils.findMethod(pathList, "makePathElements", List.class,
                    File.class, List.class);
            addElements = (Object[]) makeDexElements.invoke(pathList, dexFiles,
                    optimizedDirectory,
                    suppressedExceptions);
        } else {
            makeDexElements = Utils.findMethod(pathList, "makeDexElements",
                    List.class, File.class, List.class, ClassLoader.class);
            Field definingContextField = Utils.findField(pathList, "definingContext");
            ClassLoader definingContext = (ClassLoader) definingContextField.get(pathList);
            addElements = (Object[]) makeDexElements.invoke(pathList, dexFiles, optimizedDirectory, suppressedExceptions, definingContext);
        }


        /**
         * 3.合并两个数组
         */
        //创建一个数组
        Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass()
                .getComponentType(), dexElements.length +
                addElements.length);
        System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);
        System.arraycopy(addElements, 0, newElements, dexElements.length, addElements.length);
        /**
         * 4.替换classloader中的 element数组
         */
        dexElementsField.set(pathList, newElements);
    }

    public void getMetaData() {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                    (getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            //是否设置app_name 与 app_version
            if (null != metaData) {
                //是否存在name为app_name的meta-data数据
                if (metaData.containsKey("app_name")) {
                    app_name = metaData.getString("app_name");
                    System.out.println("ProxyApplication===============app_name = " + app_name);
                }
                if (metaData.containsKey("app_version")) {
                    app_version = metaData.getString("app_version");
                    System.out.println("ProxyApplication===============app_version = " + app_version);
                }
                if (metaData.containsKey("app_package")) {
                    app_package = metaData.getString("app_package");
                    System.out.println("ProxyApplication===============app_package = " + app_package);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取当前进程名称
     *
     * @return
     */
    private String getCurrentProcessName() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
        if (list == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : list) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }
}
