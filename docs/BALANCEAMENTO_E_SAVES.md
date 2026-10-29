# Atributos, dificuldade e saves

Revisão dos atributos/XP em 13/10/2026 e incorporação do parecer dos playtests.
Os números implementados abaixo são provisórios; testes automáticos verificam
regras, sem comprovar equilíbrio ou diversão.

## Marco 1 implementado e próximos incrementos

O [roteiro de melhorias](ROTEIRO_DE_MELHORIAS.md) define a sequência M1–M7.
Implementado em 14/10/2026: HUD essencial com XP em barra, áudio do Carrasco
apenas quando visível e Guardião mais exigente. Dano/ataques/s e detalhes ficam
em Tab; frações de XP permanecem no estado. Barra do dash e mensagem de fôlego
comunicam disponibilidade. Metas concluídas dão lugar ao acesso à passagem.
Regras e verificações: [MARCO_1_LEITURA_E_CHEFE.md](MARCO_1_LEITURA_E_CHEFE.md).

O **M2 está implementado**: Fácil, Média e Difícil são confirmadas junto à profissão
antes de criar o save. Cancelar não cria personagem; falha de gravação mantém a seleção.
Saves e relatórios antigos sem dificuldade assumem Média. A dificuldade fica fixa
e aparece nos cartões e relatórios.

| Modo | Vida dos inimigos | Dano | Óbolos/Almas da tentativa |
| --- | --- | --- | --- |
| Fácil | ×0,85 | ×0,75 | ×0,85 |
| Média | ×1 | ×1 | ×1 |
| Difícil | ×1,25 | ×1,20 | ×1,25 |

Os valores abaixo são **bases de Média**, compostos com tempo/sala e Fúria.
As moedas acumulam frações durante a tentativa e descartam apenas o resto
ao converter o total para inteiros. XP e probabilidades mantêm suas regras;
Caronte, saldos antigos e custos não são multiplicados.
Detalhes e validação: [MARCO_2_DIFICULDADES_E_ECONOMIA.md](MARCO_2_DIFICULDADES_E_ECONOMIA.md).

O **M4 foi implementado em 22/10/2026**, com seis novas dádivas gerais,
Eco do Estige regional e três compras do altar. [Tabela completa](MARCO_4_DADIVAS_E_ALTAR.md).
O **M5 foi implementado em 23/10/2026**: missão persistente da Camponesa das Cinzas
e Fazendeiro Épico com duas ferramentas. Os 290 testes unitários, a verificação
OpenGL e o pacote Windows foram aprovados; o balanceamento em partidas continua.
[Regras do marco](MARCO_5_FAZENDEIRO.md).
Comparar classes/builds em personagens novos e evoluídos, com/sem Atena e forja,
nas rotas direta/opcionais e nas três dificuldades.
As seções seguintes descrevem as regras já implementadas.

## Base visual e expansão do elenco atual

Fontes e diálogo do M3 foram ampliados em 15/10/2026 com os visuais das oito
profissões, seis papéis de inimigo, armas, baús, poderes e retrato do protagonista.
Movimento separado da mira; viradas do protagonista misturam quadros por 0,08 s (inimigos: 0,10 s) e o contato usa
o quadro predominante. As profissões, arqueiro e encontros têm oito direções
com armas integradas. EquippedArt usa alfa do recorte central de tronco/pernas
nos quadros de 64×64; comuns/rápidos/tanks conservam as formas anteriores.
[Revisão visual vigente](POLIMENTO_VISUAL_E_REFUGIO.md).
Repouso das oito classes usa quatro quadros de respiração. A caminhada passa a
0,135 s/quadro; o tempo visual do ataque não altera ataques/s, dano ou recarga.
O refúgio permite caminhar e interagir, com combate desativado.
As máscaras são específicas de cada pose;
acessórios/brilhos ficam fora da colisão. Cartões de dádiva cobrem toda a tela
e usam ícones próprios, também presentes em Tab. Espera de 1,5 s preservada.
Dano, XP, moedas, probabilidades e multiplicadores continuam nos modelos de M2.
[Detalhes e capturas](EXPANSAO_VISUAL.md).
T alterna texto instantâneo no diálogo; confirmar uma fala não concede moeda.
Detalhes: [MARCO_3_BASE_VISUAL.md](MARCO_3_BASE_VISUAL.md).

