# Décisions proposées à valider

| Sujet | Proposition de départ | Motif |
|---|---|---|
| Découpage | Flotte, Chargements, Bons, Transports | Quatre responsabilités métier compréhensibles |
| Communication | REST avec JSON | Interopérabilité entre langages |
| Stockage | Une base logique par module | Propriété claire des données et appels API réels |
| Moteur fourni | PostgreSQL, remplaçable | Schémas SQL et infrastructure d'exemple communs |
| Frameworks | Au choix, pas encore décidés | L'équipe n'a pas encore réparti les technologies |
| IDs | Chaînes opaques, UUID possibles | Éviter les conversions divergentes entre langages |
| Mesures | Kg, quantités entières, décimal | Calcul et contrôle de capacité cohérents |
| Créneaux | Une réservation active par camion | Simplifier le premier parcours |
| Documents | Bon avec instantané d'un chargement validé immuable | Conserver les données de livraison |
| Coordination | Transports déclenche réservation et clôture | Un seul module possède le voyage |
| Reprise | Étapes persistées, appels idempotents | Gérer une réponse perdue sans doublon |
| Frontend | Interface commune possible, technologie à choisir | Présenter l'ensemble sans imposer un cinquième backend |

## Ce qui vient du sujet

Quatre modules autonomes en équipe de quatre, un service exposé et un service consommé par module, Dockerfiles, Compose commun, documentation des interfaces, dépôt GitHub, suivi des séances et démonstration. Le sujet autorise REST ou SOAP ; l'équipe a demandé REST.

Le sujet ne prescrit pas ce schéma SQL, ces endpoints ni un hébergement public. Les exemples PostgreSQL ne retirent pas la liberté de choisir un autre moteur.

## Évolutions à décider plus tard

Authentification et droits, chauffeurs, annulation de bons, documents PDF, groupage, tournées, GPS, livraisons partielles et notifications. Certaines options modifient plusieurs contrats : les décider collectivement.

## Références techniques

- [OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3) : structure des contrats HTTP.
- [Docker Compose — réseau](https://docs.docker.com/compose/how-tos/networking/) : noms de service et ports internes.
- [GitHub — diagrammes Mermaid](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-diagrams) : rendu des blocs `mermaid` dans Markdown.
- [Mermaid — diagrammes de classes](https://mermaid.js.org/syntax/classDiagram.html) : syntaxe des classes et relations.

Les diagrammes du dépôt utilisent une syntaxe simple pour rester lisibles dans GitHub. Les fichiers `.mmd` permettent de modifier les diagrammes indépendamment du texte.
