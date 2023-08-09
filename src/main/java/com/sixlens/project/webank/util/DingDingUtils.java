package com.sixlens.project.webank.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @ClassName: DingDingUtils
 * @Description: //TODO 钉钉通知
 * @Author: cwy
 * @Date: 2023/6/20 0020 
 * @Version: 1.0
 */
public class DingDingUtils {

    private static Logger logger = LoggerFactory.getLogger(DingDingUtils.class);

    public static final String SIXLENS_DINGDING_WEBHOOK_URL = "https://oapi.dingtalk.com/robot/send?access_token=519c3c41cda378b0db334bdee9ae7f0903765d5bd1b00f8573c8b36b79cab551";
    // public static final String CWY_DINGDING_WEBHOOK_URL = "https://oapi.dingtalk.com/robot/send?access_token=4abefb411527668e359fdaffc3f7a2b44de3a90502898289a1693313ce95eeb4";


    public static void sendDing(String message) {

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        JSONObject json = new JSONObject();
        json.put("msgtype", "text");

        JSONObject text = new JSONObject();
        text.put("content", message);
        json.put("text", text);

        RequestBody body = RequestBody.create(mediaType, json.toJSONString());
        Request request = new Request.Builder()
                .url(SIXLENS_DINGDING_WEBHOOK_URL)
                .post(body)
                .build();


        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            // e.printStackTrace();
            logger.warn("钉钉通知失败，报错信息为：{}", e);
        }
        response.close();
    }


    public static void main(String[] args) {

        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime previousDay = currentDate.minusDays(1);
        String batchDate = previousDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        System.out.println(batchDate);

        sendDing(StrUtil.format("微众银行 {} 批次数据流程提供完成流程", batchDate));

    }

}
