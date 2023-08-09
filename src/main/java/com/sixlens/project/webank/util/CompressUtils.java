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
import java.util.ArrayList;
import java.util.List;

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

    // 切割压缩包 1.8G
    private static long SPLIT_SIZE = 1_843_200L;


    public static void main(String[] args) throws IOException {

//        File file1 = new File("D:\\data\\20230614\\encrypted_dwm_org_company_industry_hotfield.full.textfile");

//        File file2 = new File("D:\\data\\20230614\\encrypted_tmp_cwy_dwm_org_company_industry_hotfield.full.textfile");

//        compressFiles(new File[]{file1, file2}, "D:\\data\\20230614\\data.pkg.tar.gz");

//        splitCompressedFile(args[0]);

//        splitCompressedFile("C:\\Users\\Administrator\\Desktop\\Lunix\\data.20230618.pkg.tar.gz");

//        decompressFile("D:\\data\\20230614\\data.pkg.tar.gz", "D:\\data\\20230614");


        for (int i = 1; i < 3; i++) {

            String splitFileNumber = String.format("%03d", i);
            System.out.println(splitFileNumber);
        }

    }


    /**
     * @Description //TODO 压缩文件
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param sourceFiles 待压缩的文件数组
     * @Param compressedFileName 压缩后的文件名，包含路径
     * @return void
     **/
    public static void compressFiles(File[] sourceFiles, String compressedFileName) {

        try (
                // 创建文件输出流，并将其包装在缓冲输出流中
                FileOutputStream fos = new FileOutputStream(compressedFileName);
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

    }

    /**
     * @Description //TODO 压缩文件
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param batchDate 日期，格式为 "yyyyMMdd"
     * @Param compressedFileName 压缩后的文件名，包含路径
     * @return void
     **/
    public static void compressFiles(String batchDate, String compressedFileName) {

        File directory = new File("/data/cwy/webank/" + batchDate);
        File[] sourceFiles = directory.listFiles((dir, name) -> name.startsWith("encrypted_"));

        for (File sourceFile : sourceFiles) {
            System.out.println(sourceFile.getAbsolutePath());
        }

        try (
                // 创建文件输出流，并将其包装在缓冲输出流中
                FileOutputStream fos = new FileOutputStream(compressedFileName);
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

            logger.info("将加密文件压缩在一起~");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
     * @Description //TODO 切分文件
     * @Author cwy
     * @Date 2023/6/14 0014
     * @Param inputFileName 原始文件名，包含路径
     * @Param splitSize 切分大小
     * @return void
     **/
    public static List<File> splitCompressedFile(String compressedFileName) {

        List<File> splitFiles = new ArrayList<>();
        long maxFileSize = 1800L * 1024 * 1024; // 1.8 GB

        try {
            // 获取压缩文件的路径和文件名
            String[] fileNameParts = compressedFileName.split("/");
            String filePath = compressedFileName.substring(0, compressedFileName.length() - fileNameParts[fileNameParts.length - 1].length());

            // 获取压缩文件的大小
            File compressedFile = new File(compressedFileName);
            long compressedFileSize = compressedFile.length();

            // 如果需要，进行文件切割
            if (compressedFileSize > maxFileSize) {
                // 计算需要切割的数量
                int numSplits = (int) Math.ceil((double) compressedFileSize / maxFileSize);

                // 使用Linux系统的split命令切割文件
                ProcessBuilder pb = new ProcessBuilder("split", "-b", String.valueOf(maxFileSize), "-d", "-a", "3", compressedFileName, fileNameParts[fileNameParts.length - 1]);
                pb.directory(new File(filePath));
                Process p = pb.start();
                p.waitFor();

                // 获取切割后的文件列表
                for (int i = 0; i < numSplits; i++) {
                    String splitFileNumber = String.format("%03d", i);
                    File splitFile = new File(filePath + fileNameParts[fileNameParts.length - 1] + splitFileNumber);
                    // System.out.println(splitFile.toString());
                    splitFiles.add(splitFile); // 添加包括原始的压缩文件和切割后的小压缩包
//                    if (splitFile.length() <= maxFileSize) {
//                        splitFiles.add(splitFile);
//                    }
                }

                logger.info("将压缩文件切割为 {} 个小文件", splitFiles.size());

            } else {
                splitFiles.add(compressedFile);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("切割压缩文件报错 {}", e);
        }

        return splitFiles;
    }

}