# ADR 0002 – Double réservation empêchée par une contrainte d'exclusion PostgreSQL

- Statut : accepté
- Date : 2026-11-22

## Contexte

Le domaine refuse un créneau qui chevauche une réservation active de la même salle. Mais deux collaborateurs qui réservent au même instant passent tous les deux cette vérification avant que l'un ou l'autre n'ait écrit : la vérification applicative seule laisse passer des doubles réservations.

## Options étudiées

| Option | Inconvénient |
|---|---|
| Verrou applicatif ou `synchronized` | Inopérant dès qu'il y a plusieurs instances de l'API |
| Verrou pessimiste sur la salle (`SELECT ... FOR UPDATE`) | Sérialise toutes les réservations d'une salle, y compris sur des jours différents |
| Isolation `SERIALIZABLE` | Échecs de sérialisation à rejouer, sur toutes les transactions concernées |
| Contrainte d'exclusion GiST | Spécifique à PostgreSQL |

## Décision

La table `booking` porte la contrainte `booking_no_overlap` :
- elle exclut deux lignes de même `room_id` dont les intervalles `tsrange(starts_at, ends_at)` se chevauchent ;
- elle ne s'applique qu'aux réservations actives.

L'adaptateur écrit immédiatement (`saveAndFlush`) et traduit la violation de cette contrainte en règle métier `SLOT_TAKEN`. La vérification du domaine est conservée, pour un message précis dans le cas courant.

## Conséquences

- La garantie tient quel que soit le nombre d'instances de l'API ; elle est démontrée par un test d'intégration qui envoie six demandes simultanées.
- L'extension `btree_gist` est nécessaire (incluse dans l'image officielle de PostgreSQL).
- Le projet est lié à PostgreSQL. C'est assumé : la règle est trop importante pour reposer sur le seul code applicatif.
