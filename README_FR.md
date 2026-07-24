# BYD Connected Monitor — Agent voiture v0.6.0

Application Android autonome destinée à l’écran BYD DiLink. Cette version est un **agent de diagnostic et de télémétrie strictement en lecture seule**.

## Fonctions incluses

- démarrage automatique après redémarrage ;
- service Android de premier plan ;
- reconnexion MQTT automatique avec temporisation progressive ;
- MQTT 3.1.1 sans bibliothèque externe ;
- TLS 1.2/1.3 obligatoire avec vérification du nom du serveur ;
- empreinte SHA-256 du certificat facultative ;
- chiffrement du mot de passe MQTT via Android Keystore et AES-GCM ;
- publication du GPS, de la vitesse GPS, de l’état réseau et de la batterie de l’unité Android ;
- détection non intrusive de quelques classes BYD/AVM ;
- configuration développeur protégée par un code local créé au premier lancement ;
- verrouillage temporaire après cinq codes erronés ;
- aucune fausse donnée : batterie de traction, kilométrage et température véhicule restent `null` tant que l’adaptateur DiLink n’est pas validé ;
- aucun abonnement MQTT et aucun canal de commande à distance.

## Topics MQTT

Avec un préfixe tel que `asmontec/byd/0123456789abcdef` :

- `<prefix>/telemetry/live`
- `<prefix>/location/live`
- `<prefix>/status`

Les messages sont non retained et publiés en QoS 0 dans cette version.

## Configuration initiale

1. Installer l’APK sur l’unité DiLink par une méthode Android autorisée.
2. Ouvrir l’application lorsque le véhicule est stationné.
3. Autoriser la localisation.
4. Ouvrir **Configuration développeur**.
5. Créer un code numérique de 6 à 10 chiffres. Aucun code par défaut n’est intégré à l’APK.
6. Renseigner le broker privé, le port TLS, les identifiants et le préfixe MQTT.
7. Enregistrer puis vérifier l’état de connexion.

## Compilation locale

Prérequis :

- JDK 17 ;
- Android SDK Platform 35 ;
- Android Build Tools 35.0.0 ;
- Gradle 8.10.2.

Commande :

```bash
gradle clean lintDebug assembleDebug
```

APK attendu :

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Compilation GitHub Actions

Le workflow `.github/workflows/build-apk.yml` exécute les tests de sécurité, Android Lint, compile l’APK, vérifie l’archive et publie un artefact avec l’empreinte SHA-256.

## Limites de la v0.6

- Les valeurs BYD spécifiques ne sont pas encore lues.
- Aucun contrôle de verrouillage, climatisation, siège, moteur, freinage ou conduite.
- Aucun accès CAN et aucun contournement de permission.
- La caméra et le microphone ne sont pas demandés.
- Le démarrage automatique peut dépendre des restrictions propres au firmware DiLink.

## Validation sur véhicule

Tester uniquement à l’arrêt :

1. vérifier que l’application BYD native reste normale ;
2. ouvrir l’agent et autoriser le GPS ;
3. configurer un broker privé TLS ;
4. vérifier les trois topics ;
5. couper puis rétablir la 4G/Wi-Fi ;
6. redémarrer l’unité et vérifier le redémarrage de l’agent ;
7. conserver les journaux en cas d’erreur.

## Étape suivante

Créer un `BydVehicleDataSource` spécifique au firmware de la Seal U DM-i, strictement en lecture seule, après inventaire des classes et permissions officiellement accessibles.
