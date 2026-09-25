package com.vollailelink.backend.controller;

import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exports CSV du back-office.
 *
 * Chaque export est trace dans audit_log (CDC section 2.5) : qui, quoi,
 * combien de lignes, avec quels filtres.
 *
 * Les fichiers sont servis avec un BOM UTF-8 et un separateur point-virgule,
 * pour s'ouvrir correctement dans Excel et LibreOffice en francais.
 */
@RestController
@RequestMapping("/api/admin/exports")
@RequiredArgsConstructor
public class AdminExportController {

    /**
     * Horodatage du nom de fichier.
     *
     * Attention : le motif contient une heure et des minutes, il s'applique
     * donc a un LocalDateTime. Applique a un LocalDate, il echoue avec
     * "Unsupported field: HourOfDay".
     */
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    private final ExportService exportService;

    @GetMapping("/clients")
    public ResponseEntity<byte[]> exportClients() {
        return csv(exportService.exportClients(), "clients");
    }

    @GetMapping("/commandes")
    public ResponseEntity<byte[]> exportCommandes(
            @RequestParam(required = false) CommandeStatut statut
    ) {
        String suffixe = statut != null ? "_" + statut.name().toLowerCase(java.util.Locale.ROOT) : "";
        return csv(exportService.exportCommandes(statut), "commandes" + suffixe);
    }

    @GetMapping("/stock")
    public ResponseEntity<byte[]> exportStock() {
        return csv(exportService.exportStock(), "stock");
    }

    @GetMapping("/reservations")
    public ResponseEntity<byte[]> exportReservations(
            @RequestParam(required = false) ReservationStatut statut
    ) {
        String suffixe = statut != null ? "_" + statut.name().toLowerCase(java.util.Locale.ROOT) : "_actives";
        return csv(exportService.exportReservations(statut), "reservations" + suffixe);
    }

    @GetMapping("/audit")
    public ResponseEntity<byte[]> exportAuditLog(
            @RequestParam(required = false) String eventType
    ) {
        String suffixe = eventType != null && !eventType.isBlank()
                ? "_" + eventType.toLowerCase(java.util.Locale.ROOT)
                : "";
        return csv(exportService.exportAuditLog(eventType), "audit_log" + suffixe);
    }

    // ------------------------------------------------------------------

    private ResponseEntity<byte[]> csv(String content, String nomBase) {
        String nomFichier = "vollailelink_" + nomBase + "_"
                + LocalDateTime.now().format(HORODATAGE) + ".csv";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