## Profissão fixa por personagem

O menu oferece oito saves em duas páginas (setas ou Anterior/Próxima). As nove
profissões usam três páginas. Em um save vazio, o narrador pergunta a profissão.
A tela seguinte confirma profissão e dificuldade juntas; o personagem mantém ambas entre tentativas e sessões.
Para experimentar outra, crie outro personagem em um save vazio.
Na seleção de saves, **Excluir** abre uma confirmação para esvaziar o slot.
**Cancelar/Esc** preserva o personagem; **Excluir personagem/Enter** confirma.
O botão **DEL** alterna um modo de exclusão por teclado: 1–4 escolhem o slot da
página atual e também abrem a confirmação. Slots vazios não precisam de exclusão.
A página fica congelada enquanto a confirmação está aberta.

Excluir remove profissão, Óbolos, forja, compras, eventos, fragmentos, limites
regionais, missão incompleta e estatísticas pessoais. Almas e acessos ao Necromante
e Fazendeiro continuam globais. Uma missão concluída ainda não gravada globalmente
precisa ter seu desbloqueio preservado antes de excluir o personagem.
A exclusão só muda o personagem em memória após gravar o perfil vazio; backups
anteriores são invalidados para evitar recuperar o personagem excluído.
Falhas são mostradas e preservam o personagem. O slot pode ser usado novamente
para outra profissão. R consulta somente tentativas do personagem atual, mesmo
quando o slot foi reutilizado. Relatórios anteriores podem permanecer no histórico
global limitado, sem serem apresentados ao novo personagem.

Os cartões de profissão mostram o nome sem prefixos numéricos como [1]/[2].
Clique para escolher; os atalhos existentes continuam funcionando.

