# Piloto da abertura — ilustrações animadas em camadas

Incremento do M7 em **27/10/2026**, implementado e verificado. O autor aprovou
a proposta e pediu um piloto jogável, com Zeus clássico e esposa grega adulta.
A implementação é iterável: testa narrativa, câmera, camadas e leitura; o
acabamento cinematográfico continua evoluindo. A [direção e storyboard](CUTSCENES_E_ABERTURA.md)
registram as escolhas. O nome e a personalidade definitiva da esposa continuam abertos.
O atlas está fechado. **412 testes de lógica passaram sem falhas ou erros;
OpenGL e execução nativa do pacote Windows foram aprovados**.

**Orientação posterior do autor, em 27/10/2026:** conservar esta abertura agora.
Novas cutscenes e revisões futuras seguirão Symphony of the Night e Metal Slug,
com atuação em sprites no cenário, gatilhos e sincronismo por eventos. A
[direção futura](CUTSCENES_E_ABERTURA.md) registra as referências dos vídeos e
o planejamento; este documento descreve a implementação existente do piloto.

## História e entradas

[opening.json](../core/assets/cutscenes/opening.json) contém seis quadros,
totalizando 45 s: `HOME` (6), `ARRIVAL` (7), `ABDUCTION` (7), `CURSE` (9),
`STYX` (7) e `CHARON` (9), com 11 legendas. O cotidiano do casal é interrompido por Zeus;
a esposa recusa o sequestro, o protagonista o confronta e recebe a maldição.
O despertar no Estige apresenta Caronte e a possibilidade de voltar.

A maldição conserva a função já aprovada de explicar os retornos após a morte.
A abertura não define como Zeus poderia revogá-la, não concede a Centelha e
não conclui o resgate. A Centelha permanece vinculada à vitória real do Guardião.
O protagonista da introdução não impõe uma arma nem altera a profissão do save.

| Entrada | Destino e gravação |
| --- | --- |
| I ou ABERTURA no menu | Prévia sem criar personagem; volta ao menu, sem registrar conquista |
| Primeira entrada de campanha com abertura pendente | Depois de confirmar profissão/dificuldade, antes do refúgio; registra conclusão ou pulo |
| Selecionar personagem ainda pendente | Retoma a apresentação desde o começo antes do refúgio |
| I no refúgio | Replay que retorna ao refúgio, sem modificar os registros |
| Demo | Não dispara a história automaticamente |

Main.openOpening recebe um Supplier de destino e a opção de registrar ou não.
Main.enterCampaign verifica a pendência depois de confirmar a criação ou
selecionar o personagem. O destino só é criado ao sair da apresentação.
Assistir pelo menu não escolhe profissão, cria uma tentativa ou entrega recursos.

## Controles e passagem do tempo

| Ação | Controle |
| --- | --- |
| Pausar/continuar | Espaço ou botão Pausar/Continuar |
| Próximo quadro | Enter ou Próximo, após ao menos 1,5 s na cena |
| Pedir para pular | Esc ou Pular |
| Confirmar pulo | Enter ou Pular abertura, após 0,2 s e soltar o mouse |
| Cancelar a pergunta de pulo | Esc ou Continuar vendo |
| Vídeo sobreposto | F2; conserva a cena ao fechar |

Sem entrada, cada quadro avança por sua duração. A linha do tempo usa tempo
de UI, sem avançar combate, dificuldade ou economia; o renderer limita um
quadro de atualização a 0,1 s para não consumir cenas durante arrasto de janela
ou uma longa parada do processo. Pausar, perguntar sobre o pulo e abrir vídeo
congelam a apresentação. Pausa da aplicação/Alt+Tab pausa a cena; Espaço a retoma.

As legendas são completas, com falante e área própria; não exigem dublagem.
Sons são disparados no update, nunca no desenho. Cada cue é liberado uma vez;
pausa não repete eventos, e avanço/pulo limpa cues antigos. Fechar/parar a tela
interrompe os sons vinculados à apresentação.

## Modelo e renderização

