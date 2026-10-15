# Sorte, socos e raridades das dádivas

Implementado no protótipo em 09/10/2026. As regras de limite regional, socos e
categorias vêm do autor. Nome da profissão, multiplicador, atributos, classificação
dos efeitos e pesos são valores iniciais para playtests.

## Profissão: Apostador

O narrador pergunta a profissão ao criar um personagem. O Apostador luta somente
com os punhos, mirando com o mouse e segurando o botão esquerdo, como as outras
profissões. A escolha continua fixa no save. Há oito slots em duas páginas; os quatro originais mantêm seus arquivos.
O Necromante exige 1.000 Almas compartilhadas no altar, sem trocar a profissão atual.
Almas têm recompensa determinística e não recebem multiplicador de sorte.

| Atributo | Valor base |
| --- | --- |
| Vida | 110 |
| Dano por soco | 4 |
| Intervalo de ataque | 0,30 s |
| Velocidade / dash | 62 / 205 |
| Alcance / cone | 24 unidades / 80° |
| Alvos por golpe | Um: o mais próximo dentro do cone, com caminho livre |
| Sorte | 4×; as outras profissões têm 1× |

Socos atingem inimigos comuns, mini-boss e chefe. Não geram projéteis. Dádivas
e compras de atributos mantêm seus bônus percentuais sobre a base da profissão.
A animação usa uma forma curta na direção da mira; arte própria ainda está pendente.

Um Fragmento de Forja fortalece os golpes permanentemente no save: +25% de dano base,
+10% de taxa de ataque e alcance 34, mantendo um alvo por soco. O jogador
continua desarmado. Raridade e sorte da profissão permanecem após morte e entre
runs; dádivas e XP reiniciam. A demo isolada usa os atributos iniciais.

## Sorte em drops e eventos

| Sorteio | Profissões originais | Apostador |
| --- | --- | --- |
| Fragmento por mini-boss elegível | 1,5% | 6% |
| Ferreiro Errante por tentativa | 3% | 12% |

A chance é `min(100%, chanceBase × sorte)`. O ferreiro é sorteado uma vez ao
iniciar a tentativa; se selecionado, aparece após a primeira sala concluída.
Sorte não aumenta o limite de material. XP por abate, fontes garantidas de Óbolos
e os quatro prêmios de baús mantêm as regras existentes.

## Um fragmento por região por save

O limite considera todas as runs daquele personagem. Por exemplo:

1. Nas duas primeiras runs no Submundo, os mini-bosses não dropam material.
2. Na terceira run, um mini-boss concede o primeiro Fragmento de Forja.
3. O save registra o estoque e que o Submundo já concedeu seu fragmento.
4. Gastar, morrer, abandonar, trocar de sala, reiniciar ou recarregar o jogo
   não libera outro fragmento do Submundo.
5. Derrotar seu guardião e avançar para uma região diferente dará acesso ao
   limite daquela outra região.

O registro é por ID estável da região, independente do estoque. Derrotar o
mesmo guardião novamente também não reabre esse limite. Cinzas e Sombras são
salas do mesmo Submundo e compartilham o limite. Outros saves têm registros próprios.
Um sorteio sem drop não consome o direito; o mini-boss continua concedendo XP
mesmo quando a região já foi premiada.

**O protótipo atual contém somente o Submundo.** Derrotar o guardião ainda encerra
o trecho jogável; a travessia para outra região será implementada em um marco futuro.

O perfil versão 1 mantém `forgeMaterials` e recebe o campo opcional
`claimedMaterialRegions`. Saves antigos com estoque e sem esse campo reservam
o Submundo e preservam todos os fragmentos existentes. Um drop já gasto em um
save antigo com estoque zero não pode ser reconstruído sem registro histórico.
Perfis novos guardam o registro mesmo após gastar. Falha de autosave aparece
na interface e permite repetir com S no refúgio.

## Categorias das dádivas

A categoria indica a força do efeito. Cada dádiva tem uma categoria fixa;
a categoria e a quantidade de vezes obtida são informações separadas.
Em 13/10/2026, Sabedoria de Atena mudou de Rara para Épica: +150% de XP
coletada e uma escolha por tentativa, substituindo +20% em até três escolhas.

| Categoria | Dádivas atuais |
| --- | --- |
| Comum | Força de Hércules, Celeridade de Ártemis, Saúde do Minotauro, Sandálias de Hermes, Cura de Apolo |
| Rara | Resistência de Ares, Colheita de Deméter, Favor de Hades, Faísca de Zeus, Brasa de Héstia, Martelo de Hefesto |
| Épica | Sabedoria de Atena, Égide, Correnteza de Poseidon |
| Mitológica | Favor de Asclépio: uma ressurreição com metade da vida |

