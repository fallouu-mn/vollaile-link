package com.vollailelink.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling est requis pour la liberation automatique
 * des reservations de stock expirees (voir StockReservationScheduler).
 */
@EnableScheduling
@SpringBootApplication
public class VollaileLinkBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(VollaileLinkBackendApplication.class, args);
    }
}
