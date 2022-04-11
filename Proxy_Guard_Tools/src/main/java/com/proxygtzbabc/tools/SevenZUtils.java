package com.proxygtzbabc.tools;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SevenZUtils {
    /**
     * @param inCompressFile 需要解压的7zip文件
     * @param outputDir      解压后的文件的存储目录
     * @throws IOException
     */
    public static void unCompress(File inCompressFile, File outputDir) throws IOException {
        if (inCompressFile == null || !inCompressFile.exists()) {
            throw new RuntimeException("Invalid outputDir:" + outputDir);
        }
        if (outputDir == null || !outputDir.exists() || !outputDir.isDirectory()) {
            outputDir.mkdirs();
        }

        SevenZFile sevenZFile = new SevenZFile(inCompressFile);
        SevenZArchiveEntry entry = null;
        while ((entry = sevenZFile.getNextEntry()) != null) {
            String entryName = entry.getName();
            if (entry.isDirectory()) {// //handle dir
                File file = new File(outputDir, entryName);
                file.mkdirs();
                continue;
            }
            int index = entryName.lastIndexOf(File.separator);
            String entryPath = index == -1 ? "" : entryName.substring(0, index);
            File file = new File(outputDir, entryPath);
            if (!file.exists() || !file.isDirectory()) file.mkdirs();
            File newFile = new File(outputDir, entryName);
            OutputStream outputStream = null;

            try {
                outputStream = new FileOutputStream(newFile);
                int len = -1;
                byte[] buffer = new byte[2048];
                while ((len = sevenZFile.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            }
        }
    }

    /**
     * 压缩
     *
     * @param outputFile
     * @param inputFiles
     * @throws IOException
     */
    public static void compress(File outputFile, File... inputFiles) throws IOException {
        if (inputFiles.length == 0) {
            throw new RuntimeException("InputFiles is null.");
        }
        for (File inputFile : inputFiles) {
            if (!inputFile.exists()) {
                throw new RuntimeException("InputFile:" + inputFile.getPath() + "not exists.");
            }
        }

        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }

        SevenZOutputFile output = new SevenZOutputFile(outputFile);
        //LZMA2 is default
        //output.setContentCompression(SevenZMethod.LZMA2);
        try {
            for (File inputFile : inputFiles) {
                compress(output, inputFile, null);
            }
        } finally {
            output.close();
        }
    }

    private static void compress(SevenZOutputFile output, File inputFile, String name) throws IOException {
        if (name == null) name = inputFile.getName();

        if (inputFile.isDirectory()) {//compress dir
            File[] childFiles = inputFile.listFiles();
            if (childFiles.length == 0) {
                genSingleEntry(output, inputFile, name);
                output.closeArchiveEntry();
            } else {
                for (File file : childFiles) {
                    compress(output, file, name + File.separator + file.getName());
                }
            }
        } else { //compress single file

            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(inputFile));
            genSingleEntry(output, inputFile, name);
            int len = -1;
            byte[] buffer = new byte[2048];
            //if item not need compress
            //output.setContentCompression(SevenZMethod.COPY);
            while ((len = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            inputStream.close();
            output.closeArchiveEntry();
        }
    }

    private static SevenZArchiveEntry genSingleEntry(SevenZOutputFile output, File file, String entryName) throws IOException {
        SevenZArchiveEntry entry = output.createArchiveEntry(file, entryName);
        output.putArchiveEntry(entry);
        return entry;
    }


    /**
     * 追加压缩
     *
     * @param outputFile
     * @param inputFile
     * @param entryName
     * @throws IOException
     */
    public static void compress(File outputFile, File inputFile, String entryName) throws IOException {
        if (entryName == null) return;
        byte[] buffer = new byte[2048];
        int len = -1;

        File tempFile = File.createTempFile(outputFile.getName(), null);
        //必须在SevenZOutputFile前取值
        long size = outputFile.length();
        boolean renameOk = outputFile.renameTo(tempFile);
        SevenZOutputFile output = new SevenZOutputFile(outputFile);
        if (size > 0) {
            if (!renameOk) {
                throw new RuntimeException(
                        "could not rename the file " + outputFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
            }
            SevenZFile sevenZFile = null;
            sevenZFile = new SevenZFile(tempFile);
            SevenZArchiveEntry entry = null;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                output.putArchiveEntry(entry);
                while ((len = sevenZFile.read(buffer)) != -1) output.write(buffer, 0, len);
                output.closeArchiveEntry();
            }
            sevenZFile.close();
        }

        BufferedInputStream inputStream = null;
        inputStream = new BufferedInputStream(new FileInputStream(inputFile));
        SevenZArchiveEntry entry = output.createArchiveEntry(inputFile, entryName);
        output.putArchiveEntry(entry);
        while ((len = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, len);
        }
        inputStream.close();
        output.closeArchiveEntry();
        output.close();
        tempFile.delete();
    }


}
