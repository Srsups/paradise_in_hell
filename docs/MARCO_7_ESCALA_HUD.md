# Marco 7 — tamanho do HUD de combate

Incremento implementado e validado em **28/10/2026**: **439 testes de lógica
passaram, sem falhas ou erros, e a integração OpenGL foi aprovada**. Os 424
testes anteriores pertencem ao remapeamento de controles; este lote acrescenta
12 de preferências e três de layout. **A execução nativa do pacote Windows
também foi aprovada em 28/10/2026.** A revisão humana do M6, os playtests e as
demais entregas do M7 continuam abertos.

O tamanho dos indicadores de combate pode ser ajustado de **80% a 125%**, em
passos de um ponto percentual. O padrão continua sendo **100%**. A preferência
é global, independente dos personagens, dificuldade, profissão, moedas e
desbloqueios. Ela não modifica a câmera da arena, os personagens, hitboxes,
coordenadas de mira ou modo de exibição físico.

## Entrada, rascunho e prévia

Abra **F2 → TAMANHO DO HUD** no menu, no refúgio, na arena ou na sala do
Guardião. VideoPanel mantém uma página separada para HudPanel. Essa página
tem uma prévia local dos indicadores do jogador e do relógio da demo, sem
avançar a tentativa ou aplicar o rascunho à partida.

| Ação | Controle |
| --- | --- |
| Diminuir/aumentar em 5 pontos percentuais | Setas esquerda/direita ou botões −/+ |
| Escolher 80%, 100% ou 125% | Clique no preset |
| Ajustar em passos de 1 ponto percentual | Clique ou arraste a barra |
| Aplicar e salvar | Enter ou SALVAR |
| Voltar a 100% no rascunho | RESTAURAR 100%; salvar aplica |
| Repetir gravação que falhou | S na página do HUD |
| Voltar à página de imagem | Esc ou VOLTAR A VÍDEO |
| Fechar as opções de vídeo e HUD | F2 |

A proteção de entrada exige **0,2 segundo** e soltura do botão esquerdo.
Editar altera somente a prévia; fechar a página descarta o rascunho não
aplicado. Salvar torna o tamanho ativo para todos os personagens. Se a
gravação falhar, a seleção permanece nesta sessão e S repete a escrita.

O painel usa a pausa já existente de F2: movimento, projéteis, IA, padrões do
chefe e relógios ficam congelados, e os estados de diálogo, altar, escolha de
dádiva e outras consultas são preservados. Os quadros de entrada e saída são
consumidos. Aplicar ou fechar a página do HUD bloqueia comandos segurados até
a soltura física, antes de permitir outra ação de gameplay.

Sobrepor controles com F3 ou pausar a aplicação cancela um arrasto em curso.
Ao retomar a página do HUD, é preciso soltar o botão esquerdo e passar pela
proteção de 0,2 s antes de continuar editando. O rascunho permanece, mas um
arrasto anterior não pode continuar com o ponteiro em outro painel.

Esse ajuste não chama a prévia nativa de vídeo de 15 segundos. A confirmação
de janela/tela cheia e VSync continua pertencendo à página de imagem e ao seu
[incremento próprio](MARCO_7_VIDEO_E_JANELA.md).

## Indicadores cobertos

| Recebem a escala | Mantêm a apresentação atual |
| --- | --- |
| Painel do jogador: vida, estamina, XP, nível e estado do dash | Menus, seleção de dádivas, consultas, diálogos e cutscenes |
| Metas de abate e indicação da passagem | Rodapés e mensagens contextuais, incluindo baú e Fúria |
| Relógio da demo | Números de dano e rótulos sobre o mundo |
| Barra, nome e fase do Guardião | Barra sobre o Carrasco, telegráficos e marcadores de ataque |
| Caixa de aviso do Guardião | Borda de dano, que continua cobrindo a tela inteira |

Ícones, barras, molduras e fontes de cada grupo são ampliados juntos. O aviso
do Guardião foi realinhado à coluna direita, abaixo da barra do chefe, com
largura base de 422 e altura de 42 unidades lógicas e espaço para duas linhas.
Isso evita que a caixa invada os indicadores do jogador ao ampliar o HUD.

## Layout e renderização

[HudLayout](../core/src/main/java/br/com/srsups/paradiseinhell/HudLayout.java)
define os limites lógicos em 960×540, com âncora superior esquerda `(14,526)`
para o jogador e superior direita `(946,526)` para metas, relógio e chefe.
O FitViewport conserva essas proporções e o letterbox ao redimensionar.

[HudScaleRenderer](../core/src/main/java/br/com/srsups/paradiseinhell/HudScaleRenderer.java)
reutiliza matrizes e aplica a transformação ao SpriteBatch e ShapeRenderer
entre passagens de desenho completas. A matriz anterior é restaurada ao
terminar cada grupo; projeções, câmeras, viewports e input não são modificados.
Os painéis sobrepostos conservam seu tamanho e suas áreas clicáveis.

