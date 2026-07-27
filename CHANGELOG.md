# Changelog

Todos os lançamentos notáveis do OxyMusic serão documentados aqui.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## [1.3.0] — 2026-07-28

### 🐛 Corrigido
- **Bug crítico**: ExoPlayer agora é instanciado direto no `PlaybackController` (sem MediaController async). Antes, o controller podia não estar pronto quando `playTrack()` era chamado, causando falha silenciosa.
- **Bug crítico**: NewPipeExtractor agora é a fonte primária de stream URLs. Antes, Piped vinha primeiro — mas Piped retorna URLs assinadas para o IP do servidor Piped, que falham no celular do usuário.
- Banner de erro visível quando faixa falha (não falha mais silenciosamente)
- Indicator de buffering aparece durante carregamento

### ✨ Adicionado
- Botão "Testar playback (sample MP3)" na tela Player para isolar problemas
- Forward automático de erros do ExoPlayer para a UI

## [1.2.0] — 2026-07-27

### ✨ Adicionado
- GIFs de anime reais substituindo a chibi Canvas
  - `mascot_sakura.gif` (idle)
  - `mascot_sakura_dance.gif` (tocando)
  - `mascot_ghibli.gif` (idle, tema Ghibli)
  - `mascot_ghibli_dance.gif` (tocando, tema Ghibli)
- Coil-Gif decoder para reprodução dos GIFs
- YouTube watch page scraping como 3ª fonte de stream
- Banner de erro vermelho no rodapé do Player

### 🐛 Corrigido
- `PlaybackController.connect()` agora é chamado no `MainActivity.onCreate()`
- Audio session ID capturado via `Player.Listener.onAudioSessionIdChanged`
- Permissão `MODIFY_AUDIO_SETTINGS` adicionada

## [1.1.0] — 2026-07-27

### ✨ Adicionado
- Tela **Home** estilo Spotify com trending, moods, recent, OxyDJ
- **OxyDJ** — engine de recomendação 100% local (sem nuvem)
  - For You: top artists + faixas não ouvidas em 7 dias
  - Mix for Now: faixas do mesmo horário
  - Stats: top artista, top faixa, tempo total, unique artists
- **Karaoke mode** — parser LRC enhanced com word-level timestamps
- **Tema Ghibli** completo (segundo tema anime)
- Partículas Ghibli (Susuwatari dust) + Sakura petals
- Mascote chibi animada (Canvas, depois substituída por GIF na v1.2)
- Personalidade da mascote (fofa/tímida/sarcástica)

### 🔧 Mudado
- Múltiplas instâncias Piped com failover automático
- Tela de busca aprimorada
- Bottom nav reorganizada

## [1.0.0] — 2026-07-27

### ✨ Lançamento inicial (MVP)
- Player funcional (play/pause/skip/seek)
- Busca no YouTube via NewPipeExtractor
- Playback via ExoPlayer + MediaSession
- **Spectrum circular em volta da capa** (64 barras, FFT real do Visualizer)
- **Álbum morphing** (border-radius animado, rotação lenta)
- **Cores adaptativas da capa** (Palette + Material 3)
- **Lyrics sincronizadas** via LRCLIB
- **Modo Anime básico** (sakura petals + tema rosa)
- Settings com toggle de cores adaptativas e modo anime
- Histórico de reprodução (Room DB)
- Compatível com Redmi Note 11 (minSdk 31, targetSdk 35)
