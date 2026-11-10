# M7 — Hordas periódicas, Morcego do Estige e Guardião

Incremento de **04/11/2026**, após o relato de que o Guardião ainda estava fraco
e os rápidos continuavam proeminentes. O autor pediu um inimigo voador e bandos
homogêneos, com interrupção dos spawns normais antes e depois de sua entrada.
O lote anterior de [baseline e exportação](MARCO_7_BALANCEAMENTO_SUBMUNDO.md)
permanece como registro histórico; os números daquela primeira medição não
descrevem o novo ritmo de geração.

## Ritmo de combate

A primeira horda começa aos **35 s de combate ativo da sala**. As seguintes
começam a cada **40 s**. O ciclo se repete enquanto o jogador continuar na sala;
não existe um orçamento total de inimigos para a fase.

| Fase do primeiro ciclo | Tempo da sala | Novos inimigos |
| --- | --- | --- |
| Geração normal | Antes de 32 s | Ondas normais, com composição da variação da sala |
| Aviso | 32–35 s | Spawns normais interrompidos; aviso breve do tipo do bando |
| Entrada do bando | 35–40 s | Somente novos membros do mesmo papel, pela mesma fronteira |
| Respiro | 40–44 s | Spawns normais continuam interrompidos |
| Geração normal | 44–72 s | Ondas normais retomadas, sem descarregar geração acumulada |

Os inimigos que já estavam vivos **continuam se movendo e atacando** durante
aviso, entrada e respiro. A pausa é na geração normal, não no combate existente.
Pausa do jogo, Tab, escolhas de dádivas e painéis que congelam a tentativa
também congelam o relógio das hordas. O aviso textual não revela posições
nem cria indicadores de inimigos fora da tela.

Os membros entram gradualmente durante os 5 s, em uma formação compacta do
mesmo lado da tela. A formação conserva seu ponto de entrada; não se espalha
pelas outras bordas a cada membro. Posições bloqueadas, que passaram a aparecer
na câmera ou que excedem limites simultâneos são descartadas. Por isso, os
valores são máximos por bando:

| Bando | Quantidade máxima | Grupo da meta |
| --- | ---: | --- |
| Comuns | 18 | Comuns |
| Rápidos | 6 | Rápidos |
| Tanks | 8 | Tanks |
| Morcegos do Estige | 14 | Comuns |

Na campanha, a primeira horda é de morcegos. Depois há ciclos embaralhados dos
quatro papéis, com cada papel uma vez por ciclo e sem repetir o último papel
na fronteira entre ciclos. A demo começa com comuns aos 35 s e introduz
morcegos aos 75 s; tanks continuam indisponíveis antes de 120 s. Os tipos
dos bandos não recebem conversões para arqueiros.

`HordeSchedule` usa tempo ativo absoluto, uma semente estável de tentativa/sala
e poucas decisões por ciclo. Saltos de tempo não acumulam hordas expiradas.
O timer das ondas normais é descartado durante as outras fases; ao retomar,
ele começa a contar novamente. O checkpoint de início de sala recria esse
ritmo, conservando a identificação da tentativa. Não se acrescenta um novo
formato de save ou serialização de inimigos vivos.

## Presença dos rápidos

O limite simultâneo diminui de **24 para 16 rápidos vivos**, mantendo até
**2 por onda normal**. No bando dedicado, entram até 6 ao longo dos 5 s,
respeitando também o limite de 16 vivos. Uma posição do bando barrada pelo
limite expira; não se transforma em outro tipo nem reaparece depois.

| Variação | Comuns antes → agora | Rápidos antes → agora | Tanks antes → agora |
| --- | --- | --- | --- |
| Hordas mistas | 50% → 60% | 35% → 20% | 15% → 20% |
| Caçada veloz | 35% → 45% | 50% → 35% | 15% → 20% |
| Marcha pesada | 40% → 50% | 25% → 15% | 35% → 35% |

Esses são pesos brutos das ondas normais. O limite por onda/vivos pode
redistribuir excedentes de rápidos para comuns/tanks. A composição final
observada também depende dos bandos, das mortes e das conversões de arqueiros.
Todos os grupos das metas continuam possíveis. As três metas do Estige
permanecem **40 comuns, 30 rápidos e 8 tanks**; a mudança de composição pode
mudar qual delas termina por último, o que precisa ser acompanhado nas partidas.

