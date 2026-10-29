# Marco 7 — vídeo e tamanho da janela

Incremento de **27/10/2026**, implementado e verificado. A configuração de
imagem permite testar janela, tela cheia e VSync antes de salvar. Os resultados
de 372 testes, OpenGL e Windows pertencem ao incremento anterior de
[tutorial/Centelha](MARCO_7_TUTORIAL_E_CENTELHA.md). **389 testes de lógica passaram,
sem falhas ou erros, com OpenGL e execução nativa do pacote Windows aprovados**.
O M7 maior e a revisão humana do M6 permanecem em aberto.

Este documento conserva o contrato histórico de dois modos e seus 389 testes.
Em **28/10/2026**, o [incremento de três modos](MARCO_7_MODOS_DE_EXIBICAO.md)
acrescenta Janela sem bordas, adaptação à área útil/bounds do monitor,
snapshot de posição/flags e migração v1 → v2. Ele foi verificado; os
resultados do lote original não homologam essas alterações. A lógica atual
passou em **457 testes sem falhas ou erros, integração OpenGL e execução nativa
Windows em 28/10/2026**. O diagnóstico opcional do novo pacote aplicou os três
modos reais na janela oculta e restaurou o snapshot, confirmando
PACKAGED_DISPLAY_MODES_SMOKE_OK; a UI usa backend falso e não troca o modo
físico durante seus smokes. Uso visível/Alt+Tab, vários monitores e HiDPI reais
continuam em revisão manual.

## Opções e controles

**F2** abre vídeo no menu, no refúgio, na arena e no chefe. O menu tem também
um botão Vídeo. Nas três telas de jogo, o painel é uma sobreposição: a tentativa
permanece na mesma tela e a simulação fica pausada. Fechar o painel preserva
pausa, Tab, áudio, conversa, altar ou escolha de dádiva que estavam abertos.
A entrada de abertura/fechamento é consumida para evitar ataque ou interação
no mesmo quadro.

| Opção | Comportamento |
| --- | --- |
| Janela | Presets de 800×450, 960×540, 1280×720, 1600×900 e 1920×1080 |
| Tela cheia | Usa a **resolução atual do monitor**, obtida por `getDisplayMode()`; não escolhe uma resolução de hardware presumida |
| VSync ligado | Sincronização vertical; limite de primeiro plano em `refreshRate + 1`, usando 60 Hz como fallback somente quando o monitor informa zero |
| VSync desligado | Limite de 120 FPS |

O tamanho da janela fica guardado ao entrar em tela cheia. Presets maiores que
o espaço disponível no monitor podem ser recusados, preservando a imagem anterior.
O backend também oferece uma janela segura para recuperação de um modo indisponível.
Redimensionamento manual continua respeitando o mínimo de 640×360.

| Ação | Controle |
| --- | --- |
| Escolher opção | Clique ou setas acima/abaixo |
| Ajustar modo, tamanho ou VSync da opção atual | Setas esquerda/direita |
| Escolher preset de janela | 1–5, teclado numérico ou clique; desativado em tela cheia |
| Aplicar e começar a prévia | Enter ou Aplicar |
| Manter a imagem durante a prévia | Enter ou clique em Manter |
| Reverter a prévia | Esc, clique em Reverter ou tempo esgotado |
| Fechar vídeo | F2 ou Voltar/Esc; uma prévia ainda aberta é revertida |
| Tentar gravar uma confirmação pendente | S no painel de vídeo |

Editar as opções só altera um rascunho. O painel mantém uma proteção de entrada
de 0,2 s e exige soltar o botão esquerdo antes de aceitar outra confirmação.
Esc durante a prévia volta ao painel com a imagem anterior; Esc novamente sai.

## Prévia e reversão

