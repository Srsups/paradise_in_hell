# Roteiro de melhorias — revisão dos playtests

Revisado após os testes práticos de **13/10/2026**; M1/M2 implementados em **14/10/2026**.
Este documento orienta os próximos incrementos e substitui a sequência de prioridades
anterior. M1/M2 têm implementação e verificação automática/gráfica; a validação humana
de dificuldade continua. M3 e a expansão visual do elenco atual foram implementados
em **15/10/2026**. Parte do M6 foi antecipada a pedido do autor antes do M4:
[entrega visual e capturas](EXPANSAO_VISUAL.md).
**M4 implementado em 22/10/2026; M5 implementado em 23/10/2026, com 290 testes,
OpenGL e pacote Windows aprovados. Playtests/balanceamento pendentes;
M7 maior em aberto e M6 em andamento, com lote de diálogos de 23/10/2026 aprovado em
302 testes/OpenGL e execução nativa do pacote Windows.**
O [lote de dano/morte](MARCO_6_DANO_E_MORTE.md) também foi concluído em 23/10/2026,
com **320 testes unitários, OpenGL e execução nativa do pacote Windows aprovados**.
O [lote de cenários e rotas](MARCO_6_CENARIOS_E_ROTAS.md), implementado em
**25/10/2026**, passou em **336 testes de lógica, sem falhas, integração OpenGL e
execução nativa do pacote Windows**. O M6 continua em andamento e depende do
aceite visual humano em partidas completas.
Em **27/10/2026**, o [primeiro incremento do M7](MARCO_7_TUTORIAL_E_CENTELHA.md)
implementou dicas contextuais, guia de jogo e desfecho da Centelha da Insurgência.
Foi **verificado em 27/10/2026**: **372 testes de lógica passaram sem falhas**,
com 36 novos (18 de persistência, 11 do guia e sete da história), e **OpenGL foi
aprovado**, assim como a **execução nativa do pacote Windows**. Playtests humanos
continuam pendentes; os 336 testes pertencem ao lote anterior.
O incremento de **vídeo/janela foi implementado e verificado em 27/10/2026**: F2 nas quatro
entradas, presets, tela cheia na resolução atual do monitor, VSync e prévia de
15 s. **389 testes de lógica passaram, sem falhas ou erros** (base 372 + 12 de
vídeo + cinco do adaptador Gdx com Graphics simulado). **OpenGL e execução nativa
Windows aprovados**. Tela cheia física, vários monitores e HiDPI reais continuam
em revisão manual; 372 é a base histórica anterior.
[Regras e limites do incremento](MARCO_7_VIDEO_E_JANELA.md).
O autor aprovou a produção do [piloto da abertura](PILOTO_DA_ABERTURA.md):
ilustrações em camadas, Zeus clássico e esposa grega adulta. Modelo, persistência,
tela e atlas estão implementados. **412 testes de lógica passaram sem falhas ou
erros**, com 23 novos: sete de story, sete de playback e nove de persistência.
**OpenGL e execução nativa do pacote Windows aprovados em 27/10/2026**.
389 é o histórico anterior.

O incremento seguinte de **controles está implementado**:
F3/CONTROLES, duas associações por ação, teclado/mouse, rascunho e preferências
globais com backup/retry. Preserva os painéis anteriores e pausa no refúgio,
arena e chefe. [Contrato e verificações](MARCO_7_CONTROLES.md).
**424 testes de lógica passaram, sem falhas ou erros; OpenGL e execução nativa
Windows aprovados**. Revisão humana dos controles ainda em aberto.
Os 412 testes e aprovações anteriores pertencem à abertura.

Em **28/10/2026**, o incremento de **tamanho do HUD** foi implementado, com
**439 testes de lógica sem falhas ou erros, integração OpenGL e execução nativa
do pacote Windows aprovadas**:
F2 → TAMANHO DO HUD, faixa 80–125%, prévia local,
rascunho e preferência global independente dos personagens e do modo físico
de vídeo. O lote acrescenta 12 testes de preferências e três de layout.
[Escopo, controles e validação](MARCO_7_ESCALA_HUD.md). Os 424 testes e as
aprovações anteriores pertencem ao remapeamento.

Em **28/10/2026**, o incremento de **três modos de exibição passou em 457 testes
de lógica, sem falhas ou erros, integração OpenGL e execução nativa Windows**:
Janela, Janela sem bordas e Tela cheia no monitor atual, com ajuste à área
útil, cobertura do desktop ou modo exclusivo, conforme a opção. Snapshot
nativo preserva posição/flags na reversão de 15 s, e preferências v1 migram
para mode na v2. Os 439 testes acima pertencem ao HUD.
[Contrato, capturas e resultados](MARCO_7_MODOS_DE_EXIBICAO.md).

Em **29/10/2026**, a retomada por checkpoints entre salas e áreas seguras foi
adotada pelo autor. O incremento acrescenta salvamento antes do combate e nas
telas de recompensa, ferreiro, Camponesa e rota, com **Salvar e sair [F5]**.
A escolha do personagem oferece continuação ou abandono confirmado; fechar
durante um combate volta ao último ponto gravado. A tentativa mantém dádivas,
aliados e decisões já sorteadas. O encerramento usa recibo durável para não
recolher moedas novamente após recarga. **495 testes de lógica, integração OpenGL
e execução nativa do pacote Windows aprovados.**
[Contrato e limites](MARCO_7_CHECKPOINTS.md).

## Base existente e objetivo