O movimento do rápido mantém o ciclo defasado de 1 s a 70 unidades/s e 1 s a
24 unidades/s. Não há outra redução de vida, dano ou recompensa nesse inimigo.

## Morcego do Estige

É um papel novo dentro da meta de comuns, como o arqueiro, sem uma quarta quota.
Usa corpo, voo e reações próprios; não carrega armas e ataca por contato.

| Atributo base | Valor |
| --- | ---: |
| Vida | 8 |
| Dano de contato | 5 |
| Velocidade de voo | 42 unidades/s |
| XP | 2 |
| Almas base na campanha | 2 |

Vida/dano recebem a escala de tempo/sala, dificuldade e Fúria já existente.
XP e Almas seguem as regras de recompensa da tentativa; a demo não concede
moedas. O voo tem perseguição suave com pequena oscilação lateral. Atravessa
estruturas internas e respeita os limites externos quando a sala os define.
O corpo central visível define a colisão; as pontas das asas são cosméticas.
Não depende da malha de caminhos do inimigo terrestre para atravessar obstáculos.

## Pressão do Guardião

O encontro mantém seus quatro padrões e a segunda fase em 50% da vida.
As alterações de pressão são:

| Regra | Antes | Agora |
| --- | --- | --- |
| Vida base | 500 | 800 |
| Recuperação da fase 1 | 0,95 s | 0,75 s |
| Recuperação da fase 2 | 0,65 s | 0,45 s |
| Marcas simultâneas da fase 2 | 2 | 3 |
| Rajada da fase 2 | 9 projéteis | 11 projéteis |
| Velocidade da rajada da fase 2 | 150 unidades/s | 165 unidades/s |
| Velocidade da investida, fases 1/2 | 200/275 unidades/s | 230/310 unidades/s |

O dano dos padrões conserva suas bases. Os avisos das marcas/investida/rajada
continuam em 0,95/0,75 s por fase. A queda conserva o rastreamento contínuo,
3 s de duração, marcador crescente, último segundo vermelho travado e redução
da caminhada na fase 2, com dash disponível. A sequência continua anunciada;
as janelas menores de recuperação aumentam a pressão entre os avisos.
As regras completas estão em [CHEFE_DO_SUBMUNDO.md](CHEFE_DO_SUBMUNDO.md).

## Arte e reprodução

O morcego foi gerado com a ferramenta integrada `image_gen`, usando o prompt
registrado em [prompt.txt](../tools/art-sources/m7-flyer/prompt.txt). O arquivo
original RGBA está em `tools/art-sources/m7-flyer/flyer-original.png`; não é
um desenho por vetores. O restante do elenco e a abertura atual são preservados.

O prompt pediu 13 colunas por cinco direções. A imagem entregue possui **12
colunas ocupadas**. O processamento conserva seis quadros de voo, três de dano
e quatro de morte por direção, reutilizando o último quadro de dano como a
primeira antecipação da queda. Não foi inventada uma pose para preencher a coluna.
Cinco vistas desenhadas geram oito direções por espelhamento; o voo usa 0,11 s
por quadro e a mesma transição de direção do elenco.

`tools/FlyerAtlasBaker.java` recorta gutters transparentes, registra o corpo
numa raiz estável, usa escala constante/redução por vizinho mais próximo e
empacota os recortes. [manifest.json](../tools/art-sources/m7-flyer/manifest.json)
e [registration.csv](../tools/art-sources/m7-flyer/registration.csv) registram
fonte, hashes, grade real e offsets. O atlas `core/assets/visual/m7-flyer.atlas`
e sua página PNG contêm **65 regiões de 64×64**, em **1024×512**, equivalentes
a 2 MiB RGBA. Os espelhamentos oferecem 104 poses lógicas sem outras páginas.

Reprodução técnica a partir da raiz:

```powershell
java tools/FlyerAtlasBaker.java
```

## Medição, verificação e próxima rodada

`core:balanceBaseline` agora inclui o relógio das hordas, suas interrupções,
a primeira entrada de morcegos e suas recompensas próprias. O CSV acrescenta
`mean_flyers`; mantém tempo até as metas e as medianas de cada grupo. As medições
do [lote anterior](MARCO_7_BALANCEAMENTO_SUBMUNDO.md) são históricas e não devem
ser misturadas com as amostras desse ritmo novo. A hipótese continua sendo
fornecimento com abates instantâneos, não uma partida automatizada completa.