[OpeningStory](../core/src/main/java/br/com/srsups/paradiseinhell/OpeningStory.java)
carrega dados imutáveis sem gráficos. O parser valida os campos, tipos,
duplicações, IDs de camada, geometria, alfa, zoom, duração e checkpoints de
legenda/som. O contrato tem `ShotId`, `Layer`, `Subtitle`, `Cue`, `Shot` e `Script`.
As camadas têm `fadeStart`/`fadeEnd` opcionais, normalizados e validados em
0≤início<fim≤1; ausência mantém 0/1. `alphaAt` usa essa janela e suavização,
independentemente do movimento/câmera do quadro. Zeus/portal se materializam
em cerca de 2 s; no Estige o protagonista aparece em 1,75 s. A esposa mantém
opacidade enquanto recusa Zeus e dissolve depois; a queda do protagonista
começa a apagar apenas na segunda metade da maldição.
As durações/ordem do piloto são fixas; `id`, `title`, `shots` e `duration`
ficam disponíveis para a apresentação.

[OpeningPlayback](../core/src/main/java/br/com/srsups/paradiseinhell/OpeningPlayback.java)
é uma linha do tempo pura com quadro atual, tempo local/total, progresso,
legenda, pausa, avanço mínimo, término/pulo e fila pollSound. Não lê teclado,
não carrega texturas e não escreve no perfil.

[OpeningScreen](../core/src/main/java/br/com/srsups/paradiseinhell/OpeningScreen.java)
desenha camadas na ordem do JSON. Interpola posição/alfa e zoom em torno do
centro, com movimento suavizado e respiração ancorada na base do corpo.
Portal usa quatro quadros; fades evitam cortes abruptos. Legendas/controles
usam viewport 960×540 com letterbox. O atlas próprio usa filtro Linear para
acompanhar câmera/respiração e é descartado com a tela; não pertence aos
atlas compartilhados de combate. Fechar durante a apresentação não registra
progresso e libera recursos sem criar o destino.

## Perfil e compatibilidade

CampaignProfile mantém seu formato versão 1 e acrescenta o array opcional
`cinematics`, com registros `{id, state}`. A abertura usa `opening-v1`.
O JSON novo sempre escreve o array, inclusive vazio, para não inferir estados
de filmes pela contagem de tentativas de perfis modernos.

| Estado | Significado |
| --- | --- |
| `UNSEEN` | Abertura ainda pendente; padrão sem registro |
| `COMPLETED` | Primeira apresentação concluída |
| `SKIPPED` | Pulo confirmado na primeira apresentação |
| `LEGACY` | Personagem anterior ao campo, com ao menos uma tentativa; não força exibição nem declara que assistiu |

`openingState()` e `openingPending()` consultam somente esse registro.
Somente ausência do array e tentativas anteriores migram `LEGACY`; um array
explicitamente vazio permanece `UNSEEN`. IDs cinematográficos distintos são
preservados, enquanto entradas duplicadas, estados desconhecidos e tipos
inválidos são rejeitados.

ProfileService.markOpening(characterId, skipped) exige profissão escolhida e
identidade do personagem atual. Só registra quando `UNSEEN`; outros estados
são idempotentes, sem outra escrita. Preview/replay usam recordCompletion=false.
Conclusão/pulo não chama liquidação de tentativa, não muda Óbolos, Almas,
missão, dicas ou os eventos de Caronte/Centelha. Excluir o slot limpa seus
registros e mantém os desbloqueios globais.

Falhas de autosave conservam o registro em memória e mostram o erro no refúgio;
**S no refúgio** repete a gravação. Fechar antes de resolver uma falha pode
perder o que ainda não foi salvo. Fechar uma cena pendente pelo sistema não
chama markOpening: ao voltar ao personagem, a abertura começa novamente.

## Arte e reprodução

As nove fontes foram geradas com a ferramenta integrada **image_gen**.
Os [prompts](../tools/art-sources/opening/prompts.json),
[contrato de recursos](../tools/art-sources/opening/contract.json) e
[referências locais](../tools/art-sources/opening/references.json) já registram
o lote em tools/art-sources/opening. O
[manifesto com seleção e SHA-256](../tools/art-sources/opening/manifest.json) e o
[registro de recortes](../tools/art-sources/opening/registration.csv) relacionam
as fontes ao [atlas final](../core/assets/cutscenes/opening.atlas) e sua
[página PNG](../core/assets/cutscenes/opening.png): **15 regiões em 2048×2048,
16 MiB RGBA**. O atlas está fechado para integração.
Os sete recortes de personagens usam 432×480; fundos/primeiros planos, 960×540;
os quatro quadros de portal, 256×256.
As duas poses de cada ator usam escala uniforme e pivô dos pés em (216,464).
Os cortes usam os intervalos reais entre poses, preservando mãos e acessórios;
as cores e o alfa gerados são mantidos. O
[OpeningAtlasBaker](../tools/OpeningAtlasBaker.java) apenas recorta, registra,
redimensiona tecnicamente e empacota. Não há pintura de personagens por código.