A campanha já permite atravessar o Estige, escolher Cinzas/Sombras ou o Guardião,
retornar ao refúgio e manter profissão, arma forjada e compras. Há nove profissões,
22 dádivas, seis atributos no altar, três NPCs, Arqueiro do Estige, Carrasco,
Fúria dos Deuses, Almas compartilhadas e relatórios por tentativa.
Atena já foi ajustada para +150% de XP, Épica e uma escolha por tentativa.

A última verificação registrada antes desta revisão teve 201 testes aprovados,
verificação gráfica e pacote Windows validado. Esses resultados verificam regras
e execução. O relato de chefe fácil e interface carregada passa a orientar o
balanceamento e o acabamento.

O próximo grande objetivo continua sendo **uma região completa e bem apresentada**.
Expandir depois para Ascensão ao Olimpo e Olimpo/Zeus, dentro da proposta de
30–40 minutos. Novas regiões não devem multiplicar problemas de leitura,
balanceamento ou produção de arte ainda presentes no Submundo.

## Decisões confirmadas pelo autor

| Pedido | Direção registrada |
| --- | --- |
| Dificuldades | Fácil, Média e Difícil; inimigos mais poderosos e recompensas melhores nos níveis superiores |
| Quando escolher | **Na criação do save**, fixa para aquele personagem |
| Fazendeiro | Profissão especial **Épica**; ancinho em estocada e troca para foice com ataque em 360° |
| Desbloqueio do Fazendeiro | Completar um desafio de NPC que tem chance de aparecer em cada run |
| Dádivas | Ampliar opções e incluir uma dádiva exclusiva de cada andar/região |
| Altar | Mais opções de melhorias permanentes |
| HUD | Apenas informações essenciais; XP em barra, sem contador numérico durante o combate |
| Mini-boss | Sons dos seus ataques apenas enquanto ele aparece na tela |
| Guardião | Aumentar o desafio do encontro |
| Ataques e poderes | Substituir a apresentação por linhas/formas por sprites e animações |
| Tipografia e telas | Nova fonte, menus polidos e conversas com retrato/caixa de diálogo |
| Personagens | Visuais distintos para inimigos e profissões, com mudanças de direção suaves |
| Poder do Submundo — confirmado em 27/10 | Centelha da Insurgência permitirá ferir deuses; agora é conquista narrativa permanente, sem bônus de combate |
| Cutscenes e diálogos futuros — confirmado em 27/10 | Referências Symphony of the Night e Metal Slug: atuação em sprites na sala, retratos, gatilhos espaciais e ações sincronizadas por eventos; geometria 3D texturizada conforme o efeito. Conservar a abertura atual |

Profissão fixa, forja permanente, dádivas temporárias, limite regional de material,
metas independentes de abate e mira pelo mouse continuam sendo regras do jogo.
“Andar” significa uma **região com seu guardião**; Estige, Cinzas e Sombras são
salas do mesmo Submundo.

## Ordem dos marcos

| Marco | Entrega principal | Dependências | Critério para avançar |
| --- | --- | --- | --- |
| M1 — implementado; playtests pendentes | HUD limpo, áudio do Carrasco e Guardião mais exigente | Base atual | 211 testes/OpenGL aprovados; confirmar ritmo/diversão em partidas completas |
| M2 — implementado; playtests pendentes | Dificuldade fixa por save e recompensas ajustadas | M1 como referência de combate | 222 testes/OpenGL; três modos, migração e crédito único verificados; confirmar economia em partidas |
| M3 — implementado e ampliado | Fontes, atlas, animações e conversa com retratos | M1; migração visual gradual | 241 testes/OpenGL após expansão; revisar movimento e leitura na escala real |
| M4 — implementado; playtests pendentes | Seis dádivas gerais novas, Eco do Estige e três compras do altar | M2/M3 | 263 testes/OpenGL; validar builds, dificuldade e economia em partidas |
| M5 — implementado; playtests pendentes | NPC de desafio, missão persistente e Fazendeiro desbloqueável | M2–M4 | 290 testes/OpenGL/Windows aprovados; validar utilidade das ferramentas e ritmo em partidas |
| M6 — em andamento | Elenco/efeitos, diálogos, dano/morte e cenários/rotas implementados | M3–M5 | Cenários: 336 testes/0 falhas, OpenGL/Windows aprovados; revisar o conjunto em partidas |
| M7 — tutorial/Centelha, vídeo, abertura, controles, escala do HUD, três modos e checkpoints implementados | Guia, desfecho, três modos, introdução, remapeamento, tamanho do HUD e retomada entre salas | M1–M6 | 495 testes/0 falhas ou erros, OpenGL e Windows aprovados; revisão humana, monitores reais e playtests antes da expansão |

Cada marco deve gerar um build jogável e um registro curto dos resultados.
Não há estimativa de prazo fechada: medir o trabalho do primeiro pacote de arte
antes de estimar o restante das profissões e inimigos.

## M1 — Leitura do combate e pressão do Guardião

Implementado em 14/10/2026. Regras, números e verificações da entrega:
[MARCO_1_LEITURA_E_CHEFE.md](MARCO_1_LEITURA_E_CHEFE.md).
Os critérios abaixo orientam também os próximos playtests.

### HUD essencial

Durante hordas: barras de vida, estamina e XP, nível pequeno e progresso compacto
das metas. Ao cumprir as metas, substituir a contagem pelo aviso breve de passagem
liberada e um sinal claro no ponto de saída. Durante o chefe, trocar as metas por
barra de vida do chefe e identificação discreta da fase.