[VideoSettingsService](../core/src/main/java/br/com/srsups/paradiseinhell/VideoSettingsService.java)
captura o estado real anterior e testa o rascunho por **15 segundos**, usando
tempo monotônico (`System.nanoTime`) independente do relógio do combate.
Sem confirmação explícita, desfaz a mudança. Pausar a aplicação, trocar de tela,
fechar o painel ou encerrar o jogo também cancela a prévia. Cancelar não grava
o rascunho nem altera personagem, moedas ou progressão.

Confirmar depois do prazo ou quando o backend não reconhece o modo pedido é
recusado. Alterações nativas usam uma guarda de reentrada: callbacks de mudança
de tamanho não podem aplicar/confirmar o modo novamente. Se um cancelamento
ocorrer durante a operação nativa, ele é processado ao terminar essa operação.
Se o estado anterior não puder ser restaurado, tenta a janela segura e informa
a falha. Uma prévia não sobrescreve a última preferência confirmada.

[GdxVideoBackend](../core/src/main/java/br/com/srsups/paradiseinhell/GdxVideoBackend.java)
isola as chamadas nativas de janela, monitor, VSync e limite de FPS.
[VideoPanel](../core/src/main/java/br/com/srsups/paradiseinhell/VideoPanel.java)
empresta os recursos das telas de jogo; o menu usa
[VideoSettingsScreen](../core/src/main/java/br/com/srsups/paradiseinhell/VideoSettingsScreen.java).
Não há novo atlas nem alteração da área de combate.

## Preferências e falhas de gravação

O arquivo global é `%LOCALAPPDATA%\ParadiseInHell\video-settings.json`, com
`version: 1`, `width`, `height`, `fullscreen` e `vsync`. O padrão é janela
960×540 com VSync ligado. [VideoSettings](../core/src/main/java/br/com/srsups/paradiseinhell/VideoSettings.java)
valida dimensões e tipos; tamanhos manuais válidos podem ser lidos mesmo fora
dos cinco presets.

[FileVideoSettingsStore](../core/src/main/java/br/com/srsups/paradiseinhell/FileVideoSettingsStore.java)
usa UTF-8, arquivo temporário, troca atômica quando disponível e uma cópia
anterior válida em `video-settings.json.bak`. Um principal corrompido pode
recuperar a cópia; uma versão futura preserva o arquivo, sem ser substituída
por uma versão antiga. Se a leitura falhar, opções ficam apenas na sessão,
com mensagem explícita e arquivo preservado.

Só confirmar tenta gravar. Se a escrita falhar, a imagem confirmada fica ativa
e a preferência permanece em memória como pendente; **S no painel** repete a
gravação. Fechar o processo antes de resolver a falha pode perder essa alteração
ainda não salva. Falhas de modo/driver são apresentadas separadamente.

Preferências de vídeo são independentes de perfis, Almas, desbloqueios e áudio.
Trocar/excluir personagem não apaga o arquivo global. A configuração é aplicada
na inicialização; um modo salvo indisponível recupera uma janela segura sem
sobrescrever automaticamente o arquivo original.

## Limites e validação

Este incremento muda o **tamanho da janela e o modo de exibição**. Escala livre
do HUD e remapeamento de comandos continuam para um trabalho próprio de
layout/entrada; não foram entregues. Os viewports e coordenadas atuais são
preservados. Cutscenes em camadas têm [direção aprovada e proposta](CUTSCENES_E_ABERTURA.md),
mas o sistema, as artes e o roteiro final continuam futuros.

**389 testes de lógica aprovados, sem falhas ou erros**: base histórica de 372,
mais 12 de [VideoSettingsTest](../core/src/test/java/br/com/srsups/paradiseinhell/VideoSettingsTest.java)
e cinco de [GdxVideoBackendTest](../core/src/test/java/br/com/srsups/paradiseinhell/GdxVideoBackendTest.java).
**Integração OpenGL aprovada**, com `:lwjgl3:smokeTest` encerrado com código zero.
**Execução nativa do pacote Windows aprovada em 27/10/2026**, com
`WINDOWS_PACKAGE_OK` e código de saída zero no
[script de verificação](../tools/verify_windows_package.ps1). O ZIP recompilado
inclui a GUI de vídeo em seu diagnóstico, renderizada com backend em memória,
sem trocar o modo físico do monitor. A execução usou Java incluído, PATH sem
Java externo, 24 efeitos e música via OpenAL nativo silenciado, perfis em
memória e outra pasta de trabalho. Os saves reais foram preservados.

