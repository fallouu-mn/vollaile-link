# Avancement du projet - Vollaile Link

## Phase 0 : Initialisation

> Développement local : PostgreSQL natif Windows via `localhost:5433`. Docker n'est pas requis ; `docker-compose.yml` est réservé à un futur déploiement serveur.
- [x] Backend Spring Boot créé et configuré (Java 21, Spring Boot 3.2, PostgreSQL, Flyway)
- [x] Frontend Angular créé (avec SSR)
- [x] Configuration de développement PostgreSQL natif validée via `.env` (PostgreSQL 14, `localhost:5433`, schéma Flyway v9)
- [x] Docker Compose conservé uniquement comme option de déploiement serveur, sans utilisation locale
- [x] Authentification admin avec journalisation d'audit
- [x] Configuration Maven et processeurs d'annotations (Lombok + MapStruct)

## Phase 1 : Pages publiques
- [x] Accueil, disponibilités (recherche + filtres), détail d'offre
- [x] Comment ça marche, à propos, zones desservies, FAQ, contact
- [x] Mobile d'abord, SSR, métadonnées SEO et Open Graph
- [x] Formulaire de demande publique avec validation et anti-spam
- [x] Bouton WhatsApp avec message prérempli à partir de l'offre
- [x] Modèle économique conforme : aucun contact direct producteur/vendeur proposé (CTA via Vollaile Link)
- [x] Numéro WhatsApp centralisé dans `volaille-link.config.ts`, vide tant que l'officiel n'est pas fourni
- [x] Fallback formulaire `/demande-devis` quand le numéro WhatsApp n'est pas configuré
- [x] Aucun numéro fictif (`+221xxxxxxxx`) présent dans le code public
- [x] FAQ de la migration V4 alignées sur l'intermédiation
- [x] Lien `#availability` cassé corrigé vers `/disponibilites`
- [x] Vérifié : `offreId` du lien `/demande-devis?offreId=X` est bien consommé par le formulaire et rattaché à la demande
- [ ] Numéro WhatsApp officiel à saisir dans `volaille-link.config.ts`

## Phase 2 : Gestion des demandes (publique)
- [x] Endpoints publics d'offres anonymisées (`/api/public/offers/**`)
- [x] Soumission de demande publique (`/api/public/demandes`) avec validation E.164
- [x] Formulaire public aligné sur le contrat backend, avec quantité, zone, produit, date et consentement
- [x] Réponse publique limitée à une référence et un message, sans données client ou administrateur
- [x] Intégration WhatsApp et création automatique de notification administrateur

## Phase 3 : Back-office et gestion avancée (Re-développement & Restructuration Senior)
- [x] Modèle de données & Entités JPA complets (`Administrator`, `AuditLog`, `ProductCategory`, `Product`, `Producer`, `Offer`, `PriceHistory`, `Client`, `Demande`, `Commande`, `MouvementStock`, `Notification`)
- [x] Entité Client avec déduplication par téléphone (+221 format E.164 - §13)
- [x] Workflow complet des Demandes (EN_COURS -> VALIDEE / REFUSEE / EXPIREE / SANS_SUITE)
- [x] Workflow complet des Commandes (EN_ATTENTE_PAIEMENT -> PAYEE -> EN_PREPARATION -> EXPEDIEE -> LIVREE / ANNULEE)
- [x] Gestion transactionnelle du Stock (ENTREE, SORTIE, RESERVATION, LIBERATION_RESERVATION, VENTE) — **réécritconformément au CDC en Phase 6**
- [x] Système de notifications in-app administrateur (`/api/admin/notifications`)
- [x] Controller REST Admin (`/api/admin/clients`, `/api/admin/demandes`, `/api/admin/commandes`, `/api/admin/stock`, `/api/admin/notifications`)
- [x] Tests unitaires Java (JUnit 5 + Mockito : `ClientServiceTest`, `StockServiceTest`, `PriceServiceTest`) — 54/54 tests validés avec BUILD SUCCESS (`mvn test`)

## Phase 4 : Sécurité et frontend Angular
- [x] Authentification admin par téléphone avec BCrypt et JWT
- [x] Routes publiques ouvertes et routes `/api/admin/**` protégées par JWT
- [x] Bootstrap initial securely configuré via `.env`, sans placeholder
- [x] Vérification HTTP : login 200, endpoint admin avec token 200, token invalide 401
- [ ] Changement de mot de passe initial et récupération/révoquation de session à finaliser
- [x] Compilation de l'application Admin Angular validée (`npx ng build admin`)

