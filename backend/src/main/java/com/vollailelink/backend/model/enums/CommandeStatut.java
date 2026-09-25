package com.vollailelink.backend.model.enums;

public enum CommandeStatut {
    EN_ATTENTE_PAIEMENT,
    PAYEE,
    EN_PREPARATION,
    EXPEDIEE,
    LIVREE,
    ANNULEE,
    /** Reservation expiree : la commande est closee et le stock libere. */
    EXPIREE
}
