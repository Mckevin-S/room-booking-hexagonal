# Room Booking Hexagonal

[![CI](https://github.com/Mckevin-S/room-booking-hexagonal/actions/workflows/ci.yml/badge.svg)](https://github.com/Mckevin-S/room-booking-hexagonal/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6db33f)
![Next.js 14](https://img.shields.io/badge/Next.js-14-000000)

Réservation des salles de réunion d'une entreprise multi-sites. Les collaborateurs cherchent une salle par taille et par équipement, réservent au quart d'heure et confirment leur présence en arrivant. Une salle réservée mais laissée vide est rendue aux autres au bout de 15 minutes. L'office manager peut mettre une salle hors service : les réservations concernées sont annulées et leurs organisateurs prévenus.

Le projet est aussi un exercice de « craft » :
- un domaine en Java pur, développé en TDD ;
- une architecture hexagonale vérifiée par des tests ;
- des tests de propriétés sur les règles de chevauchement ;
- des tests de mutation avec un seuil bloquant en CI ;
- des scénarios métier en français ;
- des décisions d'architecture tracées dans des ADR.

> Projet personnel de démonstration. L'interface s'appelle « Quorum » ; les sites, salles et collaborateurs sont fictifs.

## Fonctionnalités

- **Recherche de salle** : site, créneau, nombre de participants et équipements (écran, visioconférence, tableau blanc, accès PMR).
  - Les salles libres sont classées de la mieux ajustée à la plus grande, pour ne pas bloquer une salle de 20 places pour une réunion à 4.
  - Si aucune salle ne convient, les trois créneaux libres les plus proches de l'horaire demandé sont proposés.
- **Planning du site** : tableau d'occupation de la journée, salle par salle, avec l'heure courante. Un clic sur une plage libre ouvre la réservation à cet horaire.
- **Confirmation de présence** : possible de 10 minutes avant à 15 minutes après le début. Sans confirmation, une tâche planifiée libère la salle et prévient l'organisateur.
- **Mes réservations** : à venir et passées, confirmation de présence, annulation.
- **Gestion des salles** (office manager) :
  - mise hors service avec un motif, qui annule les réservations à venir ;
  - annulation d'une réservation à la place de son organisateur, motif obligatoire ;
  - remise en service.
- **Notifications** : l'organisateur est informé de tout ce qui arrive à sa réservation sans son intervention.
- **Interface adaptée au mobile**, navigation au clavier, mouvements réduits respectés.

## Stack

| Couche | Technologies |
|---|---|
| Back-end | Java 21, Spring Boot 3.3 (Web, Validation, Data JPA, Security, OAuth2 Resource Server), Flyway, springdoc-openapi |
| Base de données | PostgreSQL 16 (contrainte d'exclusion GiST contre les doubles réservations) |
| Front-end | Next.js 14 (App Router), React 18, TypeScript, Tailwind CSS |
| Qualité | JUnit 5, AssertJ, jqwik (tests de propriétés), PIT (tests de mutation), Cucumber (Gherkin en français), ArchUnit, Testcontainers, JaCoCo, Vitest, Testing Library, ESLint, Playwright |
| Livraison | Docker, Docker Compose, GitHub Actions |

## Architecture

Architecture hexagonale (ports et adaptateurs).
- Le domaine et la couche application ne dépendent d'aucun framework, pas même de Spring : les cas d'usage sont de simples classes Java, assemblées dans la configuration.
- Les transactions sont déclarées autour des cas d'usage sans annotation dans leur code.
- ArchUnit fait échouer le build si une dépendance va dans le mauvais sens.

```
backend/src/main/java/com/mckevin/roombooking
├── domain/            Java pur
│   ├── booking/       Créneau, réservation (agrégat), règles d'acceptation, recherche de disponibilités
│   ├── room/          Salle, capacité, équipements, mise hors service
│   └── site/          Site et horaires d'ouverture
├── application/       Cas d'usage (réserver, annuler, confirmer sa présence, planning...) et ports
├── adapter/
│   ├── in/web/        Contrôleurs REST, erreurs RFC 7807
│   ├── in/event/      Événements du domaine → notifications
│   ├── in/scheduler/  Libération des salles inoccupées, chaque minute
│   └── out/           Persistance JPA, jetons JWT, bus d'événements
└── config/            Assemblage, frontière transactionnelle, sécurité

frontend/src
├── app/               Pages : connexion, accueil, réserver, planning, mes réservations, salles
├── components/        Plaque de salle, planning, fenêtres de réservation et de motif
└── lib/               Client API typé, session, dates et heures
```

Le front-end n'appelle que `/api` sur sa propre origine. Le serveur Next.js relaie ces appels vers l'API Spring Boot : pas de CORS ni d'adresse d'API dans le navigateur.

Plus de détails : [docs/architecture.md](docs/architecture.md) et les décisions d'architecture dans [docs/adr](docs/adr).

### Règles métier

1. Un créneau commence et finit sur un quart d'heure, dure au plus 4 heures et tient dans une journée.
2. On ne réserve ni dans le passé ni plus de 30 jours à l'avance.
3. Un créneau doit tenir dans les horaires du site (du lundi au vendredi, de 8 h à 20 h pour les sites de démonstration).
4. Une salle hors service ne se réserve pas ; le nombre de participants ne dépasse pas sa capacité.
5. Deux réservations actives d'une même salle ne se chevauchent pas. Une réunion qui commence à l'heure où la précédente finit est acceptée.
6. Un organisateur ne peut pas être dans deux salles à la fois.
7. La présence se confirme de 10 minutes avant à 15 minutes après le début, par l'organisateur seulement.
8. Sans présence confirmée 15 minutes après le début, la salle est libérée.
9. L'organisateur annule librement jusqu'à la fin de sa réunion ; un office manager peut annuler à sa place, avec un motif.
10. Une salle mise hors service annule ses réservations à venir, y compris celle en cours.

La règle 5 est vérifiée deux fois :
- par le domaine, pour un message clair ;
- par une contrainte d'exclusion PostgreSQL, qui rejette deux demandes simultanées arrivées entre la vérification et l'écriture.

## API

Documentation interactive : http://localhost:8080/swagger-ui.html (bouton *Authorize* avec le jeton obtenu à la connexion).

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/auth/login` | Se connecte, renvoie un jeton JWT |
| GET | `/api/auth/me` | Collaborateur connecté |
| GET | `/api/sites` | Sites de l'entreprise |
| GET | `/api/sites/{id}/rooms` | Salles du site |
| GET | `/api/sites/{id}/availability?date=&start=&end=&attendees=&equipments=` | Salles libres, ou créneaux proches |
| GET | `/api/sites/{id}/planning?date=` | Réservations de la journée, salle par salle |
| POST | `/api/bookings` | Réserver |
| GET | `/api/bookings/mine?past=` | Mes réservations à venir ou passées |
| POST | `/api/bookings/{id}/check-in` | Confirmer sa présence |
| POST | `/api/bookings/{id}/cancel` | Annuler (motif obligatoire pour un office manager) |
| PUT | `/api/rooms/{id}/out-of-service` | Mettre une salle hors service (office manager) |
| DELETE | `/api/rooms/{id}/out-of-service` | Remettre une salle en service (office manager) |
| GET | `/api/notifications` | Mes notifications |
| POST | `/api/notifications/read` | Tout marquer comme lu |

Exemple :

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"mckevin@demo.fr","password":"demo1234"}' | jq -r .token)

curl -s "http://localhost:8080/api/sites/LIL/availability?date=2026-12-02&start=10:00&end=11:00&attendees=5&equipments=SCREEN" \
  -H "Authorization: Bearer $TOKEN"
```

Les erreurs suivent le format Problem Details (RFC 7807) :

| Code | Cas |
|---|---|
| 400 | Champ invalide ou requête incomplète |
| 401 | Jeton absent ou expiré |
| 403 | Action réservée à l'office manager |
| 404 | Site, salle ou réservation introuvable |
| 409 | Conflit : salle déjà prise (`SLOT_TAKEN`), organisateur déjà en réunion (`ORGANIZER_UNAVAILABLE`), modification concurrente |
| 422 | Règle métier non respectée, avec un `code` stable (`OVER_CAPACITY`, `SITE_CLOSED`, `CHECK_IN_WINDOW_CLOSED`...) |

## Lancer le projet

### Avec Docker (le plus simple)

```bash
docker compose up --build
```

- Application : http://localhost:3000
- API et Swagger : http://localhost:8080/swagger-ui.html

Les données de démonstration sont chargées au premier démarrage, datées par rapport au jour courant : deux sites (Lille et Paris), neuf salles, une vingtaine de réservations. Comptes disponibles ; les trois comptes de Lille se connectent en un clic depuis l'écran de connexion (« Explorer la démonstration ») :

| Compte | Rôle |
|---|---|
| `mckevin@demo.fr` | Collaborateur, Lille |
| `julie@demo.fr` | Collaboratrice, Lille |
| `thomas@demo.fr` | Collaborateur, Paris |
| `nicolas@demo.fr` | Office manager, Lille |

Mot de passe commun : `demo1234`. Pour démarrer sans données de démonstration : `DEMO_DATA=false docker compose up --build`.

Un jour ouvré, pendant les heures d'ouverture, la démonstration contient aussi :
- une réunion qui commence dans quelques minutes, dont la présence est à confirmer depuis l'accueil ;
- une réunion commencée sans présence confirmée, que la tâche planifiée libère dès le démarrage.

### En développement

Prérequis : Java 21, Maven 3.9, Node 20, Docker (pour PostgreSQL et les tests d'intégration).

```bash
# Base de données
docker compose up -d db

# Back-end (données de démonstration activées)
cd backend
DEMO_DATA=true mvn spring-boot:run

# Front-end (relais /api → localhost:8080)
cd frontend
npm install
npm run dev
```

| Variable | Rôle | Valeur par défaut |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL | `jdbc:postgresql://localhost:5432/roombooking`, `roombooking`, `roombooking` |
| `JWT_SECRET` | Clé de signature des jetons (32 caractères minimum) | valeur de développement |
| `JWT_VALIDITY` | Durée de validité des jetons | `PT10H` |
| `DEMO_DATA` | Charge les données de démonstration sur une base vide | `false` (`true` avec Docker Compose) |
| `NO_SHOW_CHECK_INTERVAL` | Fréquence de libération des salles inoccupées | `PT1M` |
| `BACKEND_URL` (front, au build) | Adresse de l'API pour le relais `/api` | `http://localhost:8080` |

## Tests

```bash
cd backend && mvn verify                                   # unitaires, propriétés, BDD, architecture, intégration (Docker requis)
cd backend && mvn -Pmutation org.pitest:pitest-maven:mutationCoverage   # tests de mutation, après mvn verify
cd frontend && npm run lint && npm run typecheck && npm run test:ci
cd frontend && npm run e2e                                 # bout en bout, application lancée avec docker compose
```

- **Domaine (TDD)** :
  - `TimeSlotTest` : construction, chevauchement, alignement ;
  - `BookingPolicyTest` : chaque règle d'acceptation et leur ordre ;
  - `BookingTest` : présence, annulation et libération, aux bornes exactes ;
  - `AvailabilitySearchTest` : classement par ajustement, filtres, suggestions ;
  - `FreeSlotsTest`, `OpeningHoursTest`, `RoomTest`.
- **Propriétés (jqwik)** : `TimeSlotPropertiesTest` vérifie sur des milliers de créneaux générés :
  - que le chevauchement est symétrique ;
  - qu'il correspond à un modèle de référence en minutes ;
  - que deux créneaux consécutifs ne se chevauchent jamais.
- **Mutation (PIT)** : le domaine et les cas d'usage sont mutés automatiquement ; la CI échoue si moins de 80 % des mutants sont détectés. Rapport : `backend/target/pit-reports/index.html`.
- **Comportement (Cucumber, en français)** : les scénarios de `src/test/resources/features` décrivent la réservation, la confirmation de présence et la gestion des salles dans le vocabulaire du métier. Ils s'exécutent sur les cas d'usage avec des adaptateurs en mémoire. Rapport HTML : `backend/target/cucumber-report.html`.
- **Architecture (ArchUnit)** :
  - le domaine ne dépend que du JDK ;
  - l'application ne dépend d'aucun framework ;
  - les adaptateurs ne se connaissent pas entre eux.
- **Intégration (Testcontainers PostgreSQL)** : `RoomBookingApiIT` parcourt l'API complète, droits par rôle compris. Un de ses cas envoie six demandes simultanées sur le même créneau, et une seule doit aboutir.
- **Front-end (Vitest, Testing Library)** : client API et erreurs, heures et frise du planning, fenêtre de réservation, planning.
- **Bout en bout (Playwright)** : recherche et réservation, annulation, réservation depuis le planning, suggestions de créneaux, salle mise hors service et notification de l'organisateur.

La CI GitHub Actions exécute toutes ces suites à chaque push et sur chaque pull request. Les rapports JaCoCo, Cucumber et PIT sont publiés en artefacts.

## Organisation du dépôt

Le développement suit un Git Flow simplifié :
- `main` contient les versions livrées et taguées ;
- `develop` intègre la prochaine version ;
- les branches `feature/RB-<n>-<sujet>`, `fix/...` et `chore/...` partent de `develop` et y reviennent après une CI verte ;
- les messages de commit suivent Conventional Commits.

Le détail figure dans [CONTRIBUTING.md](CONTRIBUTING.md), les versions dans [CHANGELOG.md](CHANGELOG.md).

## Pistes d'évolution

- Réservations récurrentes (chaque lundi, toutes les deux semaines)
- Confirmation de présence par QR code affiché à la porte de la salle
- Synchronisation avec les agendas (Microsoft 365, Google Calendar)
- Statistiques d'occupation par salle pour dimensionner les espaces
- Fuseaux horaires propres à chaque site
