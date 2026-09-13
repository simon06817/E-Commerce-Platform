package com.example.project01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Seller reply to one product review.
 */
@Data
public class ReviewReplyRequest {

    @NotBlank(message = "reply content must not be blank")
    @Size(max = 500, message = "reply content cannot exceed 500 chars")
    private String content;
}
