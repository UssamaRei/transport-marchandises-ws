# Transport de marchandises - Web Services

Préparer les marchandises, émettre un bon de livraison et suivre le voyage du camion.

**4 backends Spring Boot, une base PostgreSQL commune sur Neon, des échanges REST / JSON.**
**Chargements**, **Camions**, **Bons de livraison** et **Transports / Suivi** ont une implémentation exécutable.
Voir [démarrage, routes et exemples Chargements](services/chargements/README.md).

**Authentification :** [Keycloak, rôles, connexion Postman et intégration des backends](auth/README.md).
Les quatre backends vérifient les JWT du realm commun. Chargements, Bons et Transports
utilisent leurs comptes techniques pour les appels inter-services.

**Intégration des API, sans frontend :** [lancer les tests des quatre services](tests/integration/README.md).

**Interface :** [frontend Transit](frontend/README.md), avec les cinq vues Chargements connectées
aux API et les autres écrans conservés en maquettes. Sur Windows, démarrer Docker Desktop puis
lancer `.\demarrer-local.ps1` ; ouvrir <http://localhost:5173>.

## Ma partie

| Étudiant | Module | Son guide |
|---|---|---|
| Ibnahmad Hassan | Camions | [Fonctions, classes et API](services/flotte/README.md) |
| Sina Oussama | Chargements | [Fonctions, classes et API](services/chargements/README.md) |
| Aitlahmous Hmad | Bons de livraison | [Fonctions, classes et API](services/bons/README.md) |
| Chammakh Ayoub | Suivi des transports | [Fonctions, classes et API](services/transports/README.md) |

## Les diagrammes

[Diagramme de classes du système](docs/diagramme-classes.md)

Une flèche signifie « appelle ce module » :

```mermaid
flowchart LR
    C[Chargements] -->|Vérifier le camion| F[Camions]
    B[Bons de livraison] -->|Lire le chargement validé| C
    T[Transports / Suivi] -->|Lire le bon et confirmer la réception| B
    T -->|Réserver et libérer le camion| F
    F -->|Vérifier les voyages actifs| T
```

## La base commune

Les quatre backends se connectent à **la même base Neon**. Chaque module gère ses tables.
Les échanges métier entre modules passent par leurs API, même avec une base commune.
Les accès sont transmis en privé et configurés dans l’IDE ou dans les variables d’environnement.
Voir [.env.example](.env.example) pour les noms des variables. Aucun mot de passe dans Git.
Les huit tables sont créées sur Neon. Voir [les tables et les comptes par module](database/README.md).
La migration V2 ajoute les données de preuve nécessaires à la reprise d'une livraison.

Une copie locale facultative de PostgreSQL peut être lancée avec :

```sh
cp .env.example .env
docker compose up -d
```

Ce Compose lance uniquement une base locale vide ; la migration initiale se trouve dans `database/`.
Le module Chargements possède son Dockerfile. Les autres Dockerfiles et le Compose
des quatre backends seront ajoutés pendant l’intégration.

## Travailler ensemble

Chacun travaille dans son dossier `services/`. Créer une branche, pousser ses changements,
puis ouvrir une pull request vers `main`. Les changements de tables se décident ensemble.
Chaque backend documentera ses routes avec Swagger / OpenAPI.
