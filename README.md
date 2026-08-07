<div align="center">

# 🎵 OxyMusic

### Player de música Android · YouTube + biblioteca local · 100% gratuito

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12-4285F4?logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-1.5.1-FF6F00?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Status](https://img.shields.io/badge/Status-v2.0.0-22d3ee)

[📥 Baixar APK](#-baixar-apk) · [✨ Features](#-features) · [🛠 Stack](#-stack-técnica) · [📚 Arquitetura](#-arquitetura) · [🚀 Build](#-build-do-zero)

</div>

---

## 📖 Sobre

OxyMusic é um player de música Android **nativo** (Kotlin + Jetpack Compose) que toca:

- **Músicas do YouTube** — sem API key, sem anúncios, sem login
- **Músicas locais do aparelho** — via `MediaStore.Audio.Media`
- **Fila mista** — você pode ter uma faixa local seguida de uma do YouTube na mesma fila

Visual **"deep tech / terminal de IA"** — mesma identidade visual do
[portfólio do NATSKY](https://kronos1027.github.io/portifolio/):
fundo quase-preto + acentos ciano/teal + âmbar/dourado (os "olhos heterocromáticos"
da persona VTuber Oto-ai).

## ✨ Features

### 🎨 Visuais (deep-tech)
- **Spectrum circular em volta da capa** — 64 barras radiais com FFT real do `Visualizer` nativo
- **Álbum morphing** — `border-radius` animado 32%↔42% + rotação lenta (estilo Caelestia)
- **Cores adaptativas da capa** — extrai paleta via `Palette` + Material 3
- **Tipografia mono** — bitrate, duração e fonte do stream em `FontFamily.Monospace` (visual "HUD técnico")
- **Lock screen controls** — `MediaSession` com `MediaStyle` notification
- **Anime mode opcional** (Sakura/Ghibli) — vira skin alternativa em Ajustes

### 🎵 Playback (4 camadas de fallback)
1. **NewPipeExtractor + poToken real via BotGuard WebView** (principal)
2. **Innertube direto** — IOS, ANDROID_MUSIC, ANDROID_VR, TVHTML5 (fast-path)
3. **YouTubeStreamResolver HTTP** — WEB_EMBEDDED + WEB+signatureTimestamp + watch page scrape
4. **Piped API com health-check dinâmico** — busca instâncias públicas vivas, sem lista hardcoded

### 📚 Biblioteca unificada
- **Aba "Meu aparelho"** — escaneia `MediaStore` local (título, artista, álbum, bitrate, capa)
- **Aba "Online (YouTube)"** — busca + trending
- **Fila mista no Media3** — local + YouTube na mesma queue
- **Permissão `READ_MEDIA_AUDIO`** (Android 13+) — solicitada na primeira vez

### 🤖 OxyDJ — recomendações 100% locais
- **Sem servidor externo** — nada sai do seu aparelho
- **3 sinais combinados**: histórico com peso + relacionados do Innertube + similaridade por artista
- **Recency boost**: últimas 24h valem 2x, última semana 1.5x
- **Fallback**: trending se histórico for insuficiente

### 🎤 Lyrics
- **Sincronizadas** via LRCLIB (gratuito, open source)
- **Karaoke mode** — destaque palavra-por-palavra quando LRC word-level disponível

### ⚙️ Settings
- Toggle de cores adaptativas on/off
- Modo anime com seletor de tema (Sakura/Ghibli) — **opcional, não é mais o tema padrão**
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
| Playback | androidx.media3 1.5.1 (ExoPlayer + OkHttpDataSource + MediaSession) |
| YouTube | NewPipeExtractor v0.26.4 + Innertube direto + YouTubeStreamResolver (HTTP) + Piped |
| poToken | BotGuard via WebView (vendored do NewPipe PR #11955) |
| Local library | `MediaStore.Audio.Media` |
| Lyrics | LRCLIB API (sem key) |
| Imagens + GIFs | Coil 2.7 + Coil-Gif |
| Cores | androidx.palette |
| Visualizer | android.media.audiofx.Visualizer (FFT real) |
| Build | Gradle 8.10 + KSP1 + version catalog |
| Sign | Keystore próprio (100 anos de validade) |

**Sem dependências pagas. Sem APIs com key. Tudo open source.**

## 📚 Arquitetura

```
oxymusic/
├── app/src/main/
│   ├── assets/
│   │   ├── po_token.html                          # BotGuard VM (vendored do NewPipe)
│   │   └── anime/                                 # GIFs (modo anime opcional)
│   ├── java/com/oxymusic/app/
│   │   ├── OxyMusicApp.kt                         # Application class
│   │   ├── MainActivity.kt                        # Entry point
│   │   ├── media/
│   │   │   ├── PlaybackController.kt              # ExoPlayer (local + YouTube)
│   │   │   ├── LocalMediaRepository.kt            # MediaStore scanner (NOVO)
│   │   │   ├── OxyDjEngine.kt                     # Recomendações locais (NOVO)
│   │   │   └── VisualizerManager.kt               # FFT 64 bands
│   │   ├── network/
│   │   │   ├── YouTubeRepository.kt               # Orquestra 4 fontes
│   │   │   ├── InnertubeClient.kt                 # IOS/ANDROID_MUSIC/VR/TVHTML5
│   │   │   ├── YouTubeStreamResolver.kt           # HTTP fallback (NOVO)
│   │   │   ├── PipedInstancesRegistry.kt          # Health-check dinâmico (NOVO)
│   │   │   ├── OxyHttpDownloader.kt               # NewPipe HTTP backend
│   │   │   └── JsonExtractor.kt                   # JSON parser manual
│   │   ├── potoken/
│   │   │   ├── PoTokenWebView.kt                  # BotGuard via WebView (vendored)
│   │   │   ├── PoTokenProviderImpl.kt             # NewPipe PoTokenProvider impl
│   │   │   ├── PoTokenGenerator.kt                # Interface
│   │   │   ├── JavaScriptUtil.kt                  # Challenge/IT parsing (nanojson)
│   │   │   └── PoTokenException.kt
│   │   ├── lyrics/
│   │   │   └── LrclibClient.kt                    # LRCLIB + LRC parser
│   │   ├── data/
│   │   │   ├── OxyDatabase.kt                     # Room DB
│   │   │   └── SettingsRepository.kt              # DataStore
│   │   ├── model/Models.kt                        # Track, Lyrics, Settings, TrackSource
│   │   └── ui/
│   │       ├── theme/Theme.kt                     # Deep-tech scheme (NOVO)
│   │       ├── components/
│   │       │   ├── MorphingAlbumWithSpectrum.kt
│   │       │   ├── SyncedLyricsView.kt
│   │       │   ├── ParticleOverlays.kt            # Sakura + Ghibli (opcional)
│   │       │   ├── AnimeMascotGif.kt              # (opcional)
│   │       │   └── MiniPlayer.kt
│   │       ├── screens/
│   │       │   ├── HomeScreen.kt
│   │       │   ├── LibraryScreen.kt               # Local + YouTube unificado (NOVO)
│   │       │   ├── OxyDjScreen.kt                 # Recomendações (NOVO)
│   │       │   ├── PlayerScreen.kt
│   │       │   └── SettingsScreen.kt
│   │       └── viewmodel/
│   │           ├── PlayerViewModel.kt
│   │           ├── LibraryViewModel.kt            # (NOVO)
│   │           ├── HomeViewModel.kt
│   │           └── SettingsViewModel.kt
│   └── res/                                       # strings, colors, themes, icons
├── gradle/libs.versions.toml                      # version catalog
├── build.gradle.kts                               # signingConfigs.release
├── keystore/oxymusic-release.jks                  # release keystore (gitignore'd)
└── .github/workflows/build.yml                    # auto-release em tags
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

### Build release assinado
```bash
# 1. Gere keystore (uma vez só — guarde para sempre!)
keytool -genkeypair -v \
  -keystore keystore/oxymusic-release.jks \
  -storepass oxymusic -alias oxymusic -keypass oxymusic \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -dname "CN=OxyMusic, OU=Dev, O=NATSKY, L=Brasil, C=BR"

# 2. Build release
export OXY_KEYSTORE_PASSWORD=oxymusic
export OXY_KEY_ALIAS=oxymusic
export OXY_KEY_PASSWORD=oxymusic
./gradlew :app:assembleRelease

# 3. APK assinado em:
ls app/build/outputs/apk/release/app-release.apk
```

### Instalar no celular
```bash
adb install app/build/outputs/apk/release/app-release.apk
```
Ou copie o APK pro celular e toque pra instalar.

## 📥 Baixar APK

### Opção 1: Releases (recomendado)
Baixe a versão mais recente (v2.0.0) na aba
**[Releases](../../releases/latest)** — APK assinado, pronto para instalar.

### Opção 2: Build automático via GitHub Actions
A cada push de tag `v*`, o GitHub Actions builda o APK release assinado e publica
como GitHub Release automaticamente. Veja a aba **[Actions](../../actions)**.

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

### Playback não funciona (erro 403 / BAD_HTTP_STATUS)
1. Vá na aba **Player** sem tocar nada
2. Toque em **"🎵 Testar playback (sample MP3)"** — se tocar, ExoPlayer está OK
3. Se a busca funciona mas tocar falha com 403:
   - O YouTube pode estar bloqueando seu IP temporariamente
   - Tente outra música (às vezes é só aquele vídeo)
   - Aguarde alguns minutos (token BotGuard pode ter expirado)
   - Verifique os logs em **Ajustes → Debug** (se disponível)

### Biblioteca local vazia
- Conceda permissão de áudio quando solicitado (Android 13+)
- Se negou: Configurações → Apps → OxyMusic → Permissões → Áudio → Permitir
- Toque no botão 🔄 no canto superior direito da aba "Meu aparelho"

### OxyDJ sem recomendações
- Toque algumas músicas primeiro (precisa de histórico)
- Toque no botão 🔄 para regenerar

### Lyrics não aparecem
- LRCLIB pode não ter a faixa — tente música popular
- Verifique se o título está limpo (sem "(Official Video)" etc.)

## 📦 Changelog

Veja [CHANGELOG.md](CHANGELOG.md) para histórico completo.

### v2.0.0 (atual)
- ✅ **Playback consertado**: 4 camadas de fallback (NewPipe+poToken / Innertube / HTTP resolver / Piped com health-check)
- ✅ **Biblioteca unificada**: abas "Meu aparelho" + "Online (YouTube)" com fila mista
- ✅ **OxyDJ**: recomendações 100% locais
- ✅ **Redesign deep-tech**: paleta idêntica ao portfólio do NATSKY
- ✅ **APK assinado** com keystore próprio

### v1.x
Veja [CHANGELOG.md](CHANGELOG.md#1130--2026-07-28-revoke--continha-stub-não-funcional)
para histórico das versões 1.x.

## 💜 Créditos

- **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)** — extração YouTube + poToken via BotGuard WebView (PR #11955)
- **[Piped](https://github.com/TeamPiped/Piped)** — proxy YouTube open source
- **[LRCLIB](https://lrclib.net)** — lyrics gratuitos
- **[InnerTune](https://github.com/z-huang/InnerTune)** — inspiração arquitetural
- **[Coil](https://github.com/coil-kt/coil)** — image loading + GIF decoder
- **[NATSKY](https://github.com/Kronos1027)** — design visual + desenvolvimento

## 📄 Licença

MIT License — veja [LICENSE](LICENSE).

## ⚠️ Aviso legal

OxyMusic não é afiliado ao YouTube, Google, ou qualquer serviço mencionado. O app usa
APIs públicas e open source. Use por sua conta e risco. O usuário é responsável por
cumprir os Termos de Serviço do YouTube em sua jurisdição.

---

<div align="center">

Feito com 💜 por **[NATSKY](https://github.com/Kronos1027)**

_"deep tech / terminal de IA" — mesma identidade visual do portfólio_

</div>
