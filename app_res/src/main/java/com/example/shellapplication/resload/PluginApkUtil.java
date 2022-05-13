package com.example.shellapplication.resload;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PluginApkUtil {

    private static final String TAG = "PluginApkUtil";
    private static PluginApkUtil INSTANCE;
    private Application mApplication;

    private static final String TEST_ASSETS_VALUE = "only_use_to_test_tinker_resource.txt";

    // original object
    private static Collection<WeakReference<Resources>> references = null;
    private static Object currentActivityThread = null;
    private static AssetManager newAssetManager = null;

    // method
    private static Method addAssetPathMethod = null;
    private static Method addAssetPathAsSharedLibraryMethod = null;
    private static Method ensureStringBlocksMethod = null;

    // field
    private static Field assetsFiled = null;
    private static Field resourcesImplFiled = null;
    private static Field resDir = null;
    private static Field packagesFiled = null;
    private static Field resourcePackagesFiled = null;
    private static Field publicSourceDirField = null;
    private static Field stringBlocksField = null;

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
        if (checkCanLoadRes()) {
            loadResource(apkPluginPath);
        }
    }

    private boolean checkCanLoadRes() {
        try {
//   - Replace mResDir to point to the external resource file instead of the .apk. This is
            //     used as the asset path for new Resources objects.
            //   - Set Application#mLoadedApk to the found LoadedApk instance

            // Find the ActivityThread instance for the current thread
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            currentActivityThread = ShareReflectUtil.getActivityThread(mApplication, activityThread);

            // API version 8 has PackageInfo, 10 has LoadedApk. 9, I don't know.
            Class<?> loadedApkClass;
            try {
                loadedApkClass = Class.forName("android.app.LoadedApk");
            } catch (ClassNotFoundException e) {
                loadedApkClass = Class.forName("android.app.ActivityThread$PackageInfo");
            }

            resDir = ShareReflectUtil.findField(loadedApkClass, "mResDir");
            packagesFiled = ShareReflectUtil.findField(activityThread, "mPackages");
            if (SDK_INT < 27) {
                resourcePackagesFiled = ShareReflectUtil.findField(activityThread, "mResourcePackages");
            }

            // Create a new AssetManager instance and point it to the resources
            final AssetManager assets = mApplication.getAssets();
            addAssetPathMethod = ShareReflectUtil.findMethod(assets, "addAssetPath", String.class);
            if (shouldAddSharedLibraryAssets(mApplication.getApplicationInfo())) {
                addAssetPathAsSharedLibraryMethod =
                        ShareReflectUtil.findMethod(assets, "addAssetPathAsSharedLibrary", String.class);
            }

            // Kitkat needs this method call, Lollipop doesn't. However, it doesn't seem to cause any harm
            // in L, so we do it unconditionally.
            try {
                stringBlocksField = ShareReflectUtil.findField(assets, "mStringBlocks");
                ensureStringBlocksMethod = ShareReflectUtil.findMethod(assets, "ensureStringBlocks");
            } catch (Throwable ignored) {
                // Ignored.
            }

            // Use class fetched from instance to avoid some ROMs that use customized AssetManager
            // class. (e.g. Baidu OS)
            newAssetManager = (AssetManager) ShareReflectUtil.findConstructor(assets).newInstance();

            // Iterate over all known Resources objects
            if (SDK_INT >= KITKAT) {
                //pre-N
                // Find the singleton instance of ResourcesManager
                final Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
                final Method mGetInstance = ShareReflectUtil.findMethod(resourcesManagerClass, "getInstance");
                final Object resourcesManager = mGetInstance.invoke(null);
                try {
                    Field fMActiveResources = ShareReflectUtil.findField(resourcesManagerClass, "mActiveResources");
                    final ArrayMap<?, WeakReference<Resources>> activeResources19 =
                            (ArrayMap<?, WeakReference<Resources>>) fMActiveResources.get(resourcesManager);
                    references = activeResources19.values();
                } catch (NoSuchFieldException ignore) {
                    // N moved the resources to mResourceReferences
                    final Field mResourceReferences = ShareReflectUtil.findField(resourcesManagerClass, "mResourceReferences");
                    references = (Collection<WeakReference<Resources>>) mResourceReferences.get(resourcesManager);
                }
            } else {
                final Field fMActiveResources = ShareReflectUtil.findField(activityThread, "mActiveResources");
                final HashMap<?, WeakReference<Resources>> activeResources7 =
                        (HashMap<?, WeakReference<Resources>>) fMActiveResources.get(currentActivityThread);
                references = activeResources7.values();
            }
            // check resource
            if (references == null) {
                throw new IllegalStateException("resource references is null");
            }

            final Resources resources = mApplication.getResources();

            // fix jianGuo pro has private field 'mAssets' with Resource
            // try use mResourcesImpl first
            if (SDK_INT >= 24) {
                try {
                    // N moved the mAssets inside an mResourcesImpl field
                    resourcesImplFiled = ShareReflectUtil.findField(resources, "mResourcesImpl");
                } catch (Throwable ignore) {
                    // for safety
                    assetsFiled = ShareReflectUtil.findField(resources, "mAssets");
                }
            } else {
                assetsFiled = ShareReflectUtil.findField(resources, "mAssets");
            }

            try {
                publicSourceDirField = ShareReflectUtil.findField(ApplicationInfo.class, "publicSourceDir");
            } catch (NoSuchFieldException ignore) {
                // Ignored.
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, "Exception = " + e.getMessage());
            return false;
        }
        return true;
    }

    private void loadResource(String apkPluginPath) {
        try {
            if (apkPluginPath == null) {
                return;
            }

            final ApplicationInfo appInfo = mApplication.getApplicationInfo();

            final Field[] packagesFields;
            if (Build.VERSION.SDK_INT < 27) {
                packagesFields = new Field[]{packagesFiled, resourcePackagesFiled};
            } else {
                packagesFields = new Field[]{packagesFiled};
            }
            for (Field field : packagesFields) {
                final Object value = field.get(currentActivityThread);

                for (Map.Entry<String, WeakReference<?>> entry
                        : ((Map<String, WeakReference<?>>) value).entrySet()) {
                    final Object loadedApk = entry.getValue().get();
                    if (loadedApk == null) {
                        continue;
                    }
                    final String resDirPath = (String) resDir.get(loadedApk);
                    if (appInfo.sourceDir.equals(resDirPath)) {
                        resDir.set(loadedApk, apkPluginPath);
                    }
                }
            }

            // Create a new AssetManager instance and point it to the resources installed under
            if (((Integer) addAssetPathMethod.invoke(newAssetManager, apkPluginPath)) == 0) {
                throw new IllegalStateException("Could not create new AssetManager");
            }

            // Add SharedLibraries to AssetManager for resolve system resources not found issue
            // This influence SharedLibrary Package ID
            if (shouldAddSharedLibraryAssets(appInfo)) {
                for (String sharedLibrary : appInfo.sharedLibraryFiles) {
                    if (!sharedLibrary.endsWith(".apk")) {
                        continue;
                    }
                    if (((Integer) addAssetPathAsSharedLibraryMethod.invoke(newAssetManager, sharedLibrary)) == 0) {
                        throw new IllegalStateException("AssetManager add SharedLibrary Fail");
                    }
                    Log.i(TAG, "addAssetPathAsSharedLibrary " + sharedLibrary);
                }
            }

            // Kitkat needs this method call, Lollipop doesn't. However, it doesn't seem to cause any harm
            // in L, so we do it unconditionally.
            if (stringBlocksField != null && ensureStringBlocksMethod != null) {
                stringBlocksField.set(newAssetManager, null);
                ensureStringBlocksMethod.invoke(newAssetManager);
            }

            for (WeakReference<Resources> wr : references) {
                final Resources resources = wr.get();
                if (resources == null) {
                    continue;
                }
                // Set the AssetManager of the Resources instance to our brand new one
                try {
                    //pre-N
                    assetsFiled.set(resources, newAssetManager);
                } catch (Throwable ignore) {
                    // N
                    final Object resourceImpl = resourcesImplFiled.get(resources);
                    // for Huawei HwResourcesImpl
                    final Field implAssets = ShareReflectUtil.findField(resourceImpl, "mAssets");
                    implAssets.set(resourceImpl, newAssetManager);
                }

                clearPreloadTypedArrayIssue(resources);

                resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
            }

            // Handle issues caused by WebView on Android N.
            // Issue: On Android N, if an activity contains a webview, when screen rotates
            // our resource patch may lost effects.
            // for 5.x/6.x, we found Couldn't expand RemoteView for StatusBarNotification Exception
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    if (publicSourceDirField != null) {
                        publicSourceDirField.set(mApplication.getApplicationInfo(), apkPluginPath);
                    }
                } catch (Throwable ignore) {
                    // Ignored.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Why must I do these?
     * Resource has mTypedArrayPool field, which just like Message Poll to reduce gc
     * MiuiResource change TypedArray to MiuiTypedArray, but it get string block from offset instead of assetManager
     */
    private static void clearPreloadTypedArrayIssue(Resources resources) {
        // Perform this trick not only in Miui system since we can't predict if any other
        // manufacturer would do the same modification to Android.
//        if (!isMiuiSystem) {
//            return;
//        }

        Log.w(TAG, "try to clear typedArray cache!");
        // Clear typedArray cache.
        try {
            Field typedArrayPoolField = ShareReflectUtil.findField(Resources.class, "mTypedArrayPool");

            final Object origTypedArrayPool = typedArrayPoolField.get(resources);

            Field poolField = ShareReflectUtil.findField(origTypedArrayPool, "mPool");

            final Constructor<?> typedArrayConstructor = origTypedArrayPool.getClass().getConstructor(int.class);
            typedArrayConstructor.setAccessible(true);
            final int poolSize = ((Object[]) poolField.get(origTypedArrayPool)).length;
            final Object newTypedArrayPool = typedArrayConstructor.newInstance(poolSize);
            typedArrayPoolField.set(resources, newTypedArrayPool);
        } catch (Throwable ignored) {
            Log.e(TAG, "clearPreloadTypedArrayIssue failed, ignore error: " + ignored);
        }
    }

    private static boolean shouldAddSharedLibraryAssets(ApplicationInfo applicationInfo) {
        return SDK_INT >= Build.VERSION_CODES.N && applicationInfo != null &&
                applicationInfo.sharedLibraryFiles != null;
    }

}
