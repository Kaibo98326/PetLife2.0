package com.petlife.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    public String askGemini(String userPrompt) {
        try {
            String url = API_URL + apiKey;

            // 建立請求體 (按照 Google Gemini API 格式)
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", "你是一位 PetLife 寵物商店的專業客服店長。你的語氣親切且專業。" +
                             "你的任務是回答關於寵物照護、商品推薦或商店資訊的問題。" +
                             "使用者問題如下：" + userPrompt);
            
            content.put("parts", Collections.singletonList(part));
            requestBody.put("contents", Collections.singletonList(content));

            // 設定 Header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 發送請求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // 解析回傳 JSON (簡化版解析)
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map contentObj = (Map) firstCandidate.get("content");
                    List parts = (List) contentObj.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    return (String) firstPart.get("text");
                }
            }
            return "對不起，我暫時無法回答您的問題，請稍後再試。";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "呼叫 AI 服務時發生錯誤：" + e.getMessage();
        }
    }
}
