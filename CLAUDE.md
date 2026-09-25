# Vollaile Link

Plateforme d'approvisionnement en volailles (Sénégal, priorité Dakar).
Le site public affiche des offres ANONYMISÉES. Un back-office privé gère producteurs,
clients, stocks, commandes, livraisons, paiements manuels et marges.
Spécification complète (source de vérité en cas de doute) : @docs/cahier-des-charges.md
Avancement : docs/PROGRESS.md (à mettre à jour à la fin de chaque phase).

## Stack
- backend/ : Java (LTS récent), Spring Boot (dernière version stable), Spring Web,
  Spring Security, Spring Data JPA, Bean Validation, Flyway, springdoc-openapi,
  PostgreSQL, Maven.
- frontend/ : Angular (dernière version stable), composants standalone, formulaires
  réactifs, lazy loading, Angular SSR pour les pages publiques (SEO + aperçus WhatsApp).
- Développement local : PostgreSQL natif installé sur Windows, accessible sur `localhost:5433`.
- `docker-compose.yml` est conservé uniquement comme option de déploiement serveur ; il n'est pas requis pour le développement local.
- Le code est en anglais (classes, variables, endpoints). Les textes affichés à
  l'utilisateur, les messages d'erreur et la documentation sont en français.

## Règles non négociables (sécurité et métier)
1. JAMAIS dans une réponse publique : prix d'achat, marge, coûts logistiques, coordonnées
   ou nom des producteurs, adresse exacte, notes internes, données des clients.
2. Ne jamais renvoyer une entité JPA dans une API. Toujours des DTO. Les DTO publics
   vivent dans un package séparé (`publicapi`) et sont mappés explicitement, champ par champ.
3. Les endpoints publics sont sous /api/public/**. Tout le reste exige une authentification
   admin, vérifiée côté backend (pas seulement par un guard Angular).
4. Stock : disponible = total - réservé - vendu. Toute variation passe par un mouvement
   de stock (type, quantité, motif, commande, utilisateur, date), dans une transaction,
   avec verrouillage (@Version ou verrou pessimiste). Jamais de mise à jour directe du stock.
5. Argent : FCFA, montants entiers (long) ou BigDecimal. Jamais de double/float.
6. Audit obligatoire pour : connexions, changements de prix, de stock, de statut, exports.
7. Suppression logique (statut archivé), pas de DELETE des données métier.
8. Secrets (mots de passe, clés, identifiants admin) : uniquement variables
   d'environnement. Jamais dans le code, les tests, les commits ni ce fichier.
   Mots de passe hachés avec BCrypt ou Argon2.
9. Numéros de téléphone stockés au format E.164 (+221...). L'admin se connecte avec son
   numéro de téléphone. Message d'erreur de login générique.
10. Fichiers privés (preuves de paiement, documents) jamais servis par une URL publique.

## Conventions
- Backend : couches controller / service / repository / dto / mapper. Validation sur les
  DTO d'entrée. Exceptions gérées par un @RestControllerAdvice. Migrations Flyway
  uniquement (jamais de ddl-auto=update).
- Frontend : un module de fonctionnalité par domaine (lazy loaded), services HTTP,
  intercepteur d'auth, guards, composants réutilisables. Mobile d'abord.
- Accessibilité minimale : contrastes, boutons larges, textes alternatifs, navigation clavier.
- Commits petits et clairs (Conventional Commits).

## Tests (obligatoires avec le code)
- Tests unitaires sur : calcul de marge, calcul du stock, transitions de statut.
- Tests d'intégration sur : endpoints privés sans authentification (doivent refuser),
  et sur les endpoints publics (aucun champ privé dans le JSON).
- Test de concurrence sur la réservation de stock.

## Commandes
### Backend (Spring Boot)
- Compiler et tester : `mvn verify`
- Lancer l'application : `mvn spring-boot:run`
- Lancer les tests unitaires : `mvn test`
- Générer le fichier JAR : `mvn package`

### Frontend (Angular)
- Installer les dépendances : `npm install`
- Lancer le serveur de développement : `ng serve`
- Compiler pour la production : `ng build --configuration=production`
- Lancer les tests unitaires : `ng test`
- Lancer l'end-to-end tests : `ng e2e`

### PostgreSQL local (Windows)
- Le développement local utilise PostgreSQL natif installé sur Windows.
- La base est accessible sur `localhost:5433` via les variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD`.
- Copier `.env.example` vers un fichier `.env` local, puis remplacer les placeholders par les identifiants PostgreSQL natifs.
- Le fichier `.env` est ignoré par Git et ne doit jamais être committé.
- Lancer le backend avec le profil `dev` ; Flyway applique les migrations au démarrage.
- Docker n'est pas requis pour le développement local.

### Déploiement serveur optionnel
- `docker-compose.yml` peut être utilisé ultérieurement pour un environnement serveur ou une intégration isolée.
- Ne pas l'utiliser pour le développement local Windows.

### Phase 0 specific
- Initialiser la base PostgreSQL native et vérifier la connexion sur `localhost:5433`.
- Lancer le backend avec le profil `dev` afin que Flyway applique les migrations.
- Créer l'administrateur initial en définissant `ADMIN_PHONE` et `ADMIN_PASSWORD` dans le `.env` local, puis lancer le backend.
## Méthode de travail
- Commence chaque phase par un plan, attends ma validation avant de coder.
- Reste dans le périmètre de la phase demandée. Signale ce qui manque au lieu de l'inventer.
- Lance les tests avant de dire que c'est terminé. Mets à jour docs/PROGRESS.md.
- En cas d'ambiguïté avec le cahier des charges, pose la question.
