# Marco 7 — modos de exibição e adaptação ao monitor

Incremento de **28/10/2026**, implementado e aprovado em **457 testes de lógica,
sem falhas, erros ou testes ignorados, integração OpenGL e execução nativa do
pacote Windows**. Os 439 testes anteriores pertencem ao
[tamanho do HUD](MARCO_7_ESCALA_HUD.md); este lote acrescenta 18 verificações
de lógica. Revisão humana, M7 maior e retomada de tentativa continuam em aberto.

## Três modos em F2

O painel de vídeo oferece **Janela**, **Janela sem bordas** e **Tela cheia**.
O monitor é identificado automaticamente a partir da janela atual; não é
necessário presumir que ele tenha origem `(0,0)` ou resolução Full HD.

| Modo | Comportamento |
| --- | --- |
| Janela | Decorada e redimensionável. O tamanho escolhido é ajustado proporcionalmente à área útil do monitor, considerando barra de tarefas e moldura; a janela fica centralizada ou limitada à área disponível. |
| Janela sem bordas | Uma janela sem decoração, cobrindo a área total do monitor atual e posicionada na origem dele. Usa o desktop sem entrar no modo exclusivo de tela cheia. |
| Tela cheia | Usa o modo exclusivo com a resolução atual do monitor em uso. O tamanho preferido da janela continua guardado para quando voltar a Janela. |

Os cinco presets de **800×450, 960×540, 1280×720, 1600×900 e 1920×1080**
ficam disponíveis somente em Janela. Nos dois outros modos, a cobertura da
tela acompanha o monitor; os presets não representam uma escolha de resolução
física. Janela sem bordas também conserva o tamanho preferido da janela.
O limite mínimo continua sendo 640×360; uma área útil incapaz de comportá-lo
é recusada com mensagem, sem gravar o modo não confirmado.

F2 continua disponível no menu, no refúgio, na arena e na sala do Guardião.
As entradas existentes pela abertura usam o mesmo painel. Editar mantém um
rascunho e pausa a simulação. Clique ou use as setas para escolher as opções;
**Enter/Aplicar** inicia a prévia. VSync e a página TAMANHO DO HUD permanecem
disponíveis, com as regras de cada preferência.

## Adaptação, confirmação e reversão

O serviço adapta o pedido à área disponível **antes** de comparar o modo
atual, aplicar a prévia ou registrar o alvo de confirmação. Assim, uma janela
grande demais pode ser reduzida para caber e mostrar o tamanho efetivamente
aplicado. A inicialização também adapta uma preferência salva sem reescrever
o arquivo automaticamente.

O prazo continua sendo **15 segundos de tempo real**. Enter/Manter confirma;
Esc/Reverter ou o prazo desfaz. F2, fechamento da tela/jogo e pausa da aplicação
também cancelam uma prévia ainda não confirmada. O snapshot nativo preserva
posição, dimensões, monitor e flags de decoração/redimensionamento, além das
preferências de modo e VSync.

Reverter tenta restaurar o estado original. Se o monitor foi removido ou o
estado anterior deixou de ser possível, usa uma janela segura ajustada ao
monitor disponível. Falha do provedor de área segura também é tratada como
erro visível; o modo não confirmado não é salvo.

O adaptador verifica o resultado nativo de tamanho, posição e flags, além do
retorno das chamadas do sistema. VideoBackend.matches também valida a
geometria na confirmação e na inicialização; mover uma janela sem bordas com
Win+setas durante a prévia não pode confirmar uma cobertura incompatível com
o monitor. Entradas de abrir/fechar e a proteção de 0,2 s/soltura do mouse
continuam evitando encaminhar um clique ao combate ou à confirmação seguinte.

## Arquitetura e persistência

[VideoSettings](../core/src/main/java/br/com/srsups/paradiseinhell/VideoSettings.java)
usa `Mode.WINDOWED`, `BORDERLESS` e `FULLSCREEN`. Seu width/height representam
o tamanho preferido da janela, inclusive quando outro modo está ativo.
O construtor booleano, fullscreen() e withFullscreen(boolean) permanecem para
compatibilidade de chamadas antigas: true corresponde somente à tela cheia
exclusiva; Janela sem bordas é um estado distinto.

O JSON de `video-settings.json` passa à **versão 2**, com `mode` em texto,
width, height e vsync. A versão 1 com fullscreen booleano migra na leitura;
salvar uma confirmação escreve a versão atual. O parser rejeita tipos,
campos, duplicações e modos inválidos. Versões futuras são reconhecidas antes
do schema atual e preservadas no principal e no backup `.bak`, inclusive em
gravações diretas ao store.

[VideoBackend](../core/src/main/java/br/com/srsups/paradiseinhell/VideoBackend.java)
oferece adaptação e snapshot/restore; o
[serviço](../core/src/main/java/br/com/srsups/paradiseinhell/VideoSettingsService.java)
mantém confirmação, prazo monotônico, callbacks protegidos e retry. Apenas
uma confirmação salva a preferência global. Falha de gravação conserva o modo
na sessão e **S no painel** repete a escrita. Falha de leitura usa uma sessão
segura, preservando o arquivo. Trocar/excluir personagem mantém as opções.

