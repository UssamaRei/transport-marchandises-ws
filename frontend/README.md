# Une interface commune, si l'équipe la retient

Le projet peut présenter quatre rubriques : Camions, Chargements, Bons de livraison et Suivi des transports. Ce dossier accueillera l'interface après le choix de sa technologie.

| Écran | Backend appelé |
|---|---|
| Liste/ajout des camions, disponibilité, maintenance | Flotte |
| Préparation et validation du chargement | Chargements |
| Émission et consultation d'un bon | Bons |
| Planification, départ, chronologie et livraison | Transports |

Le frontend appelle les APIs ; il n'accède à aucune base. Les appels métier entre backends restent dans les backends : par exemple, c'est Chargements qui demande à Flotte la capacité du camion, même si la validation est déclenchée depuis un bouton.

Un frontend commun n'oblige pas à créer un cinquième backend. Une passerelle ou un reverse proxy reste une option d'intégration. Pour un frontend sur un port local distinct, configurer son origine CORS dans chaque service.

Framework, responsable et lancement : à décider. Aucun code d'interface n'est encore implémenté dans cette base.
