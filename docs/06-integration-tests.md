# Lancement, intégration et tests

## Ce que le starter contient réellement

- Quatre guides et quatre contrats d'API : **conception**, pas implémentation métier.
- Quatre schémas SQL locaux : exemples PostgreSQL adaptables en migrations.
- `compose.yaml` : lancement des **bases uniquement**.
- `compose.backends.example.yaml` : exemple des quatre backends à utiliser après ajout de leurs applications et Dockerfiles.
- Un script de validation des contrats et liens ; ce script ne teste pas les fonctionnalités de transport.

## Lancer les bases de développement

Installer Docker et disposer de Docker Compose. À la racine :

```sh
cp .env.example .env
docker compose config
docker compose up -d
docker compose ps
```

PowerShell : `Copy-Item .env.example .env`. Modifier les valeurs locales dans `.env` et les conserver hors Git.

| Base | Accès depuis le PC | Nom dans Compose | Utilisateur / base |
|---|---|---|---|
| Flotte | `localhost:5433` | `db-flotte:5432` | `flotte` / `flotte` |
| Chargements | `localhost:5434` | `db-chargements:5432` | `chargements` / `chargements` |
| Bons | `localhost:5435` | `db-bons:5432` | `bons` / `bons` |
| Transports | `localhost:5436` | `db-transports:5432` | `transports` / `transports` |

Les ports des bases sont liés à `127.0.0.1` sur le PC. Ils peuvent être modifiés dans Compose. Les volumes gardent les données lors d'un arrêt ou d'une recréation des conteneurs.

**Initialisation :** les scripts montés dans `/docker-entrypoint-initdb.d` ne s'exécutent que sur un nouveau volume vide. Pour faire évoluer une base existante, écrire une migration. Ne pas supprimer les volumes d'un coéquipier pour appliquer un changement.

## Ajouter les applications

Chaque étudiant ajoute un vrai serveur qui écoute sur `0.0.0.0:8080`, son Dockerfile et la lecture de ses variables d'environnement. `DATABASE_URL` et `PORT` sont des conventions proposées : les adapter au framework choisi. Une chaîne JDBC ne s'écrit pas comme une URL PostgreSQL Python ou Node.

Quand ces prérequis existent, tester le gabarit avec :

```sh
docker compose -f compose.yaml -f compose.backends.example.yaml config
docker compose -f compose.yaml -f compose.backends.example.yaml up --build -d
```

Cette seconde commande ne fonctionne pas avant que les quatre Dockerfiles et applications soient ajoutés. Les services ne dépendent au démarrage que de leur propre base ; les autres APIs sont appelées quand une opération métier en a besoin.

## Jeu de données de démonstration

À créer via les APIs ou des scripts de seed du framework, une fois le code écrit :

- `CAM-001` : 10 000 kg, en service, sans réservation.
- `CAM-002` : 5 000 kg, en service.
- `CHG-001` : 160 sacs de 50 kg, Rabat vers Casablanca, Client A.
- Au départ, aucun bon et aucun transport.

Les [exemples JSON](../contracts/examples/) décrivent les formats de réponse attendus ; ce ne sont pas des données déjà chargées en base.

## Scénarios communs à vérifier

| Action | Résultat attendu |
|---|---|
| Valider 8 000 kg avec CAM-001 | Chargement VALIDE ; E2 appelle E1 |
| Émettre un bon | Bon EMIS ; E3 appelle E2 |
| Créer le transport | E4 lit E3 et réserve chez E1 ; PLANIFIE |
| Réserver deux fois le même camion simultanément | Une seule réservation active |
| Rejouer la même création | Même ressource ; aucun doublon |
| Demander maintenance avant la fin du voyage | Refus ; E1 consulte E4 |
| Déclarer départ puis livraison | Bon LIVRE, réservation libérée, transport LIVRE |
| Valider 8 000 kg sur CAM-002 | Refus de surcharge |
| Arrêter Flotte pendant validation | Erreur contrôlée ; brouillon conservé |
| Perdre la réponse d'une réservation ou libération | État en attente puis reprise sans doublon |
| Redémarrer sans supprimer les volumes | Données et étapes conservées |

Implémenter les tests métier et de concurrence avec le framework de chaque module. Les tests d'intégration utilisent les vrais services dans Compose. Une page de documentation ou un contrat valide ne prouve pas que ces scénarios sont déjà réussis.

## Diagnostic rapide

```sh
docker compose logs db-flotte
docker compose -f compose.yaml -f compose.backends.example.yaml logs transports
```

Vérifier le bon port, les noms des services, la connexion à la base et la correspondance du JSON. Pour les appels entre conteneurs, utiliser `flotte`, `bons`, etc., plutôt que `localhost`.
