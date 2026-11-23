# ADR 0004 – Front-end Next.js en relais de l'API, jetons JWT

- Statut : accepté
- Date : 2026-11-22

## Contexte

L'interface doit être rapide à charger, typée et facile à déployer à côté de l'API. L'API doit rester sans état.

## Décision

- Le front-end est une application Next.js 14 (App Router, TypeScript, Tailwind CSS) servie par son propre serveur Node (`output: standalone`).
- Le navigateur n'appelle que `/api` sur l'origine du front-end ; Next.js relaie ces requêtes vers l'API Spring Boot par une réécriture. Aucune adresse d'API ni configuration CORS n'est nécessaire en production.
- L'API émet un jeton JWT signé HS256 (support natif de Spring Security). Il porte l'identifiant du collaborateur, son nom et son rôle. Les droits métier (organisateur, office manager) sont vérifiés dans le domaine et les cas d'usage.

## Conséquences

- L'adresse de l'API est fixée à la construction de l'image du front-end (argument `BACKEND_URL`).
- Le jeton est conservé dans le stockage local du navigateur, et sa durée est limitée (10 h). Un fournisseur OIDC ou un cookie `HttpOnly` posé par le serveur Next.js serait l'étape suivante pour un déploiement réel.
- Les pages sont des composants clients : le rendu serveur n'apporterait rien à une application entièrement derrière une connexion.