## Phase 6 : Moteur de stock conforme au CDC (§2.3)
> Règle appliquée : `disponible = total - réservé - vendu`, **par offre** et non par produit.

- [x] Compteurs par offre : `quantity_total`, `quantity_reserved`, `quantity_sold` (migration V11)
- [x] `quantity_available` devient une colonne **GENERATED** : l'invariant est imposé par PostgreSQL
- [x] Contraintes base : `offers_quantity_non_negative`, `offers_quantity_not_oversold` (survente bloquée même en SQL direct)
- [x] Table `stock_reservations` avec statut `ACTIVE / RELEASED / CONSUMED / EXPIRED` et clé d'idempotence unique
- [x] `StockService` = unique chemin d'écriture, avec verrou pessimiste `SELECT ... FOR UPDATE` sur l'offre
- [x] Réservation sans décrément du total (suppression du double décompte de l'ancien modèle)
- [x] Vente = `reserved → sold` (le disponible ne bouge pas à la livraison)
- [x] Annulation et expiration libèrent la réservation
- [x] Expiration automatique planifiée (`StockReservationScheduler`, TTL 20 min conforme au plan de test)
- [x] Workflow de commande strict : `EN_ATTENTE_PAIEMENT → PAYEE → EN_PREPARATION → EXPEDIEE → LIVREE`, `ANNULEE`/`EXPIREE` terminaux
- [x] Commande liée à une offre (`commandes.offre_id`) : deux offres du même produit ont des stocks distincts
- [x] Migration V12 : élargissement de `mouvement_stocks.type_mouvement` (bug latent : `LIBERATION_RESERVATION` = 22 caractères > VARCHAR(20))
- [x] Tests unitaires `StockServiceTest` — **32/32 tests, BUILD SUCCESS**
- [x] Vérification HTTP complète : réservation, livraison, annulation, expiration, refus de survente, idempotence
- [x] Écrans Admin de stock alignés sur la nouvelle API (`/api/admin/stock/offres/**`, `/reservations`, mouvements)
- [x] Écran réservations branché sur la table réelle `stock_reservations` (plus de devinette par mouvements)
- [x] Expiration affichée depuis l'horodatage serveur (plus de `+20 min` codé en dur côté client)
- [x] Libération manuelle fonctionnelle (l'ancienne action ne faisait qu'un `console.log`)
- [x] `offerId` / `reservationId` exposés dans le JSON des mouvements (absents du mapping)
- [x] Correction du DTO `CommandeDTO` front : `totalHt`/`totalTtc` (le total s'affichait vide)
- [x] Piste d'audit branchée sur `audit_log` (§2.5 du CDC)
- [x] Événements tracés : `STOCK_ENTREE`, `STOCK_SORTIE`, `STOCK_AJUSTEMENT`, `STOCK_RESERVATION`, `STOCK_LIBERATION`, `STOCK_VENTE`, `COMMANDE_STATUT_CHANGEMENT`, `DEMANDE_STATUT_CHANGEMENT`
- [x] Métadonnées JSONB avec l'état du stock après chaque mouvement (rejouable à toute date)
- [x] Atomicité vérifiée : un échec d'audit annule le mouvement de stock (test de sabotage en base)
- [x] Lecture de l'audit : `GET /api/admin/audit`, filtre par type, et `GET /api/admin/audit/offres/{id}` (requête JSONB exacte)
- [x] Acteur et IP enregistrés automatiquement (null pour les traitements automatiques)
- [x] Variations de prix tracées : `PRIX_OFFRE_CHANGEMENT`, `PRIX_PRODUIT_CHANGEMENT` (§2.5)
- [x] Table `price_history` alimentée (elle était vide : aucun code n'écrivait de prix)
- [x] `PriceService` = unique point d'écriture des prix, motif obligatoire, atomicité vérifiée
- [x] Cycle de vie des offres : `OFFRE_STATUT_CHANGEMENT`, refus de retrait si réservations actives
- [x] Anonymisation préservée : aucun prix d'achat exposé publiquement après variation de prix
- [ ] Exports à tracer (§2.5) — aucun export implémenté à ce jour

## Phase 7 : Écrans back-office branchés sur la base
> Correction des écrans qui affichaient des données fictives ou pointaient vers des API inexistantes.

- [x] Dashboard : suppression des chiffres codés en dur (12 producteurs, 89 clients, 1540 unités)
- [x] `GET /api/admin/dashboard` : compteurs réels + répartition du stock (total/réservé/vendu/disponible)
- [x] Activité récente du dashboard alimentée par la piste d'audit (plus de texte fictif)
- [x] Rappel permanent de la règle `disponible = total − réservé − vendu` sur le dashboard
- [x] Zones de livraison : table `zone_livraison` créée (migration V13) + 3 zones de référence Dakar
- [x] `GET/POST/PUT/DELETE /api/admin/zones-livraison` — l'écran de la sidebar ne renvoie plus d'erreur réseau
- [x] Code zone normalisé en majuscules, doublons et délais incohérents refusés, suppression auditée
- [x] `GET /api/admin/products` créé — l'API appelée par les écrans clients/demandes/stock n'existait pas
- [x] Stock produit agrégé depuis les offres (suppression des champs trompeurs `prixUnitaireAchat`/`stockAlerte`)
- [x] Vérifié : `total − réservé − vendu = disponible` sur données réelles
- [x] Écran **Offres & prix** : l'API existait mais n'était atteignable depuis aucune interface
- [x] `/api/admin/stock/offres` enrichi (prix, statut, producteur) pour éviter un second appel
- [x] Dialogue de modification de prix avec calcul de la variation en % et motif obligatoire
- [x] Écran **Journal d'audit** : consultation filtrable de `audit_log`, métadonnées JSONB dépliables
- [x] Route `/stock` par défaut ajoutée (le lien de la barre latérale était une impasse)
- [x] Liens sidebar et titres de page pour Offres & prix et Journal d'audit
- [x] Correction de 8 appels à `produit.nom` / `prixUnitaireVente` qui n'existaient pas dans la réponse backend
- [x] Formulaire zones aligned sur le modèle réel (code, délais min/max, description)
- [x] Test navigateur des écrans Admin (build vert, parcours API validé, mais pas de clic réel effectué)

## Phase 8 : Exports CSV journalisés (§2.5)
> Le CDC impose une piste d'audit sur les **exports** : dernier point manquant de la §2.5.

- [x] 5 exports : clients, commandes, stock, réservations, journal d'audit
- [x] Chaque export tracé dans `audit_log` avec acteur, IP, type, volume et filtres
- [x] Le contenu exporté n'est **pas** recopié dans l'audit (seule la traçabilité de l'extraction)
- [x] **Protection contre l'injection de formule CSV (CWE-1236)** : cellules commençant par `=`, `+`, `-`, `@` neutralisées
- [x] Neutralisation insensible au contournement (espaces, tabulation ou BOM en tête)
- [x] Exception pour les nombres : `-1500` reste un nombre, sinon les montants FCFA négatifs deviennent du texte
- [x] Échappement RFC 4180 (point-virgule, guillemets, retours ligne) + BOM UTF-8 pour Excel
- [x] Refus d'un export dépassant 50 000 lignes plutôt que troncature silencieuse
- [x] Corrections de performance : N+1 supprimé sur les exports commandes et stock
- [x] Boutons d'export dans les écrans clients, commandes, stock, réservations et audit
- [x] Tests `CsvWriterTest` — 16 tests, dont 6 sur l'injection de formule
- [x] Vérifié en conditions réelles avec une charge hostile injectée en base

## Phase 5 : Frontend Angular (SSR Public + Admin SPA)
- [x] Résolution des routes dynamic server SSR (`projects/public/src/app/app.routes.server.ts`)
- [x] Application Public Angular compilée avec succès (`npx ng build public` — **0 erreur**)
- [x] Correction de l'ordre SCSS `@use` (`projects/admin/src/styles.scss`)
- [x] Correction des imports Standalone Angular Material (`MatGridListModule`, `MatFormFieldModule`, `MatSelectModule`, `MatInputModule`, `MatIconModule`, etc.) dans les composants Admin
- [x] Ajout des DTOs/services manquants (`ZoneLivraisonDTO`, `ZoneLivraisonService`) et typage TypeScript strict des composants Admin
- [x] Verification de la compilation de l'application Admin Angular (`npx ng build admin`)