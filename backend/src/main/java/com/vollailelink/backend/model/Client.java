package com.vollailelink.backend.model;

import com.vollailelink.backend.model.enums.Sexe;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(nullable = false, length = 200)
    private String prenom;

    @Column(nullable = false, unique = true, length = 20)
    private String telephone;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(length = 100)
    private String region;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sexe sexe;

    @Column(nullable = false)
    @Builder.Default
    private Boolean professionnel = false;

    @Column(length = 200)
    private String entreprise;

    @Column(name = "secteur_activite", length = 200)
    private String secteurActivite;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
