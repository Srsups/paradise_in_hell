# Marco 6 — cenários e escolha de rotas do Submundo

Incremento de **25/10/2026**, após os lotes de
[diálogos](MARCO_6_DIALOGOS_E_NPCS.md) e [dano/morte](MARCO_6_DANO_E_MORTE.md).
Este lote diferencia visualmente os ambientes já existentes, melhora a leitura
das rotas e substitui o marcador geométrico da passagem por uma animação própria.
O M6 continua com acabamento e revisão em partidas; a expansão de regiões e a
conclusão narrativa do Submundo seguem no [roteiro](ROTEIRO_DE_MELHORIAS.md).

## Ambientes e decisões

| Ambiente | Direção visual |
| --- | --- |
| Refúgio do Estige | Pedra de tons quentes e bronze, com espaço limpo para Caronte, altar e travessia |
| Margem do Estige / demo | Pedra fria azulada e marcas discretas de umidade |
| Caminho das Cinzas | Basalto, cinzas e detalhes ocres apagados |
| Pátio das Sombras | Pavimento de tons violetas e musgo discreto |
| Sala do Guardião | Pedra escura, bronze e elementos do templo do Tártaro |

Cada ambiente usa seis pisos e três obstáculos. Três pisos formam a base;
os outros três aparecem em aproximadamente 4% das células, como detalhes planos.
Os blocos sólidos representam a célula de 16×16 que já impede movimento.
As bordas do refúgio e da arena do chefe ficam fora dos limites caminháveis
originais. O cenário é desenhado antes dos personagens, corpos e perigos.
Rótulos do refúgio, instruções e avisos contextuais recebem um contorno escuro
para conservar a leitura tanto sobre pedras claras quanto sobre pisos escuros.

A seleção de rotas apresenta ilustrações de Cinzas, Sombras e dos portões do
Tártaro. Os cartões preservam título, descrição, evento sorteado, recompensa
e metas. As imagens mantêm sua proporção; os textos usam a tipografia e as
molduras compartilhadas. Um único destino fica centralizado.

Clique ou números continuam escolhendo o destino. A entrada mantém a espera
de 0,2 s e exige soltar o botão antes de aceitar um novo clique. Esc abandona
a tentativa pelas mesmas regras anteriores. Consultar uma rota não avança
combate, altera o evento nem concede recompensas.

A passagem liberada usa quatro quadros de uma porta luminosa. Sua posição
continua sendo escolhida entre pontos alcançáveis próximos ao jogador. **E/Enter
continua permitindo avançar de qualquer posição da sala**, depois das metas.
A nova imagem é somente uma indicação visual; não cria um obstáculo.

## Geração e desempenho

[SceneryTheme](../core/src/main/java/br/com/srsups/paradiseinhell/SceneryTheme.java)
seleciona o tema a partir da arena. Demo e arenas de combate desconhecidas usam
Estige; salas de chefe usam o tema do Guardião. Isso não acrescenta campos aos
saves nem altera IDs de região, classe, missão ou dificuldade.

[TileLayout](../core/src/main/java/br/com/srsups/paradiseinhell/TileLayout.java)
reproduz a geração anterior: `Random(seed ^ x*73856093L ^ y*19349663L)`, uma
escolha entre três pisos e depois a chance de obstáculo de 1,2%. A implementação
usa o mesmo gerador de 48 bits, incluindo a rejeição rara de `nextInt(3)`, sem
alocar um Random por consulta. Temas e variantes decorativas usam hashes separados.

[TileMap](../core/src/main/java/br/com/srsups/paradiseinhell/TileMap.java) conserva
as áreas seguras e limites. Consultar colisões não cria tiles visuais. O cache
mantém a janela visível com margem de duas células e teto de 8.192 tiles; sair e
retornar a uma coordenada preserva sua aparência e colisão. O cálculo da janela
considera o zoom da câmera ortográfica. O construtor legado permanece disponível.