Dano, ataques/s, velocidade, sorte, profissão, raridade, moedas, contadores
detalhados, histórico e textos explicativos ficam em Tab, pausa, altar ou resultado.
O tempo pode ficar na pausa/relatório; a demo de cinco minutos precisa de um
cronômetro discreto por ser uma condição de vitória. A barra de XP representa
XPAtual / XPParaPróximoNível, conserva frações internamente e avisa a subida de nível.

Avisos de ataque, Fúria, dano recebido, defesa e interação são contextuais.
Não esconder informação necessária para reagir: a redução de caminhada na queda
e a disponibilidade do dash precisam de sinais legíveis. Ícones/formas e animações
complementam a cor. Manter a proteção de 1,5 s e liberação do ataque nas escolhas.

**Aceite:** identificar vida, recurso de dash, progresso e saída sem ler uma
planilha; barra de XP correta em coleta fracionária, níveis acumulados, pausa,
Tab, troca de sala e nova tentativa; interface legível em diferentes resoluções.

### Carrasco audível somente na tela

Usar a interseção do sprite do mini-boss com a área de jogo realmente mostrada
pela câmera, considerando recorte/viewport. Distância ao jogador não substitui
essa checagem. Parcialmente visível conta como visível; totalmente fora, não.

Não iniciar sons de preparação/ataque fora da tela e interromper a voz de ataque
daquele emissor se ele sair. Não tocar avisos atrasados ao voltar à tela.
Isso exige identificar as vozes do Carrasco, sem silenciar música, jogador,
Guardião ou outros emissores. A simulação e os projéteis continuam normais;
som de dano efetivamente recebido pelo jogador continua sendo feedback do jogador.

A correção já usa câmera, zoom, viewport e a máscara de opacidade do quadro.
BOSS_WARNING recebe o emissor Carrasco; stopEmitter encerra somente sua voz.
Desenho/pausa também revalidam a visibilidade, sem repetir avisos ao voltar.

**Aceite:** testar bordas, zoom/redimensionamento, pausa, retorno à tela,
morte/saída e outros sons simultâneos. O salto do Guardião continua tendo os
avisos necessários, mesmo quando seu corpo está no ar.

### Guardião com mais desafio

Primeiro registrar tempo de luta, vida de entrada, DPS por origem, proporção
das duas fases, padrões executados, dano recebido e esquivas. Comparar personagem
novo e com altar/forja, rotas direta/opcional, com/sem Atena e classes de
curto alcance, distância, escudo e aliados.

Primeiro ajuste aplicado: vida 500, recuperação 0,95/0,65 s e rajada 5/9,
com velocidade 115/150 e dano 12/14; investida 200/275. O primeiro ciclo apresenta
os quatro padrões; blocos seguintes embaralham todos, sem repetição consecutiva.
O relatório mede duração das fases e padrões iniciados, além dos dados da sala. Evitar que uma build comum mate o chefe antes de conhecer
a segunda fase; não resolver o encontro apenas com uma grande reserva de vida.

Conservar a queda com crescimento até 1,5× o chefe, perseguição contínua,
trava vermelha de 1 s e pressão na caminhada da fase 2, preservando o dash.
Não encurtar essa janela para compensar dificuldade. Não sobrepor perigos que
tornem impossível a esquiva ou deixem o Guerreiro permanentemente sem chance de atacar.

**Aceite:** vencer depende de responder aos padrões; as duas fases aparecem em
lutas representativas; uma profissão não exige uma dádiva específica para vencer.
Dificuldade deve vir também de decisões e posicionamento.

## M2 — Dificuldades, saves e economia

Escolher dificuldade junto à criação do personagem e confirmar antes de salvar.
A dificuldade fica no perfil e em um snapshot da tentativa; não muda no refúgio,
durante a run ou ao recarregar. Criar outro personagem permite outra dificuldade.
Personagens antigos recebem **Média** quando o novo campo estiver ausente.
A dificuldade deve aparecer no cartão do save e no relatório, sem ocupar o HUD.

Implementado em 14/10/2026: [MARCO_2_DIFICULDADES_E_ECONOMIA.md](MARCO_2_DIFICULDADES_E_ECONOMIA.md).
**Números iniciais implementados para teste; balanceamento humano pendente:**

| Modo | Vida dos inimigos | Dano dos inimigos | Óbolos/Almas elegíveis da run |
| --- | --- | --- | --- |
| Fácil | ×0,85 | ×0,75 | ×0,85 |
| Média | ×1 | ×1 | ×1 |
| Difícil | ×1,25 | ×1,20 | ×1,25 |

Aplicar vida/dano a comuns, arqueiros, rápidos, tanks, mini-boss e chefe, em
conjunto com a escala de sala/tempo e Fúria. Média usa a base revisada no M1.
Não acelerar inicialmente o movimento dos rápidos nem reduzir os avisos:
força, ritmo e tempo de reação são variáveis diferentes.

Na primeira rodada, recompensar sobretudo com moedas de abates, baús e conclusão,
mantendo a curva de XP separada. Aumentar XP junto à dificuldade pode dar mais
dádivas e anular parte do desafio, especialmente com Atena.
Avaliar depois probabilidades melhores de dádivas/drop, com limites explícitos,
sorte da profissão e teto de um fragmento por região intactos.

Preservar limites de farming, débito do altar, crédito único e exclusão de saves.
Não multiplicar saldo antigo, presente único de Caronte ou preço do Necromante.
Aplicar modificadores fracionários com acumulador da run e conversão definida
para moedas inteiras, evitando arredondar cada abate comum para cima.
Resultados mostram o ganho efetivo; abandonar não concede bônus de conclusão.

