# Plan de test - Phase 3

Ce document décrit les scénarios de test obligatoires pour valider l'implémentation de la Phase 3 du projet Vollaile Link.

## Prérequis
- Backend Spring Boot en cours d'exécution
- Frontend Angular en cours d'exécution
- Base de données PostgreSQL initialisée avec les données de test
- Compte administrateur valide pour accéder au back-office

## Scénarios de test obligatoires

### 1. Stock suffisant
**Objectif** : Vérifier qu'une demande peut être validée quand le stock disponible >= quantité demandée

**Étapes** :
1. Se connecter au back-office en tant qu'admin
2. Vérifier qu'un produit a un stock disponible suffisant (ex: 100 unités)
3. Créer une demande pour ce produit avec une quantité inférieure au stock (ex: 20 unités)
4. Valider la demande (changer le statut en "VALIDEE")
5. Vérifier que le stock disponible a été réduit correspondant à la quantité demandée
6. Vérifier qu'un mouvement de stock de type "VENTE" ou "RESERVATION" a été créé

**Résultat attendu** : La demande est validée, le stock est réduit, un mouvement de stock est enregistré

### 2. Stock épuisé
**Objectif** : Vérifier qu'une demande est refusée ou mise en attente quand le stock disponible = 0

**Étapes** :
1. S'assurer qu'un produit a un stock disponible de 0
2. Créer une demande pour ce produit avec une quantité > 0
3. Tenter de valider la demande
4. Vérifier que la demande est soit refusée, soit placée en attente selon les règles métier
5. Vérifier que le stock reste à 0
6. Vérifier qu'aucun mouvement de stock négatif n'est créé

**Résultat attendu** : La demande n'est pas validée lorsque le stock est insuffisant

### 3. Deux demandes simultanées
**Objectif** : Test de concurrence - deux demandes pour le même produit en même temps

**Étapes** :
1. S'assurer qu'un produit a un stock disponible limité (ex: 50 unités)
2. Créer simultanément deux demandes pour ce produit :
   - Demande A: 30 unités
   - Demande B: 30 unités
3. Valider les deux demandes dans un ordre aléatoire ou simultané
4. Vérifier que :
   - Une seule demande peut être validée totalement (30 unités)
   - La deuxième demande est partiellement validée (20 unités) ou refusée selon les règles
   - Le stock total ne devient jamais négatif
   - Des mouvements de stock appropriés sont créés pour chaque transaction

**Résultat attendu** : Le système gère correctement la concurrence sans permettre un stock négatif

### 4. Réservation expirée
**Objectif** : Vérifier qu'une reservation expirée libère automatiquement le stock

**Étapes** :
1. Créer une demande pour un produit avec stock disponible
2. Valider la demande (ce qui devrait créer une réservation)
3. Vérifier qu'un mouvement de stock de type "RESERVATION" est créé
4. Attendre l'expiration de la reservation (configurée à 20 minutes par défaut)
5. Vérifier que :
   - Le stock réservé est libéré et retourné au stock disponible
   - Un mouvement de stock de type "LIBERATION_RESERVATION" est créé
   - La demande passe en statut "EXPIREE" (si applicable selon les règles)

**Résultat attendu** : Les réservations expirent automatiquement et libèrent le stock

### 5. Annulation
**Objectif** : Vérifier les différents scénarios d'annulation

**Sous-scenario 5.1: Annulation de demande en statut EN_COURS**
1. Créer une demande et la laisser en statut "EN_COURS"
2. Annuler la demande
3. Vérifier que :
   - Le statut de la demande passe à "ANNULEE" ou équivalent
   - Toutes les réservations associées sont libérées
   - Des mouvements de stock de type "LIBERATION_RESERVATION" sont créés pour chaque réservation
   - Le stock disponible est restitué à son niveau initial

**Sous-scenario 5.2: Annulation de commande EN_ATTENTE_PAIEMENT**
1. Créer une demande, la valider, puis la convertir en commande
2. Laisser la commande en statut "EN_ATTENTE_PAIEMENT"
3. Annuler la commande
4. Vérifier que :
   - Le statut de la commande passe à "ANNULEE"
   - Le stock associé est libéré si approprié
   - Aucun paiement n'est traité

