# Frontend Transit

Les quatre modules utilisent les API réelles : **Camions**, **Chargements**,
**Bons de livraison** et **Transports**. Le parcours relie la création d'un camion,
la préparation des marchandises, l'émission du bon, le voyage et sa réception.
Connexion Keycloak avec PKCE, renouvellement du JWT et accès en lecture seule pour LECTEUR.

L’accueil `/` affiche le **tableau de bord** : disponibilité des camions, brouillons,
transports actifs, bons récents et répartition des transports. Les compteurs viennent
des API et couvrent toute la base. Chaque module se charge séparément ; utiliser
**Actualiser** pour relire les données. Les cartes et les listes ouvrent les modules concernés.

## Lancer

Le lancement complet demande Java 21, Maven, Node.js 22.12+ et Docker Desktop.

Sur ce PC, depuis la racine du dépôt, ouvrir Docker Desktop puis exécuter :

```powershell
.\demarrer-local.ps1
```

Le script construit les quatre backends, lance les applications et Keycloak, puis le frontend.
Il utilise les fichiers privés `camions_app.env`, `chargements_app.env`, `bons_app.env`
et `transports_app.env` du dossier
`Desktop/Neon-WS-Credentials`. Un autre dossier peut être fourni avec `-CredentialsDirectory`.
Ces backends utilisent la vraie base commune ; les données saisies dans l'interface y sont enregistrées.
Ctrl+C arrête Vite ; les backends restent dans Docker Desktop.

### Frontend seul

Prérequis : Node.js 22.12+ et npm. Démarrer d'abord Keycloak et les quatre backends.

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

Les écrans Figma d'origine sont conservés à **`/maquettes`**, avec une bannière indiquant qu'ils
utilisent des données de démonstration. Ils ne modifient pas la base et ne prouvent pas
qu'un backend est implémenté. La recherche de chargements filtre la page affichée.

## Vérifier et construire

```powershell
npm run typecheck
npm run build
```

Pour vérifier le parcours complet dans Chrome avec une base jetable, depuis la racine :

```powershell
python tests/integration/run_frontend.py
```

Fermer le frontend déjà ouvert sur le port 5173 avant cette commande. Elle lance les
tests des backends, le frontend et les tests navigateur, puis supprime la pile de test.
Chrome doit être installé ; `E2E_CHROME_PATH` permet de préciser son exécutable.
Les captures et résultats sont enregistrés dans `.tools/frontend-systeme/`.

Pour vérifier uniquement le tableau de bord sur les services démarrés :
`npm run test:e2e -- tests/dashboard.spec.ts` depuis `frontend`.
Ces tests ne font aucune écriture métier. Les scénarios de panne, de liste vide et
de données remplies utilisent des réponses locales au navigateur. Les captures
sont dans `.tools/dashboard-qa/`.

Le résultat est dans `dist/`. En hébergement, servir `index.html` pour les routes de navigation
et acheminer `/api/v1/chargements`, `/api/v1/camions`, `/api/v1/bons` et `/api/v1/transports` vers leurs backends,
ou définir `VITE_API_BASE_URL` et configurer CORS côté backends. Le proxy Vite n'est pas un serveur de production.
