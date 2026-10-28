# Contrats REST communs

Ces fichiers décrivent les APIs **à implémenter**. Ils ne lancent pas de serveur. Les étudiants les valident ensemble, puis alignent leurs applications sur les formats retenus.

| Fournisseur | Contrat | Consommateurs métier |
|---|---|---|
| Flotte | [flotte.openapi.yaml](flotte.openapi.yaml) | Chargements et Transports |
| Chargements | [chargements.openapi.yaml](chargements.openapi.yaml) | Bons |
| Bons | [bons.openapi.yaml](bons.openapi.yaml) | Transports |
| Transports | [transports.openapi.yaml](transports.openapi.yaml) | Flotte |

Format **OpenAPI 3.0.3**, chemins complets `/api/v1/...`, plus `/health`. Les `servers` déclarent la racine de chaque serveur, sans doubler `/api/v1`. Importer chaque fichier dans un outil compatible Swagger/OpenAPI ou Postman pour explorer ses opérations.

Chaque opération indique `x-consumers`, une extension documentaire qui nomme ses appelants. [communications.yaml](communications.yaml) récapitule les appels entre backends. Le validateur vérifie que leurs routes existent chez les fournisseurs et que chaque module expose ET consomme une API.

## Exemples

- [Camion](examples/camion.json), [chargement](examples/chargement.json), [bon](examples/bon.json), [transport](examples/transport.json) et [réservation](examples/reservation.json).
- [Requêtes HTTP illustratives](examples/requetes.http).

Ces objets représentent **différentes étapes du parcours**, pas un état unique de bases déjà chargées. Le camion libre est présenté avant sa réservation. Les IDs sont pédagogiques ; les remplacer par les IDs renvoyés par les vrais backends.

Les exemples de ressources figurent aussi dans les schémas OpenAPI. Le [manifeste](examples/manifest.json) relie chaque fichier à son schéma pour la validation automatique.

## Conventions et modifications

Voir [les règles communes](../docs/04-communication-api.md) pour les unités, erreurs, pagination, idempotence et reprises. Les propriétés de création/modification n'acceptent pas les champs calculés ou les statuts arbitraires.

Avant une modification : prévenir le consommateur, changer le contrat, mettre à jour les exemples, exécuter [la validation](../scripts/README.md) puis tester les services ensemble. Ne pas laisser le contrat et le code évoluer dans deux directions différentes.

La sécurité reste à concevoir en équipe ; ces contrats n'annoncent aucun mécanisme d'authentification déjà implémenté.
