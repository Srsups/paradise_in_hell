# Arte da demo e do Submundo

Atualizado em **27/10/2026**. A direção de pedra, bronze e azul do Estige começou
no [piloto do M3](MARCO_3_BASE_VISUAL.md) e foi ampliada, a pedido do autor, antes
do M4. A primeira expansão está em [EXPANSAO_VISUAL.md](EXPANSAO_VISUAL.md).
A revisão vigente está em [POLIMENTO_VISUAL_E_REFUGIO.md](POLIMENTO_VISUAL_E_REFUGIO.md).

## Recursos usados no jogo

| Recurso | Conteúdo e utilização |
| --- | --- |
| [equipped.atlas](../core/assets/visual/equipped.atlas), [página 1](../core/assets/visual/equipped.png) e [página 2](../core/assets/visual/equipped-2.png) | 926 poses de origem, 12 quadros de Caronte/altar/travessia e 64 avisos; 1.280 poses de corpo disponíveis contando espelhamentos |
| [expanded.png](../core/assets/visual/expanded.png) / [atlas](../core/assets/visual/expanded.atlas) | 385 regiões em 1024×2048: comuns/rápidos/tanks, poderes, baús, ícones e retrato; corpos antigos permanecem como histórico e armas isoladas são usadas somente na forja |
| [pilot.png](../core/assets/visual/pilot.png) / [atlas](../core/assets/visual/pilot.atlas) | Base do M3: Caronte, ícones das barras, flecha, XP e dash; quadros antigos de Caçador/comum permanecem como histórico, substituídos na apresentação atual |
| [demo-atlas.png](../core/assets/demo-atlas.png) | Pisos/estruturas anteriores preservados como origem histórica; cristal de XP e fragmento de pedra usado nos projéteis do Carrasco |
| [m4.png](../core/assets/visual/m4.png) / [atlas](../core/assets/visual/m4.atlas) | Sete ícones de novas dádivas e um ícone de estamina preservado, sem exibição no título do altar; 256×128, 128 KiB RGBA; origem/prompts em tools/art-sources/m4 |
| [m5.png](../core/assets/visual/m5.png) / [atlas](../core/assets/visual/m5.atlas) | 140 poses do Fazendeiro com ancinho/foice integrados, retrato/corpo da Camponesa, dois ícones de forja e oito quadros de efeitos; 152 regiões em 1024×1024, 4 MiB RGBA |
| [m6-dialogue.png](../core/assets/visual/m6-dialogue.png) / [atlas](../core/assets/visual/m6-dialogue.atlas) | Quatro expressões para Caronte, Ferreiro Errante e Camponesa; 12 retratos de 256×256 em 1024×1024, 4 MiB RGBA; compartilhados pelas conversas |
| [m6-reactions.png](../core/assets/visual/m6-reactions.png) / [atlas](../core/assets/visual/m6-reactions.atlas) | Dano/morte dos 16 atores de combate; 560 regiões de 64×64, 896 poses com espelhamentos; página 2048×2048, 16 MiB RGBA |
| [m6-scenery.png](../core/assets/visual/m6-scenery.png) / [atlas](../core/assets/visual/m6-scenery.atlas) | Cinco temas com seis pisos e três blocos cada, três ilustrações de rotas e quatro quadros de passagem; 52 regiões em 1024×1024, 4 MiB RGBA |
| [opening.png](../core/assets/cutscenes/opening.png) / [atlas](../core/assets/cutscenes/opening.atlas) | Atlas próprio do piloto de abertura em camadas; 15 regiões em 2048×2048, 16 MiB RGBA, carregado/descartado pela apresentação |
| Fontes em core/assets/ui | Noto Sans para corpo e Cinzel para títulos; PNG/FNT e licenças OFL incluídos |

