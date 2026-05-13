package com.petlife.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.config.LinePayConfig;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LinePayService {

    private final LinePayConfig linePayConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ======== 產生付款網址 ========
    public String requestPayment(String merchantTradeNo, int amount, String productName) {

        try {
            // 1. 準備 request body
            Map<String, Object> body = new HashMap<>();
            body.put("amount", amount);
            body.put("currency", "TWD");
            body.put("orderId", merchantTradeNo);

            // 商品資訊
            Map<String, Object> product = new HashMap<>();
            product.put("name", productName);
            product.put("quantity", 1);
            product.put("price", amount);
            body.put("packages", List.of(
                Map.of(
                    "id", merchantTradeNo,
                    "amount", amount,
                    "products", List.of(product)
                )
            ));

            // 導回網址
            body.put("redirectUrls", Map.of(
                "confirmUrl", linePayConfig.getConfirmUrl() +
                              "?merchantTradeNo=" + merchantTradeNo,
                "cancelUrl", "http://localhost:5173/stay"
            ));

            // 2. 轉成 JSON 字串
            String bodyJson = objectMapper.writeValueAsString(body);

            // 3. 產生簽名
            String nonce = UUID.randomUUID().toString();
            String path = "/v3/payments/request";
            String signature = generateSignature(
                linePayConfig.getChannelSecret(),
                path,
                bodyJson,
                nonce
            );

            // 4. 設定 headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-LINE-ChannelId", linePayConfig.getChannelId());
            headers.set("X-LINE-Authorization-Nonce", nonce);
            headers.set("X-LINE-Authorization", signature);

            // 5. 打 LINE Pay API
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
            String url = linePayConfig.getApiUrl() + path;

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url, entity, Map.class
            );
            
            System.out.println("LINE Pay 回傳：" + response.getBody());

            // 6. 取出付款網址
            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> info = (Map<String, Object>) responseBody.get("info");
            Map<String, Object> paymentUrl = (Map<String, Object>) info.get("paymentUrl");

            return (String) paymentUrl.get("web");

        } catch (Exception e) {
            throw new RuntimeException("LINE Pay 請求失敗：" + e.getMessage());
        }
    }

    // ======== 確認付款 ========
    public boolean confirmPayment(String transactionId, int amount) {

        try {
            // 1. 準備 request body
            Map<String, Object> body = new HashMap<>();
            body.put("amount", amount);
            body.put("currency", "TWD");
            

            String bodyJson = objectMapper.writeValueAsString(body);

            // 2. 產生簽名
            String nonce = UUID.randomUUID().toString();
            String path = "/v3/payments/" + transactionId + "/confirm";
            String signature = generateSignature(
                linePayConfig.getChannelSecret(),
                path,
                bodyJson,
                nonce
            );

            // 3. 設定 headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-LINE-ChannelId", linePayConfig.getChannelId());
            headers.set("X-LINE-Authorization-Nonce", nonce);
            headers.set("X-LINE-Authorization", signature);

            // 4. 打 LINE Pay Confirm API
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
            String url = linePayConfig.getApiUrl() + path;

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url, entity, Map.class
            );
            
         
            

            // 5. 確認結果
            Map<String, Object> responseBody = response.getBody();
            String returnCode = (String) responseBody.get("returnCode");

            return "0000".equals(returnCode);

        } catch (Exception e) {
            throw new RuntimeException("LINE Pay 確認失敗：" + e.getMessage());
        }
    }

    // ======== 產生 HMAC-SHA256 簽名 ========
    private String generateSignature(
            String secret, String path, String body, String nonce) throws Exception {

        // LINE Pay 簽名規則：
        // 簽名內容 = secret + path + body + nonce
        String message = secret + path + body + nonce;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        );
        mac.init(keySpec);

        byte[] rawHmac = mac.doFinal(
            message.getBytes(StandardCharsets.UTF_8)
        );

        // 轉成 Base64
        return java.util.Base64.getEncoder().encodeToString(rawHmac);
    }
}