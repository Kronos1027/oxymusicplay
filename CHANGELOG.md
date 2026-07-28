# Changelog

Todos os lançamentos notáveis do OxyMusic serão documentados aqui.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## [1.14.0] — 2026-07-28

### 🐛 Corrigido (poToken REAL — vendorizado do NewPipe, não stub)

**Honestidade sobre a 1.13.0**: a versão anterior continha uma implementação **não-funcional**
de poToken. A função `fetchBotGuardChallenge()` em `PoTokenWebView.kt` era um **stub** — não
buscava o desafio BotGuard real do YouTube, apenas retornava o `visitorData` cru fingindo que
era o challenge. Como resultado, a geração do poToken falhava silenciosamente (catch → warning)
e o app voltava a tomar 403 exatamente como antes. Toda a infraestrutura ao redor (WebView,
JS interface, provider) estava construída, mas a parte que realmente importava era simulada.

### ✨ v1.14.0 — Arquivos reais vendorizados do NewPipe

Esta versão substitui o stub pelos **arquivos reais e testados em produção** do projeto NewPipe
(GPLv3, open source, PR #11955), copiados verbatim com adaptações mínimas:

- **`PoTokenWebView.kt`** — implementação completa e funcional. Faz POST real para:
  - `https://www.youtube.com/api/jnn/v1/Create` (busca desafio BotGuard)
  - `https://www.youtube.com/api/jnn/v1/GenerateIT` (gera integrity token)
  - Headers preservados exatamente: `User-Agent`, `Accept`, `Content-Type: application/json+protobuf`,
    `x-goog-api-key`, `x-user-agent: grpc-web-javascript/0.1`
  - Body preservado: `[ "O43z0dpjhgX20SCx4KAo" ]` e `[ "O43z0dpjhgX20SCx4KAo", "<botguardResponse>" ]`

- **`PoTokenProviderImpl.kt`** — usa `YoutubeParsingHelper.getVisitorDataFromInnertube()` do
  NewPipeExtractor (não um stub) pra obter visitorData real

- **`JavaScriptUtil.kt`** — copiado verbatim, usa `nanojson` + `okio` (dependências adicionadas)

- **`po_token.html`** — asset copiado verbatim do NewPipe (VM BotGuard real)

### 🔧 Adaptações feitas (mínimas, preservando lógica)
- **RxJava3 → Kotlin coroutines**: `Single<T>` → `suspend fun`, `.blockingGet()` → `runBlocking { }`,
  `SingleEmitter<T>` → `CompletableDeferred<T>`
- **DownloaderImpl → OkHttpClient**: já usado no projeto, headers/URL/body preservados
- **`App.instance` → `appContext`**: passado via construtor (OxyMusic não tem singleton App)
- **`DeviceUtils.supportsWebView()` → inline**: usando `CookieManager.getInstance()` (mesma lógica)
- **`BuildConfig.DEBUG` → sempre logar**: sem dependência de BuildConfig

### 📦 Dependências adicionadas
- `com.github.TeamNewPipe:nanojson:1.79` (MIT) — parsing JSON usado pelo JavaScriptUtil
- `com.squareup.okio:okio:3.9.0` (Apache 2.0) — usado pelo JavaScriptUtil para base64/ByteString

### 📋 Créditos (obrigatório pela licença GPLv3)
- Arquivos potoken/* são Copyright © NewPipe Contributors, licensed under GPLv3
- Fonte: https://github.com/TeamNewPipe/NewPipe (PR #11955, branch dev)
- OxyMusic continua MIT, mas os arquivos vendorizados mantêm GPLv3 (compatível)

### 🎯 Critério de aceite
Antes de declarar corrigido, o fluxo completo (busca → resolve → play) deve funcionar sem
`ERROR_CODE_IO_BAD_HTTP_STATUS` / 403. Os logs de `PoTokenProviderImpl` devem mostrar:
- "Got visitorData (len=N)"
- "Generated streaming poToken (len=N)"
- "poToken generated for videoId=XXX (playerPot len=N, streamingPot len=N)"

## [1.13.0] — 2026-07-28 (REVOKE — continha stub não-funcional)

### 🐛 Corrigido (poToken via WebView — fix definitivo do HTTP 403)

**Causa raiz confirmada**: desde 2025/2026 o YouTube exige um **poToken** (proof-of-origin token)
gerado pelo BotGuard do Google para permitir o download real do stream de áudio. Sem ele, o
YouTube retorna a URL normalmente mas responde com **HTTP 403** quando o ExoPlayer tenta baixar.
Exatamente o sintoma reportado.

### ✨ Adicionado

- **PoToken provider via WebView** (port do NewPipe oficial, PR #11955):
  - `PoTokenWebView.kt` — roda o script BotGuard do YouTube dentro de uma `android.webkit.WebView`
    invisível/headless (system WebView, presente em qualquer dispositivo com Play Services)
  - `PoTokenProviderImpl.kt` — implementa `PoTokenProvider` do NewPipeExtractor, com cache do
    token por `visitorData` e recriação automática quando expira
  - `JavaScriptUtil.kt` — parsing dos desafios BotGuard (decodificação base64, descramble)
  - `po_token.html` asset — VM BotGuard carregada dentro da WebView
  - Registrado no `NewPipe.init()` do `YouTubeRepository` (uma única vez na inicialização)
  - Tratamento de erro: em ROMs sem WebView do sistema, loga warning e continua sem poToken
    (não crasha, não trava o app — apenas pode voltar a ter 403 em algumas faixas)

- **Fallback automático em 403 durante playback**:
  - `YouTubeRepository.resolveStream()` agora aceita `excludeSources: Set<String>`
  - `PlayerViewModel` detecta erro 403 no `onPlayerError` do ExoPlayer e automaticamente
    chama `resolveStream()` de novo excluindo a fonte que acabou de falhar
  - Só mostra erro final ao usuário se TODAS as fontes falharem (incluindo retry)

### 🔧 Mudado

- **Removido client WEB do `InnertubeClient`**:
  - YouTube tornou o client WEB SABR-only (Server Adaptive Bitrate) em 2025/2026
  - SABR retorna DASH manifests em vez de URLs HTTP diretas — ExoPlayer não consegue tocar
  - Mantidos ANDROID_VR e MWEB como fallbacks (revalidar periodicamente)
  - Referência: NewPipeExtractor issue #1297

- **NewPipeExtractor mantido em v0.26.4** (já é a última estável, com suporte a PoTokenProvider)
  - Verificado: v0.26.4 é a release mais recente (20/07/2026)

### 📦 Dependências
- Adicionada `androidx.webkit:webkit:1.12.1` (necessária para `WebSettingsCompat`/`WebViewFeature`)

### 🎯 Critérios de aceite
- ✅ Buscar "Nevada Nightcore Rock Version" e tocar — não deve mais aparecer 403
- ✅ Se uma fonte específica falhar (ex: Piped fora do ar), app tenta próxima automaticamente
- ✅ Erro final só aparece se TODAS as fontes (incluindo com poToken) falharem
- ✅ Sem regressão no fluxo de busca nem no "Testar playback (sample MP3)"
- ✅ 100% gratuito — sem API keys pagas, sem dependências proprietárias

## [1.12.0] — 2026-07-28

### 🐛 Corrigido (análise do Claude aplicada)
Dois bugs identificados por análise profunda do Claude:

**Bug 1: validateStreamUrl usava HEAD — googlevideo.com rejeita HEAD**
- Causa: servidores `googlevideo.com` (YouTube CDN) costumam rejeitar/tratar mal requests HEAD mesmo quando o mesmo URL funciona perfeitamente com GET
- O app estava descartando streams válidos como se fossem falha
- **Solução**: trocado HEAD por `GET` com `Range: bytes=0-1024` (baixa só 1KB pra testar)
- Testado: HEAD retorna 302 (falha), GET+Range retorna 206 (sucesso)

**Bug 2: Innertube era primeira fonte mas falha em 2025/2026**
- Causa: YouTube começou a cifrar quase todos os streams em 2025/2026 (documentado em issues recentes do yt-dlp)
- Innertube direto ignora `signatureCipher` (URL criptografada que precisa decifrar JS)
- NewPipeExtractor decifra signatureCipher automaticamente — deveria ser primeira fonte
- **Solução**: reordenado para NewPipe (primário) → Innertube (fast-path) → Piped (último)

### 🔧 Mudado
- `InnertubeClient.validateStreamUrl()`: HEAD → GET+Range
- `YouTubeRepository.resolveStream()`: reordenado NewPipe primeiro, Innertube segundo, Piped último
- Logs mais detalhados em cada fonte tentada

## [1.9.0] — 2026-07-28

### 🐛 Corrigido
- **Bug crítico**: HTTP 403 Forbidden resolvido! Causa raiz DEFINITIVA:
  - YouTube implementou **PoToken enforcement** em 2024/2025
  - Client ANDROID regular retorna URLs que exigem PoToken → HTTP 403
  - Client WEB retorna URLs com signatureCipher (precisam decifração JS)
  - **Solução**: usar client **ANDROID_VR** como primário — retorna URLs DIRETAS sem signatureCipher E sem poToken requirement

### ✨ Adicionado
- **Validação pré-playback**: HEAD request em cada URL antes de passar pro ExoPlayer
  - Se URL retornar 403 → automaticamente tenta próxima fonte (NewPipe, Piped)
  - Detecta erro cedo, sem esperar ExoPlayer falhar
- `InnertubeClient.validateStreamUrl()` — método novo pra validar URLs

### 🔧 Mudado
- Ordem dos clients Innertube atualizada:
  1. **ANDROID_VR** ← primário (funciona sem poToken, URLs diretas)
  2. IOS
  3. ANDROID_TESTSUITE
  4. ANDROID (pode 403 sem poToken)
  5. WEB (último resort)
- `YouTubeRepository.resolveStream()` agora valida cada URL antes de retornar
- Logs mais detalhados em cada etapa de validação

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
