package com.ecommerce.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 回复评价请求对象。 */
@Data
public class ReviewReplyRequest {

    @NotBlank(message = "reply content must not be blank")
    @Size(max = 500, message = "reply content cannot exceed 500 chars")
    private String content;
}
