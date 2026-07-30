# BYD WebRTC Signaling

Serveur WebSocket de signalisation pour les applications BYD Car et BYD Mobile.

## Déploiement Render

1. Créer un nouveau **Blueprint** depuis ce dépôt GitHub.
2. Sélectionner `render.yaml`.
3. Déployer le service `byd-webrtc-signal`.
4. Copier l'URL HTTPS fournie et remplacer `https://` par `wss://` dans les
   deux applications.

Exemple :

```text
https://byd-webrtc-signal.onrender.com
wss://byd-webrtc-signal.onrender.com
```

Le même code de salle doit être utilisé sur la voiture et le téléphone.

Le serveur relaie uniquement `ready`, `offer`, `answer` et `candidate`. Il ne
stocke aucune télémétrie et n'accepte aucune commande du véhicule.