O [catálogo composto para revisão técnica](../build/smoke/opening-art-catalog.png)
combina camadas existentes para comparar identidade/escala. Há também
[recortes dos atores](../build/smoke/opening-actors-catalog.png) e
[fundos/primeiros planos](../build/smoke/opening-layers-catalog.png).
São imagens de QA; as capturas do jogo estão na seção de validação abaixo.

Reprodução técnica do atlas a partir das fontes já geradas:

```powershell
javac -d build/art-tools tools/OpeningAtlasBaker.java
java -cp build/art-tools OpeningAtlasBaker
```

## Validação

**412 testes de lógica passaram, sem falhas ou erros**: base histórica de 389
do vídeo e 23 novos — sete em
[OpeningStoryTest](../core/src/test/java/br/com/srsups/paradiseinhell/OpeningStoryTest.java),
sete em [OpeningPlaybackTest](../core/src/test/java/br/com/srsups/paradiseinhell/OpeningPlaybackTest.java)
e nove em [OpeningPersistenceTest](../core/src/test/java/br/com/srsups/paradiseinhell/OpeningPersistenceTest.java).
Incluem janelas de opacidade e compatibilidade dos campos opcionais.
**OpenGL aprovado**, com DemoSmokeTest encerrado com código zero após o ajuste
de alphaAt. **Execução nativa do pacote Windows aprovada em 27/10/2026**:
`verify_windows_package.ps1` retornou `WINDOWS_PACKAGE_OK`, com Java incluído,
PATH sem Java externo, perfis em memória e áudio OpenAL com dispositivo null.
O diagnóstico do executável carregou/desenhou o atlas, a linha do tempo e as
legendas, emitindo `PACKAGED_OPENING_SMOKE_OK`; não altera os saves reais.

A cobertura inclui parser/cenas, duração e mínimo de leitura, checkpoints,
pausa/cues, pulo, LEGACY, identidade, idempotência, independência de slots,
reset, falha/retry e arquivo preservado. A integração gráfica exercita
o fluxo real por proxy de entrada. Essa cobertura está em
[OpeningSmoke](../lwjgl3/src/test/java/br/com/srsups/paradiseinhell/lwjgl3/OpeningSmoke.java):
entrada por menu/personagem, pausa, mínimo de leitura, pulo/cancelamento,
F2, fechamento sem registro, replay sem ganhos e reprodução natural dos 45 s.
O teste da abertura não usa os atalhos das fixtures antigas que pulam a
introdução para continuar verificando os itens anteriores da campanha.

As seis cenas foram inspecionadas em **960×540 e 800×600**, com legendas/fontes
sem cortes, letterbox proporcional, materialização de Zeus em cerca de 2 s,
protagonista no Estige em 1,75 s e orientação de Caronte coerente com a conversa.

| Cena/estado | 960×540 | 800×600 |
| --- | --- | --- |
| Cotidiano | [captura](../build/smoke/m7-opening-home.png) | [captura](../build/smoke/m7-opening-home-800.png) |
| Chegada de Zeus | [captura](../build/smoke/m7-opening-arrival.png) | [captura](../build/smoke/m7-opening-arrival-800.png) |
| Sequestro | [captura](../build/smoke/m7-opening-abduction.png) | [captura](../build/smoke/m7-opening-abduction-800.png) |
| Maldição | [captura](../build/smoke/m7-opening-curse.png) | [captura](../build/smoke/m7-opening-curse-800.png) |
| Estige | [captura](../build/smoke/m7-opening-styx.png) | [captura](../build/smoke/m7-opening-styx-800.png) |
| Caronte | [captura](../build/smoke/m7-opening-charon.png) | [captura](../build/smoke/m7-opening-charon-800.png) |
| Vídeo sobre a abertura | [captura](../build/smoke/m7-opening-video.png) | [captura](../build/smoke/m7-opening-video-800.png) |

Revisar no piloto ritmo, sobreposição de camadas, recortes e continuidade das
poses, leitura de legendas, áudio e comportamento em diferentes tamanhos de
janela. Os smokes de vídeo continuam com backend em memória; não alteram modos
físicos do monitor. O M7 maior e o aceite artístico humano permanecem abertos.

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :lwjgl3:smokeTest
.\gradlew.bat :lwjgl3:packageWinX64
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```
