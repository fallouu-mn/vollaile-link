package com.vollailelink.backend.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequest {

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+221[0-9]{8,9}$", message = "Format de téléphone invalide")
    private String phone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 12, max = 128, message = "Le mot de passe doit contenir entre 12 et 128 caractères")
    private String password;
}
