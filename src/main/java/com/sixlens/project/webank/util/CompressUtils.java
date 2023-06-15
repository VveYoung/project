package com.sixlens.project.webank.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @ClassName: CompressUtils
 * @Description: //TODO 解、压缩工具类，提供解压缩方法，处理数据
 *                      1. 当使用 addFileToTar 方法添加文件或目录时，如果父目录为空，则需要传入空字符串 ""。
 *                      2. 当使用 splitFile 方法切分文件时，需要保证切分大小 splitSize 大于 0。
 *                      3. 当使用 createFinishFile 方法创建结束标记文件时，需要保证 outputDirectory 目录已存在。
 * @Author: cwy
 * @Date: 2023/6/12 0012 
 * @Version: 1.0
 */

@SuppressWarnings({"all"})
public class CompressUtils {

    // 用于记录日志信息
    private static Logger logger = LoggerFactory.getLogger(CompressUtils.class);


    public static void main(String[] args) {

        File file1 = new File("D:\\data\\20230614\\encrypted_dwm_org_company_industry_hotfield.full.textfile");

        File file2 = new File("D:\\data\\20230614\\encrypted_tmp_cwy_dwm_org_company_industry_hotfield.full.textfile");

//        compressFiles(new File[]{file1, file2}, "D:\\data\\20230614\\data.pkg.tar.gz");


        decompressFile("D:\\data\\20230614\\data.pkg.tar.gz", "D:\\data\\20230614");

    }


