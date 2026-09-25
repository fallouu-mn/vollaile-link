# Cahier des charges - Vollaile Link

**Version** : 1.0  
**Date** : 21 septembre 2026  
**Type de projet** : Application web responsive avec espace administrateur privé  
**Technologies requises** : Angular (Frontend) & Spring Boot (Backend)  
**Zone de lancement** : Sénégal (priorité Dakar et sa région)  
**Modèle d'affaires** : Intermédiation commerciale et centrale d'approvisionnement en volailles  

---

## 1. Présentation du projet

### 1.1. Nom du projet
Le projet s'appelle **Vollaile Link**.

Vollaile Link est une plateforme numérique destinée à centraliser les disponibilités de plusieurs producteurs de volaille et à les présenter de manière anonymisée aux acheteurs professionnels et particuliers au Sénégal (priorité Dakar et sa région).

---

## 2. Principes directeurs et règles d'or (Sécurité & Métier)

1. **Anonymisation stricte des offres publiques** : Ne JAMAIS exposer publiquement le nom, l'adresse, la marge, le prix d'achat, ou les coordonnées des producteurs.
2. **Isolation API Publique / Admin** : DTOs dédiés (`publicapi`) et endpoints distincts (`/api/public/**` vs `/api/admin/**`).
3. **Gestion rigoureuse des stocks** : `Stock disponible = Total - Réservé - Vendu`. Tout mouvement passe par une transaction avec traçabilité et verrouillage.
4. **Devise & Précision financière** : Montants exclusivement en FCFA (`long` ou `BigDecimal`).
5. **Traçabilité & Audit** : Pistes d'audit obligatoires sur les connexions, mouvements de stock, variations de prix, changements de statuts et d'exports.
6. **Dédoublonnage Client** : Identification client par numéro de téléphone unique (+221 format E.164).
