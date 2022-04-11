package com.proxygtzbabc.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;

/**
 * @author syl
 * @time 2021/12/28 14:38
 */
public class Main {
    public static String channel = "mixinzhibo";  //  有qlbf,mixinzhibo,mixin,guotang 可以选择
    public static String SIGN_FINE_NAME = "qhff";  //

    public static String destPath = "app/" + channel + "/release";
    public static String FILENAME = destPath + "/app-" + channel + "-release";
    // resGuard混淆后的包文件位置
    public static String resguardReleasePath = "app/build/outputs/apk/" + channel + "/release/AndResGuard_app-" + channel + "-release/app-" + channel + "-release_7zip_aligned_signed.apk";

    public static void main(String[] args) throws Exception {
        // 复制resGuard混淆后的apk包 复制到指定的位置
        FileUtils.copyFile(resguardReleasePath, destPath, "app-" + channel + "-release.apk");
        /**
         * 1、制作只包含解密代码的dex 文件
         */
        //1.1 解压aar 获得classes.jar
        File aarFile = new File("Proxy_Guard_Core/build/outputs/aar/Proxy_Guard_Core-release.aar");
        File aarTemp = new File("Proxy_Guard_Tools/temp");
        Zip.unZip(aarFile, aarTemp);
        File classesJar = new File(aarTemp, "classes.jar");
        //1.2 执行dx命令 将jar变成dex文件
        File classesDex = new File(aarTemp, "classes.dex");
        Process process = Runtime.getRuntime().exec("cmd /c dx --dex --output " + classesDex
                .getAbsolutePath() + " " +
                classesJar.getAbsolutePath());
        process.waitFor();
        //失败
        if (process.exitValue() != 0) {
            throw new RuntimeException("dex error");
        }

        /**
         * 2、加密apk中所有dex文件
         */
        File apkFile = new File(FILENAME + ".apk");
        File apkTemp = new File("app/build/outputs/apk/temp");
        Zip.unZip(apkFile, apkTemp);
        File[] dexFiles = apkTemp.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".dex");
            }
        });
//        File newClassDecryptFileDir = new File(apkTemp, "xed");
//        for (File dex : dexFiles) {
//            if (!newClassDecryptFileDir.exists()) {
//                newClassDecryptFileDir.mkdirs();
//            }
//            dex.renameTo(new File(newClassDecryptFileDir, dex.getName()));
//            dex.delete();
//        }
//        Zip.zip(newClassDecryptFileDir, new File(apkTemp, "xed.xed"));
//        for (File file : newClassDecryptFileDir.listFiles()) {
//            file.delete();
//        }
//        newClassDecryptFileDir.delete();
        for (File dex : dexFiles) {
            byte[] bytes = getBytes(dex);
            byte[] encrypt = EncryptUtils.getInstance().encrypt(bytes);
            FileOutputStream fos = new FileOutputStream(new File(apkTemp, SIGN_FINE_NAME + dex.getName().replace("dex", "xed")));
            fos.write(encrypt);
            fos.flush();
            fos.close();
            dex.delete();
        }

        /**
         * 3、把classes.dex 放入 apk解压目录 在压缩成apk
         */
        classesDex.renameTo(new File(apkTemp, "classes.dex"));
        File unSignedApk = new File("app/build/outputs/apk/app-unsigned.apk");
        Zip.zip(apkTemp, unSignedApk);

        /**
         * 4、对齐与签名
         */
        File alignedApk = new File("app/build/outputs/apk/app-unsigned-aligned.apk");
        process = Runtime.getRuntime().exec("cmd /c zipalign -f 4 " + unSignedApk
                .getAbsolutePath() + " " +
                alignedApk.getAbsolutePath());
        process.waitFor();
        //失败
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("zipalign error");
        }

        //4.2 签名
//        apksigner sign  --ks jks文件地址 --ks-key-alias 别名 --ks-pass pass:jsk密码 --key-pass
// pass:别名密码 --out  out.apk in.apk
        File signedApk = new File(FILENAME + "-signed-aligned.apk");
        File jks = new File("app/signature/" + SIGN_FINE_NAME + ".jks");
        process = Runtime.getRuntime().exec("cmd /c apksigner sign  --ks " + jks.getAbsolutePath
                () + " --ks-key-alias yeyan --ks-pass pass:yeyan123 --key-pass  pass:yeyan123 --out" +
                " " + signedApk.getAbsolutePath() + " " + alignedApk.getAbsolutePath());
        process.waitFor();
        //失败
        if (process.exitValue() != 0) {
            throw new RuntimeException("apksigner error");
        }

    }

    public static byte[] getBytes(File file) throws Exception {
        RandomAccessFile r = new RandomAccessFile(file, "r");
        byte[] buffer = new byte[(int) r.length()];
        r.readFully(buffer);
        r.close();
        return buffer;
    }
}