[SceneryArt](../core/src/main/java/br/com/srsups/paradiseinhell/SceneryArt.java)
é compartilhado e descartado por PilotArt. Mapas e telas apenas usam suas regiões.
[SceneryVisuals](../core/src/main/java/br/com/srsups/paradiseinhell/SceneryVisuals.java)
desenha as bordas e prepara os recortes das ilustrações. A navegação, XP, ondas,
metas, economia, dificuldade e fluxo de encontros mantêm as regras existentes.

## Arte e reprodução

A arte usa a skill **imagegen**, pela ferramenta integrada **image_gen**.
As nove fontes selecionadas estão em `tools/art-sources/m6-scenery/`: cinco
folhas 3×3 de pisos/blocos, três ilustrações de destino e uma folha de quatro
passagens. Os cinco temas passaram por uma edição para diferenciar as paletas
e dar volume aos blocos. A seleção final usa os arquivos `*-tiles-refined.png`;
os cinco `*-tiles-original.png` permanecem preservados como primeira versão.
As ilustrações de destino e a passagem conservam seus originais aprovados.

| Tema | Folha selecionada | Paleta e leitura |
| --- | --- | --- |
| Refúgio | [refuge-tiles-refined.png](../tools/art-sources/m6-scenery/refuge-tiles-refined.png) | Arenito bege quente, juntas castanhas e bronze discreto |
| Estige | [styx-tiles-refined.png](../tools/art-sources/m6-scenery/styx-tiles-refined.png) | Ardósia azul-petróleo e verde-azulado frio |
| Cinzas | [ashes-tiles-refined.png](../tools/art-sources/m6-scenery/ashes-tiles-refined.png) | Carvão castanho, poeira e cinza ocre apagada |
| Sombras | [shadows-tiles-refined.png](../tools/art-sources/m6-scenery/shadows-tiles-refined.png) | Pedra violeta dessaturada e musgo discreto |
| Guardião | [guardian-tiles-refined.png](../tools/art-sources/m6-scenery/guardian-tiles-refined.png) | Obsidiana castanho-vinho e bronze antigo |

Cada folha mantém pisos planos na primeira e terceira fileiras. A fileira
central traz três blocos maciços, com topo liso, bevel claro e frente espessa
escura. A silhueta quadrada ocupa a célula sólida inteira, sem esconder cantos
de colisão. O [catálogo em 64 e 16 pixels](../build/smoke/m6-scenery-catalog.png)
permite comparar paleta e volume; a [repetição dos pisos](../build/smoke/m6-scenery-tiling.png)
mostra as variantes de base em mosaico.

O [atlas final](../core/assets/visual/m6-scenery.atlas) e sua
[página PNG](../core/assets/visual/m6-scenery.png) têm **52 regiões em
1024×1024**: 45 tiles opacos de 64×64, três prévias opacas de 320×180 e quatro
quadros de passagem de 96×96 com transparência real. A página usa **4 MiB RGBA**
na memória gráfica, compartilhada por todas as telas. Os nomes são
`{tema}-floor-{0..5}`, `{tema}-obstacle-{0..2}`, `route-{ashes,shadows,guardian}`
e `passage-{0..3}`.

Os [prompts iniciais](../tools/art-sources/m6-scenery/prompts.json),
[prompts exatos das cinco edições](../tools/art-sources/m6-scenery/prompts-revisions.json),
[contrato](../tools/art-sources/m6-scenery/contract.json) e
[manifesto com seleção e SHA-256](../tools/art-sources/m6-scenery/manifest.json)
registram as 14 gerações e as nove fontes finais. O
[registro de recortes](../tools/art-sources/m6-scenery/registration.csv) relaciona
cada região à sua origem. A referência do piso anterior foi apenas recortada
tecnicamente; as novas paletas e os blocos foram editados pela ferramenta de imagem.

O [SceneryAtlasBaker](../tools/SceneryAtlasBaker.java) faz somente recorte,
redução com vizinho mais próximo, registro de um pivô comum para os quatro
quadros de passagem e empacotamento. Preserva as cores e o alfa gerados, sem
pintura ou recoloração por código. Os originais e prompts ficam fora do pacote
do jogador. Reprodução local com JDK:

