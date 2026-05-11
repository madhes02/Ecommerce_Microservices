package com.deloitte.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.deloitte.user_service.entity.Role;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Role is required")
    private Role role;

}
