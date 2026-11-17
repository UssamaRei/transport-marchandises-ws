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

[V2__transport_preuve_reprise.sql](V2__transport_preuve_reprise.sql) ajoute les trois colonnes
de preuve préparée utilisées par Transports : réceptionnaire, date et commentaire. Elles restent
nullables pour conserver les transports existants. Bons conserve la preuve officielle ; cette copie
locale permet de reprendre un appel interrompu. Les anciens transports sans copie locale ne sont
pas complétés avec des données inventées.
V2 a été testée sur une branche Neon isolée, puis appliquée à la base commune le **17 novembre 2026**.

IDs : `String` (`varchar(64)`, UUID par défaut). Poids : `BigDecimal` en kg.
Instants : `OffsetDateTime` (`timestamptz`). `transport.preuve_date_reception` est une date civile
(`LocalDate` / `date`). Le contrat de preuve de Bons utilise aussi `dateReception` ; voir
[le format API et sa représentation dans livre_le](../services/bons/README.md#contrat-de-preuve-utilisé-par-transports).
Les tables et colonnes utilisent `snake_case`.
