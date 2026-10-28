# Travailler ensemble

1. Choisir son module et renseigner le tableau dans [l'organisation](docs/05-organisation.md).
2. Créer une branche pour une tâche : `codex/flotte-camions`, `codex/chargements-validation`, etc. Ce préfixe est une convention de départ adaptable en équipe.
3. Modifier le code et le contrat de son module ensemble.
4. Faire des commits courts et explicites : `feat(flotte): ajouter la consultation des camions`.
5. Ouvrir une pull request et demander une relecture à un coéquipier, surtout au consommateur de l'API modifiée.
6. Après intégration, tester le parcours commun et mettre à jour l'avancement.

Ne pas écrire directement dans la base d'un autre service. Ne pas renommer un champ d'API sans prévenir son consommateur. Ne pas committer `.env`, mots de passe réels ou fichiers de dépendances générés.

Une fonctionnalité est terminée lorsque son endpoint, ses règles, sa documentation, son test utile et son intégration sont prêts. Un diagramme ou un contrat décrit le comportement prévu ; il ne prouve pas que ce comportement est déjà développé.
