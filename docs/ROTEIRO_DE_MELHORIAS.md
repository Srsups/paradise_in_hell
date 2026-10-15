# Roteiro de melhorias — revisão dos playtests

Revisado após os testes práticos de **13/10/2026**; M1/M2 implementados em **14/10/2026**.
Este documento orienta os próximos incrementos e substitui a sequência de prioridades
anterior. M1/M2 têm implementação e verificação automática/gráfica; a validação humana
de dificuldade continua. **M3–M7 permanecem planejados**.

## Base existente e objetivo

A campanha já permite atravessar o Estige, escolher Cinzas/Sombras ou o Guardião,
retornar ao refúgio e manter profissão, arma forjada e compras. Há oito profissões,
15 dádivas, três atributos no altar, dois NPCs, Arqueiro do Estige, Carrasco,
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

Profissão fixa, forja permanente, dádivas temporárias, limite regional de material,
metas independentes de abate e mira pelo mouse continuam sendo regras do jogo.
“Andar” significa uma **região com seu guardião**; Estige, Cinzas e Sombras são
salas do mesmo Submundo.

## Ordem dos marcos

| Marco | Entrega principal | Dependências | Critério para avançar |
| --- | --- | --- | --- |
| M1 — implementado; playtests pendentes | HUD limpo, áudio do Carrasco e Guardião mais exigente | Base atual | 211 testes/OpenGL aprovados; confirmar ritmo/diversão em partidas completas |
| M2 — implementado; playtests pendentes | Dificuldade fixa por save e recompensas ajustadas | M1 como referência de combate | 222 testes/OpenGL; três modos, migração e crédito único verificados; confirmar economia em partidas |
| M3 | Base visual: fontes, atlas, animações e conversa de Caronte | M1; migração visual gradual | Uma classe, um inimigo, uma habilidade e um diálogo com qualidade que possa ser repetida |
| M4 | Novas dádivas, primeira dádiva regional e novas compras do altar | M2; ícones/fontes de M3 | Escolhas mudam a forma de jogar e mantêm chefe/economia viáveis |
| M5 | NPC de desafio e Fazendeiro desbloqueável | M2–M4 | Desafio persiste corretamente; duas ferramentas têm utilidade; desbloqueio permite criar novo personagem |
| M6 | Aplicar a direção visual a todas as classes, inimigos, poderes e NPCs | M3; catálogo de M4/M5 | Animações, efeitos, colisões e telas consistentes na travessia inteira |
| M7 | Fechar Submundo, variedade narrativa e preparação da expansão | M1–M6 | Região completa aprovada em playtests; custos de produzir outra região conhecidos |

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
a partir de TTF. Essas fontes/ferramentas ainda não foram incorporadas ao build.

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

### Catálogo de dádivas

Proposta do primeiro lote: **seis dádivas gerais e uma exclusiva do Submundo**,
levando o catálogo inicial de 15 para 22. Não fixar os sete efeitos em código
antes de testar suas funções: dois de sobrevivência/risco, dois de dash/utilidade
e dois de ataque/sinergia, com comportamento além de um aumento plano de atributo.

Candidatos para protótipos: bônus no próximo golpe após dash, defesa breve após
dano e efeito limitado ao abater um alvo. Definir duração, recarga, condição,
limite de rank, raridade e fonte de dano; evitar cadeias infinitas de procs.
Comparar os efeitos por DPS/tempo de ativação, para pistola e foice com vários
alvos não dispararem um benefício muito mais vezes que arco ou ancinho.

Proposta regional do Submundo: **Eco do Estige**, um eco ofensivo breve após dash,
com alcance/recarga próprios e dano proporcional ao DPS da profissão.
O efeito precisa ser distinguível das seis opções gerais antes de fechar o lote.
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

Além de vida, dano e movimento, testar inicialmente três opções:

