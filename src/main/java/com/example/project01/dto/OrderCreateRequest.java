package com.example.project01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderCreateRequest {

    @NotBlank(message = "idempotency key must not be blank")
    @Size(max = 64, message = "idempotency key length cannot exceed 64")
    private String idempotencyKey;

    @NotBlank(message = "receiver name must not be blank")
    @Size(max = 50, message = "receiver name length cannot exceed 50")
    private String receiverName;

    @NotBlank(message = "receiver phone must not be blank")
    @Size(max = 20, message = "receiver phone length cannot exceed 20")
    private String receiverPhone;

    @NotBlank(message = "receiver address must not be blank")
    @Size(max = 200, message = "receiver address length cannot exceed 200")
    private String receiverAddress;
}