    /**
     * @Description //TODO 压缩文件
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param sourceFiles 待压缩的文件数组
     * @Param outputFileName 压缩后的文件名，包含路径
     * @return void
     **/
    public static void compressFiles(File[] sourceFiles, String outputFileName) {

        try (
                // 创建文件输出流，并将其包装在缓冲输出流中
                FileOutputStream fos = new FileOutputStream(outputFileName);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                // 创建Gzip压缩流，并将其包装在缓冲输出流中
                GzipCompressorOutputStream gzipCompressorOutputStream = new GzipCompressorOutputStream(bos);
                // 创建Tar归档输出流
                TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(gzipCompressorOutputStream)) {

            // 遍历待压缩的文件数组
            for (File sourceFile : sourceFiles) {
                // 向tar文件中添加文件或目录
                addFileToTar(sourceFile, "", tarArchiveOutputStream);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // splitFile(outputFileName, 1800 * 1024 * 1024); // 数据量很小，暂不考虑切分数据包，且切分数据包的代码还需进一步优化
    }


    /**
     * @Description //TODO 向 tar 文件中添加文件或者目录
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param file 待添加的文件或者目录
     * @Param parent 父目录
     * @Param tarArchiveOutputStream TarArchiveOutputStream 对象
     * @return void
     **/
    private static void addFileToTar(File file, String parent, TarArchiveOutputStream tarArchiveOutputStream) throws IOException {

        // 创建一个 TarArchiveEntry 对象
        TarArchiveEntry entry = new TarArchiveEntry(file, parent + file.getName());
        // 向 tar 文件中添加 entry
        tarArchiveOutputStream.putArchiveEntry(entry);
        if (file.isFile()) {

            try (
                    // 创建文件输入流，并将其包装在缓冲输入流中
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis)) {
                // 将输入流中的数据复制到输出流中
                IOUtils.copy(bis, tarArchiveOutputStream);
            }
            // 关闭当前 entry
            tarArchiveOutputStream.closeArchiveEntry();
        } else if (file.isDirectory()) {
            // 关闭当前 entry
            tarArchiveOutputStream.closeArchiveEntry();
            // 获取当前目录下的所有子文件和子目录
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    // 递归添加子文件和子目录
                    addFileToTar(child, parent + file.getName() + "/", tarArchiveOutputStream);
                }
            }
        }

    }

    private static void splitFile(String inputFileName, long splitSize) {

        // 根据原始文件名获取文件对象
        File inputFile = new File(inputFileName);
        // 获取文件长度
        long inputFileSize = inputFile.length();

        // 如果切分大小小于等于0或者原始文件长度小于等于切分大小，则直接返回
        if (splitSize <= 0 || inputFileSize <= splitSize) {
            return;
        }


        // 构造linux命令
        String command = String.format("split -b %dM -d -a 3 %s %s",
                (int) (splitSize / 1024 / 1024),
                inputFileName,
                inputFileName + "."
        );

        // 执行split命令进行切分
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (IOException e) {
            // e.printStackTrace();
            logger.error("切分数据包，报错信息为：", e);
        } catch (InterruptedException e) {
            // e.printStackTrace();
            logger.error("切分数据包，报错信息为：", e);
        }


    }


    /**
     * @Description //TODO 解压文件
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param inputFileName 待解压的文件名，包含路径
     * @Param outputFileName 解压的路径
     * @return void
     **/
    public static void decompressFile(String inputFileName, String outputFileName) {
        try (
                // 创建文件输入流，并将其包装在缓冲输入流中
                FileInputStream fis = new FileInputStream(inputFileName);
                BufferedInputStream bis = new BufferedInputStream(fis);
                // 创建 Gzip 解压流，并将其包装在缓冲输入流中
                GzipCompressorInputStream gzipCompressorInputStream = new GzipCompressorInputStream(bis);
                // 创建 Tar 归档输入流
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipCompressorInputStream)
        ) {
            TarArchiveEntry entry;
            // 逐个解压 entry
            while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
                // 构造输出文件目录
                Path outputPath = Paths.get(outputFileName, entry.getName());
                System.out.println(outputPath);

                // 如果 entry 是目录，则直接创建目录
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    // 否则创建文件并写入数据
                    File file = outputPath.toFile();
                    try (
                            FileOutputStream fos = new FileOutputStream(file);
                            BufferedOutputStream bos = new BufferedOutputStream(fos)
                    ) {
                        IOUtils.copy(tarArchiveInputStream, bos);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // 记录异常信息
            logger.error("解压文件，报错信息：{}", e);
        } catch (EOFException e) {
            // 文件已到达末尾，终止解压缩操作
            logger.warn("文件已到达末尾，解压缩操作已终止");
        } catch (IOException e) {
            // 记录异常信息
            logger.error("解压文件，报错信息：{}", e);
        }
    }


    /**
     * @Description //TODO 创建结束标记文件
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param packageName 包名
     * @Param outputDirectory 输出目录
     * @Param date 日期
     * @return void
     **/
    public static void createFinishFile(String packageName, String outputDirectory, String date) throws IOException {
        // 构造结束标记文件名
        String finishFileName = String.format("%s/%s_%s.finish", outputDirectory, packageName, date);
        // 创建结束标记文件
        Files.createFile(Paths.get(finishFileName));
    }


    /**
     * @Description //TODO 切分文件
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param inputFileName 原始文件名，包含路径
     * @Param splitSize 切分大小
     * @return void
     **/
    private static void splitFile01(String inputFileName, long splitSize) throws IOException {
        // 根据原始文件名获取文件对象
        File inputFile = new File(inputFileName);
        // 获取文件长度
        long inputFileSize = inputFile.length();

        // 如果切分大小小于等于 0 或者原始文件长度小于等于切分大小，则直接返回
        if (splitSize <= 0 || inputFileSize <= splitSize) {
            return;
        }

        // 计算切分后的文件数量
        int splitFileCount = (int) Math.ceil(inputFileSize * 1.0 / splitSize);
        // 创建一个缓冲区，用于存储读取的数据
        byte[] buffer = new byte[1024 * 1024];
        // 创建一个输入流，用于读取原始文件数据
        try (
                FileInputStream fis = new FileInputStream(inputFile);
                BufferedInputStream bis = new BufferedInputStream(fis)
        ) {
            // 逐个切分文件
            for (int i = 0; i < splitFileCount; i++) {
                // 构造切分文件名
                String splitFileName = String.format("%s.%02d", inputFileName, i + 1);
                // 创建一个输出流，用于写入切分后的数据
                try (
                        FileOutputStream fos = new FileOutputStream(splitFileName);
                        BufferedOutputStream bos = new BufferedOutputStream(fos)
                ) {
                    // 计算切分文件的大小
                    long splitFileSize = Math.min(splitSize, inputFileSize - i * splitSize);
                    // 逐个读取数据并写入切分文件
                    int readBytes;
                    long remainSize = splitFileSize;
                    while ((readBytes = bis.read(buffer, 0, (int) Math.min(buffer.length, remainSize))) != -1) {
                        bos.write(buffer, 0, readBytes);
                        remainSize -= readBytes;
                        if (remainSize <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

}