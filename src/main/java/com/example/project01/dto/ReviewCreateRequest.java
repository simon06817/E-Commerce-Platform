package com.example.project01.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Buyer review payload. The order item proves the buyer really purchased it.
 */
@Data
public class ReviewCreateRequest {

    @NotNull(message = "order item id must not be null")
    private Long orderItemId;

    @NotNull(message = "rating must not be null")
    @Min(value = 1, message = "rating must be at least 1")
    @Max(value = 5, message = "rating must be at most 5")
    private Integer rating;

    @Size(max = 500, message = "review content cannot exceed 500 chars")
    private String content;
}
