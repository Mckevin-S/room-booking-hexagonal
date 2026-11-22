# language: fr
Fonctionnalité: Réserver une salle de réunion
  Pour tenir mes réunions sans conflit
  En tant que collaborateur
  Je veux trouver une salle adaptée et la réserver

  Contexte:
    Soit nous sommes le mardi 1er décembre 2026 à 09:00

  Scénario: La salle la mieux ajustée est proposée en premier
    Quand Camille cherche une salle demain de 10:00 à 11:00 pour 5 personnes
    Alors les salles proposées sont "Vauban, Beffroi, Grand-Place"

  Scénario: Les équipements demandés filtrent les salles
    Quand Camille cherche une salle demain de 10:00 à 11:00 pour 2 personnes avec "Visioconférence, Écran"
    Alors les salles proposées sont "Beffroi, Grand-Place"

  Scénario: Une salle réservée n'est plus proposée sur le créneau
    Soit "Vauban" est réservée demain de 10:00 à 11:00 par Hugo
    Quand Camille cherche une salle demain de 10:30 à 11:30 pour 5 personnes
    Alors les salles proposées sont "Beffroi, Grand-Place"

  Scénario: Deux réunions qui se suivent dans la même salle
    Soit "Vauban" est réservée demain de 10:00 à 11:00 par Hugo
    Quand Camille réserve "Vauban" demain de 11:00 à 12:00
    Alors la réservation est confirmée

  Scénario: Quand tout est pris, les créneaux libres les plus proches sont suggérés
    Soit "Vauban" est réservée demain de 10:00 à 11:00 par Hugo
    Et "Grand-Place" est réservée demain de 08:00 à 12:00 par Nadia
    Quand Camille cherche une salle demain de 10:00 à 11:00 pour 6 personnes avec "Tableau blanc"
    Alors aucune salle n'est proposée
    Et les créneaux suggérés sont :
      | salle  | début | fin   |
      | Vauban | 09:00 | 10:00 |
      | Vauban | 11:00 | 12:00 |
      | Vauban | 08:45 | 09:45 |

  Scénario: Un collaborateur ne peut pas être dans deux salles à la fois
    Soit "Beffroi" est réservée demain de 10:00 à 11:00 par Camille
    Quand Camille réserve "Vauban" demain de 10:30 à 11:30
    Alors la réservation est refusée : "ORGANIZER_UNAVAILABLE"

  Plan du scénario: Une demande hors des règles est refusée
    Quand Camille réserve "<salle>" <jour> de <début> à <fin> pour <personnes> personnes
    Alors la réservation est refusée : "<motif>"

    Exemples:
      | salle       | jour        | début | fin   | personnes | motif            |
      | Vauban      | demain      | 10:00 | 11:00 | 7         | OVER_CAPACITY    |
      | Vauban      | demain      | 10:10 | 11:00 | 2         | SLOT_NOT_ALIGNED |
      | Vauban      | aujourd'hui | 08:00 | 08:30 | 2         | SLOT_IN_PAST     |
      | Vauban      | demain      | 07:30 | 08:30 | 2         | SITE_CLOSED      |
      | Vauban      | samedi      | 10:00 | 11:00 | 2         | SITE_CLOSED      |
      | Grand-Place | demain      | 08:00 | 12:15 | 2         | TOO_LONG         |
      | Vauban      | dans 31 jours | 10:00 | 11:00 | 2       | BEYOND_HORIZON   |