No Windows, os arquivos ficam em `%LOCALAPPDATA%\ParadiseInHell\`:

| Save | Arquivo | Cópia anterior |
| --- | --- | --- |
| 1 | profile.json — preserva o perfil anterior | profile.json.bak |
| 2 | profile-2.json | profile-2.json.bak |
| 3 | profile-3.json | profile-3.json.bak |
| 4 | profile-4.json | profile-4.json.bak |
| 5–8 | profile-5.json até profile-8.json | Cada arquivo com sufixo .bak |

Saldo, compras, eventos e estatísticas são independentes. A demo usa a profissão
do save escolhido, com seus atributos base, sem compras ou recompensas da campanha.
Selecionar um personagem pela primeira vez também salva sua profissão na demo.

## Retomada da campanha por checkpoints

Decisão confirmada pelo autor em **29/10/2026**: guardar a tentativa entre salas
e áreas seguras. O checkpoint é gravado antes de entrar em uma sala e ao chegar
à recompensa, ao Ferreiro, à Camponesa e à escolha de rota. **Salvar e sair [F5]**
nessas telas volta ao menu sem morte, abandono, crédito de moedas ou novo retorno.
A demo de cinco minutos conserva seu fluxo próprio e não sobrescreve essa tentativa.

O cartão do personagem indica uma tentativa disponível. Selecioná-lo abre a
retomada: **Enter** continua, **Esc** volta ao menu e **Del** pede confirmação
para abandonar. Abandonar recolhe os ganhos do checkpoint e limpa as dádivas da
tentativa; atributos permanentes, arma, missão e desbloqueios permanecem.
Não se pode iniciar outra tentativa por cima de um checkpoint ativo.

Fechar durante um combate retorna ao último ponto gravado. A sala começa de
novo, com suas entidades recriadas; não se recuperam posição, projéteis ou
inimigos no instante do fechamento. O snapshot conserva vida, estamina, nível/XP,
dádivas e recargas, ferramenta ativa, ressurreição consumida, aliados, tempo e
estatísticas, frações de recompensa, variantes e escolhas/sorteios já decididos.
As melhorias permanentes aplicadas no começo da tentativa não são somadas outra vez.

Forja e aceitação da missão atualizam o perfil e o checkpoint no mesmo commit.
Semente e limite regional do material permanecem fatos pessoais já conquistados,
inclusive quando o combate volta ao checkpoint. O limite de um fragmento por
região/personagem continua valendo entre tentativas e retomadas.

Perfis e progresso compartilhado usam formato **2**, lendo os arquivos de
formato **1**. O checkpoint tem sua própria versão e identifica personagem,
tentativa, sequência e conteúdo da região. Conteúdo incompatível bloqueia a
continuação e oferece descarte confirmado somente da tentativa.
Morte, vitória e abandono gravam um recibo pessoal e depois o crédito compartilhado;
um registro durável de sequências consumidas impede créditos repetidos após recarga,
mesmo fora do histórico visual de 20 relatórios. Falhas pausam as transições ou
deixam a liquidação pendente para retry, antes de iniciar outra tentativa ou excluir o save.

Implementação e validação final: [MARCO_7_CHECKPOINTS.md](MARCO_7_CHECKPOINTS.md).
Este incremento não altera bases de atributos, custos, probabilidades ou a curva
de dificuldade. O próximo trabalho continua sendo jogar o Submundo completo e
comparar classes/rotas/dificuldades antes de ampliar a campanha.

## Moedas e desbloqueios compartilhados

O desbloqueio do Necromante custa **1.000 Almas compartilhadas** no altar.
Almas são somadas entre os oito saves e gastas uma vez para liberar novos personagens.
Óbolos, arma, compras e fragmentos continuam independentes. Ganhos por derrota:
comum 1, arqueiro 3, rápido 2, tank 5, mini-boss 20 e chefe 100, com redução por Fúria para
inimigos. São guardados após morte, vitória ou abandono; a demo não concede moedas.
Desbloqueios antigos são preservados. Regras: [ALMAS_E_RELATORIOS.md](ALMAS_E_RELATORIOS.md).

A Camponesa das Cinzas tem 10% de chance base por tentativa, multiplicada pela
sorte: 40% para o Apostador. O sorteio é único e independente, após liberar a
passagem do Estige; o encontro opcional ocorre em segurança, depois de eventuais
recompensa e ferreiro. Não avança o relógio de combate.
Aceite a missão, derrote um Carrasco para obter uma semente garantida e vença o
Guardião do Submundo. A semente independe do Fragmento de Forja e de seu limite.
Aceitação/semente persistem no personagem após morte, abandono e recarga; a
conclusão libera o Fazendeiro para novos personagens, sem gastar moedas.
O perfil concluído permite recuperar o desbloqueio após falha no arquivo global.
J abre o diário no refúgio; Tab mostra o objetivo aceito durante a tentativa.

## Preferências e leitura do combate

**O** abre volumes de efeitos/música e silenciar no menu, refúgio e arenas.
Preferências usam audio-settings.json e não pertencem a um personagem.
A exclusão de saves não apaga esses valores. O painel congela combate, fúria,
recargas, aliados e a espera das dádivas, preservando a pausa anterior.
Os sinais de dano, bloqueio e proteção vêm do resultado efetivo do combate;
sons de impacto exigem vida retirada. Efeitos são cosméticos, sem mudar hitboxes
ou números de balanceamento. Música/sons são provisórios e exigem avaliação.
Origem e verificações: [AUDIO_E_FEEDBACK.md](AUDIO_E_FEEDBACK.md).

## Resumo para playtests

R no resultado/refúgio consulta duração por sala, vida de entrada/saída, metas,
dádivas e níveis, dano efetivo por origem, causa da morte, ressurreições e moedas.
Setas laterais trocam páginas e acima/baixo percorrem tentativas do personagem.
O arquivo compartilhado conserva as 20 últimas tentativas de todos os saves.

Bloqueios, invulnerabilidade e overkill não entram no dano. Os dados permitem
comparar classes, rotas e construções; não comprovam diversão automaticamente.
Testar tentativas completas, avaliando também legibilidade, ritmo e vontade de repetir.

## Bases e bônus percentuais

| Profissão | Vida | Dano | Intervalo de ataque | Velocidade | Velocidade do dash |
| --- | --- | --- | --- | --- | --- |
| Caçador / arco | 100 | 5 | 0,65 s | 60 | 200 |
| Ferreiro / espada | 130 | 8 | 0,60 s | 54 | 180 |
| Vigilante / pistola | 85 | 2 | 0,25 s | 66 | 220 |
| Apostador / socos | 110 | 4 | 0,30 s | 62 | 205 |
| Feiticeira / varinha | 90 | 6 | 0,75 s | 60 | 200 |
| Sacerdote / maldição | 105 | 3 | 0,80 s | 56 | 190 |
| Necromante / cajado | 85 | 4 | 0,65 s | 58 | 195 |
| Guerreiro / espada e escudo | 150 | 7 | 0,70 s | 50 | 175 |
| Fazendeiro / ancinho | 120 | 9 | 0,70 s | 57 | 195 |
| Fazendeiro / foice | 120 | 6 | 0,85 s | 57 | 195 |

Na escolha da profissão, o valor de movimento foi substituído por **ataques/s**.
O refúgio e a consulta por Tab mostram a taxa atual, incluindo dádivas e
forja, com até duas casas decimais. Taxa = 1 / intervaloAtual. Bases:
Caçador 1,54; Ferreiro 1,67; Vigilante 4; Apostador 3,33; Feiticeira 1,33;
Sacerdote 1,25; Necromante 1,54; Guerreiro 1,43; Fazendeiro 1,43 com ancinho
ou 1,18 com foice, em ataques/s.
A velocidade de movimento e seus bônus continuam existindo.

O Fazendeiro tem sorte 1×. Ancinho: estocada estreita de alcance 52, ou 64 raro.
Foice: corte de 360°, alcance 34, ou 44 raro. Q troca somente no combate ativo,
preservando a recarga de ataque compartilhada. A ferramenta permanece entre salas
e volta ao ancinho na próxima tentativa. Uma forja melhora ambas; bônus de dano
e cadência são calculados sobre a base da ferramenta ativa, sem acumular ao trocar.

A espada pode atingir vários alvos, mas exige proximidade. O Vigilante é mais
rápido e tem menos vida; o Ferreiro tem mais vida e menor velocidade. Intervalo,
alcance, área e sobrevivência também importam para comparar armas. O Apostador
atinge um alvo no cone de 80°, alcance 24, e recebe sorte 4×; as outras profissões
têm sorte 1×. Classificação e probabilidades das dádivas:
[SORTE_E_RARIDADE.md](SORTE_E_RARIDADE.md).

| Melhoria | Ganho por nível | Limite | Duração |
| --- | --- | --- | --- |
| Força de Hércules | +20% do dano base | 6 | Tentativa |
| Saúde do Minotauro | +20% da vida base | 4 | Tentativa |
| Sandálias de Hermes | +10% da velocidade base | 4 | Tentativa |
| Celeridade de Ártemis | +12% da taxa de ataque base | 6 | Tentativa |
| Vitalidade no altar | +10% da vida base | 5 | Permanente no save |
| Força no altar | +5% do dano base | 5 | Permanente no save |
| Agilidade no altar | +3% da velocidade base | 5 | Permanente no save |
| Reserva de estamina | +5% da capacidade base | 5 | Permanente no save |
| Recuperação de estamina | +8% da regeneração base | 5 | Permanente no save |
| Armadura | +3 pontos percentuais de redução de dano | 5 | Permanente no save |

Os percentuais se somam sobre a base da profissão, sem multiplicar os bônus
anteriores. Uma dádiva de força aumenta o dano do arco de 5 para 6 e o da pistola
de 2 para 2,4: ambos recebem 20%. Uma compra de força e uma dádiva dão +25% no total.
Dano e vida mantêm frações; a interface mostra até uma casa decimal.

Armadura substitui o alcance de coleta do altar e chega a 15% de redução em cinco
níveis. Soma com a resistência existente até o limite total de 75%; Amparo reduz
o dano restante em 30% enquanto ativo. Um golpe de 100 causa 85 com Armadura
máxima, ou 59,5 sob Amparo. Contato, projéteis e áreas usam a mesma redução.
Níveis antigos de COLLECTION migram para ARMOR sem custo; se ambas as chaves
existirem, ARMOR prevalece e ambas são validadas. Ímã e Rastro continuam
temporários, e o alcance inicial de atração sem dádivas permanece 36.

Cadência calcula intervalo = intervaloBase / (1 + 0,12 × níveis), respeitando o
intervalo mínimo da arma. Cura imediata, cura por abate, XP e poderes como a aura
mantêm suas regras próprias; não são bônus ao atributo base da arma.

## Escolhas, consulta e maldição em área

Cada escolha de nível e cada dádiva extra das Sombras espera **1,5 s** antes
de aceitar entrada. Após essa espera, o botão de ataque precisa estar solto;
só um novo clique seleciona. O bloqueio reinicia para cada nível pendente.
Pausa e Tab congelam a espera. Celeridade de Ártemis mostra a taxa atual e
a taxa resultante antes de aplicar a compra de nível, incluindo o limite da arma.

**Tab** abre/fecha a lista da tentativa, nas hordas, no chefe e na recompensa
de rota. Mostra raridade, nível e descrição; setas trocam páginas de seis
dádivas. O combate, o tempo da dificuldade, a fúria, efeitos e aliados ficam
pausados. Esc fecha a lista e preserva a tela/pausa anterior. Cura já aplicada
e ressurreição consumida são identificadas. Uma nova tentativa tem lista vazia.

A maldição do Sacerdote atinge o alvo direto e inimigos a até **32 unidades**
do centro do impacto, respeitando estruturas. A área funciona mesmo se o golpe
direto matar o primeiro inimigo. Cada alvo sofre **60% do DPS direto por segundo**
durante 3 s, ou 5 s com arma Rara. DPS = danoAtual × ataquesPorSegundo: aumentar
dano base, bônus percentuais, cadência ou raridade aumenta o próximo efeito.
Reaplicar renova a duração e preserva o maior DPS; não acumula cópias nem se espalha
recursivamente. Alvos fora da área ou fora do alcance, como o chefe no ar, são excluídos.

## Colisão dos sprites

Contato entre jogador e inimigos usa máscaras de opacidade do quadro desenhado,
com posição, escala e espelhamento correspondentes. Pixels transparentes e sombras
muito fracas não causam contato; tanks usam sua escala de 21 unidades.
Projéteis verificam o percurso contra a máscara do alvo e o raio do projétil,
inclusive contra Carrasco e Guardião. O contato desses dois encontros também
exige sobreposição visível. Ataques sinalizados em área conservam sua geometria,
separada do contato corporal; a navegação mantém sua colisão de paredes.

As máscaras são calculadas uma vez por atlas, em grade de 64×64 por quadro,
com alfa mínimo de 96/255. O contato usa uma checagem de limites seguida de
amostragem de 0,25 unidade somente em pares próximos. Esses valores permitem
ignorar margens invisíveis sem ler pixels da textura a cada quadro.

## Arma rara

Um Fragmento de Forja transforma a arma comum em rara: ×1,25 no dano base e
×1,10 na taxa de ataque. Dádivas e compras continuam percentuais sobre a nova base,
sem depender da ordem: dano = danoBase × raridade × (1 + bônus).
Intervalo = intervaloBase / ((1 + bônusCadência) × taxaRaridade), respeitando o mínimo.

Flecha rara atravessa mais um alvo; pistola ricocheteia uma vez em um alvo próximo
com caminho livre; espada mantém o cone e ganha alcance de 36 para 48.
Socos ganham alcance de 24 para 34, continuando em um alvo. Varinha aumenta
explosão de 18 para 32; maldição passa de 3 para 5 s; cajado ganha chance de
reviver ×1,25 e duração dos aliados 75 s; escudo ganha 35% de chance de revide.
Regras: [CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md).
A forja ocorre uma vez por personagem nesta versão: Comum → Rara, permanentemente.
Material, limite de uma unidade por região e raridade persistem no save;
gastar não reabre o drop. Dádivas reiniciam na nova run; a arma mantém bônus e poder. Regras: [FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md).

## Recompensas e variação das rotas

Cinzas mantém metas 24/16/4 e dificuldade 60, com intervalo de ondas ×1,25;
a conclusão oferece cura de até 40 ou 5 Óbolos. Sombras mantém metas 40/30/8 e
dificuldade 120, com intervalo ×0,85 e dádiva extra Rara ou superior. Sorte ainda
influencia as opções elegíveis. Se todas as categorias superiores estiverem no
limite, oferece 8 Óbolos. Recompensas não repetem na mesma sala/tentativa.
Eventos alteram os pesos dos tipos, mantendo todos disponíveis para as metas.
Pesos e verificação: [ROTAS_DO_SUBMUNDO.md](ROTAS_DO_SUBMUNDO.md).

## Ataque com defesa do Guerreiro

**Botão direito** mantém a defesa frontal e **esquerdo** ataca simultaneamente,
com dano e cadência normais, inclusive com arma Rara. Atacar não desliga a defesa
nem acrescenta um custo de estamina. Permanecem os custos de 8/s e 12 por bloqueio,
movimento a 55%, ausência de regeneração durante a defesa e vulnerabilidade a
ataques pelas costas e de chão. Solte o escudo para iniciar um dash.

## Pressão dos inimigos rápidos

Ajuste motivado pelo relato de rápidos acumulando e exigindo dádivas de movimento.
Agora há até **2 rápidos por onda** e **24 ativos por arena**. Novos rápidos que
excederiam os limites são substituídos por comuns/tanks, conforme os pesos relativos
da sala; a onda não encolhe. Na introdução da demo, o substituto ainda é comum.
O total de inimigos ao longo da fase continua ilimitado e a meta de rápidos não muda.

Cada rápido alterna **1 s a 70 unidades/s** e **1 s a 24 unidades/s**.
Os ciclos são defasados por spawn, evitando arrancadas sincronizadas de toda a horda.
Ciano indica arrancada; tom verde-azulado indica recuperação. A média de um ciclo
em caminho livre é 47 unidades/s, abaixo da velocidade base das nove profissões.
Isso cria espaço para reposicionar sem Hermes; obstáculos e múltiplas direções
ainda exigem reação. Vida, dano, XP/Almas e escala continuam próprios do rápido.
Pausa, Tab e escolhas também congelam esse ciclo.

Os pesos de eventos continuam sendo os pesos do sorteio inicial. Os limites
redistribuem parte dos resultados, então não representam a composição final
exata de cada onda. Metas, risco das rotas e duração ainda precisam de playtests.

## Arqueiro do Estige

Variação de comum: vida 14, velocidade 28, contato 6, flecha 10, XP/Almas 3/3.
Aviso fixo de 1 s e recuperação de 2 s; não dispara fora da tela.
Vida e ambos os danos seguem a escala e Fúria. A partir de 15 s de campanha
(60 s na demo), chance de 20% entre comuns elegíveis, até 2 por onda e 6 ativos.
Flechas respeitam paredes, máscara visível e escudo frontal.
Metas, saves e profissões permanecem compatíveis.
Regras e matriz de validação manual: [ARQUEIRO_DO_ESTIGE.md](ARQUEIRO_DO_ESTIGE.md).

## Sabedoria de Atena e a curva de XP

Ajuste em 13/10/2026 após o relato de que o bônus de 20% não compensava
a dádiva gasta. **Sabedoria de Atena** passa a conceder **+150% de XP coletada
(2,5×)**, com categoria **Épica** e limite de **uma escolha por tentativa**.
É um investimento para obter outras dádivas, principalmente no início da run.

O primeiro nível exige 10 XP. Cada requisito seguinte é o anterior ×1,5,
arredondado para baixo: 10, 15, 22, 33, 49, 73, 109... O nível 6 exige 129 XP
acumulada; o nível 8, 311. Um aumento pequeno não garante um nível adicional,
e gastar a escolha em XP também adia um atributo de combate.

Exemplo reproduzível: coletar os cristais das metas do Estige, sem arqueiros,
mini-boss, baús, abates extras ou Fúria, rende 40×1 + 30×2 + 8×5 = **140 XP base**.
Ao escolher Atena no nível 2, após coletar exatamente 10 XP:

| Regra | XP total recebida | Nível final | Escolhas disponíveis para outras dádivas |
| --- | --- | --- | --- |
| Sem Atena | 140 | 6 | 5 |
| Atena antiga: +20% | 10 + 130×1,2 = 166 | 6 | 4 |
| Atena atual: +150% | 10 + 130×2,5 = 335 | 8 | 6 |

Assim, a regra atual devolve a escolha investida e rende outra dádiva nesse
cenário. Os testes também verificam retorno positivo ao obtê-la nos níveis 2
ou 3 e seguir por Cinzas ou Sombras. A comparação conta apenas escolhas por
nível; não inclui a recompensa extra da rota. Ela **não garante lucro se escolhida
tarde**: obtê-la no nível 6 perto da saída pode não render outro nível antes do chefe.
A curva e o bônus são valores iniciais para playtests.

O multiplicador vale para cristais coletados e XP de baús **após** a escolha,
inclusive cristais já no chão. Não multiplica XP já no contador, Óbolos, Almas
nem a quantidade de abates para liberar a saída. A redução de XP pela Fúria
continua aplicada ao cristal antes do bônus. Frações são conservadas, representadas pela barra e consultadas
em Tab; um comum rende 2,5, um rápido 5, um tank 12,5, um arqueiro 7,5 e
um mini-boss 50 XP sem Fúria. Um baú de 12 XP base passa a conceder e anunciar 30 XP.
Atena permanece entre salas e reinicia ao voltar para uma nova tentativa.
Relatórios anteriores com duas/três escolhas continuam legíveis e preservam
o histórico e as Almas compartilhadas; novas tentativas continuam limitadas a uma.

Com o M4, a chance de Atena no **primeiro cartão** depende da origem e das
sinergias disponíveis. No Submundo, há quatro Épicas elegíveis normalmente:
6% / 4 = **1,5%** para sorte 1. Com Convergência elegível (Sacerdote ou queimadura),
passam a cinco: **1,2%**. O Apostador tem aproximadamente **7,45%** ou **5,96%**.
Na demo sem Convergência, continuam três: 2% e aproximadamente 9,9%.
São chances do primeiro cartão, não do conjunto de três. Pesos das categorias intactos.
Regras: [SORTE_E_RARIDADE.md](SORTE_E_RARIDADE.md).

## Ímã de XP

**Colheita de Deméter** aumenta o alcance e a velocidade de atração dos cristais.
A coleta ocorre ao alcançar 14 unidades do centro do jogador, sem ultrapassá-lo
nem conceder XP duas vezes.

| Nível da dádiva | Alcance de atração | Velocidade de atração |
| --- | --- | --- |
| 0 | 36 | 120 |
| 1 | 68 | 160 |
| 2 | 100 | 200 |
| 3 | 132 | 240 |

O ímã acompanha a tentativa entre salas e desaparece na próxima tentativa.
Pausa e seleção de dádiva congelam o movimento dos cristais.

## Escala linear dos inimigos

Com **t** = minutos totais de combate da tentativa e
**s** = difficultySeconds da sala dividido por 60:

- Vida = vidaBase × (1 + 0,06t + 0,12s).
- Dano = danoBase × (1 + 0,03t + 0,06s).

Na arena inicial, após dez minutos, um comum passa de 10 para 16 de vida
e de 8 para 10,4 de dano. Com dificuldade de sala 60, no mesmo tempo,
esses valores são 17,2 e 10,88. XP e velocidade do inimigo permanecem próprios
do tipo. A quantidade/intensidade das hordas conserva sua curva separada.

A escala usa o tempo total, sem reiniciar ao trocar de sala e sem parar aos cinco
minutos. O inimigo recebe os valores ao nascer; o chefe, ao entrar na sala.
Inimigos vivos conservam sua vida e dano definidos no nascimento, preservando
o dano já recebido. O HUD do chefe mostra a vida máxima resultante.

A demo usa dificuldade de sala zero. Pausa, conversa, escolha de rota e seleção
de dádivas não aumentam o relógio de combate.

## Fúria e dano elemental

Choque, queimadura e impacto usam DPS atual e recargas próprias. A Fúria começa
somente por farming após liberar a saída, com tolerância e aviso antes das
penalidades. Ela aumenta vida/dano no nascimento e reduz XP do cristal no abate;
a curva normal de sala/tempo continua. Avançar limpa a pressão. Fórmulas,
limites e exemplos: [CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md).

## Mensagens e verificação

Confirmações de conversa com Caronte e compras somem após 3,5 segundos no refúgio.
Erros de persistência têm apresentação separada e continuam visíveis enquanto
pendentes.

Os testes cobrem bloqueio da profissão após recarga, saves independentes,
preservação do arquivo anterior, percentuais proporcionais e fracionários,
curva linear, escala do chefe, coleta com ímã e reset. Também verificam o quarto
save, limite regional persistente, socos, sorte e distribuição das raridades. A verificação gráfica
também testa a mensagem de Caronte e a aplicação real nos inimigos.

Jogar cada profissão pela rota direta e opcional deve orientar os próximos
ajustes. A variedade futura entre tentativas está registrada no
[design](DESIGN_DO_JOGO.md#variedade-entre-tentativas--desenvolvimento-futuro).
