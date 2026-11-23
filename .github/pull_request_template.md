## Objet

<!-- Ce que change cette PR et pourquoi. Référencez le ticket : RB-xx -->

## Type de changement

- [ ] Nouvelle fonctionnalité (`feat`)
- [ ] Correction (`fix`)
- [ ] Refactorisation sans changement de comportement (`refactor`)
- [ ] Documentation, CI, outillage (`docs`, `ci`, `chore`)

## Vérifications

- [ ] Les tests passent en local (`mvn verify`, `npm run test:ci`, `npm run lint`, `npm run typecheck`)
- [ ] Le score de mutation reste au-dessus du seuil (`mvn -Pmutation org.pitest:pitest-maven:mutationCoverage`)
- [ ] Les nouvelles règles métier sont couvertes par un scénario Cucumber ou un test unitaire
- [ ] `HexagonalArchitectureTest` passe (aucune dépendance du domaine vers un framework)
- [ ] La documentation (README, ADR) est à jour si besoin
- [ ] Pas de secret ni de donnée personnelle dans le code

## Captures ou notes pour la revue

<!-- Facultatif -->
