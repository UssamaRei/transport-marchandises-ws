# Modèles relationnels par base

Ces quatre diagrammes montrent **les tables locales et leurs vraies clés étrangères**. Il n'y a aucune relation SQL entre les quatre bases. Les colonnes de référence externes sont annotées dans les scripts SQL.

## Base Flotte

```mermaid
erDiagram
    CAMION ||--o{ RESERVATION_CAMION : historique
    CAMION {
      varchar id PK
      varchar immatriculation UK
      numeric capacite_kg
      varchar etat
      boolean archive
      timestamptz created_at
    }
    RESERVATION_CAMION {
      varchar id PK
      varchar camion_id FK
      varchar transport_id UK
      numeric poids_kg
      varchar statut
      varchar motif_liberation
      timestamptz released_at
    }
```

Index unique partiel : un seul `camion_id` dont la réservation est ACTIVE. `transport_id` référence l'API Transports, sans FK locale.

## Base Chargements

```mermaid
erDiagram
    CHARGEMENT ||--o{ LIGNE_CHARGEMENT : contient
    CHARGEMENT {
      varchar id PK
      varchar origine
      varchar destination
      varchar destinataire_nom
      varchar camion_souhaite_id
      numeric poids_total_kg
      varchar statut
      integer version
    }
    LIGNE_CHARGEMENT {
      varchar id PK
      varchar chargement_id FK
      varchar designation
      integer quantite
      numeric poids_unitaire_kg
    }
```

Le brouillon peut être vide ; l'application exige au moins une ligne pour passer à VALIDE. `camion_souhaite_id` est une référence Flotte.

## Base Bons

```mermaid
erDiagram
    BON_LIVRAISON ||--|{ LIGNE_BON : copie
    BON_LIVRAISON {
      varchar id PK
      varchar numero UK
      varchar chargement_id UK
      varchar camion_souhaite_id
      varchar origine
      varchar destination
      numeric poids_total_kg
      varchar statut
      varchar transport_preuve_id UK
      varchar recepteur
      timestamptz livre_le
    }
    LIGNE_BON {
      varchar id PK
      varchar bon_id FK
      varchar designation
      integer quantite
      numeric poids_unitaire_kg
    }
```

L'application crée le bon et au moins une ligne ensemble. Les données des lignes et adresses sont des copies historiques. Les IDs chargement, camion et transport sont externes.

## Base Transports

```mermaid
erDiagram
    TRANSPORT ||--o{ EVENEMENT_SUIVI : chronologie
    TRANSPORT {
      varchar id PK
      varchar bon_id
      varchar camion_id
      varchar reservation_id UK
      varchar statut
      varchar sync_etat
      timestamptz depart_prevu
      timestamptz depart_reel
      timestamptz arrivee_reelle
      boolean preuve_confirmee
      boolean liberation_confirmee
    }
    EVENEMENT_SUIVI {
      varchar id PK
      varchar transport_id FK
      varchar type
      varchar description
      timestamptz date_evenement
      numeric latitude
      numeric longitude
    }
```

Index unique partiel : un seul transport non ANNULE par `bon_id`. Bon, camion et réservation sont externes.

Ces diagrammes sélectionnent les champs métier importants. Les [scripts SQL complets](../03-modele-donnees.md) contiennent aussi les horodatages, contrôles et colonnes techniques de reprise.
