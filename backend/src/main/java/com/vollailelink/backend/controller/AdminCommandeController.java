package com.vollailelink.backend.controller;

import com.vollailelink.backend.dto.CommandeDTO;
import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.service.CommandeService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/commandes")
@RequiredArgsConstructor
public class AdminCommandeController {

    private final CommandeService commandeService;

    @GetMapping
    public ResponseEntity<List<CommandeDTO>> getAllCommandes() {
        return ResponseEntity.ok(commandeService.getAllCommandes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeDTO> getCommandeById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.getCommandeById(id));
    }

    @PostMapping
    public ResponseEntity<CommandeDTO> createCommande(@Valid @RequestBody CommandeDTO dto) {
        CommandeDTO created = commandeService.createCommande(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<CommandeDTO> updateStatut(
            @PathVariable Long id,
            @RequestBody CommandeStatusRequest request) {
        CommandeDTO updated = commandeService.updateStatut(id, request.getStatut());
        return ResponseEntity.ok(updated);
    }

    @Data
    public static class CommandeStatusRequest {
        private CommandeStatut statut;
    }
}
