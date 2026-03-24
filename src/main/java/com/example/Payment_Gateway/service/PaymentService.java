package com.example.Payment_Gateway.service;

import com.example.Payment_Gateway.entity.PaymentOrder;
import com.example.Payment_Gateway.repo.PaymentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private EmailService emailService;

    private final WebClient webClient;

    public PaymentService(@Value("${nvidia.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://integrate.api.nvidia.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String createOrder(PaymentOrder orderDetails) {
        System.out.println("inside service..........");

        // 1. Save order to DB directly (no payment gateway)
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        orderDetails.setOrderId(orderId);
        orderDetails.setStatus("CREATED");
        orderDetails.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(orderDetails);

        // 2. Call Nvidia AI to generate a confirmation message
        String prompt = String.format(
                "Generate a short friendly order confirmation message for a student named %s " +
                        "who just enrolled in the Spark 3.0 Java Full Stack course for Rs %.0f. " +
                        "Keep it under 3 sentences. Include the order ID: %s.",
                orderDetails.getName(),
                orderDetails.getAmount(),
                orderId
        );

        String requestBody = "{"
                + "\"model\": \"meta/llama-3.1-8b-instruct\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt.replace("\"", "\\\"") + "\"}],"
                + "\"max_tokens\": 150"
                + "}";

        try {
            String aiResponse = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String message = extractContent(aiResponse);
            return orderId + "||" + message;

        } catch (Exception e) {
            System.out.println("Nvidia AI call failed: " + e.getMessage());
            return orderId + "||Order confirmed! Your seat in Spark 3.0 is reserved. Order ID: " + orderId;
        }
    }

    // Extract "content" field from Nvidia JSON response
    private String extractContent(String json) {
        try {
            int start = json.indexOf("\"content\":\"") + 11;
            int end = json.indexOf("\"", start);
            return json.substring(start, end).replace("\\n", " ");
        } catch (Exception e) {
            return "Order confirmed successfully!";
        }
    }

    public void updateOrderStatus(String paymentId, String orderId, String status) {
        PaymentOrder order = paymentRepo.findByOrderId(orderId);
        order.setPaymentId(paymentId);
        order.setStatus(status);
        paymentRepo.save(order);

        if ("SUCCESS".equalsIgnoreCase(status)) {
            emailService.sendEmail(
                    order.getEmail(),
                    order.getName(),
                    order.getCourseName(),
                    order.getAmount()
            );
        }
    }
}
