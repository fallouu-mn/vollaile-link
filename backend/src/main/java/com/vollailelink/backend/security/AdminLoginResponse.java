package com.vollailelink.backend.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;
    private Long administratorId;
    private String phone;
    private boolean mustChangePassword;
}
