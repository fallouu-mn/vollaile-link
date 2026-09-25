package com.vollailelink.backend.model.enums;

/**
 * Cycle de vie d'une reservation de stock.
 *
 * ACTIVE   : la quantite est comptee dans offers.quantity_reserved
 * RELEASED : liberee (annulation de commande)
 * CONSUMED : convertie en vente (reserved -> sold)
 * EXPIRED  : liberee automatiquement car expires_at depasse
 */
public enum ReservationStatut {
    ACTIVE,
    RELEASED,
    CONSUMED,
    EXPIRED
}
