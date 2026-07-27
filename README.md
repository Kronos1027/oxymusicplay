<div align="center">

# 🎵 OxyMusic

### Player de música Android adaptativo inspirado em Waybar Media + Caelestia Shell

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12-4285F4?logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-1.5.1-FF6F00?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Status](https://img.shields.io/badge/Status-MVP%20v1.3.0-success)

[📥 Baixar APK](#-baixar-apk) · [🎨 Features](#-features) · [🛠 Stack](#-stack-técnica) · [📚 Arquitetura](#-arquitetura) · [🚀 Build](#-build-do-zero)

</div>

---

## 📖 Sobre

OxyMusic é um player de música Android **nativo** (Kotlin + Jetpack Compose) que toca músicas do YouTube **sem API key, sem anúncios, sem login**. Inspirado visualmente em:

- **Waybar Media** — info-dense, monospaced, indicador "playing" pulsante
- **Caelestia Shell** — morphing, cores adaptativas da capa, anime mode

A fonte de áudio usa a mesma estratégia de apps como **InnerTune / RiMusic / ViMusic**: NewPipeExtractor + Piped API (multi-instance com failover).

## ✨ Features

### 🎨 Visuais
- **Spectrum circular em volta da capa** — 64 barras radiais com FFT real do `Visualizer` nativo
- **Álbum morphing** — `border-radius` animado 32%↔42% + rotação lenta (estilo Caelestia)
- **Cores adaptativas da capa** — extrai paleta via `Palette` + Material 3 dynamic
- **Lock screen controls** — `MediaSession` com `MediaStyle` notification

### 🌸 Modo Anime
- **GIFs reais de chibi animada** (4 variantes, geradas proceduralmente)
  - `mascot_sakura.gif` / `mascot_sakura_dance.gif`
  - `mascot_ghibli.gif` / `mascot_ghibli_dance.gif`
- **2 temas**: Sakura (rosa pastel + pétalas) e Ghibli (verde + poeira mágica)
- **Speech bubbles contextuais** da mascote

### 🎵 Playback & Lyrics
- **Busca no YouTube** sem API key
- **Multi-source failover**: NewPipe → Piped (4 instâncias)
- **Lyrics sincronizadas** via LRCLIB (gratuito, open source)
- **Karaoke mode** — destaque palavra-por-palavra quando LRC word-level disponível
- **Banner de erro visível** quando faixa falha (não falha silenciosamente)
- **Test playback button** — MP3 sample pra isolar problemas

### ⚙️ Settings
- Toggle de cores adaptativas on/off
- Modo anime com seletor de tema
- Intensidade de partículas (4-40)
- Toggle mascote + personalidade (fofa/tímida/sarcástica)
- Toggle karaoke mode
- Crossfade (0-12s)
- Cache offline (100-5000 MB)

## 🛠 Stack técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin 2.0.21 + Coroutines + Flow |
| UI | Jetpack Compose + Material 3 + Compose Animation |
| DI | Hilt + KSP |
| DB | Room (histórico) |
| Settings | DataStore Preferences |
| Playback | androidx.media3 (ExoPlayer direto + MediaSession) |
| YouTube | NewPipeExtractor v0.24.3 + Piped API (4 instâncias) |
| Lyrics | LRCLIB API (sem key) |
| Imagens + GIFs | Coil 2.7 + Coil-Gif |
| Cores | androidx.palette |
| Visualizer | android.media.audiofx.Visualizer (FFT real) |
| Build | Gradle 8.10 + KSP1 + version catalog |

**Sem dependências pagas. Sem APIs com key. Tudo open source.**

## 📚 Arquitetura

```
oxymusic/
├── app/src/main/
│   ├── assets/anime/                          # GIFs de anime
│   │   ├── mascot_sakura.gif                  # chibi rosa idle
│   │   ├── mascot_sakura_dance.gif            # chibi rosa dançando
│   │   ├── mascot_ghibli.gif                  # chibi verde idle
│   │   └── mascot_ghibli_dance.gif            # chibi verde dançando
│   ├── java/com/oxymusic/app/
│   │   ├── OxyMusicApp.kt                     # Application class
│   │   ├── MainActivity.kt                    # Entry point
│   │   ├── media/
│   │   │   ├── PlaybackController.kt          # ExoPlayer direto (sem service)
│   │   │   └── VisualizerManager.kt           # FFT 64 bands
│   │   ├── network/
│   │   │   ├── YouTubeRepository.kt           # NewPipe + Piped failover
│   │   │   ├── OxyHttpDownloader.kt           # NewPipe HTTP backend
│   │   │   └── JsonExtractor.kt               # JSON parser manual
│   │   ├── lyrics/
│   │   │   └── LrclibClient.kt                # LRCLIB + LRC parser
│   │   ├── data/
│   │   │   ├── OxyDatabase.kt                 # Room DB
│   │   │   └── SettingsRepository.kt          # DataStore
│   │   ├── model/Models.kt                    # Track, Lyrics, Settings
│   │   └── ui/
│   │       ├── theme/Theme.kt                 # OxyMusicTheme
│   │       ├── components/
│   │       │   ├── MorphingAlbumWithSpectrum.kt
│   │       │   ├── SyncedLyricsView.kt
│   │       │   ├── ParticleOverlays.kt        # Sakura + Ghibli
│   │       │   └── AnimeMascotGif.kt
│   │       ├── screens/
│   │       │   ├── SearchScreen.kt
│   │       │   ├── PlayerScreen.kt
│   │       │   └── SettingsScreen.kt
│   │       └── viewmodel/
│   │           ├── PlayerViewModel.kt
│   │           ├── SearchViewModel.kt
│   │           └── SettingsViewModel.kt
│   └── res/                                   # strings, colors, themes, icons
├── gradle/libs.versions.toml                  # version catalog
├── build.gradle.kts
└── settings.gradle.kts
```

## 🚀 Build do zero

### Pré-requisitos
- JDK 17
- Android SDK 35 + Build Tools 35.0.0
- Gradle 8.10+

### Passo a passo
```bash
# 1. Clone
git clone https://github.com/Kronos1027/oxymusicplay.git
cd oxymusicplay

# 2. Configure o SDK
export ANDROID_HOME=/caminho/pro/android-sdk

# 3. Build debug APK
./gradlew :app:assembleDebug

# 4. APK está em:
ls app/build/outputs/apk/debug/app-debug.apk
```

### Instalar no celular
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
Ou copie o APK pro celular e toque pra instalar.

## 📥 Baixar APK

### Opção 1: Releases (recomendado)
Baixe a versão mais recente na aba **[Releases](../../releases)**.

### Opção 2: Build automático via GitHub Actions
A cada push na `main`, o GitHub Actions builda o APK automaticamente e publica como artifact. Veja a aba **[Actions](../../actions)**.

### Opção 3: Build local
Siga os passos em [🚀 Build do zero](#-build-do-zero).

## 📱 Compatibilidade

- **Mínimo**: Android 12 (API 31)
- **Alvo**: Android 15 (API 35)
- **Testado em**: Redmi Note 11 (codename `spes`, Android 13 HyperOS)

### Otimizações HyperOS/MIUI
Após instalar, configure:
1. **Configurações → Apps → OxyMusic → Bateria** → "Sem restrições"
2. **Segurança → Permissões → Auto-start** → ative
3. **Notificações** → permita (lock screen controls)

## 🎯 Troubleshooting

### Playback não funciona
1. Vá na aba **Player** sem tocar nada
2. Toque em **"🎵 Testar playback (sample MP3)"**
3. Se tocar → ExoPlayer está OK, problema é YouTube (tente outra música)
4. Se não tocar → abra uma issue com o erro do banner vermelho

### Lyrics não aparecem
- LRCLIB pode não ter a faixa — tente música popular
- Verifique se o título está limpo (sem "(Official Video)" etc.)

### Modo anime não aparece
- Vá em **Ajustes → Modo Anime** → ative
- Escolha tema: Sakura ou Ghibli
- Volte pro Player sem música tocando → chibi aparece

## 📦 Changelog

### v1.3.0 (atual)
- ✅ **Bug crítico corrigido**: ExoPlayer agora direto (sem async MediaController)
- ✅ **Bug crítico corrigido**: NewPipe agora é fonte primária (URLs assinadas pro IP do usuário)
- ✅ Banner de erro visível quando faixa falha
- ✅ Botão "Testar playback" com MP3 sample
- ✅ Indicator de buffering

### v1.2.0
- ✅ PlaybackController.connect() no startup
- ✅ GIFs de anime reais (4 variantes via Coil-Gif)
- ✅ YouTube watch page scraping como 3ª fonte
- ✅ Banner de erro visível

### v1.1.0
- ✅ Tela Home estilo Spotify
- ✅ OxyDJ (IA local de recomendação)
- ✅ Karaoke mode (LRC word-level)
- ✅ Tema Ghibli completo
- ✅ Mascote chibi

### v1.0.0
- ✅ MVP inicial
- ✅ Spectrum circular
- ✅ Álbum morphing
- ✅ Cores adaptativas
- ✅ Modo Anime (sakura)
- ✅ Lyrics via LRCLIB

## 💜 Créditos

- **[Waybar Media](https://github.com/yurihs/waybar-media)** — inspiração visual
- **[Caelestia Shell](https://github.com/caelestia-dots/shell)** — inspiração visual + anime mode
- **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)** — extração YouTube
- **[Piped](https://github.com/TeamPiped/Piped)** — proxy YouTube open source
- **[LRCLIB](https://lrclib.net)** — lyrics gratuitos
- **[InnerTune](https://github.com/z-huang/InnerTune)** — inspiração arquitetural
- **[Coil](https://github.com/coil-kt/coil)** — image loading + GIF decoder

## 📄 Licença

MIT License — veja [LICENSE](LICENSE).

## ⚠️ Aviso legal

OxyMusic não é afiliado ao YouTube, Google, ou qualquer serviço mencionado. O app usa APIs públicas e open source. Use por sua conta e risco. O usuário é responsável por cumprir os Termos de Serviço do YouTube em sua jurisdição.

---

<div align="center">

Feito com 💜 por [@Kronos1027](https://github.com/Kronos1027)

</div>
