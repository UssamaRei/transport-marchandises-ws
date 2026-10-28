# Commencer aujourd'hui

**But : quatre étudiants développent quatre backends qui coopèrent pour réaliser une livraison.**

## Les décisions à prendre ensemble

- Attribuer Flotte, Chargements, Bons et Transports ; désigner un chef de projet.
- Chaque étudiant choisit un framework qu'il connaît. Les frameworks peuvent être identiques ou différents.
- Valider le périmètre de la première version et les noms de champs des [contrats](../contracts/README.md).
- Choisir le moteur de chaque base. PostgreSQL et les schémas SQL du dépôt sont une proposition de départ.

## Le premier travail individuel

| Module | Première fonctionnalité | Premier appel sortant à réaliser |
|---|---|---|
| Flotte | Ajouter puis consulter un camion | Lire les transports actifs d'un camion |
| Chargements | Créer un chargement avec ses lignes | Consulter le camion avant validation |
| Bons | Créer un bon à partir d'un chargement | Lire le détail du chargement |
| Transports | Créer puis consulter un transport | Lire un bon de livraison |

Chaque étudiant crée son projet **dans son dossier `services/...`**, documente son lancement et expose `/health`. Commencer par un endpoint métier simple. On peut utiliser une réponse de démonstration pour démarrer, mais annoncer clairement qu'elle ne lit pas encore une base.

## Premier objectif collectif

Faire fonctionner **un vrai appel HTTP entre deux backends**. Ensuite relier les quatre modules, puis intégrer le tout dans Compose.

## Ordre de lecture individuel

1. Son propre `services/<module>/README.md` : rôle, fonctionnalités, APIs.
2. Le [modèle de données](03-modele-donnees.md) : comprendre ses tables et les identifiants externes.
3. Le [contrat API de son module](../contracts/README.md) et celui du service appelé.

Les options, la reprise après panne et les fonctionnalités avancées se réalisent après le premier parcours intégré. Pour la présentation, utiliser [le plan de huit minutes](07-presentation.md) et indiquer l'avancement réel.