| Compra proposta | Papel | Limite a definir nos playtests |
| --- | --- | --- |
| Reserva de estamina | Maior capacidade de dash/defesa | Ganho pequeno e finito sobre a base; não remove exaustão |
| Recuperação de estamina | Recuperar o recurso mais depressa | Bônus percentual; não regenera defendendo |
| Alcance inicial de coleta | Mais conforto para pegar cristais | Ganho moderado; Colheita de Deméter continua muito superior |

Definir custos em Óbolos e limites antes do lote. Mostrar efeito atual → próximo
e preço; compras só são aplicadas após gravação. Adiar sorte permanente e aumento
permanente de XP até revisar economia e raridades, para não neutralizar Apostador,
Atena ou o desafio do Difícil. Custos e bases são pessoais; Almas continuam para
desbloqueios compartilhados. Revisar defesa/dash e chefe também com altar máximo.

**Aceite:** variedade de preparação, preços alcançáveis sem farming infinito,
reset das dádivas intacto e snapshot correto dos atributos em novas tentativas.

## M5 — NPC de desafio e Fazendeiro Épico

A raridade Épica é da **profissão**, separada da raridade da arma e das dádivas.
O Fazendeiro mantém ferramentas iniciais comuns e recebe a forja permanente
conforme as regras da classe. O Necromante permanece Lendário, por 1.000 Almas.

NPC proposto: **Camponesa das Cinzas**, possivelmente Deméter disfarçada.
Identidade, chance e missão são propostas: começar testando **10% base por run**,
uma única rolagem registrada na tentativa, com sorte aplicada como nos outros
encontros. Revelar o encontro em um ponto seguro após progresso no Estige,
evitando procurar NPC fora da tela ou reiniciar instantaneamente para vê-lo.
Medir espera real e avaliar proteção contra muitas tentativas sem encontro.

Desafio candidato: aceitar a missão, obter uma semente de missão garantida ao
derrotar um Carrasco e concluir o Submundo. A semente é um item de missão,
não o Fragmento de Forja de 1,5%. A proposta conserva aceitação/item entre mortes;
condições finais e necessidade de uma única run ainda serão discutidas.
A conclusão não depende de reencontrar o NPC no mesmo sorteio: reconhecer o
objetivo cumprido no retorno e registrar a recompensa uma vez.

Proposta de escopo: desbloqueio compartilhado para **novos saves**; missão em
andamento no personagem que aceitou. Excluir esse personagem apaga sua missão
incompleta, mas não desfaz um desbloqueio já concedido. Isso não troca a profissão
de personagens existentes. Chance, condições e escopo do progresso devem ser
confirmados antes de implementar a missão.

| Ferramenta | Comportamento confirmado | Diferenciação proposta |
| --- | --- | --- |
| Ancinho | Estocada dirigida pela mira | Maior alcance e DPS concentrado; faixa estreita, respeita paredes |
| Foice | Corte circular de 360° ao redor | Melhor contra grupos, menor alcance/DPS por alvo e ataque mais lento |

Proposta de controle: **Q** alterna ferramentas, com ícone discreto e informação
completa em Tab. As duas compartilham a recarga de ataque: trocar não concede
golpe gratuito nem reinicia efeitos. Uma execução não acerta o mesmo alvo várias
vezes por quadro. Animação, dano e cadência seguem a ferramenta ativa; vida,
profissão, dificuldade e upgrades continuam do personagem.

A mesma melhoria da forja beneficia ambas; definir os poderes raros e bases
de cada ferramenta depois de comparar grupo, arqueiro, Carrasco e Guardião.
Não permitir que a troca duplique compras, bônus de altar ou dádivas.

**Aceite:** NPC/objetivo legíveis, progresso salvo após morte/reload, recompensa
resistente a falhas de gravação e sem repetição; criação de personagem desbloqueado;
ancinho útil contra alvos prioritários e foice útil nas hordas. Prever paginação
para nove profissões, mantendo os oito slots de save existentes.

