package com.example.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;

/**
 * @author syl
 * @time 2021/12/28 14:38
 */
public class Main_Res {
    public static String channel = "jiagu";  //  有qlbf,mixinzhibo,mixin,guotang 可以选择
    public static String SIGN_FINE_NAME = "gtzbabc";  //

    public static String destPath = "app_res/" + channel + "/debug";
    public static String FILENAME = destPath + "/app-" + channel + "-debug";

    public static void main(String[] args) throws Exception {
        // 复制resGuard混淆后的apk包 复制到指定的位置
//        FileUtils.copyFile(resguardReleasePath, destPath, "app-" + channel + "-release.apk");
        /**
         * 1、制作只包含解密代码的dex 文件
         */
        //1.1 解压aar 获得classes.jar
        File arscFile = new File("Proxy_Guard_Resource/build/outputs/apk/release/Proxy_Guard_Resource-release.apk");
        File arscTempRes = new File("Proxy_Guard_Tools/temp_res");
        Zip.unZipApk(arscFile, arscTempRes);

        /**
         * 2、加密apk中所有的zip文件
         */
        File apkFile = new File(FILENAME + ".apk");
        File apkTemp = new File("app_res/build/outputs/apk/temp");
//        Zip.unZip(apkFile, apkTemp);
        //压缩apk中的zip文件
        File[] resFiles = apkTemp.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".zip");
            }
        });
        if (resFiles != null) {
            for (File res : resFiles) {
                byte[] bytes = getBytes(res);
                byte[] encrypt = EncryptUtils.getInstance().encrypt(bytes);
                String childFileName = res.getName().replace("zip", "piz");
                FileOutputStream fos = new FileOutputStream(new File(apkTemp, childFileName));
                fos.write(encrypt);
                fos.flush();
                fos.close();
                res.delete();
            }
        }

//        /**
//         * 3、替换reouces.arsc
//         */
//        File apkResArsc = new File(apkTemp, "resources.arsc");
//        File resArsc = new File(arscTempRes, "resources.arsc");
//        apkResArsc.delete();
//        resArsc.renameTo(apkResArsc);
        File unSignedApk = new File("app_res/build/outputs/apk/app-unsigned.apk");
        Zip.zip(apkTemp, unSignedApk);

        /**
         * 4、对齐与签名
         */
        File alignedApk = new File("app_res/build/outputs/apk/app-unsigned-aligned.apk");
        Process process = Runtime.getRuntime().exec("cmd /c zipalign -f 4 " + unSignedApk
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
        File jks = new File("app_res/signature/" + SIGN_FINE_NAME + ".jks");
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
