package com.vollailelink.backend.controller;

import com.vollailelink.backend.dto.ZoneLivraisonDTO;
import com.vollailelink.backend.service.ZoneLivraisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Back-office zones de livraison.
 *
 * L'ecran existait dans la navigation sans API derriere : cet endpoint
 * le rend enfin operationnel.
 */
@RestController
@RequestMapping("/api/admin/zones-livraison")
@RequiredArgsConstructor
public class AdminZoneLivraisonController {

    private final ZoneLivraisonService zoneLivraisonService;

    @GetMapping
    public ResponseEntity<List<ZoneLivraisonDTO>> getAllZones() {
        return ResponseEntity.ok(zoneLivraisonService.getAllZones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZoneLivraisonDTO> getZoneById(@PathVariable Long id) {
        return ResponseEntity.ok(zoneLivraisonService.getZoneById(id));
    }

    @PostMapping
    public ResponseEntity<ZoneLivraisonDTO> createZone(@Valid @RequestBody ZoneLivraisonDTO dto) {
        return new ResponseEntity<>(zoneLivraisonService.createZone(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZoneLivraisonDTO> updateZone(
            @PathVariable Long id,
            @Valid @RequestBody ZoneLivraisonDTO dto
    ) {
        return ResponseEntity.ok(zoneLivraisonService.updateZone(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        zoneLivraisonService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }
}
