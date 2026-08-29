# Jeu éducatif — Alphabet Tifinagh (Amazigh Marocain)

Un mini-jeu web autonome (HTML/CSS/JS, sans dépendance ni installation) pour apprendre les 33 lettres de l'alphabet Tifinagh (norme IRCAM).

## Lancer le jeu

Ouvrir simplement `index.html` dans un navigateur, ou servir le dossier :

```bash
python3 -m http.server 8000
# puis ouvrir http://localhost:8000
```

## Modes de jeu

- **Accueil** : présentation rapide de l'alphabet Tifinagh.
- **Apprendre** : grille des 33 lettres, cliquer sur une lettre affiche sa transcription latine, son nom et un repère de prononciation.
- **Quiz** : 12 questions à choix multiple (lettre Tifinagh → transcription latine), avec score et meilleur score sauvegardé localement (`localStorage`).
- **Mémoire** : jeu de paires (8 paires) associant chaque lettre Tifinagh à sa transcription latine.

## Fichiers

- `index.html` — structure des trois vues
- `style.css` — thème visuel (couleurs inspirées du drapeau amazigh/marocain)
- `script.js` — données de l'alphabet et logique des trois modes de jeu
