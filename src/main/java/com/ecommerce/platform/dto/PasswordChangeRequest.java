package com.ecommerce.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改密码请求对象。 */
@Data
public class PasswordChangeRequest {

    @NotBlank(message = "old password must not be blank")
    private String oldPassword;

    @NotBlank(message = "new password must not be blank")
    @Size(min = 6, max = 100, message = "password length must be 6-100")
    private String newPassword;
}