**Sous-scenario 5.3: Annulation après paiement**
1. Créer une demande, la valider, la convertir en commande, et la marquer comme payée
2. Tenter d'annuler la commande
3. Vérifier que :
   - L'annulation suit une procédure différente (peut nécessiter un remboursement)
   - Selon les règles métier, cela peut créer un avoir ou déclencher un processus de remboursement

**Résultat attendu** : Les annulations libèrent correctement les ressources associées selon leur stade

### 6. Correction manuelle
**Objectif** : Vérifier qu'un administrateur peut corriger manuellement les niveaux de stock

**Étapes** :
1. Noter le niveau de stock actuel d'un produit
2. Accéder à l'écran de correction de stock (à créer si n'existe pas)
3. Modifier manuellement le niveau de stock (augmenter ou diminuer)
4. Justifier la correction avec un motif
5. Vérifier que :
   - Le stock est mis à jour avec la nouvelle valeur
   - Un mouvement de stock de type "AJUSTEMENT" est créé
   - Le mouvement inclut le motif de la correction
   - Un journal d'audit enregistre la modification
   - La correction est tracable dans l'historique

**Résultat attendu** : Les corrections manuelles sont tracables et créent des mouvements d'ajustement

## Vérifications transversales

### Journaux d'audit
Pour chaque scénario ci-dessus, vérifier que :
- Les connexions au back-office sont journalisées
- Les changements de statut des demandes/commandes sont journalisés
- Les modifications de stock sont journalisées
- Les exports de données sont journalisés (si applicable)
- Les journaux contiennent : utilisateur, action, timestamp, détails de la modification

### Sécurité et confidentialité
Vérifier que :
- Aucune information sensible (prix d'achat, marges, coordonnées producteurs) n'est exposée dans l'interface publique
- Toutes les réponses API publiques ne contiennent que des DTO publics
- Les endpoints privés (/admin/) nécessitent une authentification valide
- Les mots de passe sont stockés hachés (vérifier dans la base de données)
- Les numéros de téléphone sont stockés au format E.164

## Guide de test manuel

### Vérification rapide du stock (API, sans interface)

Le stock est porté par l'offre et suit `disponible = total - réservé - vendu`.

```text
GET  /api/admin/stock/offres                 -> état de toutes les offres
GET  /api/admin/stock/offres/{offerId}       -> total / réservé / vendu / disponible
POST /api/admin/stock/offres/{offerId}/entree      { "quantite": 50, "motif": "..." }
POST /api/admin/stock/offres/{offerId}/sortie      { "quantite": 10, "motif": "..." }
POST /api/admin/stock/offres/{offerId}/ajustement  { "quantite": -5, "motif": "..." }
GET  /api/admin/stock/reservations           -> réservations actives
POST /api/admin/stock/reservations/{id}/liberer  { "motif": "..." }
POST /api/admin/stock/reservations/expire    -> balayage manuel (équivaut au planificateur)
GET  /api/admin/stock/mouvements?offerId=1   -> journal des mouvements
```

### Vérification de la piste d'audit (CDC §2.5)

Les opérations métier sont tracées dans `audit_log` avec l'administrateur,
l'IP et un blob JSON contenant l'état du stock après coup.

```text
GET /api/admin/audit                      -> journal paginé (du plus récent)
GET /api/admin/audit?eventType=STOCK_VENTE -> filtre par type d'événement
GET /api/admin/audit/offres/{offerId}     -> historique complet d'un lot
```

Contrôle d'atomicité (l'audit ne peut pas diverger du stock) :

1. Noter `quantity_total` d'une offre.
2. Provoquer un échec d'écriture dans `audit_log` (contrainte CHECK interdisant
   les événements `STOCK%`).
