# Armas integradas, oito direções e encontros animados

Registro da entrega anterior. A revisão vigente de direções, repouso, alinhamento,
refúgio e métricas está em [POLIMENTO_VISUAL_E_REFUGIO.md](POLIMENTO_VISUAL_E_REFUGIO.md).
As contagens e os tempos abaixo descrevem aquela entrega.

Revisão de **15/10/2026** após o parecer sobre a primeira
[expansão visual](EXPANSAO_VISUAL.md). Esta entrega substitui suas armas em camadas
por personagens equipados e antecipa mais uma parte do M6.

## Resultado

- As oito profissões têm arma desenhada nas mãos em cada quadro, com **oito
  direções próprias**: quatro cardinais e quatro diagonais. São seis quadros de
  caminhada e quatro de ataque por direção. O Guerreiro conserva espada/escudo
  juntos e pode atacar defendendo.
- A passada continua entre viradas; histerese angular evita tremor perto das
  divisões de direção. A transição dura 0,10 s. Durante ataques, a orientação
  acompanha a mira, inclusive nos intervalos entre disparos.
- Os ícones de armas isoladas da expansão anterior aparecem **somente na forja**.
  A tela do Ferreiro Errante mostra a arma e sua raridade, mantendo a compra
  permanente, custo e tratamento de falha de gravação existentes.
- O Arqueiro do Estige tem arco integrado, oito direções e seis poses de
  preparar, puxar a corda, disparar e recuperar. Um contador de disparos reais
  distingue soltura da corda de cancelamento do aviso. Flechas continuam em sprites.
- O Carrasco tem caminhada em oito direções e sequências corporais de giro com
  espada e lançamento de pedras. Mantém perseguição, ataques estacionários e
  sons apenas quando visível.
- O Guardião tem respiração em quatro quadros/oito direções, corrida, ombrada,
  conjuração, impulsão para o salto e aterrissagem. A queda inclui joelho e punho
  apoiados no chão antes de levantar. A pose final da ombrada permanece por
  0,20 s da recuperação existente; isso não acrescenta tempo ao encontro.
- Mira do arqueiro, giro/leque do Carrasco, marcas, trajeto de investida e círculo
  da queda usam sequências de imagens. Os ataques desses encontros não desenham
  linhas, arcos ou círculos por ShapeRenderer. HUD e sinal da saída continuam
  com seus elementos de interface existentes.

Comuns, rápidos e tanks preservam os corpos distintos da expansão anterior, ainda
com quatro direções. Oito direções nesta revisão atendem às profissões, arqueiro,
Carrasco e Guardião. Não foram implementados deslocamentos extras do chefe:
a respiração mantém atividade visual nas pausas seguras de seu padrão.

## Tempos, avisos e colisão

EncounterAnimation consulta o estado e o progresso dos modelos para escolher
poses. A consulta não aplica acertos, move inimigos ou avança relógios.
TacticalVisuals usa centros, raios e trajetos dos mesmos modelos, inclusive o
leque maior da rajada na fase 2. Círculo da queda conserva perseguição contínua,
crescimento, trava vermelha no último segundo e redução de caminhada da fase 2.
A duração e a dificuldade dos ataques permanecem nas regras existentes.

Arma e corpo agora formam **uma imagem**, sem rotação de equipamento solto.
EquippedArt calcula o contato a partir do alfa na faixa central de tronco/pernas:
x de 30% a 70% e y de 16% a 86% do quadro técnico. Usa alfa mínimo 96/255.
Assim, ponta da espada, arco, cajado e margens não passam a alargar o contato.
Cada pose/direção tem forma e posição próprias; durante a mistura, a máscara
acompanha a orientação predominante. A navegação permanece independente.

Os tempos visuais vêm da simulação: pausa, Tab e áudio congelam caminhada,
ataques e respiração. Projéteis, dano, alcance, recargas, moedas, chances e
persistência não foram rebalanceados.

## Recursos e reprodução

**28 imagens de origem** em tools/art-sources/equipped, geradas com a ferramenta
integrada image_gen, pela habilidade imagegen, sem CLI/API. As referências do
autor orientam poses/composição; a aterrissagem usa um guardião grego original.
Os [prompts e grades](../tools/art-sources/equipped/prompts.json) registram a geração.

O [EquippedAtlasBaker.java](../tools/EquippedAtlasBaker.java) faz o preparo técnico
de recorte, escala e empacotamento. Cada sequência conserva escala e origem
comuns, sem reposicionar os pés a cada quadro. Detecta faixas de transparência
entre linhas/colunas para corrigir o espaçamento irregular das folhas geradas,
impedindo fragmentos da linha vizinha nos quadros. Os desenhos vêm dos PNGs gerados.

[Atlas nomeado](../core/assets/visual/equipped.atlas), com
[primeira página](../core/assets/visual/equipped.png) e
[segunda página](../core/assets/visual/equipped-2.png): **1.064 regiões**,
sendo 1.000 poses de corpo e 64 quadros de avisos/impacto.
As páginas têm 2048×2048 e 2048×512, totalizando **20 MiB RGBA sem mipmaps**.
São carregadas uma vez por PilotArt/Main, compartilhadas entre telas e
descartadas no encerramento. Originais ficam fora do pacote distribuído.

```powershell
javac -d build/art-tools tools/EquippedAtlasBaker.java tools/PolishedFrames.java
java -cp build/art-tools EquippedAtlasBaker
.\gradlew.bat core:test lwjgl3:smokeTest lwjgl3:packageWinX64 --console=plain --warn
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```

## Verificação e capturas

**237 testes unitários e integração OpenGL aprovados.** Os testes cobrem oito
direções, continuidade da passada, histerese,
respiração sem deslocamento, corrida/ombrada e salto/aterrissagem sem novos acertos.
A integração gráfica confere as máscaras dos 1.000 quadros, diagonais distintas,
variação da respiração e congelamento com Tab, além das regras anteriores.

O [pacote Windows](../lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip)
foi atualizado e verificado com seu Java incluído, PATH sem Java e diretório de
trabalho externo: **48.700.426 bytes**. As duas páginas e o atlas estão no JAR,
as instruções estão atualizadas e os placeholders antigos permanecem excluídos.
O teste de áudio nativo verifica 24 efeitos e música em streaming via OpenAL nulo.
Perfis de teste são temporários/em memória; saves reais não foram alterados.

O teste de esquiva do arqueiro usa um corredor livre de obstáculos aleatórios
para observar a flecha durante todo o intervalo previsto; as colisões com paredes
continuam verificadas nos testes específicos.

Capturas regeneradas por smokeTest em build/smoke:

- [Oito direções das profissões](../build/smoke/equipped-eight-directions.png).
- [Sequências do Guardião](../build/smoke/equipped-guardian-actions.png).
- [Ataques do arqueiro e Carrasco](../build/smoke/equipped-enemy-actions.png).
- [Corrida](../build/smoke/equipped-boss-run.png),
  [ombrada](../build/smoke/equipped-boss-shoulder.png) e
  [aterrissagem na arena](../build/smoke/equipped-boss-landing.png).

Validar o movimento em partidas completas e em hordas grandes. Ainda faltam
poses de dano/morte, novos NPCs/cenários e arte dos próximos conteúdos de M4/M5.
O próximo marco de gameplay continua sendo novas dádivas, dádiva regional e altar.

