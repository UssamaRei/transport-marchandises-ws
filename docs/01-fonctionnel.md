# Ce que permet l'application

## Le besoin

Une entreprise doit savoir quelles marchandises transporter, quel camion utiliser, quel document accompagne la livraison et où en est le voyage. L'application regroupe ces informations.

## Le parcours principal

1. Un opérateur enregistre un camion et sa capacité.
2. Il prépare un chargement : origine, destination, destinataire, marchandises et poids.
3. Il vérifie que le camion souhaité convient et valide le chargement.
4. Il crée un bon de livraison à partir de ce chargement.
5. Il planifie le transport ; le camion est alors réellement réservé.
6. Il enregistre le départ puis les événements du voyage.
7. Il confirme la livraison ; le bon reçoit sa preuve et le camion est libéré.

Exemple : transporter **160 sacs de 50 kg**, soit **8 000 kg**, de Rabat à Casablanca avec un camion de **10 000 kg** de capacité.

## Fonctions de la première version

| Flotte | Chargements | Bons | Transports |
|---|---|---|---|
| Gérer les camions | Préparer les marchandises | Émettre un bon | Affecter le camion |
| Lire leur capacité | Calculer le poids | Consulter le document | Déclarer le départ |
| Vérifier la disponibilité | Valider un chargement | Garder une copie des données | Suivre les événements |
| Réserver/libérer un camion | Exposer son détail | Confirmer la réception | Terminer ou annuler |
| Gérer la maintenance | Garder le validé immuable | Exposer le bon au suivi | Exposer les voyages actifs |

## Règles simples proposées

- Un transport utilise un camion, un bon et un chargement.
- Un camion n'a qu'une réservation active à la fois. Pas de planning de créneaux futurs dans le socle.
- Un chargement peut contenir plusieurs lignes, mais une seule origine et une seule destination.
- Le poids total est calculé par le serveur, pas décidé par le frontend.
- Le camion souhaité lors de la préparation est celui utilisé dans le premier socle ; ce choix ne le réserve pas encore.
- Un chargement validé ne se modifie plus. Pour corriger, créer un nouveau brouillon.
- Un bon correspond à un chargement. Il peut avoir des tentatives de transport annulées, mais au plus une tentative non annulée.

## Extensions au choix

Camions : chauffeurs, documents, maintenance préventive. Chargements : volume, catégories, photos. Bons : impression PDF, QR code, signature. Transports : incidents, carte, GPS, retards et statistiques.

Ces extensions ne sont pas nécessaires pour commencer. Le groupage, les tournées multi-clients et les livraisons partielles changent plusieurs modules : les décider ensemble avant de coder.
