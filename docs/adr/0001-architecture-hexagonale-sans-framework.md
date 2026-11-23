# ADR 0001 – Architecture hexagonale, application sans framework

- Statut : accepté
- Date : 2026-11-22

## Contexte

Les règles de réservation (chevauchement, horaires, présence, libération) sont le cœur du produit. Elles doivent pouvoir être développées en TDD, rapidement, et rester lisibles indépendamment de la base de données ou du framework web.

## Décision

- Le code suit l'architecture hexagonale : `domain`, `application` (cas d'usage et ports), `adapter`, `config`.
- La couche application n'a aucune dépendance vers Spring. Les cas d'usage sont instanciés dans `ApplicationConfig`.
- Les transactions sont posées par un intercepteur déclaré dans `TransactionConfig` (méthodes publiques des classes `*Service`), plutôt que par `@Transactional` dans le code métier.
- ArchUnit vérifie à chaque build que le domaine ne dépend que du JDK, que l'application ne dépend d'aucun framework et que les adaptateurs ne dépendent pas les uns des autres.

## Conséquences

- Les tests du domaine et des cas d'usage tournent sans Spring, en quelques millisecondes ; ils sont la cible des tests de mutation.
- La frontière transactionnelle repose sur une convention de nommage : un nouveau cas d'usage doit être une classe `*Service` du paquet `application`. Cette convention est documentée dans CONTRIBUTING.
- Plus de code d'assemblage qu'avec des composants annotés ; c'est le prix d'un cœur métier indépendant.