Os atlas são compartilhados entre telas. As armas fazem parte dos quadros de corpo
das nove profissões e do arqueiro; o Carrasco também segura sua espada integrada.
As profissões têm seis posições de caminhada, quatro de respiração e quatro de ataque.
O Fazendeiro acrescenta dois conjuntos completos, um por ferramenta, sem alterar
os oito anteriores. Seu golpe conserva a ferramenta da pose inicial durante a troca Q.
Cinco direções de origem geram oito vistas com espelhamento horizontal das três da direita.
O arqueiro tem seis poses de disparo; o Carrasco gira e lança pedras; o Guardião
respira, corre com ombrada, conjura, salta e aterrissa. Comuns/rápidos/tanks
continuam com quatro direções. Viradas preservam a passada: mistura de 0,08 s no protagonista e 0,10 s nos inimigos.

Os dois modelos de baú têm quatro estados de abertura. A abertura é cosmética;
a recompensa é resolvida uma vez pela regra existente. Os 22 tipos de dádiva têm
ícones próprios no menu de tela inteira e na consulta por Tab. Poderes ativos
têm efeitos animados; bônus passivos têm um breve efeito na escolha de nível.

Caronte, Ferreiro Errante e Camponesa têm quatro expressões ilustradas cada;
o protagonista conserva seu retrato. Cada fala escolhe retrato/expressão no JSON,
com revelação gradual/instantânea e confirmação por fala. O painel é compartilhado
pelos três NPCs; leitura e compras continuam ações distintas. A entrega está em
[MARCO_6_DIALOGOS_E_NPCS.md](MARCO_6_DIALOGOS_E_NPCS.md).
As [reações de dano/morte](MARCO_6_DANO_E_MORTE.md) também estão implementadas e
verificadas. O lote de [cenários e rotas](MARCO_6_CENARIOS_E_ROTAS.md) acrescenta
ambientes próprios ao Refúgio, Estige, Cinzas, Sombras e Guardião. O M6 geral
continua em andamento, com expansão dos cenários, outros personagens e sprites
de mundo do Ferreiro para futuros encontros exploráveis.

Os cinco ambientes têm paletas distintas e discretas: arenito bege no Refúgio,
ardósia azul-petróleo no Estige, carvão castanho com cinza ocre em Cinzas,
pedra violeta em Sombras e obsidiana castanho-vinho com bronze no Guardião.
Pisos decorados continuam planos; obstáculos têm topo maciço, bevel claro e
frente escura, cobrindo o quadrado de colisão de 16×16. As três rotas usam
ilustrações próprias, e a passagem liberada pulsa em quatro quadros com alfa.
SceneryArt compartilha uma única página entre mapas e telas. O
[catálogo em 64/16 pixels](../build/smoke/m6-scenery-catalog.png) e o
[mosaico dos pisos](../build/smoke/m6-scenery-tiling.png) registram a revisão artística.

O autor aprovou ilustrações animadas em camadas e pediu o
[piloto da abertura](PILOTO_DA_ABERTURA.md), com Zeus clássico e esposa grega
adulta. São seis quadros/45 s, com fundo, personagens e primeiro plano movidos
separadamente, zoom suave, respiração, fades e portal em quatro quadros.
Não usa flashes obrigatórios de tela inteira. O acabamento continua iterável;
o nome e a personalidade definitiva da esposa permanecem abertos.

As nove fontes desse lote foram geradas pela ferramenta integrada image_gen,
com [prompts](../tools/art-sources/opening/prompts.json),
[contrato](../tools/art-sources/opening/contract.json) e
[referências locais](../tools/art-sources/opening/references.json) já registrados.
São 15 regiões: dois fundos, dois primeiros planos, sete poses de personagens
de 432×480 e quatro quadros de portal. O
[manifesto final com hashes](../tools/art-sources/opening/manifest.json) e o
[registro de recortes](../tools/art-sources/opening/registration.csv) documentam
seleção, escala uniforme e pivô dos pés em (216,464), preservando alfa e cores.
O [catálogo composto de QA](../build/smoke/opening-art-catalog.png),
[atores](../build/smoke/opening-actors-catalog.png) e
[camadas](../build/smoke/opening-layers-catalog.png) permitem revisar os recortes.
OpeningScreen possui o atlas e o
descarta ao sair; usa filtro Linear para câmera e respiração. Isso é separado
dos atlas e máscaras de colisão do combate.
Os 412 testes de lógica passaram sem falhas ou erros; OpenGL e a execução
nativa do pacote Windows do piloto foram aprovados em 27/10/2026, incluindo
o ajuste das janelas de opacidade e o carregamento/desenho do atlas empacotado.

