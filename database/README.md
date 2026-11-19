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

[V1__initial_schema.sql](V1__initial_schema.sql) décrit les tables et contraintes initiales.
Ne pas rejouer V1/V2 sur une base déjà initialisée. Toute évolution future prend la forme
d'une nouvelle migration appliquée par le responsable DB avec son accès propriétaire,
dans une transaction.

[V2__transport_preuve_reprise.sql](V2__transport_preuve_reprise.sql) ajoute les trois colonnes
de preuve préparée utilisées par Transports : réceptionnaire, date et commentaire. Elles restent
nullables pour conserver les transports existants. Bons conserve la preuve officielle ; cette copie
locale permet de reprendre un appel interrompu. Les anciens transports sans copie locale ne sont
pas complétés avec des données inventées.
V2 a été testée sur une branche Neon isolée.

Pour une base PostgreSQL vide, exécuter V1 puis V2 une seule fois avec le compte
propriétaire de la **base commune choisie pour tous les services**. Chaque application
utilise ensuite son compte limité et `ddl-auto=validate`. Les scripts n'insèrent
aucune donnée métier. Le lanceur de [tests d'intégration](../tests/integration/README.md)
effectue cette initialisation sur sa base locale jetable.

IDs : `String` (`varchar(64)`, UUID par défaut). Poids : `BigDecimal` en kg.
Instants : `OffsetDateTime` (`timestamptz`). `transport.preuve_date_reception` est une date civile
(`LocalDate` / `date`). Le contrat de preuve de Bons utilise aussi `dateReception` ; voir
[le format API et sa représentation dans livre_le](../services/bons/README.md#confirmer-la-livraison).
Les tables et colonnes utilisent `snake_case`.
