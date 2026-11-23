# Architecture

## Vue d'ensemble

```
┌──────────────────┐   /api (même origine)   ┌────────────────────────────────────────────┐
│  Navigateur      │ ──────────────────────▶ │  Next.js 14 « Quorum »                     │
│  (React)         │                         │  pages + relais /api → API (réécritures)   │
└──────────────────┘                         └──────────────────────┬─────────────────────┘
                                                                    │ HTTP/JSON + JWT
                                             ┌──────────────────────▼─────────────────────┐
                                             │  API Spring Boot (hexagone)                │
                                             │                                            │
                                             │  in.web ─┐                ┌─ out.persistence│──▶ PostgreSQL 16
                                             │  in.event ├─▶ application ─┤  out.security   │
                                             │  in.scheduler┘   │ ports  └─ out.event      │
                                             │                  ▼                         │
                                             │               domain (Java pur)            │
                                             └────────────────────────────────────────────┘
```

## Couches

| Couche | Contenu | Dépend de |
|---|---|---|
| `domain` | `TimeSlot`, `Booking` (agrégat), `BookingPolicy`, `AvailabilitySearch`, `FreeSlots`, `Room`, `Site`, événements | rien (Java pur) |
| `application` | Cas d'usage (`BookingService`, `AvailabilityService`, `RoomService`, `NotificationService`, `AuthenticationService`), ports, vues | `domain` |
| `adapter.in.web` | Contrôleurs REST, DTO, `ApiExceptionHandler` | `application` |
| `adapter.in.event` | `NotificationListener` : événements du domaine → notifications | `application` |
| `adapter.in.scheduler` | `NoShowScheduler` : libération des salles inoccupées | `application` |
| `adapter.out.persistence` | Entités et dépôts JPA, données de démonstration | `application`, `domain` |
| `adapter.out.security` | Jetons JWT, vérification des mots de passe | `application` |
| `adapter.out.event` | Publication des événements sur le bus de Spring | `application` |
| `config` | Assemblage des cas d'usage, transactions, sécurité, format JSON | tout |

La couche application n'utilise aucune annotation Spring. `TransactionConfig` déclare un intercepteur transactionnel sur les méthodes publiques des classes `*Service` du paquet `application` : lecture seule pour les consultations, lecture-écriture pour le reste.

## Réserver une salle

1. `BookingService.book` charge la salle et son site, construit le `TimeSlot` et une `BookingRequest`.
2. `BookingPolicy` vérifie les règles dans l'ordre où un utilisateur les comprend :
   - la demande (objet, participants) ;
   - le créneau (quart d'heure, passé, horizon, durée) ;
   - le site (horaires) ;
   - la salle (hors service, capacité) ;
   - les conflits (salle prise, organisateur déjà en réunion).
3. La réservation confirmée est enregistrée. `BookingRepositoryAdapter` écrit immédiatement en base (`saveAndFlush`). Si une demande concurrente a pris le créneau entre-temps, la contrainte d'exclusion `booking_no_overlap` la rejette et l'adaptateur la traduit en `SLOT_TAKEN` (409).
4. Les événements de l'agrégat (`BookingEvent.Confirmed`) sont publiés après l'enregistrement, dans la même transaction.

## Présence et libération

```
          10 min avant          début           15 min après
               │                  │                   │
  CONFIRMÉE ───┼──── fenêtre de confirmation ─────────┼──▶ LIBÉRÉE (tâche planifiée)
               │        checkIn() ─▶ PRÉSENCE CONFIRMÉE
```

`NoShowScheduler` appelle `BookingService.releaseNoShows` chaque minute. Les réservations confirmées commencées depuis 15 minutes passent à l'état `RELEASED` ; l'événement `Released` produit une notification pour l'organisateur. Les deux bornes sont testées à la seconde près dans `BookingTest`.

## Recherche de disponibilités

`AvailabilitySearch` :
1. retient les salles du site en service, assez grandes et équipées ;
2. écarte celles qu'une réservation active occupe sur le créneau ;
3. trie les salles restantes par places inoccupées (meilleur ajustement), puis par nom ;
4. si aucune ne convient, calcule avec `FreeSlots` les créneaux libres de même durée dans la journée d'ouverture. Elle renvoie les trois plus proches de l'horaire demandé, sans jamais proposer un créneau déjà commencé.

## Modèle de données

| Table | Rôle |
|---|---|
| `site` | Sites et horaires d'ouverture |
| `room` | Salles, capacité, équipements, motif de mise hors service |
| `app_user` | Collaborateurs et office managers |
| `booking` | Réservations, avec verrou optimiste (`version`) et contrainte d'exclusion |
| `notification` | Messages adressés aux organisateurs |

```sql
CONSTRAINT booking_no_overlap EXCLUDE USING gist (
    room_id WITH =,
    tsrange(starts_at, ends_at) WITH &&
) WHERE (status IN ('CONFIRMED', 'CHECKED_IN'))
```

`tsrange` exclut la borne de fin : une réunion de 10 h à 11 h et une autre de 11 h à 12 h coexistent. Les réservations annulées ou libérées ne bloquent plus la salle.

Le schéma est géré par Flyway (`db/migration`). Les heures sont stockées en heure locale du site (`TIMESTAMP`), l'horloge de l'application étant réglée sur `Europe/Paris`.

## Front-end

- Pages clientes de l'App Router ; la session (jeton et collaborateur) est conservée dans le stockage local et expirée côté client à la date du jeton.
- `lib/api.ts` centralise les appels, ajoute le jeton et convertit les erreurs RFC 7807 en messages affichables. Une réponse 401 ferme la session.
- Le planning (`components/Timeline.tsx`) positionne chaque réservation en pourcentage de la journée d'ouverture. Un clic sur une plage libre est converti en quart d'heure (`timeAtOffset`), puis ouvre la fenêtre de réservation.
