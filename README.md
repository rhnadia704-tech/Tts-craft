# EdgeTTS Pro - Synthèse Vocale & Amélioration IA

Application Android haute performance et optimisée pour la synthèse vocale neuronale Microsoft Edge TTS, avec un moteur d'amélioration naturelle de la voix par Intelligence Artificielle (Gemini AI).

Revisité et optimisé à partir du projet original [manankasinadan-tech/EdgeTTS](https://github.com/manankasinadan-tech/EdgeTTS).

---

## ✨ Fonctionnalités Principales

### 🎙️ Synthèse Vocale Edge TTS
- **Voix neuronales naturelles** : Plus de 50+ voix haute fidélité (Français, Anglais, Espagnol, Allemand, Italien, Arabe, etc.).
- **Contrôles fins** :
  - Vitesse / Débit (-50% à +100%)
  - Tonalité / Pitch (-50Hz à +50Hz)
  - Volume (0% à 100%)
- **Moteur hybride résilient** : Connectivité WebSocket Edge TTS temps réel avec basculement automatique et transparent vers le moteur natif Android TTS en cas d'absence de connexion réseau.
- **Export & Partage** : Sauvegarde dans le stockage de l'appareil et partage direct des fichiers audio (MP3).

### 🤖 NOUVEAU : Amélioration IA de l'Audio (Page Dédiée)
- **Réservé aux audios déjà générés** : Sélectionnez n'importe quel audio de votre bibliothèque.
- **Champ de personnalisation naturel** : Décrivez librement les améliorations souhaitées (ex. *"Donner un ton plus chaleureux et narratif, ajouter des pauses dramatiques, accentuer les fins de phrases"*).
- **Préréglages d'amélioration express** :
  - *🎙️ Voix narrative chaleureuse & humaine*
  - *🎭 Intonation dramatique & pauses émotionnelles*
  - *⚡ Énergie dynamique & dynamisme commercial*
  - *🧘 Ton posé, apaisant et articulation douce*
  - *📻 Style podcast & radio broadcast*
- **Analyse prosodique par IA** : Modulation du contour de pitch, insertion de pauses respiratoires naturelles (`<break time="..."/>`), accentuation des mots-clés (`<emphasis>`), et égalisation acoustique.
- **Comparaison A/B interactive** : Écoutez l'original et la version IA côte à côte avant d'enregistrer.

### 📚 Historique & Bibliothèque Locale
- Stockage persistant avec **Room Database**.
- Recherche et filtrage (Originaux vs Améliorés par IA).
- Lecteur audio intégré avec barre de progression interactive.

---

## 🛠️ Compilation avec GitHub Actions

Ce dépôt est **100% prêt pour GitHub Actions**.

### Étapes pour compiler sur GitHub :
1. Créez votre dépôt sur GitHub et poussez ce projet :
   ```bash
   git init
   git add .
   git commit -m "Initial commit - EdgeTTS Pro"
   git remote add origin https://github.com/<votre-utilisateur>/EdgeTTS.git
   git push -u origin main
   ```
2. (Optionnel) Dans votre dépôt GitHub, allez dans **Settings > Secrets and variables > Actions** et ajoutez le secret :
   - `GEMINI_API_KEY` : Votre clé API Gemini (pour l'amélioration IA).
3. L'action GitHub `.github/workflows/android.yml` se déclenchera automatiquement à chaque push et produira le fichier APK `EdgeTTS-Pro-Debug-APK` téléchargeable dans l'onglet **Actions > Artifacts**.

---

## 🏗️ Architecture & Technologies
- **Langage** : Kotlin 2.x
- **UI** : Jetpack Compose & Material Design 3
- **Base de données** : Room (avec KSP)
- **Réseau** : OkHttp 4 (WebSockets pour Edge TTS), Retrofit 2 + Moshi
- **IA** : Gemini API (Prosodie, modulations naturelles et SSML)
- **Audio** : MediaPlayer & AudioTrack
