package com.vollailelink.backend.controller;

import com.vollailelink.backend.dto.DemandeDTO;
import com.vollailelink.backend.model.enums.DemandeStatut;
import com.vollailelink.backend.service.DemandeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/demandes")
@RequiredArgsConstructor
public class AdminDemandeController {

    private final DemandeService demandeService;

    @GetMapping
    public ResponseEntity<List<DemandeDTO>> getAllDemandes() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeDTO> getDemandeById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getDemandeById(id));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<DemandeDTO> updateStatut(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        DemandeDTO updated = demandeService.updateStatut(id, request.getStatut(), request.getCommentairesAdmin());
        return ResponseEntity.ok(updated);
    }

    @Data
    public static class StatusUpdateRequest {
        private DemandeStatut statut;
        private String commentairesAdmin;
    }
}