[GdxVideoBackend](../core/src/main/java/br/com/srsups/paradiseinhell/GdxVideoBackend.java)
recebe [WindowPlacement](../core/src/main/java/br/com/srsups/paradiseinhell/WindowPlacement.java).
O launcher Windows fornece
[Lwjgl3WindowPlacement](../lwjgl3/src/main/java/br/com/srsups/paradiseinhell/lwjgl3/Lwjgl3WindowPlacement.java)
por Main(VideoBackend), obtendo do GLFW a geometria real do monitor, área útil,
posição, moldura e flags da janela. Monitores à esquerda/acima da tela
principal conservam suas origens negativas. Tamanhos da janela são medidos
em coordenadas lógicas do desktop; framebuffer HiDPI não é usado como tamanho
de janela. O fallback genérico de Graphics é aproximado e não substitui esse
adaptador desktop na produção.

As preferências de HUD e controles continuam em seus arquivos próprios.
Este incremento não altera a câmera, as colisões, a economia, os saves da
campanha ou as artes/encenação da abertura.

## Validação

**457 testes de lógica passaram, sem falhas, erros ou ignorados.** São 18 novos
em relação aos 439 anteriores: nove de modelo/serviço/store e nove do
adaptador. **Integração OpenGL aprovada**, com `M7_DISPLAY_MODES_OK` e
`DEMO_SMOKE_OK`, encerrada com código zero. **Execução nativa do pacote Windows
aprovada em 28/10/2026**, com `WINDOWS_PACKAGE_OK` e código de saída zero.

[VideoSettingsTest](../core/src/test/java/br/com/srsups/paradiseinhell/VideoSettingsTest.java)
tem 21 casos de modelo/serviço/store, incluindo migração, schema, versões
futuras, adaptação, confirmação, snapshot de posição, falhas, callbacks e
provedor de janela segura indisponível. Os 14 casos do
[adaptador](../core/src/test/java/br/com/srsups/paradiseinhell/GdxVideoBackendTest.java)
usam monitores e janelas simulados para verificar área útil, molduras,
coordenadas negativas, HiDPI, transições, confirmação de geometria e restauração.

A integração
[DisplayModeSmoke](../lwjgl3/src/test/java/br/com/srsups/paradiseinhell/lwjgl3/DisplayModeSmoke.java)
usa preferências em memória e backend de janela falso, sem mudar o modo físico
do monitor. Percorre as três seleções, presets ativos somente em Janela,
adaptação, prévia, confirmação/reversão, persistência/retry e as quatro entradas.
Os três botões e as descrições foram inspecionados em Janela sem bordas a
960×540 e Tela cheia a 800×600, sem cortes de leitura.

| Cena/estado | 960×540 | 800×600 |
| --- | --- | --- |
| Janela | [captura](../build/smoke/m7-display-windowed.png) | [captura](../build/smoke/m7-display-windowed-800.png) |
| Janela sem bordas | [captura](../build/smoke/m7-display-borderless.png) | [captura](../build/smoke/m7-display-borderless-800.png) |
| Tela cheia | [captura](../build/smoke/m7-display-fullscreen.png) | [captura](../build/smoke/m7-display-fullscreen-800.png) |
| Confirmação da prévia | [captura](../build/smoke/m7-display-preview.png) | [captura](../build/smoke/m7-display-preview-800.png) |

O launcher consulta a geometria nativa no primeiro quadro, com marcador
`PACKAGED_MONITOR_SMOKE_OK`, e renderiza os três botões em janela oculta no
diagnóstico habitual. O diagnóstico opcional **`--display-smoke-test`** também
aplica os três modos nativos reais na janela oculta, usando a resolução/taxa
atuais do monitor, e restaura o snapshot em `finally`. Exige o marcador
`PACKAGED_DISPLAY_MODES_SMOKE_OK` antes de declarar sucesso.

O [script do pacote](../tools/verify_windows_package.ps1) executa os diagnósticos
de jogo, áudio e modos; esse terceiro diagnóstico é distinto dos smokes de UI
com backend falso. **Os três diagnósticos passaram no executável empacotado**,
com Java incluído, PATH sem Java externo, perfis/preferências em memória e
OpenAL com dispositivo null para áudio. O teste nativo aplicou Janela, Janela
sem bordas e Tela cheia reais no monitor atual, sem escolher outra resolução
ou taxa de atualização, e confirmou a restauração do snapshot, posição e flags
por `PACKAGED_DISPLAY_MODES_SMOKE_OK`. O launcher protege o render contra
reentrada durante callbacks nativos de redimensionamento.

Uma janela oculta no monitor atual não homologa uso visível em tela cheia,
Alt+Tab visível, vários monitores físicos, remoção real de monitor ou HiDPI
real; esses cenários permanecem em revisão manual no Windows. O M7 maior e
os playtests continuam abertos.

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :lwjgl3:smokeTest
.\gradlew.bat :lwjgl3:packageWinX64
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```