### Primeira medição com os bandos

Resultados de 04/11/2026, com 1.000 sementes por sala/variação. Tempos em
segundos; morcegos e XP são médias por amostra. A XP é o potencial dos inimigos
fornecidos, antes dos modificadores da tentativa, e não a XP coletada por um
jogador. O cenário não representa obstáculos, dano sofrido ou tempo de abate.

| Sala | Variação | p50 até todas as metas | p90 até todas as metas | Morcegos médios | XP base potencial média |
| --- | --- | ---: | ---: | ---: | ---: |
| Estige | Hordas mistas | 142,08 | 166,72 | 21,17 | 391,37 |
| Estige | Caçada veloz | 105,18 | 128,40 | 15,55 | 245,47 |
| Estige | Marcha pesada | 166,72 | 183,15 | 22,96 | 595,99 |
| Cinzas | Hordas mistas | 70,98 | 95,07 | 14,01 | 218,38 |
| Cinzas | Caçada veloz | 49,95 | 61,40 | 13,18 | 135,57 |
| Cinzas | Marcha pesada | 90,22 | 110,08 | 14,60 | 336,49 |
| Sombras | Hordas mistas | 60,42 | 68,10 | 13,90 | 407,28 |
| Sombras | Caçada veloz | 30,42 | 48,63 | 4,68 | 243,99 |
| Sombras | Marcha pesada | 70,13 | 90,64 | 14,00 | 659,61 |

No Estige com Hordas mistas, o piso mediano passa de **94,86 s para 142,08 s**
em relação ao lote anterior. A média passa de 95,73 s para 143,08 s. A meta de
30 rápidos é a última na mediana atual; a de comuns chega em 69,67 s e a de
tanks em 66,92 s. Comuns e tanks continuam chegando durante a espera pelo
tipo restante. A XP base potencial média cresce de **199,85 para 391,37**.

Esses números não provam uma redução da pressão sentida pelo jogador nem
uma duração adequada da sala. O novo limite de rápidos e a variedade visual
resolvem partes diferentes do problema; o fornecimento mais lento da meta de
rápidos pode alongar a travessia e ampliar os ganhos dos outros tipos. A próxima
rodada deve verificar se é possível **reduzir sua proeminência sem prolongar
a sala nem multiplicar a XP de forma indesejada**. Também observar Marcha
pesada e Sombras, cujos excedentes e XP potencial são maiores. Não concluir
que esses valores serão coletados integralmente em combate real.

**534 testes de lógica passaram, sem falhas, erros ou testes ignorados**: base
anterior de 515 mais sete de hordas, seis do voo, dois de formação e quatro de
pressão do Guardião. O baseline com bandos foi gerado em 9.000 amostras.

**Integração OpenGL aprovada**: voo em oito direções com máscaras corporais e
reações próprias; entrada homogênea fora da tela, formação persistente, aviso e
respiro sem spawn acumulado, limites dos rápidos, geração contínua e relógio
congelado por pausa, Tab e vídeo. As verificações antigas de geração imediata
passam a considerar as janelas normais. Marcador: `HORDE_SMOKE_OK`.
Capturas em `build/smoke/m7-flyer-flight`, `m7-horde-warning` e
`m7-horde-flyers`, incluindo versões 800×600 com sufixo `-800`.

**Execução nativa do pacote Windows aprovada** com Java incluído, diretório
externo à instalação, PATH sem Java externo, áudio nativo silenciado e três
modos de exibição. O diagnóstico desenha voo, reação e queda do morcego e
verifica as janelas da horda usando dados em memória.
Marcador: `PACKAGED_FLYING_HORDE_SMOKE_OK`.
A automação verifica regras e recursos; não comprova equilíbrio do encontro.

Nas partidas, observar: distinção/colisão do morcego, leitura do aviso, coesão
do bando, recuperação de espaço sem Hermes, tempo para concluir a última meta,
vida e consumo de dash no Guardião. Exportar os relatórios por **R → E** e anotar
manualmente variação da sala e atributos iniciais. Comparar novos/evoluídos e
três dificuldades sem inferir esses dados dos históricos antigos.

Essa revisão continua dentro do M7; não abre outra região nem encerra seus
playtests. Cutscenes futuras seguem SOTN/Metal Slug; a abertura permanece atual.
