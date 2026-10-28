# Vérifier la base documentaire

Avec Python 3.12 ou une version compatible avec les dépendances :

```sh
python -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements.txt
.venv/bin/python scripts/validate_docs.py
```

Sous Windows / PowerShell :

```powershell
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r scripts/requirements.txt
.venv/Scripts/python.exe scripts/validate_docs.py
```

Le script vérifie les quatre contrats OpenAPI, les exemples JSON, les appels entre services et les liens de fichiers Markdown. Il n'appelle aucun serveur externe. Il ne prouve pas que les backends métier sont déjà implémentés ni que les conteneurs ont été démarrés.

Les diagrammes Mermaid sont visibles directement dans GitHub. Ils ont aussi leurs sources `.mmd` et leurs exports dans [le catalogue](../docs/diagrams/README.md). Modifier le bloc Mermaid dans son document et synchroniser la source correspondante si le diagramme change.
