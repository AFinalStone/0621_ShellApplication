package com.proxygtzbabc.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static void copyFile(String srcPath, String destPath, String destFileName) {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        if (!srcFile.exists()) {
            throw new RuntimeException("can not find original file");
        }
        // clear all file in destPath
        deleteFile(destFile);
        if (srcFile.isFile()) {
            if (destFile.isFile()) {
                copy(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
            } else if (destFile.isDirectory()) {
                File newFile;
                if (destFileName != null && destFileName.length() > 0) {
                    newFile = new File(destFile, destFileName);
                } else {
                    newFile = new File(destFile, srcFile.getName());
                }
                copy(srcFile.getAbsolutePath(), newFile.getAbsolutePath());
            }

        } else if (srcFile.isDirectory()) {
            if (destFile.isFile()) {
                throw new RuntimeException("destFile is File not Files");
            } else if (destFile.isDirectory()) {
                File fs[] = srcFile.listFiles();
                for (File f : fs) {
                    File newFile = new File(destFile, f.getName());
                    if (f.isDirectory()) {
                        copyFile(f.getAbsolutePath(), newFile.getAbsolutePath(), "");
                    }
                    if (f.isFile()) {
                        copy(f.getAbsolutePath(), newFile.getAbsolutePath());
                    }
                }
            }

        }

    }

    private static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteFile(f);
            }
        } else {
            file.delete();
        }
    }

    public static void copy(String srcPth, String destPath) {
        try {
            FileInputStream fis = new FileInputStream(srcPth);
            FileOutputStream fos = new FileOutputStream(destPath);
            byte datas[] = new byte[1024 * 8];
            int len = 0;
            while ((len = fis.read(datas)) != -1) {
                fos.write(datas, 0, len);
            }
            fis.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