A lógica cobre prévia/confirmar/reverter, tempo monotônico, reentrada, fallback,
backup, versão futura, retry e independência dos saves. O adaptador é testado
com Graphics simulado: tamanho lógico separado do framebuffer HiDPI,
preservação de janela ao usar tela cheia, modos recusados, VSync em taxas
positivas (limite de 31 FPS para 30 Hz) e rejeição de sucesso nativo sem redimensionamento.

[VideoSmoke](../lwjgl3/src/test/java/br/com/srsups/paradiseinhell/lwjgl3/VideoSmoke.java)
percorre as quatro entradas de F2 e os controles reais de rascunho, aplicar,
confirmar e reverter, incluindo proteção contra ataque segurado. Verifica
persistência, falha/retry S, quadros sem gravações repetidas e reversão ao
trocar/fechar tela. Na arena e no chefe, movimento, dash, projéteis e relógios
ficam congelados; Tab, F1, áudio, pausa e escolha de dádiva conservam seus
estados. No refúgio, também preserva a fala já revelada durante uma conversa.
Textos são medidos com a fonte real nos cartões/botões e no rodapé de erro.

As capturas foram inspecionadas em **960×540 e 800×600**, com enquadramento
proporcional/letterbox na janela menor. O menu mantém os botões Áudio e Vídeo
legíveis; painel e prévia couberam sem cortes.

| Entrada/estado | 960×540 | 800×600 |
| --- | --- | --- |
| Vídeo pelo menu | [captura](../build/smoke/m7-video-menu.png) | [captura](../build/smoke/m7-video-menu-800.png) |
| Confirmação da prévia | [captura](../build/smoke/m7-video-preview.png) | [captura](../build/smoke/m7-video-preview-800.png) |
| Vídeo sobre combate | [captura](../build/smoke/m7-video-combat.png) | [captura](../build/smoke/m7-video-combat-800.png) |
| Vídeo no refúgio | [captura](../build/smoke/m7-video-refuge.png) | [captura](../build/smoke/m7-video-refuge-800.png) |
| Vídeo no chefe | [captura](../build/smoke/m7-video-boss.png) | [captura](../build/smoke/m7-video-boss-800.png) |

Nos smokes, preferências usam memória e o backend de vídeo é falso para evitar
trocar a configuração física do monitor ou gravar opções reais. Esses testes
não constituem homologação de tela cheia, VSync ou resolução física no hardware
do jogador. O diagnóstico do launcher também renderiza a GUI com backend em
memória, sem mudança física de modo. Tela cheia, VSync e resoluções físicas
em diferentes monitores, HiDPI real e leitura em partidas continuam pendentes
de revisão manual no Windows.

Reprodução a partir da raiz, com JDK 21 e ambiente gráfico para OpenGL:

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :lwjgl3:smokeTest
.\gradlew.bat :lwjgl3:packageWinX64
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```

## Incrementos posteriores

Os limites e os **389 testes** acima descrevem a entrega histórica de vídeo
de 27/10/2026. O [remapeamento de controles](MARCO_7_CONTROLES.md) foi entregue
em um incremento separado. Em **28/10/2026**, a página
[TAMANHO DO HUD](MARCO_7_ESCALA_HUD.md) foi implementada dentro de F2, com
preferência própria e prévia local. Passou em **439 testes de lógica, sem falhas
ou erros, integração OpenGL e execução nativa Windows** em 28/10/2026. Ela não altera
o modo físico ou as regras de confirmação deste lote. O
[piloto da abertura](PILOTO_DA_ABERTURA.md) também tem registro próprio;
a direção das próximas cutscenes está no [planejamento](CUTSCENES_E_ABERTURA.md).
