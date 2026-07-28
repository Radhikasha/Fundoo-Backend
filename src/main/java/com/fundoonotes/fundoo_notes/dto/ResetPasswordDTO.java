package com.fundoonotes.fundoo_notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordDTO {

    @NotBlank(message = "Token is required")
    private String token;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @NotBlank(message = "New password is required")
    private String newPassword;
}
