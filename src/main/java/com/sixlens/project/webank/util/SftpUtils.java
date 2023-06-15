package com.sixlens.project.webank.util;

import com.jcraft.jsch.*;

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


    public static ChannelSftp getSftpChannel() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(SFTP_USERNAME, SFTP_IP, SFTP_PORT);
        session.setPassword(SFTP_PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        return channel;
    }


    public static void uploadFile(File file, String remotePath) throws SftpException, FileNotFoundException, JSchException {

        ChannelSftp channel = getSftpChannel();
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

        disconnect(channel);
    }

    public static void disconnect(ChannelSftp channel) throws JSchException {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
            channel.getSession().disconnect();
        }
    }

}