Em 125%, o painel do jogador mede 312,5 unidades e o do chefe, 527,5. Há 92
unidades de separação horizontal. Todas as porcentagens aceitas mantêm as
caixas dentro da área lógica, sem sobreposição entre jogador, metas, barra
do chefe e aviso. A faixa é limitada para preservar essa composição.

## Modelo e persistência

[HudSettings](../core/src/main/java/br/com/srsups/paradiseinhell/HudSettings.java)
é um record imutável com `percent`, limites 80/125, padrão 100 e conversão
`scale()` para o fator de desenho. O JSON versão 1 contém `version` e `percent`
inteiros; campos ausentes, desconhecidos ou duplicados e valores inválidos
são rejeitados. Versões futuras são detectadas antes do schema atual.

[HudSettingsService](../core/src/main/java/br/com/srsups/paradiseinhell/HudSettingsService.java)
aplica somente a confirmação, conserva falhas de gravação como pendentes e
evita escrever novamente valores já salvos. Se a leitura inicial falhar,
assume 100% e permite alterações apenas na sessão, preservando o arquivo.

[FileHudSettingsStore](../core/src/main/java/br/com/srsups/paradiseinhell/FileHudSettingsStore.java)
usa `hud-settings.json`, fora da instalação, junto às preferências existentes.
A escrita usa UTF-8, temporário, movimento atômico quando suportado e backup
validado `.bak`. Um principal inválido tenta recuperar o backup. Um principal
com versão futura impede o fallback; uma versão futura no principal ou no
backup impede sobrescrita, inclusive por chamada direta ao store.

O arquivo é separado de `video-settings.json`, `control-settings.json`, perfis
e progresso compartilhado. Trocar ou excluir um personagem não apaga o tamanho
do HUD. Preferências de vídeo e os saves não precisam de migração neste lote.

## Validação

[HudSettingsTest](../core/src/test/java/br/com/srsups/paradiseinhell/HudSettingsTest.java)
acrescenta 12 testes: faixa inteira, round-trip, schema, versões futuras,
armazenamento em memória, confirmação, falha/retry, somente sessão, escrita
atômica, backups e independência da criação/exclusão dos slots.
[HudLayoutTest](../core/src/test/java/br/com/srsups/paradiseinhell/HudLayoutTest.java)
acrescenta três testes para âncoras, limites e separação das caixas em todas as
porcentagens aceitas. **Os 439 testes passaram sem falhas ou erros.**

A integração OpenGL final encerrou com código zero, **M7_HUD_OK e
DEMO_SMOKE_OK**, após a proteção do arrasto ao perder o foco. A cobertura está
em [HudSmoke](../lwjgl3/src/test/java/br/com/srsups/paradiseinhell/lwjgl3/HudSmoke.java):
fluxo de F2 nas quatro entradas, rascunho, slider, presets, cancelar/salvar/
restaurar/falhar/repetir, preservação do combate e das matrizes, além dos textos
em 960×540 e 800×600. A regressão de F3 verifica arrasto, soltura em outro painel
e retorno na arena, refúgio e chefe. A pausa da aplicação também é exercitada
pela tela de vídeo independente.

As capturas foram geradas nas duas resoluções. A inspeção visual incluiu
prévia de 125% e combate de 125% em 960×540, prévia de 80%, chefe de 125% e
erro de gravação em 800×600, com leitura e separação dos indicadores preservadas.

| Estado | 960×540 | 800×600 |
| --- | --- | --- |
| Página do HUD | [captura](../build/smoke/m7-hud-menu.png) | [captura](../build/smoke/m7-hud-menu-800.png) |
| Prévia em 80% | [captura](../build/smoke/m7-hud-80.png) | [captura](../build/smoke/m7-hud-80-800.png) |
| Prévia em 125% | [captura](../build/smoke/m7-hud-125.png) | [captura](../build/smoke/m7-hud-125-800.png) |
| Combate em 125% | [captura](../build/smoke/m7-hud-combat-125.png) | [captura](../build/smoke/m7-hud-combat-125-800.png) |
| Guardião em 125% | [captura](../build/smoke/m7-hud-boss-125.png) | [captura](../build/smoke/m7-hud-boss-125-800.png) |
| Falha de gravação | [captura](../build/smoke/m7-hud-save-error.png) | [captura](../build/smoke/m7-hud-save-error-800.png) |

**PackageWinX64 e a verificação nativa Windows encerraram com código zero**, com
WINDOWS_PACKAGE_OK, Java incluído, PATH sem Java externo, perfis/preferências
em memória e áudio OpenAL null com 24 efeitos e música em streaming. O
diagnóstico empacotado abre a página do HUD pelo construtor próprio de
VideoSettingsScreen, desenha HudPanel e seus indicadores de amostra no quadro
29 e emite PACKAGED_HUD_SMOKE_OK. Os construtores antigos de Main conservam
HudSettingsStore em memória, para não gravar a preferência real nos smokes.

Essa execução preserva os saves e as configurações físicas do monitor.
Revisão humana dos controles/leitura, o M7 maior e a retomada de tentativa
continuam em aberto; a abertura atual foi preservada.

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :lwjgl3:smokeTest
.\gradlew.bat :lwjgl3:packageWinX64
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```
