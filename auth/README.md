# Authentification commune

**Keycloak émet les JWT. Chaque backend les vérifie avec Spring Security.**
Realm : `transport-ws`. Connexion utilisateur : Authorization Code + PKCE (S256).
Les mots de passe ne passent pas par nos contrôleurs REST.

## Rôles retenus

| Rôle utilisateur | Accès métier |
|---|---|
| `ADMIN` | Lire et modifier les données ; comptes administrés séparément dans Keycloak |
| `AGENT` | Lire, créer, modifier, supprimer les brouillons et exécuter les opérations métier |
| `LECTEUR` | Consulter uniquement |

Les restrictions métier restent applicables : même un ADMIN ne peut modifier un chargement validé.
Les comptes techniques ont des rôles distincts ; ils ne doivent jamais recevoir ADMIN ou AGENT.

## Démarrer sur ce PC

Ouvrir Docker Desktop, puis à la racine du dépôt :

```powershell
.\auth\demarrer.ps1
```

Le script crée des secrets aléatoires dans `.secrets/auth.env` (ignoré par Git), puis lance Keycloak
sur <http://localhost:8090>. La première initialisation peut prendre une minute.
Console : <http://localhost:8090/admin/>, utilisateur `admin`, mot de passe `KEYCLOAK_ADMIN_PASSWORD`.

| Compte de démonstration | Mot de passe dans le fichier privé | Rôle |
|---|---|---|
| `admin-ws` | `WS_ADMIN_PASSWORD` | ADMIN |
| `agent-ws` | `WS_AGENT_PASSWORD` | AGENT |
| `lecteur-ws` | `WS_LECTEUR_PASSWORD` | LECTEUR |

Les comptes et clients sont importés une seule fois. Modifier le JSON ou le fichier privé ne modifie
pas un realm déjà importé : utiliser la console pour changer les comptes ou faire tourner les secrets.
Les données Keycloak sont conservées dans son volume, séparément de la base métier Neon.

Dans le fichier privé utilisé pour démarrer Chargements, ajouter :

```properties
AUTH_ISSUER_URI=http://localhost:8090/realms/transport-ws
CHARGEMENTS_CLIENT_SECRET=<valeur du même nom dans .secrets/auth.env>
```

Puis démarrer Chargements avec `run-local.ps1` comme auparavant. Toutes ses routes métier exigent
un jeton. Swagger reste accessible ; **Authorize** accepte le JWT sans le préfixe `Bearer`.

## Se connecter avec Postman

Dans Authorization : **OAuth 2.0**, puis **Get New Access Token** :

| Paramètre | Valeur |
|---|---|
| Grant type | Authorization Code (With PKCE) |
| Callback URL | `https://oauth.pstmn.io/v1/browser-callback` |
| Authorize using browser | Activé |
| Auth URL | `http://localhost:8090/realms/transport-ws/protocol/openid-connect/auth` |
| Access Token URL | `http://localhost:8090/realms/transport-ws/protocol/openid-connect/token` |
| Client ID | `transport-web` |
| Client secret | Vide (client public) |
| Code challenge method | SHA-256 / S256 |
| Scope | `openid` |

Se connecter avec l'un des comptes ci-dessus, puis **Use Token**. La requête porte
`Authorization: Bearer <access_token>`. Le jeton d'accès dure cinq minutes.
Sans jeton/jeton invalide : **401**. Jeton valide mais rôle insuffisant : **403**.

Le frontend utilise `transport-web` avec PKCE. Les adresses locales autorisées sont
`http://localhost:5173` et `http://localhost:3000`. Aucun secret client dans le navigateur.
La configuration exportée autorise la déconnexion vers ces mêmes adresses.
Pour un realm déjà importé avant l'ajout du frontend, ouvrir **Clients → transport-web → Settings**
et mettre **Valid post logout redirect URIs** à `+` (réutilise les adresses de redirection autorisées).

## Appels entre les quatre backends

Chaque backend obtient un jeton avec `grant_type=client_credentials` et son propre secret.
Il ne transmet pas le jeton utilisateur à ses voisins.

| Appelant / client ID | Rôle dans son JWT | API destinataire / audience |
|---|---|---|
| `chargements-service` | `SERVICE_CHARGEMENTS` | Camions : `camions-api`, lecture du camion |
| `bons-service` | `SERVICE_BONS` | Chargements : `chargements-api`, lecture du chargement |
| `transports-service` | `SERVICE_TRANSPORTS` | Camions : `camions-api`, réserver/libérer ; Bons : `bons-api`, lire/confirmer |
| `camions-service` | `SERVICE_CAMIONS` | Transports : `transports-api`, lire les voyages actifs |

Les jetons utilisateurs ont les quatre audiences. Les comptes techniques ont uniquement les audiences
de leurs destinataires. Le realm ajoute les rôles dans le claim **`roles`**.

### À reprendre par chaque étudiant

Les quatre backends sont protégés dans la branche d'intégration. Les configurations
suivantes expliquent le fonctionnement commun :

1. Ajouter `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-oauth2-client`
   et `spring-security-test` (test).
2. Reprendre `SecurityConfig.java` de Chargements : claim `roles`, préfixe `ROLE_`, mode sans session.
   Adapter les routes et les rôles techniques selon le tableau ci-dessus ; refuser les autres routes.
3. Configurer l'émetteur et l'audience de **son** API :

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AUTH_ISSUER_URI}
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${AUTH_ISSUER_URI}/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt.audiences=camions-api
```

Ici `camions-api` est un exemple : remplacer par `bons-api` ou `transports-api` selon le backend.
La signature, l'expiration, l'émetteur et l'audience doivent tous être vérifiés.

4. Pour OpenFeign, reprendre les propriétés OAuth2 de Chargements : remplacer le client ID, le secret
   et le nom d'enregistrement ; conserver le fournisseur `transport` et le même endpoint `/token`.
   Le secret vient des variables d'environnement. Pour Transports, un même client technique peut
   appeler Camions et Bons, car son jeton contient les deux audiences.

**Un seul fournisseur pour l'équipe.** Le serveur installé ici est local : `localhost` chez un ami
désigne son PC. Pour une démonstration avec plusieurs machines, utiliser une URL Keycloak commune
joignable par tous, adapter `KC_HOSTNAME`, `AUTH_ISSUER_URI` et les URL de redirection/CORS.
Le mode `start-dev` et HTTP sont prévus pour le développement local. L'hébergement doit utiliser HTTPS
et une configuration Keycloak de production ; il n'est pas mis en place par ces fichiers.

## Vérifier

```powershell
cd services/chargements
.\mvnw.cmd verify
```

Les tests vérifient les signatures JWT, l'émetteur, l'audience, l'expiration, les rôles, CORS et l'envoi
du jeton technique par OpenFeign. Ils utilisent H2 et des serveurs HTTP locaux, sans accès à Neon.

Références : [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html),
[OAuth2 OpenFeign](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html),
[Keycloak](https://www.keycloak.org/server/containers).
