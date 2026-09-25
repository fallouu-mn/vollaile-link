package com.vollailelink.backend.security;

import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.enums.AdminStatus;
import com.vollailelink.backend.repository.AdministratorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService securityAuditService;

    @Value("${ADMIN_PHONE:}")
    private String adminPhone;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!isValidConfiguration()) {
            log.warn("Bootstrap administrateur ignoré : renseignez ADMIN_PHONE et ADMIN_PASSWORD avec de vraies valeurs.");
            return;
        }

        if (administratorRepository.existsByPhone(adminPhone)) {
            log.info("Un administrateur existe déjà pour le téléphone configuré.");
            return;
        }

        Administrator administrator = Administrator.builder()
                .phone(adminPhone)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .status(AdminStatus.ACTIVE)
                .mustChangePassword(true)
                .failedLoginAttempts(0)
                .build();

        administratorRepository.save(administrator);
        securityAuditService.recordSuccess(
                administrator,
                "ADMIN_BOOTSTRAP",
                "Compte administrateur initial créé",
                null);
        log.info("Compte administrateur initial créé. Le changement du mot de passe sera requis.");
    }

    private boolean isValidConfiguration() {
        return adminPhone != null
                && adminPhone.matches("^\\+221[0-9]{8,9}$")
                && adminPassword != null
                && adminPassword.length() >= 12
                && !adminPassword.startsWith("your_")
                && !adminPassword.contains("change_this");
    }
}
