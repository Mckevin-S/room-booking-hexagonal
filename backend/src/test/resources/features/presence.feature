# language: fr
Fonctionnalité: Confirmer sa présence pour ne pas bloquer les salles
  Une salle réservée mais inoccupée est libérée pour les autres :
  l'organisateur confirme sa présence entre 10 minutes avant et 15 minutes après le début.

  Contexte:
    Soit nous sommes le mardi 1er décembre 2026 à 09:00
    Et "Vauban" est réservée aujourd'hui de 10:00 à 11:00 par Camille

  Scénario: L'organisateur confirme sa présence à l'heure
    Soit il est 09:55
    Quand Camille confirme sa présence
    Alors la réservation de Camille est "Présence confirmée"
    Quand il est 10:30
    Et les salles inoccupées sont libérées
    Alors la réservation de Camille est "Présence confirmée"

  Scénario: Trop tôt pour confirmer sa présence
    Soit il est 09:45
    Quand Camille confirme sa présence
    Alors l'action est refusée : "CHECK_IN_WINDOW_CLOSED"

  Scénario: Sans présence confirmée, la salle est libérée et l'organisateur prévenu
    Soit il est 10:15
    Quand les salles inoccupées sont libérées
    Alors la réservation de Camille est "Libérée (absence)"
    Et Camille est prévenue : "La salle a été libérée faute de présence confirmée"
    Et Hugo peut réserver "Vauban" aujourd'hui de 10:30 à 11:00

  Scénario: La salle n'est pas libérée avant la fin du délai de grâce
    Soit il est 10:14
    Quand les salles inoccupées sont libérées
    Alors la réservation de Camille est "Confirmée"
