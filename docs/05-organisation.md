# Répartition, suivi et intégration

## Tableau à compléter en équipe

| Module | Étudiant | Framework | Moteur de données | État réel |
|---|---|---|---|---|
| Flotte | À désigner | À choisir | À confirmer | À développer |
| Chargements | À désigner | À choisir | À confirmer | À développer |
| Bons | À désigner | À choisir | À confirmer | À développer |
| Transports | À désigner | À choisir | À confirmer | À développer |

**Chef de projet : à désigner parmi les quatre.** PostgreSQL est fourni comme exemple, mais ce tableau reste à confirmer.

Le propriétaire du dépôt ajoute les coéquipiers comme collaborateurs lorsque leurs comptes GitHub sont connus. Chacun clone ensuite le dépôt et travaille sur sa branche. Cette base ne crée aucune invitation automatiquement.

## Responsabilités de chaque étudiant

- Comprendre les fonctionnalités, classes et règles de son module.
- Développer son backend et ses migrations dans son dossier.
- Exposer ses endpoints et intégrer les appels sortants prévus.
- Mettre à jour son contrat OpenAPI quand son API évolue.
- Tester son travail, écrire son Dockerfile et participer au test global.
- Expliquer une API fournie, une API consommée et le traitement d'une erreur.

Le chef de projet coordonne les décisions, l'avancement et le Compose commun. Il doit expliquer la vision globale ; il ne développe pas toutes les parties à la place des autres.

## Suivi à chaque séance

Créer une issue par tâche importante. Pour chaque membre, présenter : ce qui est terminé et vérifié, ce qui reste à faire, le blocage éventuel et la prochaine étape. Utiliser des commits réguliers et des pull requests pour rendre les contributions visibles.

| Date / séance | Membre | Terminé et preuve | Blocage | Prochaine action |
|---|---|---|---|---|
| À compléter | À compléter | À compléter | À compléter | À compléter |

## Jalons proposés

1. **Cadrage** : rôles, technologies, contrats et données d'exemple validés ensemble.
2. **Premier échange** : deux backends communiquent réellement par HTTP.
3. **Quatre modules** : un appel entrant et sortant démontré pour chacun.
4. **Parcours complet** : chargement, bon, réservation, départ et livraison.
5. **Stabilisation** : pannes, concurrence, conteneurs et README vérifiés.
6. **Remise** : dépôt propre, documentation et démonstration reproductible.

Le sujet fixe la livraison au **20 novembre 2026** et le début des soutenances au **26 novembre 2026**. Le découpage des tâches et les fonctionnalités détaillées restent nos propositions de conception.

## Avant de changer un contrat

Prévenir le consommateur, modifier les exemples et le fichier OpenAPI, faire relire, puis tester les deux services ensemble. Changer seulement son code local peut casser le travail d'un autre étudiant.

Voir aussi [les règles de contribution](../CONTRIBUTING.md).
