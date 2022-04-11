package com.proxygtzbabc.tools;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author syl
 * @time 2021/12/28 11:45
 */
public class EncryptUtils {
    private final byte[] KEY = "179D278870E26DBF".getBytes(); // 加密使用的key
    private final byte[] IV = "FBD62E078872D971".getBytes(); // 偏移值
    private final String ALGORITHM = "AES/CBC/PKCS5Padding"; // 加密算法
    private Cipher encryptCipher; // 加密
    private Cipher decryptCipher; // 解密

    /**
     * 使用单例
     */
    private EncryptUtils() {
        try {
            // 初始化加密算法
            decryptCipher = Cipher.getInstance(ALGORITHM);
            encryptCipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(KEY, "AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV));
            decryptCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SingletonHolder {
        private static final EncryptUtils INSTANCE = new EncryptUtils();
    }

    public static EncryptUtils getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 加密
     *
     * @param data
     * @return
     */
    public byte[] encrypt(byte[] data) {
        try {
            return encryptCipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     *
     * @param data
     * @return
     */
    public byte[] decrypt(byte[] data) {
        try {
            return decryptCipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
