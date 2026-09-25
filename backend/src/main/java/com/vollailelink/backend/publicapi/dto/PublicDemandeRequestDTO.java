package com.vollailelink.backend.publicapi.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicDemandeRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 200, message = "Le nom ne doit pas dépasser 200 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 200, message = "Le prénom ne doit pas dépasser 200 caractères")
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+221[0-9]{8,9}$", message = "Format de téléphone invalide (exemple: +221771234567)")
    private String telephone;

    @Email(message = "L'adresse email est invalide")
    @Size(max = 255, message = "L'email ne doit pas dépasser 255 caractères")
    private String email;

    @NotBlank(message = "Le type de client est obligatoire")
    @Size(max = 50, message = "Le type de client est trop long")
    private String typeClient;

    @NotBlank(message = "La zone de livraison est obligatoire")
    @Size(max = 100, message = "La zone de livraison est trop longue")
    private String zoneLivraison;

    @Positive(message = "L'identifiant du produit est invalide")
    private Long produitId;

    @NotBlank(message = "Le produit souhaité est obligatoire")
    @Size(max = 200, message = "Le nom du produit est trop long")
    private String nomProduit;

    @NotNull(message = "La quantité souhaitée est obligatoire")
    @Positive(message = "La quantité doit être supérieure à zéro")
    private Integer quantiteSouhaitee;

    @DecimalMin(value = "0.01", message = "Le poids doit être supérieur à zéro")
    @Digits(integer = 6, fraction = 2, message = "Le poids comporte trop de chiffres")
    private BigDecimal poidsSouhaite;

    @DecimalMin(value = "0", message = "Le prix souhaité ne peut pas être négatif")
    @Digits(integer = 13, fraction = 2, message = "Le prix souhaité comporte trop de chiffres")
    private BigDecimal prixUnitaireSouhaite;

    @NotNull(message = "La date souhaitée est obligatoire")
    @FutureOrPresent(message = "La date souhaitée ne peut pas être dans le passé")
    private LocalDate dateSouhaitee;

    @Size(max = 300, message = "L'adresse de livraison est trop longue")
    private String adresseLivraison;

    @Size(max = 50, message = "La préférence de contact est trop longue")
    private String preferenceContact;

    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères")
    private String message;

    @Positive(message = "L'identifiant de l'offre est invalide")
    private Long offreId;

    @NotNull(message = "Le consentement est obligatoire")
    @AssertTrue(message = "Vous devez accepter la politique de confidentialité")
    private Boolean consentement;
}
