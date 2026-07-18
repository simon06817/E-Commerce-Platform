package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type= IdType.AUTO)
     private Long id;

     @NotBlank(message = "用户名不能为空")
     @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
     private String username;

     @NotBlank(message = "密码不能为空")
     @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
     private String password ;

     @Email(message = "邮箱格式不正确")
     private String email;


     @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
     private String phone;

     private String address;

     @TableField(fill= FieldFill.INSERT)
     private LocalDateTime createTime;

     @TableField(fill= FieldFill.INSERT_UPDATE)
     private LocalDateTime updateTime;

     @TableLogic
     private Integer deleted;  //0未删除，1已删除


}