A tela mostra o nome e a cor da categoria. Ao subir de nível, sorteia até três
dádivas distintas: primeiro a categoria, depois um efeito elegível dentro dela.
Efeitos no limite de repetição e cura com vida cheia saem do conjunto. Categorias
sem efeitos disponíveis deixam de participar, e os pesos restantes são normalizados.

| Categoria | Peso base | Chance base | Peso com sorte 4× | Chance do Apostador |
| --- | --- | --- | --- | --- |
| Comum | 70 | 70% | 70 | 21,7% |
| Rara | 23 | 23% | 92 | 28,6% |
| Épica | 6 | 6% | 96 | 29,8% |
| Mitológica | 1 | 1% | 64 | 19,9% |

As chances da tabela são do **primeiro sorteio**, com todas as categorias
disponíveis. Não são a chance total de ver a categoria entre as três opções.
Cada escolha remove o efeito sorteado; isso pode esgotar uma categoria e mudar
as chances das próximas opções. Peso final = pesoBase × sorte^índice,
com índices 0, 1, 2 e 3, respectivamente. Esse aumento favorece os poderes
mais raros de forma acentuada no Apostador e deve ser ajustado com partidas completas.

Sabedoria de Atena ocupa uma das três opções Épicas do catálogo. No primeiro
cartão, com catálogo inicial e sorte 1, a chance é 6% / 3 = 2%; antes era
23% / 7 ≈ 3,3%. Para o Apostador, é (96 / 322) / 3 ≈ 9,9%:
a sorte continua favorecendo os efeitos mais fortes, mesmo após a mudança de categoria.
Esses valores mudam conforme opções saem do conjunto. Depois de escolhida, Atena
deixa de participar pelo resto da tentativa. O bônus afeta apenas XP futura,
não moedas. Matemática e cenários: [BALANCEAMENTO_E_SAVES.md](BALANCEAMENTO_E_SAVES.md#sabedoria-de-atena-e-a-curva-de-xp).

## Catálogo regional e profissão Épica planejados

O M4 do [roteiro](ROTEIRO_DE_MELHORIAS.md) propõe seis novas dádivas gerais e
uma exclusiva do Submundo no primeiro lote. Cada região futura terá sua própria
opção. A restrição define onde sortear; um poder já obtido acompanha a tentativa
nas regiões seguintes. Sorte, limites de rank e ausência de duplicatas permanecem.

Ao ampliar o catálogo, recalcular a chance de cada efeito dentro de sua categoria,
inclusive Atena: os percentuais das categorias não são a chance de cada dádiva.
As tabelas acima continuam descrevendo as 15 opções atualmente implementadas.
Revisar oportunidades reais de escolha e combinações em partidas, além da distribuição.

Épica será também uma classificação do Fazendeiro, obtido por desafio de NPC;
classificação da profissão, categoria da dádiva e raridade da arma são separadas.
Chance do encontro, influência da sorte e condições da missão ainda são propostas
do M5. O desbloqueio não requer o drop raro de Fragmento de Forja.

## Verificação

Os testes da suíte incluem limites exatos de sorteios, falhas de drop antes
do primeiro sucesso, registro regional depois de gasto/morte/reinício/recarga,
saves independentes, migração de perfis, quarto slot, geometria dos socos,
categorias fixas, limites e ausência de opções duplicadas. Amostras reproduzíveis
de 30.000 sorteios por profissão verificam a distribuição inicial das categorias.

O teste com OpenGL real cria o quarto personagem, ataca com socos sem projéteis,
verifica um alvo por ataque, opções com raridade, forja dos punhos, socos contra
o chefe, preservação da forja e bloqueio regional em outra run/sala após gastar
o fragmento. Também percorre os eventos e recompensas próprias das rotas.
Capturas ficam em `build/smoke/`. Essas verificações não substituem playtests
de dificuldade, ritmo e valor dos recursos.

Rotas: [ROTAS_DO_SUBMUNDO.md](ROTAS_DO_SUBMUNDO.md).
Bases gerais: [BALANCEAMENTO_E_SAVES.md](BALANCEAMENTO_E_SAVES.md).
Mini-boss e forja: [FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md).

Novas classes, recargas e combinações elementais: [CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md).