3. Effectuer une entrée de stock : l'appel doit échouer.
4. Vérifier que `quantity_total` est **inchangé** et qu'aucun mouvement n'a été écrit.
5. Retirer la contrainte : l'opération doit réussir et être tracée.

Motifs d'événements : `STOCK_ENTREE`, `STOCK_SORTIE`, `STOCK_AJUSTEMENT`,
`STOCK_RESERVATION`, `STOCK_LIBERATION`, `STOCK_VENTE`,
`COMMANDE_STATUT_CHANGEMENT`, `DEMANDE_STATUT_CHANGEMENT`, `LOGIN_SUCCESS`.


Scénario de référence (offre à 150 disponibles, commande de 20) :

| Étape | total | réservé | vendu | disponible |
|---|---|---|---|---|
| Initial | 150 | 0 | 0 | 150 |
| Création commande (réservation) | 150 | 20 | 0 | 130 |
| PAYEE / EN_PREPARATION / EXPEDIEE | 150 | 20 | 0 | 130 |
| LIVREE (réservation → vente) | 150 | 0 | 20 | 130 |
| Annulation d'une commande active | 150 | 0 | 0 | 150 |

Le disponible **ne doit jamais bouger** entre l'expédition et la livraison :
c'est précisément le double décompte que l'ancien modèle produisait.

Durée de validité d'une réservation : 20 minutes
(`STOCK_RESERVATION_TTL_MINUTES`, planificateur toutes les 5 minutes).



### Accès au back-office
1. Naviguer vers `http://localhost:4200/admin` (ou l'URL configurée)
2. Se connecter avec les credentials administrateur
3. Naviguer vers les différentes sections :
   - Clients (/admin/clients)
   - Demandes (/admin/demandes)
   - Commandes (/admin/commandes)
   - Stock (/admin/stock)
   - Notifications (/admin/notifications)

### Tests des composants UI
Pour chaque section :
- Vérifier que la liste se charge correctement avec pagination
- Tester les filtres de recherche
- Vérifier que le tri fonctionne sur toutes les colonnes
- Tester la création, l'édition et la suppression d'entités
- Vérifier les messages de confirmation et d'erreur
- Tester la responsivité sur différentes tailles d'écran

### Vérification des prix et de la traçabilité (§2.5)

Toute variation de prix est historisée **et** auditée, dans la même transaction.

```text
GET   /api/admin/offres/{id}/prix                  historique (portée : produit)
GET   /api/admin/offres/produits/{id}/prix        historique du produit
PATCH /api/admin/offres/{id}/prix       { "prix": 3900, "motif": "..." }
PATCH /api/admin/offres/produits/{id}/prix-defaut { "prix": 3200, "motif": "..." }
PATCH /api/admin/offres/{id}/statut     { "statut": "WITHDRAWN", "motif": "..." }
```

Règles vérifiables :

| Cas | Attendu |
|---|---|
| Prix négatif | 400, message explicite |
| Motif vide | 400, motif obligatoire |
| Prix identique | 200, **aucune** ligne ajoutée |
| Retrait d'offre avec réservation active | 400, retrait refusé |
| Variations successives | une ligne `price_history` + une entrée `audit_log` par variation |

Contrôle d'atomicité : saboter `price_history` (contrainte interdisant le prix
retenu) puis tenter une variation — l'appel doit échouer **et** `offers.unit_price`
doit rester inchangé.

### Vérification de la piste d'audit (CDC §2.5)
- Outils de développement du navigateur (Chrome DevTools, Firefox Developer Tools)
- Postman ou similaire pour tester les API directement
- Requêtes SQL pour vérifier l'état de la base de données
- Logs de l'application backend pour vérifier les journaux d'audit

## Critères d'acceptation
La Phase 3 est considérée comme terminée lorsque :
- Tous les scénarios de test obligatoires passent avec succès
- Aucun régressions n'est introduit dans les phases précédentes
- Les journaux d'audit sont correctement générés pour toutes les actions significatives
- L'interface utilisateur est responsive et accessible
- Le code respecte les conventions du projet (Conventional Commits, style de code)