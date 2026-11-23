# language: fr
Fonctionnalité: Gérer les salles et les imprévus
  L'office manager peut libérer une salle ou la mettre hors service ;
  les organisateurs concernés sont prévenus avec le motif.

  Contexte:
    Soit nous sommes le mardi 1er décembre 2026 à 09:00
    Et "Vauban" est réservée demain de 10:00 à 11:00 par Camille
    Et "Vauban" est réservée demain de 14:00 à 15:00 par Hugo

  Scénario: Une salle mise hors service annule les réservations à venir
    Quand Nadia met la salle "Vauban" hors service pour "Dégât des eaux"
    Alors 2 réservations sont annulées
    Et Camille est prévenue : "Motif : Salle Vauban hors service : Dégât des eaux"
    Et Hugo est prévenu : "a été annulée"
    Et Camille ne peut pas réserver "Vauban" demain de 16:00 à 17:00 : "ROOM_OUT_OF_SERVICE"

  Scénario: Seul un office manager peut mettre une salle hors service
    Quand Camille met la salle "Vauban" hors service pour "Je préfère une autre salle"
    Alors l'action est interdite

  Scénario: L'office manager annule une réservation avec un motif
    Quand Nadia annule la réservation de Camille pour "Visite du comité de direction"
    Alors la réservation de Camille est "Annulée"
    Et Camille est prévenue : "Motif : Visite du comité de direction"

  Scénario: Un collègue ne peut pas annuler la réservation d'un autre
    Quand Hugo annule la réservation de Camille pour "J'ai besoin de la salle"
    Alors l'action est refusée : "NOT_ORGANIZER"
