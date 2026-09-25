package com.vollailelink.backend.security;

import com.vollailelink.backend.exception.AccountLockedException;
import com.vollailelink.backend.exception.InvalidCredentialsException;
import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.enums.AdminStatus;
import com.vollailelink.backend.repository.AdministratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityAuditService securityAuditService;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request, String ipAddress) {
        Administrator administrator = administratorRepository.findByPhone(request.getPhone()).orElse(null);
        OffsetDateTime now = OffsetDateTime.now();

        if (administrator == null || administrator.getStatus() != AdminStatus.ACTIVE) {
            securityAuditService.recordFailure("LOGIN_FAILED", "Identifiants administrateur invalides", ipAddress);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (administrator.getLockedUntil() != null) {
            if (administrator.getLockedUntil().isAfter(now)) {
                securityAuditService.recordFailure("LOGIN_FAILED", "Compte administrateur verrouillé", ipAddress);
                throw new AccountLockedException("Account locked");
            }
            administrator.setLockedUntil(null);
            administrator.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(request.getPassword(), administrator.getPasswordHash())) {
            int attempts = administrator.getFailedLoginAttempts() == null
                    ? 1
                    : administrator.getFailedLoginAttempts() + 1;
            administrator.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                administrator.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            }
            administratorRepository.save(administrator);
            securityAuditService.recordFailure("LOGIN_FAILED", "Mot de passe administrateur invalide", ipAddress);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        administrator.setFailedLoginAttempts(0);
        administrator.setLockedUntil(null);
        administrator.setLastLogin(now);
        administratorRepository.save(administrator);
        securityAuditService.recordSuccess(administrator, "LOGIN_SUCCESS", "Connexion administrateur réussie", ipAddress);

        return AdminLoginResponse.builder()
                .token(jwtService.generateToken(administrator))
                .administratorId(administrator.getId())
                .phone(administrator.getPhone())
                .mustChangePassword(Boolean.TRUE.equals(administrator.getMustChangePassword()))
                .build();
    }

    @Transactional
    public void changePassword(
            AdminPrincipal principal,
            ChangePasswordRequest request,
            String ipAddress) {
        Administrator administrator = administratorRepository.findById(principal.getId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), administrator.getPasswordHash())) {
            securityAuditService.recordFailure(
                    administrator,
                    "PASSWORD_CHANGE_FAILED",
                    "Mot de passe actuel invalide",
                    ipAddress);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (passwordEncoder.matches(request.getNewPassword(), administrator.getPasswordHash())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        administrator.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        administrator.setMustChangePassword(false);
        administratorRepository.save(administrator);
        securityAuditService.recordSuccess(
                administrator,
                "PASSWORD_CHANGE",
                "Mot de passe administrateur modifié",
                ipAddress);
    }
}
