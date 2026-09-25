package com.vollailelink.backend.exception;

/**
 * Regle de metier non respectee, avec un message destine a l'utilisateur.
 *
 * Different de IllegalArgumentException / IllegalStateException, dont le
 * message est masque par le handler generique ("Bad request").
 * Utilise quand l'administrateur doit comprendre POURQUOI l'action est
 * refusee afin de corriger sa saisie.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
