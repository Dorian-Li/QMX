package com.example.qmx.server;

import com.example.qmx.domain.QualityDetection;
import com.example.qmx.mapper.QualityDetectionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class ZljcService {

    @Autowired
    private QualityDetectionMapper qualityDetectionMapper;

    // 从配置读取质量检测接口地址（默认本地）
    @Value("${zljc.api:http://127.0.0.1:8000/latest}")
    private String zljcApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 定时任务：每50秒查询一次
    @Scheduled(fixedRate = 50000)
    public void scheduledFetchQuality() {
        fetchQualityResultFromApi();
    }

    // 对外方法：供其他服务调用
    public void fetchQualityResultFromApi() {
        HttpURLConnection conn = null;
        try {
            if (zljcApi == null || zljcApi.isEmpty()) {
                System.err.println("质量检测API地址未配置：zljc.api 为空");
                return;
            }

            java.net.URL url = new java.net.URL(zljcApi);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            int statusCode = conn.getResponseCode();
            InputStream in = (statusCode >= 200 && statusCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            if (statusCode != 200) {
                throw new RuntimeException("质量检测API请求失败，HTTP状态码：" + statusCode + "，响应：" + sb.toString());
            }

            JsonNode json = objectMapper.readTree(sb.toString());
            JsonNode resultNode = json.get("result");
            if (resultNode == null || resultNode.isNull() || !resultNode.isNumber()) {
                throw new IllegalStateException("质量检测API未返回有效的 result 字段");
            }
            int result = resultNode.asInt();

            QualityDetection qd = new QualityDetection();
            qd.setResult(result);
            qd.setTime(LocalDateTime.now());

            int n = qualityDetectionMapper.insert(qd);
            if (n > 0) {
                System.out.println("quality_result 插入成功: result=" + result);
            } else {
                System.out.println("quality_result 插入失败: result=" + result);
            }
        } catch (Exception e) {
            System.err.println("质量检测API读取异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
