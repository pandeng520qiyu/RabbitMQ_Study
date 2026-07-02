package com.study.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度为3~50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6~100位")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;
}
