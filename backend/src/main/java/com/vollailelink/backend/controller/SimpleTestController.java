package com.vollailelink.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@Profile("dev")
@RequestMapping("/api/public")
public class SimpleTestController {

    @GetMapping("/pages/home")
    public ResponseEntity<Map<String, Object>> getHomePage() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", 1);
        response.put("title", "Accueil - Vollaile Link");
        response.put("slug", "home");
        response.put("content", "<h1>Bienvenue sur Vollaile Link</h1><p>Votre plateforme de mise en relation entre producteurs et consommateurs au Sénégal.</p>");
        response.put("metaTitle", "Accueil - Vollaile Link");
        response.put("metaDescription", "Plateforme de mise en relation directe entre producteurs agricoles et consommateurs au Sénégal");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pages/{slug}")
    public ResponseEntity<Map<String, Object>> getPageBySlug(@PathVariable String slug) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", 1);
        response.put("title", "Page " + slug);
        response.put("slug", slug);
        response.put("content", "<h1>Page " + slug + "</h1><p>Contenu de la page " + slug + ".</p>");
        response.put("metaTitle", "Page " + slug + " - Vollaile Link");
        response.put("metaDescription", "Page " + slug + " de Vollaile Link");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Backend API fonctionne correctement !");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}