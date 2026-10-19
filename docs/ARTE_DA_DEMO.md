# Arte da demo e do Submundo

Atualizado em **15/10/2026**. A direção de pedra, bronze e azul do Estige começou
no [piloto do M3](MARCO_3_BASE_VISUAL.md) e foi ampliada, a pedido do autor, antes
do M4. A primeira expansão está em [EXPANSAO_VISUAL.md](EXPANSAO_VISUAL.md).
A revisão vigente está em [POLIMENTO_VISUAL_E_REFUGIO.md](POLIMENTO_VISUAL_E_REFUGIO.md).

## Recursos usados no jogo

| Recurso | Conteúdo e utilização |
| --- | --- |
| [equipped.atlas](../core/assets/visual/equipped.atlas), [página 1](../core/assets/visual/equipped.png) e [página 2](../core/assets/visual/equipped-2.png) | 926 poses de origem, 12 quadros de Caronte/altar/travessia e 64 avisos; 1.280 poses de corpo disponíveis contando espelhamentos |
| [expanded.png](../core/assets/visual/expanded.png) / [atlas](../core/assets/visual/expanded.atlas) | 385 regiões em 1024×2048: comuns/rápidos/tanks, poderes, baús, ícones e retrato; corpos antigos permanecem como histórico e armas isoladas são usadas somente na forja |
| [pilot.png](../core/assets/visual/pilot.png) / [atlas](../core/assets/visual/pilot.atlas) | Base do M3: Caronte, ícones das barras, flecha, XP e dash; quadros antigos de Caçador/comum permanecem como histórico, substituídos na apresentação atual |
| [demo-atlas.png](../core/assets/demo-atlas.png) | Pisos, estruturas, cristal de XP e fragmento de pedra usado nos projéteis do Carrasco |
| Fontes em core/assets/ui | Noto Sans para corpo e Cinzel para títulos; PNG/FNT e licenças OFL incluídos |

Os atlas são compartilhados entre telas. As armas fazem parte dos quadros de corpo
das oito profissões e do arqueiro; o Carrasco também segura sua espada integrada.
As profissões têm seis posições de caminhada, quatro de respiração e quatro de ataque.
Cinco direções de origem geram oito vistas com espelhamento horizontal das três da direita.
O arqueiro tem seis poses de disparo; o Carrasco gira e lança pedras; o Guardião
respira, corre com ombrada, conjura, salta e aterrissa. Comuns/rápidos/tanks
continuam com quatro direções. Viradas preservam a passada: mistura de 0,08 s no protagonista e 0,10 s nos inimigos.

Os dois modelos de baú têm quatro estados de abertura. A abertura é cosmética;
a recompensa é resolvida uma vez pela regra existente. Os 15 tipos de dádiva têm
ícones próprios no menu de tela inteira e na consulta por Tab. Poderes ativos
têm efeitos animados; bônus passivos têm um breve efeito na escolha de nível.

Caronte e protagonista têm retratos ilustrados. Os textos seguem o JSON do M3,
com revelação gradual/instantânea e confirmação por fala. Ainda faltam expressões
adicionais, retratos de outros NPCs e sprites de mundo do Ferreiro/refúgio.

## Colisão e leitura

ExpandedArt e EquippedArt extraem formas de alfa uma vez por quadro; cada entidade
tem posições próprias. Corpos equipados usam alfa no recorte central de
tronco/pernas, excluindo as extensões das armas. Posição/escala/pose coincidem
com o desenho e a orientação predominante durante a mistura.
[Regra do recorte e testes](ANIMACOES_E_EQUIPAMENTOS.md#tempos-avisos-e-colisão).

Comuns/rápidos/tanks usam as células anteriores de 32×32. Os novos corpos
equipados usam 64×64 e têm 1.280 poses, incluindo as espelhadas, verificadas pixel a pixel, além da base
anterior. A navegação continua separada. Avisos de mira, giro, pedras, marcas,
investida e queda usam sprites animados com as áreas e tempos reais do modelo.

Ainda faltam poses de dano/morte e arte dos próximos conteúdos. Revisar o
resultado em partidas completas, principalmente com muitos efeitos simultâneos.

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
- Áudio sintetizado e avaliação sonora: [AUDIO_E_FEEDBACK.md](AUDIO_E_FEEDBACK.md).

As ferramentas Java fazem apenas recorte, ajuste técnico de tamanho e
empacotamento; não desenham os recursos. Os originais ficam fora do pacote.
Reprodução local com o JDK:

```powershell
javac -d build/art-tools tools/EquippedAtlasBaker.java tools/PolishedFrames.java
java -cp build/art-tools EquippedAtlasBaker
java tools/ExpandedAtlasBaker.java
java tools/PilotAtlasBaker.java
java tools/FontBaker.java
```

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
