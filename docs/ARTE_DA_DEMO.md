# Arte da demo

Arquivo utilizado: [core/assets/demo-atlas.png](../core/assets/demo-atlas.png).

O atlas foi gerado para este projeto com a ferramenta integrada **image_gen**,
por meio da habilidade imagegen. Não foi usado o caminho CLI/API.
Os sprites de terceiros anteriores não foram utilizados como referência na geração.

O arquivo gerado tem 1254 x 1254 pixels, com canal alfa. O jogo divide o atlas
proporcionalmente em quatro linhas e quatro colunas, sem depender de um tamanho
fixo por célula. Os sprites são desenhados em dimensões do mundo do jogo;
as bordas finas dos pisos são recortadas no carregamento das regiões.

| Linha | Células da esquerda para a direita |
| --- | --- |
| 1 | Arqueiro: frente A, frente B, costas A, costas B |
| 2 | Arqueiro: direita A, direita B; morto-vivo A, morto-vivo B |
| 3 | Três pisos de pedra; coluna quebrada |
| 4 | Flecha, cristal de XP, escudo, runa de água |

A esquerda do arqueiro é espelhada a partir da direita. As variantes rápida e
resistente do inimigo usam cores/atributos diferentes sobre a mesma animação.
Escudo e aura são representados por formas durante a partida; as respectivas
células ficam disponíveis para futuros indicadores.

## Arte provisória dos novos encontros

O Carrasco das Sombras reutiliza os quadros do inimigo do atlas, ampliados e
tingidos de laranja. Círculo, espada giratória e pedras são formas desenhadas
pelo jogo. O Ferreiro Errante usa um painel com bigorna geométrica; ainda não
tem sprite próprio. Não foram adicionadas imagens externas neste incremento.
O Apostador compartilha o sprite do protagonista; o soco é representado por uma
forma curta na direção da mira. Ainda faltam quadros próprios de luta desarmada.
As raridades das dádivas usam nomes e cores na interface, sem novos arquivos de imagem.
Esses elementos precisam de arte e animação próprias antes da versão final.

## Revisão pendente

Este atlas é uma primeira versão de arte para a demo. Revisar consistência entre
quadros, legibilidade em escala pequena, pisos e silhuetas dos três tipos de inimigo.
Não há promessa de exclusividade nem substituição de uma revisão de direitos.

Os arquivos spritesheet.png, button_spritesheet.png e Foto_BG.jpg foram identificados
pelo autor como placeholders de terceiros. Eles foram preservados localmente,
mas excluídos dos recursos distribuídos pelo core/build.gradle.

Se a arte gerada continuar na versão enviada à Steam, preencher a seção de conteúdo
pré-gerado com IA no [Content Survey](https://partner.steamgames.com/doc/gettingstarted/contentsurvey).

## Prompt usado

```text
Use case: stylized-concept. Asset type: production sprite atlas for a top-down Greek mythology survival game called Paradise in Hell. Create one square 1024 by 1024 PNG atlas with genuine transparent background, divided into an EXACT invisible 4 by 4 uniform grid of 256 by 256 pixel cells. All cells align with the canvas edges. Crisp low-resolution pixel art, strong silhouettes, limited palette, no antialiasing, no text, no labels, no grid lines, no watermark. Each character occupies about 80 percent of its own cell height, centered on exactly the same baseline, never crossing into neighboring cells. Use orthographic overhead three-quarter game sprites, original designs, no copied game characters. Row 1 left to right: a Greek mortal archer in a bronze crested helmet and ivory tunic, front-facing walking pose A; same archer front-facing walking pose B; same archer back-facing walking pose A; same archer back-facing walking pose B. Row 2 left to right: same archer right-facing walking pose A; same archer right-facing walking pose B; an undead Greek soldier front-facing walking pose A with ragged violet tunic and small round shield; same undead soldier front-facing walking pose B. Row 3 left to right: a flat dark slate stone floor seamless tile that COMPLETELY FILLS its 256 pixel cell, no transparency and no perspective; a second seamless slate floor tile same palette COMPLETELY FILLS cell; a third seamless slate floor tile with faint cracks COMPLETELY FILLS cell; a broken Greek marble pillar chunk seen from above centered on transparency. Row 4 left to right: one slender golden arrow pointing straight up centered on transparency; one small glowing cyan experience crystal centered on transparency; a bronze round shield centered on transparency; a cyan circular water rune centered on transparency. Maintain exact 4x4 cell positions so software can crop cells without moving them. Transparent means an actual alpha channel, never a painted checkerboard.
```
