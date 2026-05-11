package com.petlife.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.petlife.service.OrderService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    // 綠界付完錢後，會私下傳資料到這個網址：
    // https://enable-impeach-caress.ngrok-free.dev/api/payment/callback
    @PostMapping("/callback")
    public String handlePaymentCallback(@RequestParam Map<String, String> result) {
        System.out.println("綠界回復：" + result);

        // 1跟OK是成功，0是付款失敗
        if ("1".equals(result.get("RtnCode"))) {
            String tradeNo = result.get("MerchantTradeNo");
            // 去把資料庫裡的訂單改成「已付款」
            orderService.updateOrderPaymentStatus(tradeNo);
            return "1|OK"; // 綠界沒收到1就會一直發訊息，好任性
        }
        return "0|Error";
    }
}
