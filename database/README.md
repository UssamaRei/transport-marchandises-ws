# Base PostgreSQL commune

La base Neon `neondb` contient les huit tables du [diagramme](../docs/diagramme-classes.md).

| Backend | Login | Tables qu’il peut lire et modifier |
|---|---|---|
| Camions | `camions_app` | `camion`, `reservation_camion` |
| Chargements | `chargements_app` | `chargement`, `ligne_chargement` |
| Bons | `bons_app` | `bon_livraison`, `ligne_bon` |
| Transports | `transports_app` | `transport`, `evenement_suivi` |

Chaque étudiant reçoit son fichier de connexion en privé et définit les variables
dans la configuration de lancement de son IDE. Un fichier `.env` ne se charge pas
automatiquement dans Spring Boot. Pour un module JPA, utiliser `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.
Chargements utilise `JdbcTemplate` ; il n’a pas besoin des variables JPA et ne modifie pas le schéma.
Les comptes des modules ne créent ni ne suppriment les tables ; les échanges entre modules passent par leurs API.

[V1__initial_schema.sql](V1__initial_schema.sql) crée les tables et contraintes.
Cette version est déjà appliquée sur Neon. Ne pas la réexécuter sur cette base.
Toute évolution prend la forme d’une nouvelle migration discutée en pull request.
Le responsable applique les migrations avec son accès propriétaire, dans une transaction.

IDs : `String` (`varchar(64)`, UUID par défaut). Poids : `BigDecimal` en kg.
Dates : `OffsetDateTime` (`timestamptz`). Les tables et colonnes utilisent `snake_case`.
Chargements implémente son calcul de poids et sa validation. Les autres règles métier
et transitions restent à développer dans les modules concernés.
