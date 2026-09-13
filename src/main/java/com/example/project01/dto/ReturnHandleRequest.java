package com.example.project01.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Seller handling note for approve/reject actions.
 */
@Data
public class ReturnHandleRequest {

    @Size(max = 500, message = "handle note cannot exceed 500 chars")
    private String note;
}
