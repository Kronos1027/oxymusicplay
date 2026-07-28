# Changelog

Todos os lançamentos notáveis do OxyMusic serão documentados aqui.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## [1.8.0] — 2026-07-28

### 🐛 Corrigido
- **Trending resolvido**: `FEtrending` do Innertube retornava `INVALID_ARGUMENT`. YouTube mudou a API.
  - **Solução**: agora usa `FEmusic_home` com client `ANDROID_MUSIC` (YouTube Music home) → retorna 51+ músicas populares
  - Fallback: busca por "músicas populares 2026" se YouTube Music falhar
  - Parser de `musicTwoRowItemRenderer` recursivo (walks the tree)
- **Playback mais robusto**: trocado `DefaultHttpDataSource` por `OkHttpDataSource` (muuuito melhor)
  - `OkHttpClient` configurado com `followRedirects(true)`, `followSslRedirects(true)`, `retryOnConnectionFailure(true)`
  - `setUserAgent("com.google.android.youtube/...")` ← User-Agent do app YouTube oficial
  - Headers default: `Accept: */*`, `Accept-Language: pt-BR,pt;q=0.9,en;q=0.8`

### ✨ Adicionado
- **Erros de playback muito mais detalhados**: agora mostra
  - Código do erro ExoPlayer (ex: `ERROR_CODE_IO_BAD_HTTP_STATUS`)
  - Causa (ex: `InvalidResponseCodeException`)
  - HTTP status code (ex: `403` ou `404`)
  - Explicação do status code (403 = URL expirada ou IP rejeitado)
- Logs via `android.util.Log` em todas as etapas (visíveis via `adb logcat | grep PlaybackController`)
- Dependência `media3-datasource-okhttp` (OkHttp-based DataSource)

### 🔧 Mudado
- `PlaybackController.buildDetailedErrorMessage()` extrai causa e status code do PlaybackException
- `InnertubeClient.trending()` reescrito com YouTube Music endpoint
- `extractTrackFromItem()` genérico funciona com vários tipos de renderer

## [1.7.0] — 2026-07-28

### 🐛 Corrigido
- **Bug crítico**: `ERROR_CODE_IO_BAD_HTTP_STATUS` resolvido! Causa raiz definitiva:
  - ExoPlayer usava DataSource padrão que não seguia redirects cross-protocol (HTTP→HTTPS)
  - YouTube CDN usa redirects cross-protocol → ExoPlayer falhava
- **Solução**: configurado `DefaultHttpDataSource.Factory()` com:
  - `setAllowCrossProtocolRedirects(true)` ← principal fix
  - `setUserAgent("com.google.android.youtube/...")` ← User-Agent do app YouTube oficial
  - `setConnectTimeoutMs(15000)` e `setReadTimeoutMs(20000)`
- Adicionado `&ratebypass=yes` nas URLs do googlevideo.com (sem isso, seek além de 30s retorna 403)

### ✨ Adicionado
- **Trending via Innertube `/browse` endpoint** com `browseId=FEtrending`
  - Mais confiável que Piped (que estava falhando)
  - Piped agora é apenas fallback
  - Sem erro "Sem conexão com trending" na tela Home

### 🔧 Mudado
- `PlaybackController` agora usa `DefaultMediaSourceFactory` customizado
- Adicionada dependência `media3-datasource`
- InnertubeClient: novo método `trending()` com parser de `richGridRenderer`
- URLs do YouTube sanitizadas (remove trailing `&`)
- Logs mais detalhados em `trending()`

## [1.6.0] — 2026-07-28

### 🐛 Corrigido
- **Bug crítico**: `ERROR_CODE_IO_BAD_HTTP_STATUS` resolvido! Causa raiz: o client WEB retornava URLs com `signatureCipher` que precisam de decifração JavaScript (que não fazíamos) → ExoPlayer recebia HTTP 403 ao tentar tocar.
- **Solução**: ANDROID client agora é tentado PRIMEIRO (retorna URLs diretas sem signatureCipher). Adicionado filtro que pula qualquer stream com `signatureCipher` ou `cipher` — só usa URLs que funcionam diretamente.
- Ordem atual: ANDROID → IOS → ANDROID_VR → ANDROID_TESTSUITE → WEB (último resort)

### ✨ Adicionado (estilo Spotify)
- **Tela Home**: greeting dinâmico (Bom dia/tarde/noite), trending BR, tocadas recentemente, sugestões de mood
- **Tela Histórico**: agrupado por data (Hoje, Ontem, Esta semana, etc.) com horário de cada faixa
- **MiniPlayer**: barra flutuante acima do bottom nav com thumbnail + título + play/pause + skip + progress bar
- 4 abas: Início / Buscar / Histórico / Ajustes
- MiniPlayer aparece automaticamente quando uma música está tocando
- Tocar no MiniPlayer abre o Player completo

### 🎨 Melhorias de design
- HomeScreen: cards de trending em horizontal scroll, gradient header
- HistoryScreen: agrupamento por data, empty state bonito, botão limpar
- MiniPlayer: progress bar fino no topo, animações slide+fade
- Bottom nav com 4 abas (era 3)
- Empty states refinados em todas as telas

