# ADR 0003 – Tests de propriétés et tests de mutation

- Statut : accepté
- Date : 2026-11-22

## Contexte

Une couverture de lignes élevée ne prouve pas que les tests détectent une erreur : un test peut exécuter une ligne sans vérifier son résultat. Par ailleurs, les règles de chevauchement ont de nombreux cas limites (bornes égales, créneau inclus, créneaux consécutifs) qu'une liste d'exemples couvre mal.

## Décision

- **jqwik** vérifie les propriétés de `TimeSlot` sur des créneaux générés :
  - symétrie du chevauchement ;
  - équivalence avec un modèle de référence en minutes ;
  - absence de chevauchement entre deux créneaux consécutifs.
- **PIT** mute le bytecode du domaine et des cas d'usage (conditions inversées, bornes modifiées, valeurs de retour remplacées). Un mutant qui survit révèle un test manquant. La CI échoue sous **80 %** de mutants détectés ou **90 %** de couverture des lignes mutées.
- Les tests de mutation tournent dans un profil Maven dédié, après `mvn verify`, pour ne pas ralentir la boucle de développement locale.

## Conséquences

- Les tests du domaine vérifient les bornes exactes (10 minutes avant, 15 minutes après, 4 heures, 30 jours) : c'est là que les mutants de comparaison survivent sinon.
- Le rapport PIT est publié en artefact de CI.
- Le seuil peut bloquer une contribution pour un test faible : c'est voulu.