**Aceite:** diferenças mensuráveis nas três dificuldades, progressão confortável
no Fácil e melhor recompensa no Difícil; perfil/backup antigos legíveis;
dificuldade imutável; relatórios antigos tratados como Média; nenhuma duplicação
de moedas, desbloqueios ou fragmentos.

## M3 — Base visual e conversa piloto

Piloto implementado em 15/10/2026: [MARCO_3_BASE_VISUAL.md](MARCO_3_BASE_VISUAL.md).
A direção abaixo foi aplicada ao Caçador/comum, efeitos básicos e diálogo de Caronte;
a revisão artística pelo autor continua necessária antes de estender o conjunto.

As duas imagens anexadas são referências de composição: menu com título forte,
botões e molduras consistentes; conversa com retrato lateral, nome e caixa escura
que deixa o cenário visível. Aplicar essa organização à identidade grega e ao humor
do jogo, com arte própria.

Proposta de direção: pixel art de resolução moderada para o mundo e personagens,
silhuetas claras, paleta controlada e animações suaves; retratos ilustrados para
NPCs. Validar esse conjunto em uma cena antes de produzir todos os assets.
Como candidatos de fonte, testar Cinzel em títulos e Noto Sans em textos,
mantendo acentos portugueses, números e legibilidade em escala pequena.
Fontes decorativas ficam em títulos; corpo de diálogo usa leitura confortável.
Arquivos oficiais: [Cinzel](https://github.com/google/fonts/tree/main/ofl/cinzel)
e [Noto Sans](https://github.com/google/fonts/tree/main/ofl/notosans).

Manter Java/libGDX. A produção visual passa a usar imagens nomeadas em atlas,
animações por estado/direção e estilos compartilhados de interface. libGDX já
oferece organização de sprites em [TextureAtlas/TexturePacker](https://libgdx.com/wiki/tools/texture-packer)
e recursos de fonte/moldura/cor em [Skin](https://libgdx.com/wiki/graphics/2d/scene2d/skin).
Para fontes, usar tamanhos pré-gerados ou avaliar
[gdx-freetype](https://libgdx.com/wiki/extensions/gdx-freetype), que gera BitmapFont
a partir de TTF. O piloto adotou PNG/FNT pré-gerados com FontBaker, incluindo licenças OFL,
sem extensão nativa nova. PilotAtlasBaker produz o atlas nomeado; Main controla sua vida útil.

Criar um pacote piloto: Caçador, comum, flecha/impacto, ícones das três barras,
cartão de dádiva e conversa de Caronte. Centralizar carregamento e descarte;
usar descrições de assets/quadros e diálogos em dados. Separar apresentação das
regras de dano, colisão, recompensa e persistência.

Conversa piloto: retrato com expressão, nome, texto com quebra de linha,
opções/avançar e sinal discreto de continuação. O primeiro clique completa
o texto; outro avança, sem consumir duas falas. Permitir texto instantâneo,
preservar escolhas por teclado/mouse e congelar o combate. Diálogos condicionais
consultam eventos salvos e não concedem recompensas duas vezes.

**Aceite:** pacote piloto coerente nas escalas/resoluções testadas, acentos
completos, texto sem cortes e transições suaves. A cena precisa ser revisada
visualmente antes de repetir o estilo para todo o catálogo.

## M4 — Dádivas regionais e altar

Implementado em **22/10/2026**. [Entrega, custos, limites e capturas](MARCO_4_DADIVAS_E_ALTAR.md).
Amparo, Furor, Fôlego, Rastro, Emboscada, Convergência e Eco do Estige compõem o lote.
Reserva, recuperação de estamina e Armadura completam seis compras em duas páginas.
Os critérios abaixo continuam orientando playtests e as regiões futuras.

### Catálogo de dádivas

Primeiro lote implementado: **seis dádivas gerais e uma exclusiva do Submundo**,
levando o catálogo de 15 para 22. Há dois poderes de sobrevivência/risco, dois de
dash/utilidade e dois de ataque/sinergia, além do eco regional. Os testes verificam
função, orçamento de dano, recargas e limites; equilíbrio/diversão exigem partidas.

Implementados: bônus de um alvo no próximo ataque após dash, defesa breve após
dano e recuperação de estamina limitada por recarga ao abater um alvo. Definir duração, recarga, condição,
limite de rank, raridade e fonte de dano; evitar cadeias infinitas de procs.
Comparar os efeitos por DPS/tempo de ativação, para pistola e foice com vários
alvos não dispararem um benefício muito mais vezes que arco ou ancinho.

Regional implementada: **Eco do Estige**, dano em raio 44 na origem do dash após
0,35 s, com recarga de 3 s e 60% do DPS por nível (até 2).
Ascensão ao Olimpo e Olimpo recebem pelo menos uma dádiva exclusiva cada quando
suas regiões forem produzidas; nomes e efeitos permanecem abertos.

Exclusividade indica **onde obter**, não remoção ao sair: somente a região
correspondente sorteia sua dádiva, e o poder adquirido acompanha a run até a morte.
Não sortear opções de regiões ainda não acessadas; mostrar origem e efeito em Tab.
Pesos por raridade, sorte, limites, cura inútil e opções sem duplicatas continuam.
Revisar o peso efetivo de cada efeito ao ampliar categorias.

**Aceite:** ao menos três builds com decisões diferentes, inclusive para a mesma
profissão; todas as armas aproveitam as opções sem benefício obrigatório;
origem regional respeitada na oferta e poder preservado entre andares.

### Novas compras permanentes

Além de vida, dano e movimento, foram implementadas três opções:

| Compra implementada | Papel | Limite inicial |
| --- | --- | --- |
| Reserva de estamina | Maior capacidade de dash/defesa | +5% por nível, até 5; máximo 125 |
| Recuperação de estamina | Recuperar o recurso mais depressa | +8% por nível, até 5; sem regenerar defendendo |
| Armadura | Reduzir dano recebido | +3 pontos percentuais por nível, até 5; máximo 15% |

Custos por nível: reserva 6/11/16/21/26; recuperação 7/12/17/22/27;
Armadura 5/9/13/17/21. O altar mostra atual → próximo e preço em duas páginas;
compras só são aplicadas após gravação. Adiar sorte permanente e aumento
permanente de XP até revisar economia e raridades, para não neutralizar Apostador,
Atena ou o desafio do Difícil. Custos e bases são pessoais; Almas continuam para
desbloqueios compartilhados. Revisar defesa/dash e chefe também com altar máximo.
Armadura substitui a antiga compra de coleta, convertendo seus níveis sem custo.
Soma com a resistência existente até 75% no total; Amparo multiplica o dano restante.
Ímã e Rastro mantêm seus bônus temporários de coleta.

**Aceite:** variedade de preparação, preços alcançáveis sem farming infinito,
reset das dádivas intacto e snapshot correto dos atributos em novas tentativas.

## M5 — NPC de desafio e Fazendeiro Épico

Implementado em **23/10/2026**, com **290 testes unitários, integração OpenGL e
pacote Windows aprovados**. Balanceamento humano ainda pendente.
[Regras e capturas](MARCO_5_FAZENDEIRO.md).

A raridade Épica é da **profissão**, separada da raridade da arma e das dádivas.
O Fazendeiro começa com ferramentas comuns; uma forja permanente melhora ambas.
O Necromante permanece Lendário, por 1.000 Almas.

A **Camponesa das Cinzas** tem **10% de chance base por run**, multiplicada pela
sorte: Apostador 4× tem **40%**. O sorteio é independente e acontece uma única vez
após liberar a passagem do Estige. O encontro opcional ocorre em segurança,
depois de eventuais recompensa e ferreiro, sem avançar o tempo de combate.
Reabrir telas ou mudar de sala não oferece outro sorteio.

Missão: aceitar, obter uma semente garantida ao derrotar um Carrasco e vencer
o Guardião do Submundo. O Carrasco derrotado antes da aceitação não conta.
A semente é um item de missão, independente da chance e do limite do Fragmento
de Forja. Aceitação e semente persistem entre mortes, abandonos e sessões;
não é preciso cumprir tudo na mesma tentativa nem reencontrar a NPC.

O progresso pertence ao personagem; a conclusão libera o Fazendeiro para
**novos personagens de todos os saves**, sem mudar profissões existentes ou moedas.
Aceitar só altera o estado após gravar. Ganhos de missão ficam em memória para
repetir a gravação; um perfil concluído permite recuperar o acesso global após
falha de gravação. Excluir o personagem apaga sua missão incompleta; uma conclusão
pendente precisa ter o desbloqueio preservado antes da exclusão.
J no refúgio abre o diário; Tab mostra o objetivo da missão aceita na tentativa.

Bases iniciais: vida 120, movimento 57, dash 195 e sorte 1×.

| Ferramenta | Dano / intervalo base | Alcance comum / raro | Comportamento |
| --- | --- | --- | --- |
| Ancinho | 9 / 0,70 s | 52 / 64 | Estocada estreita na direção da mira |
| Foice | 6 / 0,85 s | 34 / 44 | Corte circular de 360° |

**Q** alterna ferramentas somente em combate ativo. A recarga é compartilhada:
trocar não concede golpe extra, reinicia efeitos ou duplica bônus. Dano, cadência
e bônus percentuais seguem a ferramenta ativa. A seleção continua entre salas;
uma nova tentativa começa com ancinho. Há nove profissões em três páginas e
oito saves em duas páginas.

Persistência, migração, falhas/retry, exclusão e criação foram verificadas nas
regras e no fluxo gráfico. **Aceite humano pendente:** comparar ancinho/foice
contra grupos, arqueiros, Carrasco e Guardião, em diferentes dificuldades e
com/sem forja; avaliar espera pelo encontro, leitura das animações e vontade de repetir.

## M6 — Arte e animação da travessia inteira

**Lote de diálogos implementado em 23/10/2026**, com **302 testes unitários,
OpenGL e execução nativa do pacote Windows aprovados**.
[Entrega do lote](MARCO_6_DIALOGOS_E_NPCS.md).
Caronte, Ferreiro e Camponesa recebem quatro expressões cada, com retrato definido
por fala/etapa. As três telas compartilham DialoguePanel e leitura gradual/instantânea;
T mantém o texto já revelado ao mudar o modo.

Caronte conserva seus eventos/presente e acrescenta comentários de missão/forja
sem recompensas novas. O Ferreiro responde a profissão, arma, material, raridade e
sucesso/falha da compra: Espaço/clique na fala lê, 1/E compra e 2/Enter/Esc sai.
A Camponesa mantém uma fala por estado, proteção de 1,5 s, soltura do mouse e diário
por J. Os encontros continuam seguros; leitura não avança combate nem altera compras.

**Parte antecipada em 15/10/2026**, por solicitação do autor antes do M4.
Oito profissões com roupa/arma próprias, oito direções e seis quadros de
caminhada; seis papéis de inimigo com quatro quadros/direção, incluindo Carrasco
e Guardião. Há transição de 0,08 s no protagonista e 0,10 s nos inimigos ao virar, preservando a passada e a mira
independente. Máscaras por corpo/quadro acompanham a orientação predominante.
Baús têm quatro estados, o protagonista tem retrato ilustrado e as 15 dádivas
têm ícones próprios em cartões de tela inteira e na consulta por Tab.
[Implementação, origens e verificação](EXPANSAO_VISUAL.md).

A revisão seguinte integrou as armas aos quadros, criou as quatro diagonais,
poses de ataque das oito profissões, disparo do arqueiro, giro/pedras do Carrasco
e respiração/corrida/ombrada/conjuração/salto/aterrissagem do Guardião.
[Estado vigente](POLIMENTO_VISUAL_E_REFUGIO.md). As armas isoladas aparecem na forja.

As oito classes agora respiram; Caronte, altar e travessia do refúgio têm sprites
animadas, e o refúgio bloqueia ataques. Cinco vistas desenhadas geram oito direções,
com alinhamento dos quadros e cadência revista.

O [lote de dano/morte](MARCO_6_DANO_E_MORTE.md) acrescenta três quadros de impacto
e quatro de queda para todos os corpos de combate, com oito direções lógicas.
Corpos derrotados têm duração e quantidade limitadas e não retêm entidades vivas.
Os ganhos são resolvidos no abate; a apresentação final não adia a liquidação.
As 896 poses e suas máscaras passaram na integração gráfica; 320 testes e pacote
Windows também foram aprovados. Outros NPCs continuam futuros.
Revisar também a apresentação dos novos conteúdos do M5. Preservar escala, continuidade da passada
e orientação da mira ao ampliar as folhas. Avaliar as oito direções em partidas.

**Cenários e rotas implementados em 25/10/2026:** cinco temas distinguem o
refúgio, Estige/demo, Cinzas, Sombras e sala do Guardião. Cada tema tem seis pisos
e três obstáculos; três pisos básicos ocupam aproximadamente 96% das células e
três detalhes, 4%. O atlas compartilhado tem 52 regiões em 1024×1024, com 4 MiB
RGBA: 45 tiles, três ilustrações de destino e quatro quadros da passagem.

Os cartões preservam título, descrição, evento, recompensa, metas e destaque
ao passar o mouse. As ilustrações são recortadas mantendo a proporção; um destino
único fica centralizado. A entrada mantém 0,2 s de espera e soltura do mouse.
A porta animada indica um ponto alcançável, sem exigir proximidade: E/Enter
continua avançando de qualquer posição depois das metas. As bordas desenhadas
ficam fora dos limites caminháveis existentes.

TileLayout conserva exatamente a sequência de java.util.Random da colisão
anterior; hashes visuais são separados. Navegação não cria tiles visuais, e o
cache tem teto de 8.192, janela com margem e suporte ao zoom. Os mapas emprestam
o atlas de PilotArt. Economia, RNG de gameplay, saves e IDs não mudam; não foi
acrescentada uma região. **336 testes/0 falhas, OpenGL e execução nativa do pacote
Windows aprovados.** A verificação nativa usa Java incluído, PATH sem Java externo,
áudio OpenAL nativo silenciado e saves em memória.
[Entrega e revisão](MARCO_6_CENARIOS_E_ROTAS.md).

Socos, cortes, feitiços, maldição, aura, escudo, raios, pedras e flechas já usam
sprites/efeitos. Marcas, mira, giro, leque, investida e queda também têm avisos
animados por imagens, usando centros/raios/tempos dos modelos. ShapeRenderer
permanece nas interfaces; a geometria de cálculo segue no movimento e colisão.

Atualizar máscaras para os novos quadros de corpo e manter a navegação separada
de acessórios/brilhos da arma. Efeitos não podem adicionar contato invisível.
Limitar duração/quantidade de partículas e reutilizar recursos; medir tempo de
quadro/memória com hordas, aliados e efeitos simultâneos.

Aplicar a mesma fonte, molduras, ícones e feedback a menu, criação de save,
profissões, altar, diálogos, rotas, pausa, resultado e histórico. Retratos de
Caronte, Ferreiro e Camponesa já têm expressões e estados de conversa neste lote;
validar sua leitura e coerência durante uma travessia completa.
Registrar origem/licença dos assets incorporados e manter revisão humana das
folhas geradas, especialmente consistência entre quadros.

**Aceite:** nenhuma troca de direção brusca ou silhueta ambígua na amostra aprovada;
ataques reconhecíveis com vários efeitos; área desenhada coerente com dano;
fontes/diálogos sem cortes; desempenho comparável à base e recursos liberados
ao trocar de tela. Avaliação humana obrigatória para animação e mistura sonora.

## M7 — Fechar o Submundo e preparar a expansão

O **primeiro incremento foi implementado e verificado em 27/10/2026**:
tutorial contextual com uma dica por vez, guia por F1 e desfecho da primeira
travessia. H controla dicas por personagem; aprendizados e preferência ficam
no save. O guia pausa a simulação e não substitui os avisos de combate.

O autor aprovou a **Centelha da Insurgência**, que permitirá ferir os deuses.
Neste lote ela é uma conquista narrativa permanente por vitória real no
Submundo, sem bônus em combate. Depois da queda do Guardião, Caronte explica
o poder e o protagonista escolhe voltar voluntariamente ao refúgio para preparar
a ascensão. Zeus e o resgate continuam como objetivo futuro.

A leitura tem proteção de 1,5 s/soltura, modo gradual/instantâneo e adiamento por
Esc. V no refúgio e C no resultado retomam/reveem a conversa sem duplicar ganhos.
Concluir a leitura impede sua abertura automática nas próximas vitórias.
Migração e progresso pessoal, inclusive falhas/retry, estão no
[documento do incremento](MARCO_7_TUTORIAL_E_CENTELHA.md).

Ampliar desafios/eventos que mudem escolhas de rota, preservando sua duração:
recompensas distintas, modificadores de combate e encontros narrativos.
Caronte comenta a maldição; Ferreiro reage à arma; o NPC do Fazendeiro conecta
o desafio ao mundo. Externalizar textos e registrar condições/eventos no save.
Desenvolver a aplicação jogável futura da Centelha e a passagem à próxima região,
preservando o nome e a função narrativa já aprovados.

Validar o tutorial em partidas. O incremento de janela foi **implementado e verificado em 27/10/2026**:
janela/tela cheia na resolução atual do monitor, presets de 800×450 a 1920×1080,
VSync (desligado: 120 FPS) e prévia de 15 s com confirmação/reversão. Preferências
globais usam video-settings.json independente dos saves, com backup e retry S
no painel. F2 no menu/refúgio/arena/chefe pausa sem trocar a tentativa.
Os 389 testes de lógica passaram sem falhas ou erros, com OpenGL e execução nativa
Windows aprovados. Testes com backend falso não homologam tela cheia física;
monitores reais, resoluções e HiDPI continuam em revisão manual.
[Detalhes](MARCO_7_VIDEO_E_JANELA.md).
O tamanho do HUD e o remapeamento têm incrementos separados, descritos abaixo.

**Piloto da abertura implementado e verificado em 27/10/2026:** seis quadros/45 s com
ilustrações em camadas, Zeus clássico e esposa grega adulta, conforme escolhas
do autor. I no menu abre prévia sem criar personagem; I no refúgio revê sem
ganhos. Personagens pendentes veem a história após confirmar profissão, antes
do refúgio. Espaço pausa, Enter avança após 1,5 s, Esc pede pulo confirmado e
F2 conserva a cena ao abrir vídeo. Cinematics pessoais distinguem UNSEEN,
COMPLETED, SKIPPED e LEGACY sem alterar fatos de combate. Fechar no meio não
conclui; autosave falho retém o registro para retry S no refúgio.
Os 412 testes de lógica passaram, sem falhas ou erros; OpenGL e a execução
nativa Windows do piloto foram aprovados, com atlas fechado e desenhado pelo
diagnóstico do executável empacotado.
É um piloto para refinamento de movimento, composição, legendas e sons,
sem encerrar o acabamento cinematográfico. [Direção adotada](CUTSCENES_E_ABERTURA.md)
e [implementação do piloto](PILOTO_DA_ABERTURA.md).

**Direção futura registrada em 27/10/2026, sem refazer a abertura:** o autor
escolheu Symphony of the Night e Metal Slug como referências de encenação e
animação. Quando retomarmos cutscenes, prototipar uma sequência curta na sala,
com gatilho por cruzamento, modo CUTSCENE, fala/retrato, gesto e objeto acionados
por eventos, pulo seguro e devolução do controle. Planejar planos/geometria 3D
texturizados para efeitos de perspectiva, com custo medido. Isso é trabalho
futuro; o piloto e o pacote existentes permanecem como entregues.
[Direção, referências dos vídeos e contrato técnico](CUTSCENES_E_ABERTURA.md).

**Remapeamento implementado, com 424 testes, OpenGL e Windows aprovados:** F3 abre controles pelo
menu e sobrepõe o painel no refúgio/arena/chefe, conservando seus estados. Onze
ações têm um ou dois comandos de teclado/mouse, com captura protegida,
conflitos explícitos, rascunho, restauração e salvamento global. Menus e mira
conservam seus controles próprios. Bloquear entradas seguradas até soltar ao
voltar; guardas de escolha continuam consultando o ataque físico.
[Detalhes, capturas e resultados](MARCO_7_CONTROLES.md). O diagnóstico do pacote
desenha a tela de controles com preferências em memória. A revisão humana
dos controles e o restante do M7 permanecem em aberto.

**Tamanho do HUD implementado em 28/10/2026, com 439 testes, OpenGL e Windows aprovados:** F2 abre
uma página separada com prévia local, ajuste de 80% a 125%, presets e rascunho.
Salvar aplica uma preferência global em hud-settings.json com backup/retry.
Esc volta à imagem; F2 fecha. Indicadores do jogador, metas/relógio e barra/
aviso do chefe escalam por âncoras; o aviso fica alinhado à coluna direita.
Mundo, câmera, colisões, menus, diálogos, cutscenes e rodapés mantêm o layout.
[Contrato do incremento](MARCO_7_ESCALA_HUD.md). A validação final cobre todas
as porcentagens, textos e separação de caixas em duas resoluções, rascunho,
persistência e estados pausados. Arrasto é cancelado por F3/pausa da aplicação,
exigindo soltura e proteção de 0,2 s na volta. A execução nativa Windows passou
com Java incluído, perfis em memória e a página do HUD desenhada pelo diagnóstico.

**Checkpoints implementados e verificados em 29/10/2026:** interromper/retomar entre salas e áreas
seguras. Gravar no início das salas e nas telas de recompensa, ferreiro,
Camponesa e rota; F5 salva e volta ao menu sem liquidar a tentativa. A continuação
preserva poderes, aliados, relógios e decisões daquele ponto. Fechar durante o
combate reinicia a sala pelo último checkpoint. Não serializar entidades/telas.
Migração pessoal/compartilhada v1 → v2 e recibos de encerramento impedem duplicar
moedas após recarga; backup terminal não deve reabrir tentativa encerrada.
[Implementação, limites e verificação](MARCO_7_CHECKPOINTS.md).

**Modos de exibição implementados em 28/10/2026, com 457 testes, OpenGL e Windows aprovados:** F2 oferece janela decorada,
janela sem bordas e tela cheia exclusiva, adaptadas ao monitor atual. A janela
cabe na área útil; os outros modos usam a área/resolução do monitor e guardam
o tamanho preferido da janela. Reversão por prazo/Esc/F2/pausa/fechamento usa
snapshot de posição, dimensões, monitor e flags, com fallback seguro quando
necessário. Usar geometria lógica para HiDPI e origens negativas. Preferências
globais migram v1 → v2 sem alterar HUD, controles ou saves. A confirmação
verifica a geometria real, inclusive depois de mover sem bordas com Win+setas.
O diagnóstico opcional do pacote aplica três modos reais numa janela oculta
e restaura o snapshot; a execução passou com PACKAGED_DISPLAY_MODES_SMOKE_OK,
Java incluído e preferências em memória, no monitor atual e sem escolher outra
resolução/taxa. Uso visível/Alt+Tab, vários monitores e HiDPI reais permanecem
em revisão; essa aprovação não encerra o M7 maior.
[Contrato e limites](MARCO_7_MODOS_DE_EXIBICAO.md).

Repetir travessias por profissão/dificuldade e comparar personagem novo/evoluído,
rotas e builds. Testar sessões longas, pausa/Tab/Alt+Tab, minimizar, resoluções,
falhas de gravação, exclusão de personagem e outro hardware Windows.
Conservar a meta anterior de feedback de pelo menos cinco pessoas externas ao projeto.

Somente depois ampliar Ascensão/Olimpo, seus inimigos, dádivas regionais,
chefes e Zeus. Fechar escopo da 1.0, revisão das músicas/sons e materiais de loja.
Preparação Steam e homologação continuam no [plano de lançamento](PLANO_DE_LANCAMENTO.md).

**Aceite:** repetir a região gera decisões novas e mantém objetivos claros;
nenhuma build domina as demais em todas as situações; curva e economia registradas
por dificuldade; produção de nova região reutiliza dados/assets e os mesmos sistemas.

## Próxima entrega concreta e decisões restantes

Concluir a verificação dos checkpoints e então **balancear o Submundo em partidas
completas**. Comparar personagens novos/evoluídos por profissão e dificuldade,
incluindo Fazendeiro/Necromante, rotas diretas/opcionais, tempo até as passagens,
pressão dos rápidos, Guardião e economia. Ajustes precisam registrar o problema
observado e o efeito esperado para a rodada seguinte.

Em seguida, revisar estabilidade e apresentação em sessões longas e hardware
Windows real, ampliar variedade entre tentativas e prototipar o sistema futuro
de cutscenes por gatilhos/eventos conforme SOTN/Metal Slug, preservando a abertura
atual. Só depois ampliar Ascensão/Centelha, Olimpo e Zeus e preparar o lançamento.

**Entrega artística antecipada concluída para o elenco atual**:
[EXPANSAO_VISUAL.md](EXPANSAO_VISUAL.md). Revisar caminhada/viradas, distinção dos
inimigos, armas, poderes, baús e o menu de dádivas numa travessia completa.
Os 241 testes e a integração gráfica verificam regras/recursos; o parecer do
autor orienta o próximo acabamento.

**M4 e M5 implementados e verificados:** jogar o desafio com personagens
novos/evoluídos nas três dificuldades, incluindo desbloqueio e troca de ferramentas.
Revisar as três famílias de builds, o altar e a espera pelo encontro da Camponesa.

O primeiro incremento do M7 passou em 372 testes de lógica/zero falhas, OpenGL
e execução nativa Windows. Partidas continuam pendentes. A revisão humana do M6 segue em paralelo; avaliar
clareza das dicas e do desfecho antes de produzir outras regiões.
Ajustar chance do encontro, bases das ferramentas, dádivas e
custos do altar a partir das partidas; preservar as regras de persistência e
os limites regionais já estabelecidos.
O lote de diálogos de 23/10 passou nos testes de regras, OpenGL e execução nativa
do pacote Windows. As novas poses de dano/morte também foram verificadas.
Os cenários do Submundo e as rotas foram implementados em 25/10, com 336 testes
de lógica/0 falhas, OpenGL e execução nativa do pacote Windows aprovados.
Revisar contraste dos perigos, obstáculos, portais e cartões em uma partida
completa antes de avançar o conteúdo; novas regiões permanecem posteriores ao M7.

Documentos relacionados: [design](DESIGN_DO_JOGO.md),
[balanceamento e saves](BALANCEAMENTO_E_SAVES.md), [arte](ARTE_DA_DEMO.md),
[chefe](CHEFE_DO_SUBMUNDO.md), [classes](CLASSES_SINERGIAS_E_DESTINO.md),
[refúgio](REFUGIO_E_PROGRESSAO.md) e [áudio](AUDIO_E_FEEDBACK.md).

