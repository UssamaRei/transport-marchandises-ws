# Diagramme de classes du système

Les huit classes métier sont stockées dans **une base PostgreSQL commune**.

```mermaid
classDiagram
    direction LR
    class Camion {
      -String id
      -String immatriculation
      -decimal capaciteKg
      -String etat
      +estDisponible() boolean
    }
    class ReservationCamion {
      -String id
      -decimal poidsKg
      -String statut
      +liberer()
    }
    class Chargement {
      -String id
      -String origine
      -String destination
      -decimal poidsTotalKg
      -String statut
      +calculerPoids() decimal
      +valider()
    }
    class LigneChargement {
      -String id
      -String designation
      -int quantite
      -decimal poidsUnitaireKg
    }
    class BonLivraison {
      -String id
      -String numero
      -String statut
      +emettre()
      +confirmerReception()
    }
    class LigneBon {
      -String id
      -String designation
      -int quantite
      -decimal poidsUnitaireKg
    }
    class Transport {
      -String id
      -String statut
      -DateTime departPrevu
      +demarrer()
      +confirmerLivraison()
    }
    class EvenementSuivi {
      -String id
      -String type
      -String description
      -DateTime dateEvenement
    }
    Camion "1" -- "0..*" ReservationCamion : réservations
    Transport "1" -- "0..1" ReservationCamion : réservation
    Camion "0..1" -- "0..*" Chargement : camion souhaité
    Chargement "1" *-- "0..*" LigneChargement : contient
    Chargement "1" -- "0..1" BonLivraison : produit
    BonLivraison "1" *-- "1..*" LigneBon : contient
    BonLivraison "1" -- "0..*" Transport : tentatives
    Camion "1" -- "0..*" Transport : affectation
    Transport "1" *-- "0..*" EvenementSuivi : suivi
```

- `1` : exactement un ; `0..1` : facultatif ; `0..*` : plusieurs possibles ; `1..*` : au moins un.
- Le losange plein indique une composition : les lignes appartiennent à leur document.
- Un brouillon peut être vide. La validation exige au moins une ligne et un camion adapté.
- Une seule réservation active par camion et un seul transport non annulé par bon.

[Retour au projet](../README.md)