## M6 — Arte e animação da travessia inteira

Prioridade: piloto do M3 → rápidos/tanks/arqueiro → Carrasco e Guardião →
demais profissões/Fazendeiro → demais poderes, NPCs e cenários.
As diferenças de silhueta, equipamento e postura devem identificar cada papel;
cores continuam complementares, sem serem a única distinção.

Definir pivô nos pés, escala do corpo, estados de repouso/locomoção/ataque/dano
e ritmo de quadros. Prototipar oito direções ou quatro com transições próprias
antes de decidir o custo de todas as folhas de sprites. Resolver movimento
e mira separadamente para caminhar mirando; evitar tremor nas fronteiras das
direções e reiniciar a caminhada a cada pequena mudança de ângulo.

Substituir visualmente socos, cortes, feitiços, maldição, aura, escudo, raios,
pedras, flechas, marcas e queda por sprites/efeitos animados. Áreas de perigo
continuam nítidas, com preenchimento e borda texturizados e estado de aviso;
decoração não altera o raio real. Vetores/geometria seguem nos cálculos de
movimento e colisão; desenhos de depuração ficam ocultos na apresentação normal.

Atualizar máscaras para os novos quadros de corpo e manter a navegação separada
de acessórios/brilhos da arma. Efeitos não podem adicionar contato invisível.
Limitar duração/quantidade de partículas e reutilizar recursos; medir tempo de
quadro/memória com hordas, aliados e efeitos simultâneos.

Aplicar a mesma fonte, molduras, ícones e feedback a menu, criação de save,
profissões, altar, diálogos, rotas, pausa, resultado e histórico. Retratos de
Caronte, Ferreiro e NPC do desafio precisam de expressões e estados de conversa.
Registrar origem/licença dos assets incorporados e manter revisão humana das
folhas geradas, especialmente consistência entre quadros.

**Aceite:** nenhuma troca de direção brusca ou silhueta ambígua na amostra aprovada;
ataques reconhecíveis com vários efeitos; área desenhada coerente com dano;
fontes/diálogos sem cortes; desempenho comparável à base e recursos liberados
ao trocar de tela. Avaliação humana obrigatória para animação e mistura sonora.

## M7 — Fechar o Submundo e preparar a expansão

Ampliar desafios/eventos que mudem escolhas de rota, preservando sua duração:
recompensas distintas, modificadores de combate e encontros narrativos.
Caronte comenta a maldição; Ferreiro reage à arma; o NPC do Fazendeiro conecta
o desafio ao mundo. Externalizar textos e registrar condições/eventos no save.
Fechar a função do poder obtido no Submundo e a passagem narrativa à próxima região.

Concluir tutorial contextual, opções de vídeo/escala e remapeamento de comandos.
Definir quando será permitido interromper/retomar uma run antes de implementar
esse salvamento. Manter backup e migração dos perfis, preferências e histórico.

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

**Próximo incremento de código: M3**, base visual e conversa piloto.
M1/M2 já permitem novas partidas com HUD limpo, chefe revisado e dificuldade
por personagem; usar os relatórios para confirmar ritmo e economia dos três modos.

Antes de implementar os respectivos marcos, confirmar: estilo visual do piloto/fontes;
chance/identidade do NPC;
condições e persistência do desafio; alcance/dano/forja das duas ferramentas;
efeitos das novas dádivas e custos/limites do altar. São decisões localizadas,
não impedem medir as partidas do M1/M2 e preparar o M3.

Documentos relacionados: [design](DESIGN_DO_JOGO.md),
[balanceamento e saves](BALANCEAMENTO_E_SAVES.md), [arte](ARTE_DA_DEMO.md),
[chefe](CHEFE_DO_SUBMUNDO.md), [classes](CLASSES_SINERGIAS_E_DESTINO.md),
[refúgio](REFUGIO_E_PROGRESSAO.md) e [áudio](AUDIO_E_FEEDBACK.md).

