package com.petlife.service;

import com.petlife.model.Order;
import com.petlife.model.OrderDetail; // 假設明細 Bean 名稱
import com.petlife.repository.OrderRepository;
import com.petlife.repository.OrderDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class OrderAdminService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    // 取得所有訂單
    public List<Order> findActiveOrders(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return orderRepository.findByOrderNameContainingAndIsDeletedFalseOrderByOrderDateAsc(search);
        }
        return orderRepository.findByIsDeletedFalseOrderByOrderDateDesc();
    }

    // 取得訂單詳細明細
    public Map<String, Object> getOrderWithDetails(Integer id) {
        Map<String, Object> map = new HashMap<>();
        Order order = orderRepository.findById(id).orElse(null);
        List<OrderDetail> details = orderDetailRepository.findByOrderBean_OrderId(id);

        map.put("order", order);
        map.put("details", details);
        return map;
    }
 
    // 更新訂單
    public void updateStatusAndPayment(Integer id, String status, String payment) {
        orderRepository.findById(id).ifPresent(o -> {
            o.setOrderStatus(status);
            o.setOrderPayment(payment);
            orderRepository.save(o);
        });
    }

    // 軟刪除
    public boolean performSoftDelete(Integer id) {
        return orderRepository.findById(id).map(o -> {
            o.setIsDeleted(true);
            orderRepository.save(o);
            return true;
        }).orElse(false);
    }
}