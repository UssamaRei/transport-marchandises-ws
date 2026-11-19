# Frontend Transit

Les vues **Chargements** et **Bons de livraison** utilisent les API réelles : préparation et validation
des chargements, émission d’un bon depuis un chargement validé, puis consultation du snapshot et de la preuve.
Connexion Keycloak avec PKCE, renouvellement du JWT et accès en lecture seule pour LECTEUR.

## Lancer

Le lancement complet demande Java 21, Maven, Node.js 22.12+ et Docker Desktop.

Sur ce PC, depuis la racine du dépôt, ouvrir Docker Desktop puis exécuter :

```powershell
.\demarrer-local.ps1
```

Le script construit les deux backends, lance Camions, Chargements et Keycloak, puis le frontend.
Il utilise les fichiers privés `camions_app.env` et `chargements_app.env` du dossier
`Desktop/Neon-WS-Credentials`. Un autre dossier peut être fourni avec `-CredentialsDirectory`.
Ces backends utilisent la vraie base commune ; les données saisies dans l'interface y sont enregistrées.
Ctrl+C arrête Vite ; les backends restent dans Docker Desktop.

### Frontend seul

Prérequis : Node.js 22.12+ et npm. Démarrer d'abord Keycloak et les backends
[Chargements](../services/chargements/README.md), [Camions](../services/flotte/README.md) et
[Bons](../services/bons/README.md).

```powershell
cd frontend
npm install
npm run dev
```

Ouvrir <http://localhost:5173>. Le bouton **Se connecter** ouvre la page Keycloak.
Les comptes de développement sont décrits dans [auth/README.md](../auth/README.md).
Ne jamais mettre de mot de passe ou de secret de compte technique dans le frontend.

Par défaut : API Camions `8081`, Chargements `8082`, Bons `8083`, Transports `8084`, Keycloak `8090`.
Les routes `/api/v1/chargements`, `/api/v1/camions`, `/api/v1/bons` et `/api/v1/transports`
passent par le proxy Vite en développement.
Copier `.env.example` vers `.env.local` uniquement pour changer les adresses.
Les redirections Keycloak doivent autoriser l'adresse du frontend, y compris la déconnexion.

Les écrans Figma non connectés sont conservés à **`/maquettes`**, avec une bannière indiquant qu'ils
utilisent des données de démonstration. Ils ne modifient pas la base et ne prouvent pas
qu'un backend est implémenté. La recherche de chargements filtre la page affichée.

## Vérifier et construire

```powershell
npm run typecheck
npm run build
```

Le résultat est dans `dist/`. En hébergement, servir `index.html` pour les routes de navigation
et acheminer `/api/v1/chargements`, `/api/v1/camions`, `/api/v1/bons` et `/api/v1/transports` vers leurs backends,
ou définir `VITE_API_BASE_URL` et configurer CORS côté backends. Le proxy Vite n'est pas un serveur de production.
