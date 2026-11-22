# Villes et communes du Maroc

Source : [HCP — Population légale, RGPH 2024](https://www.hcp.ma/Population-legale-du-Royaume-du-Maroc-repartie-par-regions-provinces-et-prefectures-et-communes-selon-les-resultats-du_a3975.html),
[fichier Excel officiel](https://www.hcp.ma/file/242341/). Extraction : 22 novembre 2026.

Le catalogue contient **1 547 communes et centres urbains distincts**. Il comprend
les communes rurales pour couvrir aussi les destinations hors des grandes villes.
Les doublons entre une commune et son centre urbain portant le même nom dans la
même province sont fusionnés. Les arrondissements, cercles et totaux sont exclus.
Les noms français et arabes et les codes géographiques viennent du fichier HCP.

La province distingue les lieux homonymes. Dans ce cas, elle est aussi conservée
dans la valeur enregistrée. Les anciennes adresses restent lisibles et modifiables.

Pour régénérer le fichier depuis la racine du dépôt, avec Python 3 :

```powershell
python frontend/scripts/import-moroccan-locations.py
```

Le JSON et le composant sont chargés séparément à l’ouverture du formulaire.
La recherche est locale, insensible aux accents ; le défilement est virtualisé
avec Headless UI. Aucune clé, requête externe par frappe ou nouvelle table SQL.