## Colisão e leitura

ExpandedArt e EquippedArt extraem formas de alfa uma vez por quadro; cada entidade
tem posições próprias. Corpos equipados usam alfa no recorte central de
tronco/pernas, excluindo as extensões das armas. Posição/escala/pose coincidem
com o desenho e a orientação predominante durante a mistura.
[Regra do recorte e testes](ANIMACOES_E_EQUIPAMENTOS.md#tempos-avisos-e-colisão).

Comuns/rápidos/tanks usam as células anteriores de 32×32. Os novos corpos
equipados usam 64×64 e têm 1.504 poses, incluindo as 224 do Fazendeiro e as espelhadas, verificadas pixel a pixel, além da base
anterior. A navegação continua separada. Avisos de mira, giro, pedras, marcas,
investida e queda usam sprites animados com as áreas e tempos reais do modelo.

ReactionArt acrescenta máscaras próprias nas poses de impacto. Corpos derrotados
são apenas imagens e não possuem contato. Revisar o resultado em partidas
completas, principalmente com muitos efeitos simultâneos e nos próximos cenários.
Os 16 conjuntos de reação usam cinco vistas canônicas, três quadros de dano e
quatro de morte. Escala e pivô são calibrados pelo corpo vivo correspondente;
comuns/rápidos/tanks compensam a diferença entre seus atlas de 32×32 e os novos
recortes de 64×64. Uma escala única por ator mantém a proporção durante a queda.

## Origens e reprodução

As imagens incorporadas foram geradas para o projeto com a ferramenta integrada
**image_gen**, pela habilidade imagegen, sem CLI/API de geração. As referências
enviadas orientaram composição e tema; não foram copiadas como recursos do jogo.
Os placeholders de terceiros não foram usados como referência de personagem.

- Corpos equipados e avisos: 28 originais em tools/art-sources/equipped, com os
  [prompts exatos](../tools/art-sources/equipped/prompts.json).
- Primeira expansão: 20 originais em tools/art-sources/expansion, com os
  [prompts exatos](../tools/art-sources/expansion/prompts.json).
- Piloto do M3: cinco originais em tools/art-sources/m3 e
  [prompts](../tools/art-sources/m3/prompts.json).
- Fontes: [origens, licenças e comandos](../tools/font-sources/README.md).
- M5: originais em tools/art-sources/m5, [prompts exatos](../tools/art-sources/m5/prompts.json)
  e [manifesto/seleção dos recortes](../tools/art-sources/m5/manifest.json).
- Diálogos do M6: três folhas originais em tools/art-sources/m6-dialogue,
  [prompts exatos](../tools/art-sources/m6-dialogue/prompts.json) e
  [manifesto/seleção e SHA-256](../tools/art-sources/m6-dialogue/manifest.json).
- Dano/morte do M6: 16 folhas selecionadas em tools/art-sources/m6-reactions,
  [prompts das classes](../tools/art-sources/m6-reactions/classes-prompts.json),
  [prompts dos inimigos](../tools/art-sources/m6-reactions/enemies-prompts.json),
  [revisão do rápido e demais registros](../tools/art-sources/m6-reactions/enemies-prompts-revisions.json)
  e [manifesto com hashes/escala/pivôs](../tools/art-sources/m6-reactions/manifest.json).
- Cenários do M6: nove fontes selecionadas em tools/art-sources/m6-scenery,
  com [prompts iniciais](../tools/art-sources/m6-scenery/prompts.json),
  [cinco edições de paleta/blocos](../tools/art-sources/m6-scenery/prompts-revisions.json),
  [contrato](../tools/art-sources/m6-scenery/contract.json),
  [manifesto/seleção e SHA-256](../tools/art-sources/m6-scenery/manifest.json) e
  [registro de recortes](../tools/art-sources/m6-scenery/registration.csv).
  As cinco folhas `*-tiles-refined.png` compõem o atlas atual; os cinco
  `*-tiles-original.png` permanecem preservados. As três prévias e a passagem
  conservam seus originais. Foram 14 gerações pela ferramenta integrada.
- Áudio sintetizado e avaliação sonora: [AUDIO_E_FEEDBACK.md](AUDIO_E_FEEDBACK.md).
- Abertura em camadas: nove fontes em tools/art-sources/opening,
  [prompts](../tools/art-sources/opening/prompts.json),
  [contrato](../tools/art-sources/opening/contract.json),
  [referências](../tools/art-sources/opening/references.json),
  [manifesto/SHA-256](../tools/art-sources/opening/manifest.json) e
  [recortes](../tools/art-sources/opening/registration.csv).

As ferramentas Java fazem apenas recorte, ajuste técnico de tamanho e
empacotamento; não desenham os recursos. Os originais ficam fora do pacote.
Reprodução local com o JDK:

```powershell
javac -d build/art-tools tools/EquippedAtlasBaker.java tools/PolishedFrames.java
java -cp build/art-tools EquippedAtlasBaker
java tools/ExpandedAtlasBaker.java
java tools/PilotAtlasBaker.java
java tools/FontBaker.java
javac -d build/art-tools tools/DialoguePortraitAtlasBaker.java
java -cp build/art-tools DialoguePortraitAtlasBaker
javac -d build/art-tools tools/ReactionAtlasBaker.java
java -cp build/art-tools ReactionAtlasBaker
javac -d build/art-tools tools/SceneryAtlasBaker.java
java -cp build/art-tools SceneryAtlasBaker
javac -d build/art-tools tools/OpeningAtlasBaker.java
java -cp build/art-tools OpeningAtlasBaker
```

O [baker de cenários](../tools/SceneryAtlasBaker.java) preserva as cores e o alfa
gerados; faz somente recorte, redução com vizinho mais próximo, alinhamento
dos quatro quadros de passagem pelo mesmo pivô e empacotamento. A opção
`--review` recria apenas o catálogo e o mosaico, sem alterar o atlas.

Os arquivos spritesheet.png, button_spritesheet.png e Foto_BG.jpg foram
identificados pelo autor como placeholders de terceiros. Permanecem localmente,
excluídos da distribuição pelo core/build.gradle. A revisão artística e a
preparação comercial continuam no [plano de lançamento](PLANO_DE_LANCAMENTO.md).

## Histórico do primeiro atlas

O atlas inicial tem 1254×1254 e grade proporcional de 4×4. Os pisos preenchem as
células; as demais imagens têm alfa. Fileiras: arqueiro frente/costas; arqueiro
lateral e morto-vivo; pisos e coluna; flecha, XP, escudo e água. Seus atores foram
substituídos na apresentação atual.

## Prompt usado

```text
Use case: stylized-concept. Asset type: production sprite atlas for a top-down Greek mythology survival game called Paradise in Hell. Create one square 1024 by 1024 PNG atlas with genuine transparent background, divided into an EXACT invisible 4 by 4 uniform grid of 256 by 256 pixel cells. All cells align with the canvas edges. Crisp low-resolution pixel art, strong silhouettes, limited palette, no antialiasing, no text, no labels, no grid lines, no watermark. Each character occupies about 80 percent of its own cell height, centered on exactly the same baseline, never crossing into neighboring cells. Use orthographic overhead three-quarter game sprites, original designs, no copied game characters. Row 1 left to right: a Greek mortal archer in a bronze crested helmet and ivory tunic, front-facing walking pose A; same archer front-facing walking pose B; same archer back-facing walking pose A; same archer back-facing walking pose B. Row 2 left to right: same archer right-facing walking pose A; same archer right-facing walking pose B; an undead Greek soldier front-facing walking pose A with ragged violet tunic and small round shield; same undead soldier front-facing walking pose B. Row 3 left to right: a flat dark slate stone floor seamless tile that COMPLETELY FILLS its 256 pixel cell, no transparency and no perspective; a second seamless slate floor tile same palette COMPLETELY FILLS cell; a third seamless slate floor tile with faint cracks COMPLETELY FILLS cell; a broken Greek marble pillar chunk seen from above centered on transparency. Row 4 left to right: one slender golden arrow pointing straight up centered on transparency; one small glowing cyan experience crystal centered on transparency; a bronze round shield centered on transparency; a cyan circular water rune centered on transparency. Maintain exact 4x4 cell positions so software can crop cells without moving them. Transparent means an actual alpha channel, never a painted checkerboard.
```

Caronte em corpo inteiro substitui o esqueleto do refúgio; altar e travessia têm
quatro quadros próprios. As oito classes respiram em repouso. O preparo novo registra
cabeça/tronco entre quadros e mantém a escala por sequência. Os
[prompts do polimento](../tools/art-sources/polish/prompts.json) e o
[manifesto final](../tools/art-sources/polish/manifest.json) registram as novas origens.

Em 22/10/2026, o [M4](MARCO_4_DADIVAS_E_ALTAR.md) ampliou o catálogo para 22
ícones/dádivas. Novos efeitos combinam animações existentes de água, proteção,
magnetismo, fogo, raio e ressurreição; o Eco mostra o ponto de origem e seu raio.
O atlas adicional tem carga/descarte compartilhados com ExpandedArt.

Em 23/10/2026, o [M5](MARCO_5_FAZENDEIRO.md) acrescentou o Fazendeiro com duas
ferramentas integradas ao corpo, cinco vistas canônicas e espelhamentos, retrato
da Camponesa e efeitos próprios de estocada/giro. O novo atlas é compartilhado
por ExpandedArt e EquippedArt. O processo técnico de reprodução e a captura das
caminhadas estão no documento do marco.

Ainda em 23/10/2026, o [lote de diálogos/NPCs do M6](MARCO_6_DIALOGOS_E_NPCS.md)
entregou 12 retratos com expressão por fala, diálogos em JSON para os três NPCs
e um painel de conversa compartilhado. ExpandedArt é proprietário do atlas
adicional; as telas apenas usam os recortes. Este lote não altera os sprites
dos combates, a economia, os saves, o dano ou a raridade. O restante do M6
continua planejado/em andamento.

O lote de diálogos foi aprovado em **302 testes unitários e integração OpenGL**.
A revisão das capturas confirmou legibilidade em 960×540 e 800×600, com
letterbox na janela menor. O painel da Camponesa comporta o texto do diário
concluído em 285 unidades de altura. Veja o
[catálogo das 12 expressões](../build/smoke/m6-portrait-expressions.png),
[Caronte](../build/smoke/m6-charon.png),
[forja concluída](../build/smoke/m6-blacksmith-success.png) e
[Camponesa com a promessa concluída](../build/smoke/m6-camponesa-global.png).
As variantes de resolução e condições estão no
[documento do lote](MARCO_6_DIALOGOS_E_NPCS.md#validação-e-revisão-visual).
A execução nativa do pacote Windows também foi aprovada, com Java incluído,
sem Java no PATH e com áudio nativo via dispositivo OpenAL silencioso.

O lote seguinte de [dano e morte](MARCO_6_DANO_E_MORTE.md) acrescentou 16 folhas
geradas, 560 recortes e 896 poses de reação, com armas integradas e calibração
pelos atlas existentes. A [folha frontal de revisão](../build/smoke/m6-reactions-contact-sheet.png)
permite comparar o impacto e a queda de cada ator. O
[baker independente](../tools/ReactionAtlasBaker.java) apenas processa os desenhos;
[calibration.csv](../tools/art-sources/m6-reactions/calibration.csv) e
[registration.csv](../tools/art-sources/m6-reactions/registration.csv) registram
escala, apoio dos pés e deslocamentos. **320 testes unitários, integração OpenGL
e execução nativa do pacote Windows aprovados**, incluindo as 896 poses e máscaras.
Em **25/10/2026**, o [lote de cenários e rotas](MARCO_6_CENARIOS_E_ROTAS.md)
acrescentou as cinco paletas, blocos maciços, ilustrações de destino e passagem
animada. **336 testes de lógica, integração OpenGL e execução nativa do pacote
Windows aprovados.** As capturas em 960×540 e 800×600, critérios e comandos
estão no documento do lote; a revisão em partidas completas continua pendente.
O M6 geral continua em andamento, com expansão de cenários, encontros/interações
de NPCs e revisão humana em partidas completas.