```powershell
javac -d build/art-tools tools/SceneryAtlasBaker.java
java -cp build/art-tools SceneryAtlasBaker
```

Para recriar apenas as imagens de revisão, sem escrever no atlas, use
`java -cp build/art-tools SceneryAtlasBaker --review`. A opção `--original`
seleciona a primeira versão para comparação; a reprodução padrão usa as
cinco folhas refinadas.

## Validação

Os **336 testes de lógica passaram, sem falhas**. Cobrem a equivalência da
geometria procedural anterior, inclusive a rejeição rara do gerador, a seleção
dos temas e variantes, áreas seguras, limites e o cache visual com zoom e revisitas.
**Integração OpenGL e pacote Windows aprovados em 25/10/2026.** A verificação
gráfica carrega as 52 regiões e confere opacidade, proporção das ilustrações,
quatro quadros distintos da passagem e descarte do atlas compartilhado.
Também percorre os cinco temas, pausas, revisitas/zoom, os três eventos possíveis
de cada rota e viagens reais por clique e teclado. A saída continua funcionando
com o jogador longe da porta. Textos e botões não se sobrepõem nos cartões;
um clique mantido ao entrar não escolhe o destino.

Os ataques do Carrasco, marcas, queda do Guardião e mira do arqueiro foram
desenhados sobre cada tema e comparados à imagem sem avisos. As capturas em
960×540 e 800×600 foram inspecionadas; rótulos sobre o piso receberam contorno
após essa revisão. Os testes de ancinho/foice usam uma parede procedural real,
com a semente 34, e verificam bloqueio antes e dano depois de tornar o trecho seguro.

Comandos executados:

```powershell
.\gradlew.bat core:test lwjgl3:smokeTest lwjgl3:packageWinX64 --console=plain --warn
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```

O executável do ZIP foi iniciado de outra pasta, com `JAVA_HOME` removido e
`PATH` sem Java externo. Os diagnósticos usam perfis em memória. O áudio nativo
verificou 24 efeitos e música pelo dispositivo silencioso do OpenAL. O
[pacote Windows](../lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip)
inclui o Java e as [instruções atuais](DEMO_WINDOWS.txt).

### Capturas reais

| Tela | 960×540 | 800×600 |
| --- | --- | --- |
| Refúgio | [imagem](../build/smoke/m6-scenery-refuge.png) | [imagem](../build/smoke/m6-scenery-refuge-800.png) |
| Estige | [imagem](../build/smoke/m6-scenery-styx.png) | [imagem](../build/smoke/m6-scenery-styx-800.png) |
| Cinzas | [imagem](../build/smoke/m6-scenery-ashes.png) | [imagem](../build/smoke/m6-scenery-ashes-800.png) |
| Sombras | [imagem](../build/smoke/m6-scenery-shadows.png) | [imagem](../build/smoke/m6-scenery-shadows-800.png) |
| Arena e investida do Guardião | [imagem](../build/smoke/m6-scenery-guardian.png) | [imagem](../build/smoke/m6-scenery-guardian-800.png) |
| Três destinos | [imagem](../build/smoke/m6-route-three.png) | [imagem](../build/smoke/m6-route-three-800.png) |
| Destino único | [imagem](../build/smoke/m6-route-single.png) | [imagem](../build/smoke/m6-route-single-800.png) |
| Passagem liberada | [imagem](../build/smoke/m6-passage.png) | [imagem](../build/smoke/m6-passage-800.png) |

Avisos sobre os cinco pisos: [Refúgio](../build/smoke/m6-scenery-signals-refuge.png),
[Estige](../build/smoke/m6-scenery-signals-styx.png),
[Cinzas](../build/smoke/m6-scenery-signals-ashes.png),
[Sombras](../build/smoke/m6-scenery-signals-shadows.png) e
[Guardião](../build/smoke/m6-scenery-signals-guardian.png).

A revisão humana deve conferir identificação dos ambientes, leitura de
projéteis/avisos, distinção dos obstáculos e suavidade visual durante uma
travessia completa.
