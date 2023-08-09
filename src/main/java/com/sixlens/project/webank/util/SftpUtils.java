package com.sixlens.project.webank.util;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static com.sixlens.project.webank.config.WebankConfig.*;

/**
 * @ClassName: SftpUtils
 * @Description: //TODO 用于通过SFTP将压缩后的文件发送到指定的服务器
 * @Author: cwy
 * @Date: 2023/6/7 0007
 * @Version: 1.0
 */
public class SftpUtils {

    private static Logger logger = LoggerFactory.getLogger(SftpUtils.class);

    public static void main(String[] args) {

        System.out.println(getSftpChannel());
    }

    /**
     * @Description //TODO 获取SFTP通道
     * @Author cwy
     * @Date 2023/6/16 0016
     * @Param
     * @return com.jcraft.jsch.ChannelSftp
     **/
    public static ChannelSftp getSftpChannel() {
        JSch jsch = new JSch();

        Session session = null;
        ChannelSftp channel = null;

        try {
            session = jsch.getSession(SFTP_USERNAME, SFTP_IP, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
        } catch (JSchException e) {
            logger.error("建立SFTP通道连接失败：{}", e);
        }
        return channel;
    }


    /**
     * @Description //TODO 上传文件至SFTP服务器
     * @Author cwy
     * @Date 2023/6/16 0016
     * @Param file 需要上传的文件
     * @Param remotePath SFTP的远程路径
     * @return void
     **/
    public static boolean uploadFile(File file, String remotePath) {
        boolean flag = false;
        ChannelSftp channel = getSftpChannel();
        try {
            try {
                channel.cd(remotePath);
            } catch (SftpException e) {
                // 如果远程 sftp 服务器中的目录不存在，尝试创建
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    channel.mkdir(remotePath);
                    channel.cd(remotePath);
                } else {
                    throw e;
                }
            }

            channel.put(new FileInputStream(file), file.getName());
            logger.info("成功上传文件: {}", file.getName());
            flag = true;
        } catch (SftpException e) {
            logger.error("上传SFTP服务器失败：{}", e);
        } catch (FileNotFoundException e) {
            logger.error("找不到压缩包文件：{}", e);
        } finally {
            // 断开SFTP连接
            try {
                disconnect(channel);
            } catch (JSchException e) {
                logger.error("断开SFTP服务连接失败：{}", e);
            }
        }

        return flag;
    }

    /**
     * @Description //TODO 断开 SFTP 服务器连接
     * @Author cwy
     * @Date 2023/6/16 0016
     * @Param channel
     * @return void
     **/
    public static void disconnect(ChannelSftp channel) throws JSchException {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
            channel.getSession().disconnect();
        }
    }

}
