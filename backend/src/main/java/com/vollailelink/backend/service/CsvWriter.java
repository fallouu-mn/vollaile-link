package com.vollailelink.backend.service;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.function.Function;

/**
 * Generation de CSV sur pour le back-office.
 *
 * Deux risques traites ici :
 *
 * 1. L'echappement RFC 4180 (separateur, guillemets, retours ligne) : sans
 *    guillemets, une virgule dans une adresse decale toutes les colonnes.
 *
 * 2. L'injection de formule (CWE-1236). Les noms et commentaires proviennent
 *    du formulaire public et ne sont pas filtres. Une cellule commencant par
 *    =, +, -, @, ou un caractere de controle est executee par Excel ou
 *    LibreOffice a la simple ouverture du fichier. On la neutralise en la
 *    prefixant d'une apostrophe, que le tableur interprete comme du texte.
 */
public final class CsvWriter {

    /** Separateur point-virgule : convention des tableurs en francais. */
    public static final String SEPARATOR = ";";

    /**
     * BOM UTF-8. Sans lui, Excel interprete le fichier en ANSI et les
     * accents deviennent illisibles.
     */
    public static final String UTF8_BOM = "﻿";

    /**
     * Prefixes interpretes comme formule par les tableurs.
     * Le caractere de tabulation et le retour chariot sont inclus parce
     * qu'ils permettent de contourner un filtre naif sur le premier caractere.
     */
    private static final char[] FORMULA_PREFIXES = {'=', '+', '-', '@', '\t', '\r'};

    private final StringBuilder buffer = new StringBuilder();
    private int lines;

    /** Ecrit une ligne d'en-tetes. */
    public CsvWriter header(String... columns) {
        return row(columns);
    }

    /** Ecrit une ligne de valeurs, en appliquant echappement et neutralisation. */
    public CsvWriter row(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                buffer.append(SEPARATOR);
            }
            buffer.append(escape(format(values[i])));
        }
        buffer.append("\r\n");
        lines++;
        return this;
    }

    /**
     * Ecrit une collection en reutilisant le meme mapping.
     * La fonction doit retourner un tableau de la meme taille que les
     * en-tetes, sinon les lignes seraient desynchronisees.
     */
    public <T> CsvWriter rows(Collection<T> items, Function<T, Object[]> mapper) {
        for (T item : items) {
            row(mapper.apply(item));
        }
        return this;
    }

    public String build() {
        return UTF8_BOM + buffer;
    }

    /** Nombre de lignes ecrites, en-tete comprise. */
    public int lineCount() {
        return lines;
    }

    // ------------------------------------------------------------------

    private String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            // Point decimal en base : le tableur applique la locale d'affichage.
            return decimal.toPlainString();
        }
        if (value instanceof TemporalAccessor temporal) {
            return temporal.toString();
        }
        if (value instanceof Enum<?> enumeration) {
            return enumeration.name();
        }
        return String.valueOf(value);
    }

    /**
     * Neutralise les formules puis echappe selon RFC 4180.
     * L'apostrophe de neutralisation est placee AVANT l'echappement, donc
     * elle fait partie du contenu de la cellule.
     */
    String escape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String value = neutralizeFormula(raw);

        boolean mustQuote = value.indexOf(SEPARATOR) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0
                || value.startsWith(" ");

        if (!mustQuote) {
            return value;
        }

        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * Prefixe d'une apostrophe toute valeur interpretable comme une formule.
     *
     * Exception : les nombres negatifs ne sont PAS neutralises. Les montants en
     * FCFA sont frequemment negatifs (remise, avoir, ajustement) ; en prefixant
     * une apostrophe, Excel les traiterait comme du texte et les additions de
     * colonne seraient faussees. Un vrai nombre est sans risque.
     */
    String neutralizeFormula(String raw) {
        if (raw.isEmpty() || isNumeric(raw)) {
            return raw;
        }

        int index = 0;
        // Ignore les espaces, tabulations et BOM en tete pour l'inspection :
        // un attaquant pourrait les placer pour contourner le test.
        while (index < raw.length() && isIgnorable(raw.charAt(index))) {
            index++;
        }
        if (index >= raw.length()) {
            return raw;
        }

        char first = raw.charAt(index);
        for (char prefix : FORMULA_PREFIXES) {
            if (first == prefix) {
                return "'" + raw;
            }
        }
        return raw;
    }

    /**
     * Vrai si la valeur est un nombre simple, eventuellement signe ou decimal.
     * Le motif est volontairement strict : il n'accepte ni espace, ni separateur
     * de milliers, ni notation exponentielle, ce qui exclut de fait les charges
     * utiles malveillantes comme "-cmd|'/C calc'!A0".
     */
    private boolean isNumeric(String raw) {
        int i = 0;
        int length = raw.length();

        if (i < length && (raw.charAt(i) == '-' || raw.charAt(i) == '+')) {
            i++;
        }

        int chiffres = 0;
        int points = 0;
        while (i < length) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                chiffres++;
            } else if (c == '.' || c == ',') {
                if (++points > 1) {
                    return false;
                }
            } else {
                return false;
            }
            i++;
        }
        return chiffres > 0;
    }

    private boolean isIgnorable(char c) {
        return c == '﻿' || c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
