package com.sixlens.project.webank.util;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

/**
 * @ClassName: EncrypUtils
 * @Description: //TODO 加密工具类，提供SM4加密、解密文件等功能
 * @Author: cwy
 * @Date: 2023/6/12 0012 
 * @Version: 1.0
 */
public class EncryptUtils {

    private static Logger logger = LoggerFactory.getLogger(EncryptUtils.class);

    // 密钥长度
    private static final int KEY_SIZE = 128;

    // 算法名称
    private static final String ALGORITHM_NAME = "AES";

    // 密钥
    private static final byte[] KEY = ByteUtils.fromHexString("b5f62f117e6424c1808f2217680c2bcd");

    // 加密文件后缀
    // private static final String ENCRYPTED_FILE_SUFFIX = ".encrypted";


    // 静态代码块，用于添加Bouncy Castle提供者，以支持SM4加密算法
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * @Description //TODO 生成密钥的方法
     * @Author cwy
     * @Date 2023/6/12 0012
     * @return byte[]
     **/
    public static byte[] generateKey() {

        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
            kg.init(KEY_SIZE, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            // e.printStackTrace();
            logger.error("生成密钥，报错信息为：" + e);
        }
        return kg.generateKey().getEncoded();
    }


    /**
     * @Description //TODO 生成 Cipher 对象
     * @Author cwy
     * @Date 2023/6/19 0019
     * @Param mode
     * @Param keyData
     * @return javax.crypto.Cipher
     **/
    public static Cipher generateCipher(int mode, byte[] keyData) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", BouncyCastleProvider.PROVIDER_NAME);
            Key sm4Key = new SecretKeySpec(keyData, "SM4");
            cipher.init(mode, sm4Key);
        } catch (Exception e) {
            // e.printStackTrace();
            logger.error("生成 Cipher对象，报错信息为：" + e);
        }
        return cipher;
    }

    /**
     * @Description //TODO 加密文件
     * @Author cwy
     * @Date 2023/6/12 0012
     * @Param [keyData, sourchPath, targetPath] 密钥数据   原始文件路径  加密后文件路径
     * @return void
     **/
    public static File encryptFile(String sourchPath, String targetPath) {

        File file = null;
        try {
            Cipher cipher = generateCipher(Cipher.ENCRYPT_MODE, KEY);
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(sourchPath), cipher);

            File targetFile = new File(targetPath);
            if (targetFile.exists()) {
                targetFile.delete();
            }

            file = FileUtil.writeFromStream(cipherInputStream, targetPath);

            IoUtil.close(cipherInputStream);
        } catch (Exception e) {
            logger.error("加密文件时，报错信息为：" + e);
        }
        return file;
    }


    /**
     * @Description //TODO 解密文件
     * @Author cwy
     * @Date 2023/6/12 0012
     * @Param [keyData, sourcePath, targetPath] 密钥数据   加密文件路径   解密后文件路径
     * @return void
     **/
    public static void decryptFile(String sourcePath, String targetPath) {
        FileInputStream in = null;
        ByteArrayInputStream byteArrayInputStream = null;
        OutputStream out = null;
        CipherOutputStream cipherOutputStream = null;
        try {
            in = new FileInputStream(sourcePath);
            byte[] bytes = IoUtil.readBytes(in);
            byteArrayInputStream = IoUtil.toStream(bytes);
            Cipher cipher = generateCipher(Cipher.DECRYPT_MODE, KEY);
            out = new FileOutputStream(targetPath);
            cipherOutputStream = new CipherOutputStream(out, cipher);
            IoUtil.copy(byteArrayInputStream, cipherOutputStream);
        } catch (Exception e) {
            // e.printStackTrace();
            logger.error("解密文件时，报错信息为： {}", e);
        } finally {
            IoUtil.close(cipherOutputStream);
            IoUtil.close(out);
            IoUtil.close(byteArrayInputStream);
            IoUtil.close(in);
        }
    }

    public static void main(String[] args) throws Exception {
//        String oriFile = "D:\\data\\cwy.docx";
//        String encryFile = "D:\\data\\cwy加密文件.docx";
//        String decryFile = "D:\\data\\cwy解密文件.docx";

//        byte[] bytes = generateKey();
//        String key = ByteUtils.toHexString(bytes);
//        System.out.println("密钥： " + key); // b5f62f117e6424c1808f2217680c2bcd

//        byte[] keyData = ByteUtils.fromHexString(key);
//        encryptFile(keyData, oriFile, encryFile);
//        decryptFile(keyData, encryFile, decryFile);

        decryptFile("D:\\data\\20230614\\encrypted_dwm_org_company_industry_hotfield.full.textfile",
                "D:\\data\\20230614\\decrypted_dwm_org_company_industry_hotfield.full.textfile"
        );
        decryptFile("D:\\data\\20230614\\encrypted_tmp_cwy_dwm_org_company_industry_hotfield.full.textfile",
                "D:\\data\\20230614\\decrypted_tmp_cwy_dwm_org_company_industry_hotfield.full.textfile"
        );

    }

}