## [1.5.0] — 2026-07-28

### 🐛 Corrigido
- **Bug crítico**: playback agora funciona! Adicionado `InnertubeClient.resolveStream()` que usa o endpoint `/player` do YouTube com múltiplos clients (WEB, ANDROID_VR, ANDROID_TESTSUITE, ANDROID, IOS). Pelo menos um deles funciona em IPs residenciais brasileiros.
- Antes: `resolveStream` só tentava NewPipe (bloqueado por poToken) e Piped (também bloqueado) → retornava null → nada tocava.
- Agora: Innertube player endpoint é primário, com 5 clients diferentes em fallback.

### ✨ Adicionado
- Indicador "🔍 Resolvendo stream…" com info de qual fonte está tentando: "Tentando: Innertube → NewPipe → Piped"
- Indicador "via [fonte]" mostra qual fonte funcionou quando a música toca (ex: "via Innertube/WEB")
- Mensagem de erro detalhada quando todas as fontes falham: explica que YouTube pode estar bloqueando o IP
- Logs detalhados via `android.util.Log` em todas as etapas (InnertubeClient, YouTubeRepository)
- Novo `ResolveResult` data class com info de sucesso/fonte/erro

### 🔧 Mudado
- `YouTubeRepository.resolveStream()` agora retorna `ResolveResult` em vez de `Track?`
- `PlayerViewModel.playTrack()` mostra mensagem da mascote com a fonte que funcionou
- PlayerScreen exibe fonte ativa no rodapé quando música toca

## [1.4.0] — 2026-07-28

### 🐛 Corrigido
- **Bug crítico**: busca agora funciona! Implementado cliente **Innertube direto** (mesma estratégia do InnerTune/RiMusic) que não depende de NewPipe nem Piped. YouTube bloqueava NewPipe por causa do `poToken` enforcement 2024/2025.
- Innertube usa a chave API pública do YouTube (`AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8`) com client ANDROID, que funciona anonimamente pra search.
- Ordem das fontes: Innertube (search) → NewPipe (fallback) → Piped (último recurso)

### ✨ Adicionado
- Empty states bonitos: sugestões de busca, estado "nenhum resultado", estado de erro com retry
- Loading state com mensagem "Buscando no YouTube…"
- Indicador "🔍 Resolvendo stream…" quando buscando URL do stream
- Indicador "⏳ Carregando…" durante buffering do ExoPlayer
- Indicador "TOCANDO AGORA" / "PAUSADO" / "PRONTO PARA TOCAR" com dot animado
- Contagem de resultados encontrados
- Chips de sugestão pra buscas rápidas (lofi, midnight city, anime op, etc.)
- Card de seção nos Settings (Aparência, Playback, Sobre)

### 🎨 Melhorias de design
- PlayerScreen: layout mais caprichado, gradient vertical, dot pulsante, controles maiores
- SearchScreen: header com subtítulo, cards de resultado com gradient fallback, badges de duração
- SettingsScreen: seções em cards, divisores sutis, melhor hierarquia visual
- MorphingAlbum: pulse sutil + sombra + ring de fundo + melhor glow
- Bordas arredondadas consistentes (14-16dp)
- Tipografia mais hierárquica (Bold headlines, Medium labels, Small captions)

## [1.3.0] — 2026-07-28

### 🐛 Corrigido
- Bug crítico: ExoPlayer agora é direto no PlaybackController (sem async MediaController)
- NewPipeExtractor agora é a fonte primária de stream URLs (URLs assinadas pro IP do usuário)
- Banner de erro visível quando faixa falha
- Indicator de buffering aparece durante carregamento

### ✨ Adicionado
- Botão "Testar playback (sample MP3)" na tela Player
- Forward automático de erros do ExoPlayer para a UI

## [1.2.0] — 2026-07-27

### ✨ Adicionado
- GIFs de anime reais substituindo a chibi Canvas (4 variantes)
- Coil-Gif decoder para reprodução dos GIFs
- YouTube watch page scraping como 3ª fonte de stream
- Banner de erro vermelho no rodapé do Player

### 🐛 Corrigido
- `PlaybackController.connect()` chamado no `MainActivity.onCreate()`
- Audio session ID capturado via `Player.Listener.onAudioSessionIdChanged`
- Permissão `MODIFY_AUDIO_SETTINGS` adicionada

## [1.1.0] — 2026-07-27

### ✨ Adicionado
- Tela Home estilo Spotify com trending, moods, recent, OxyDJ
- OxyDJ — engine de recomendação 100% local
- Karaoke mode — parser LRC enhanced com word-level timestamps
- Tema Ghibli completo (segundo tema anime)
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
- Spectrum circular em volta da capa (64 barras, FFT real do Visualizer)
- Álbum morphing (border-radius animado, rotação lenta)
- Cores adaptativas da capa (Palette + Material 3)
- Lyrics sincronizadas via LRCLIB
- Modo Anime básico (sakura petals + tema rosa)
- Settings com toggle de cores adaptativas e modo anime
- Histórico de reprodução (Room DB)
- Compatível com Redmi Note 11 (minSdk 31, targetSdk 35)
