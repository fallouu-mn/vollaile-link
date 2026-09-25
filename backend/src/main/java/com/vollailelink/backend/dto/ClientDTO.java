package com.vollailelink.backend.dto;

import com.vollailelink.backend.model.enums.Sexe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+\\d{1,3}\\d{9,15}$", message = "Format de téléphone invalide (E.164 requis: +221...)")
    private String telephone;

    private String email;
    private String adresse;
    private String ville;
    private String region;
    private LocalDate dateNaissance;
    private Sexe sexe;
    private Boolean professionnel;
    private String entreprise;
    private String secteurActivite;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
