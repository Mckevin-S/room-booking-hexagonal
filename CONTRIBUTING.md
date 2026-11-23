# Contribuer à Room Booking Hexagonal

Ce document décrit la façon de travailler sur le projet : branches, commits, revue et qualité.

## Branches (Git Flow simplifié)

| Branche | Rôle | Règle |
|---|---|---|
| `main` | Code livré, chaque version est taguée (`v1.0.0`) | Jamais de commit direct ; uniquement des merges depuis `release/*` ou `hotfix/*` |
| `develop` | Intégration de la prochaine version | Reçoit les branches de travail après revue et CI verte |
| `feature/RB-<n>-<sujet>` | Une fonctionnalité | Créée depuis `develop`, fusionnée dans `develop` |
| `fix/<sujet>` | Une correction sur `develop` | Idem |
| `chore/<sujet>`, `docs/<sujet>` | Outillage, configuration, documentation | Idem |
| `release/<version>` | Stabilisation d'une version | Créée depuis `develop`, fusionnée dans `main` (tag) puis dans `develop` |
| `hotfix/<version>` | Correction urgente en production | Créée depuis `main`, fusionnée dans `main` et `develop` |

Les noms de branches sont en minuscules, avec des tirets : `feature/RB-2-domaine-reservation`, `fix/annulation-motif`.

Les fusions se font sans avance rapide (`--no-ff`) pour garder la trace de chaque branche dans l'historique.

## Messages de commit (Conventional Commits)

```
<type>(<portée>): <description à l'impératif, en minuscules>

<corps facultatif : le pourquoi, pas le comment>
```

| Type | Usage |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction d'anomalie |
| `refactor` | Restructuration sans changement de comportement |
| `test` | Ajout ou correction de tests |
| `docs` | Documentation |
| `build` | Dépendances, Maven, npm, Docker |
| `ci` | Intégration continue |
| `chore` | Maintenance diverse |

Portées utilisées : `domaine`, `reservation`, `presence`, `salles`, `api`, `auth`, `db`, `demo`, `front`, `e2e`.

Exemples :

```
feat(reservation): suggérer les créneaux libres les plus proches
fix(presence): libérer la salle à 15 minutes pile après le début
test(domaine): propriétés du chevauchement de créneaux
```

## Pull requests

1. Une PR = un sujet. Elle cible `develop`.
2. La CI doit être verte : build, tests unitaires et de propriétés, scénarios Cucumber, règles d'architecture, tests d'intégration, score de mutation d'au moins 80 %, lint, types, tests de bout en bout.
3. Le modèle de PR (`.github/pull_request_template.md`) sert de liste de contrôle.
4. Toute nouvelle règle métier arrive avec son scénario Gherkin ou son test unitaire.

## Qualité du code

### Back-end

- Le domaine se développe en TDD : un test rouge, le code minimal, puis la refactorisation. Le score de mutation (PIT) sert de garde-fou : un test qui ne détecte pas qu'on a cassé une règle ne protège rien.
- Architecture hexagonale : `domain` (Java pur), `application` (cas d'usage et ports, sans Spring), `adapter` (web, événements, planificateur, persistance, sécurité), `config` (assemblage). Les règles sont vérifiées par `HexagonalArchitectureTest`.
- Les objets métier protègent leurs invariants : `TimeSlot` refuse un créneau vide ou sur deux jours, `Booking` n'accepte que les transitions prévues.
- Une règle de réservation se teste d'abord dans `BookingPolicyTest`, puis se raconte dans un scénario Gherkin si elle parle au métier.
- Les propriétés générales (symétrie, cas limites) passent par jqwik plutôt que par une liste d'exemples.
- Erreurs métier : `BusinessRuleViolation` avec un code stable, traduite en réponse RFC 7807 (409 pour les conflits, 422 sinon).
- Tests d'intégration sur une vraie base PostgreSQL (Testcontainers), suffixe `IT`.

### Front-end

- App Router de Next.js, composants clients typés, aucun appel direct à l'API depuis le navigateur hors de `/api`.
- Un seul client HTTP (`lib/api.ts`) ; les erreurs de l'API sont affichées avec leur message métier.
- Les jetons de design (couleurs, rayons, ombres) sont définis dans `tailwind.config.ts`, pas dans les composants.
- Accessibilité : focus visible, fenêtres modales natives (`<dialog>`), libellés sur les boutons d'icône, mouvements réduits respectés.
- `npm run lint` et `npm run typecheck` doivent passer sans avertissement.

## Commandes utiles

```bash
# Back-end
cd backend && mvn verify                                       # tous les tests (Docker requis)
cd backend && mvn test -Dtest=CucumberTest                     # scénarios métier seuls
cd backend && mvn -Pmutation org.pitest:pitest-maven:mutationCoverage   # mutation, après mvn verify

# Front-end
cd frontend && npm run lint && npm run typecheck && npm run test:ci && npm run build

# Bout en bout
docker compose up -d --build && cd frontend && npm run e2e
```
