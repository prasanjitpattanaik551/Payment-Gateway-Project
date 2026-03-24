package com.example.Payment_Gateway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="payment_order")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String  name;
    private String email;
    private String phone;
    private String courseName;
    private Double amount;
    private String orderId;
    private String paymentId;
    private String status;
    private LocalDateTime createdAt;

}
