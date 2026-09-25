package com.vollailelink.backend.scheduler;

import com.vollailelink.backend.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Liberation automatique des reservations dont l'echeance est depassee.
 *
 * Sans ce traitement, une reservation bloquerait indefiniment du stock
 * disponible alors que la commande correspondante a ete abandonnee.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReservationScheduler {

    private final StockService stockService;

    @Scheduled(
            initialDelayString = "${stock.reservation.scheduler-initial-delay-ms:60000}",
            fixedDelayString = "${stock.reservation.scheduler-delay-ms:300000}"
    )
    public void expireOverdueReservations() {
        try {
            int liberees = stockService.expireOverdueReservations();
            if (liberees > 0) {
                log.info("Expiration du stock : {} reservation(s) liberee(s) automatiquement.", liberees);
            }
        } catch (RuntimeException ex) {
            // Une erreur de planificateur ne doit jamais arreter l'application.
            log.error("Echec de l'expiration automatique des reservations", ex);
        }
    }
}
