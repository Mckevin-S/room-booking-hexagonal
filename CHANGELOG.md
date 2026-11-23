# Changelog

Toutes les évolutions notables du projet sont consignées ici.
Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet respecte le [versionnage sémantique](https://semver.org/lang/fr/).

## [1.0.0] – 2026-11-22

### Ajouté

- Domaine de réservation en Java pur : créneau au quart d'heure, réservation et son cycle de vie, règles d'acceptation, recherche de disponibilités avec suggestions.
- Confirmation de présence et libération automatique des salles inoccupées.
- Gestion des salles par l'office manager : mise hors service, annulation avec motif, remise en service.
- Notifications des organisateurs à partir des événements du domaine.
- API REST documentée (OpenAPI), authentification JWT, erreurs RFC 7807.
- Contrainte d'exclusion PostgreSQL contre les doubles réservations simultanées.
- Interface « Quorum » en Next.js : recherche, planning cliquable, réservations, gestion des salles, notifications.
- Données de démonstration datées par rapport au jour courant : deux sites, neuf salles, quatre comptes.
- Tests : unitaires (TDD), propriétés (jqwik), mutation (PIT, seuil 80 %), scénarios Cucumber en français, règles ArchUnit, intégration Testcontainers, front-end Vitest, bout en bout Playwright.
- Docker Compose et CI GitHub Actions.

[1.0.0]: https://github.com/Mckevin-S/room-booking-hexagonal/releases/tag/v1.0.0
