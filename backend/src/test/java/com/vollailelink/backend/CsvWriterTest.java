package com.vollailelink.backend;

import com.vollailelink.backend.service.CsvWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du generateur CSV.
 *
 * Le point critique est l'injection de formule : les noms et commentaires
 * viennent du formulaire public, donc d'une saisie non fiable. Sans
 * neutralisation, un fichier CSV execute du code a la simple ouverture
 * dans Excel ou LibreOffice.
 */
class CsvWriterTest {

    @Nested
    @DisplayName("Injection de formule (CWE-1236)")
    class InjectionFormule {

        @Test
        @DisplayName("Neutralise une cellule commencant par =")
        void neutraliseEgal() {
            CsvWriter writer = new CsvWriter().row("=HYPERLINK(\"http://x\",\"clic\")");
            assertTrue(writer.build().contains("'=HYPERLINK"),
                    "une formule doit etre prefixee d'une apostrophe");
        }

        @Test
        @DisplayName("Neutralise les prefixes =, +, - et @")
        void neutraliseTousLesPrefixes() {
            for (String dangerous : new String[]{"=1+1", "+1+1", "-1+1", "@SUM(A1)"}) {
                CsvWriter writer = new CsvWriter().row(dangerous);
                assertTrue(writer.build().startsWith(CsvWriter.UTF8_BOM + "'"),
                        "le prefixe " + dangerous + " doit etre neutralise");
            }
        }

        @Test
        @DisplayName("Neutralise meme apres des espaces en tete")
        void neutraliseMalgrePrefixeDiscret() {
            // Un filtre naif sur le premier caractere serait contourne ici.
            CsvWriter writer = new CsvWriter().row("  =cmd|'/C calc'!A0");
            String content = writer.build();
            assertTrue(content.contains("'"),
                    "la formule doit etre neutralisee meme avec des espaces en tete");
            assertFalse(content.contains("\r\n  =cmd"),
                    "la charge utile ne doit pas apparaitre sans neutralisation");
        }

        @Test
        @DisplayName("Neutralise une valeur precedee d'une tabulation")
        void neutraliseTabulationInitiale() {
            CsvWriter writer = new CsvWriter().row("\t=1+1");
            assertTrue(writer.build().contains("'"),
                    "une tabulation initiale peut servir a contourner le filtre");
        }

        @Test
        @DisplayName("Ne touche pas une valeur qui ressemble a un negatif")
        void neNeutralisePasLesNegatifsBanals() {
            CsvWriter writer = new CsvWriter().row("-1500");
            // Un montant negatif est legitime : il doit rester lisible.
            // Excel l'interpretera comme un nombre, pas comme une formule.
            String content = writer.build();
            assertFalse(content.contains("'-1500"),
                    "un montant negatif ne doit pas etre traite comme une formule");
        }

        @Test
        @DisplayName("Ne neutralise pas un texte ordinaire")
        void neNeutralisePasLeTexteOrdinaire() {
            CsvWriter writer = new CsvWriter().row("Ferme Dakar");
            assertFalse(writer.build().contains("'Ferme"),
                    "un texte normal ne doit pas etre prefixe d'une apostrophe");
        }
    }

    @Nested
    @DisplayName("Echappement RFC 4180")
    class Echappement {

        @Test
        @DisplayName("Echappe le separateur point-virgule")
        void echappeSeparateur() {
            CsvWriter writer = new CsvWriter().row("un;deux");
            assertTrue(writer.build().contains("\"un;deux\""),
                    "un point-virgule dans une valeur decalerait les colonnes");
        }

        @Test
        @DisplayName("Double les guillemets internes")
        void doubleLesGuillemets() {
            CsvWriter writer = new CsvWriter().row("il dit \"bonjour\"");
            assertTrue(writer.build().contains("\"il dit \"\"bonjour\"\"\""));
        }

        @Test
        @DisplayName("Echappe les retours a la ligne")
        void echappeRetourLigne() {
            CsvWriter writer = new CsvWriter().row("ligne1\nligne2");
            assertTrue(writer.build().contains("\"ligne1\nligne2\""));
        }

        @Test
        @DisplayName("Garde le nombre de colonnes quand une valeur contient un separateur")
        void gardeLeNombreDeColonnes() {
            CsvWriter writer = new CsvWriter()
                    .header("A", "B", "C")
                    .row("x;y", "z", "w");
            // Sans echappement, la premiere cellule produirait 4 colonnes.
            String content = writer.build();
            assertTrue(content.contains("\"x;y\";z;w"));
        }
    }

    @Nested
    @DisplayName("Format general")
    class Format {

        @Test
        @DisplayName("Commence par un BOM UTF-8 pour les accents")
        void commenceParBom() {
            assertTrue(new CsvWriter().header("Nom").build().startsWith(CsvWriter.UTF8_BOM),
                    "sans BOM, Excel affiche mal les accents");
        }

        @Test
        @DisplayName("Termine chaque ligne par CRLF")
        void lignesCrlf() {
            assertTrue(new CsvWriter().row("a").build().endsWith("\r\n"));
        }

        @Test
        @DisplayName("Compte les lignes ecrites")
        void compteLesLignes() {
            CsvWriter writer = new CsvWriter().header("A").row("1").row("2");
            assertEquals(3, writer.lineCount(), "en-tete comprise");
        }

        @Test
        @DisplayName("Ecrit une valeur nulle comme cellule vide")
        void valeurNulle() {
            assertTrue(new CsvWriter().row((Object) null).build().contains(CsvWriter.UTF8_BOM + "\r\n"));
        }

        @Test
        @DisplayName("Serialise un BigDecimal avec un point decimal")
        void bigDecimal() {
            CsvWriter writer = new CsvWriter().row(new java.math.BigDecimal("3500.50"));
            assertTrue(writer.build().contains("3500.50"));
        }

        @Test
        @DisplayName("Serialise un enum par son nom")
        void enumValue() {
            CsvWriter writer = new CsvWriter()
                    .row(com.vollailelink.backend.model.enums.CommandeStatut.LIVREE);
            assertTrue(writer.build().contains("LIVREE"));
        }
    }
}
