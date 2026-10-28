# Tests d'intégration à développer

Ce dossier accueillera les tests exécutés contre les vrais backends. Aucun test métier n'est encore implémenté dans ce starter.

Scénario principal : créer camion et chargement, valider, émettre le bon, planifier, partir, livrer et vérifier la libération du camion.

Cas critiques : double réservation concurrente, même demande rejouée, surcharge, service indisponible et reprise après une réponse perdue.

Voir [le plan détaillé](../../docs/06-integration-tests.md). Choisir l'outil ensemble : collection Postman/Newman ou client HTTP automatisé dans un langage maîtrisé par l'équipe.
