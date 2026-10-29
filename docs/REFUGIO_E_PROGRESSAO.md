# Refúgio, atributos permanentes e baús

Este incremento fecha o ciclo inicial da campanha: preparar no refúgio, jogar,
voltar após morte/vitória/abandono e começar de novo. O autor confirmou compras
permanentes e baús aleatórios. Óbolos, fontes, custos e números são provisórios.

## Refúgio e Caronte

Escolha um dos oito saves (duas páginas, com setas ou Anterior/Próxima) antes de começar. Em um save vazio, o narrador pergunta
a profissão e abre o Refúgio do Estige. A profissão fica fixa: outra exige um
personagem em outro save vazio. Há nove profissões em três páginas; os oito saves
continuam em duas. O perfil anterior é preservado no Save 1.
C conversa, L abre o altar, P abre os saves e Enter parte. E interage perto dos
locais; as mesmas ações têm botões. O refúgio é seguro e tem movimento; conversas
e loja ficam em painéis. J abre o diário da promessa da Camponesa das Cinzas.
F1 abre o guia de duas páginas da campanha, com setas/Anterior/Próxima para
consultar e F1/Esc para fechar. H ativa/desativa dicas do personagem. A leitura
contextual dura até sete segundos ativos ou termina ao executar a ação; não
avança enquanto conversa, altar, guia ou áudio estão abertos. Dicas e lições
aprendidas acompanham o save, sem conceder recursos.

Caronte explica a maldição e a diferença entre dádivas e compras. Concluir sua
primeira conversa entrega cinco Óbolos uma única vez. Ele oferece a conversa do
suborno após uma morte e comenta a primeira vitória contra o Guardião. Concluir
essas conversas registra eventos; cancelar permite retomá-las depois.
Depois desses eventos, Caronte pode comentar a etapa da promessa da Camponesa
ou a arma forjada. São falas opcionais, sem novo evento/presente em Óbolos.
As confirmações de conversa e compra desaparecem após 3,5 segundos; mensagens
sobre falhas de gravação permanecem até a resolução. Falas vêm de
[charon.json](../core/assets/dialogues/charon.json), com retratos de Caronte e
protagonista. Enter/E/clique revela o texto e depois avança uma fala; T alterna
gradual/instantâneo na sessão, preservando a fala já revelada ao mudar o modo.
O retrato acompanha cada fala. [Apresentação inicial](EXPANSAO_VISUAL.md) e
[lote atual de diálogos](MARCO_6_DIALOGOS_E_NPCS.md).

O [lote de cenários e rotas de 25/10/2026](MARCO_6_CENARIOS_E_ROTAS.md) dá ao
refúgio pedra de tons quentes e bronze, com piso limpo para os NPCs, altar e
travessia. Estige, Cinzas, Sombras e sala do Guardião têm temas próprios.
Cada ambiente usa seis pisos e três obstáculos; os detalhes planos ocupam
aproximadamente 4% das células. As bordas do refúgio/chefe ficam fora dos limites
caminháveis originais. As texturas são compartilhadas por PilotArt, emprestadas
às telas; não acrescentam bloqueios, compras ou campos ao perfil.

A seleção de destino conserva título, descrição, evento, recompensa e metas,
com ilustrações proporcionais, hover e entrada protegida por 0,2 s/soltura do
mouse. A passagem liberada usa quatro quadros de uma porta luminosa; **E/Enter
continua funcionando de qualquer posição da arena após cumprir as metas**.
O tema não muda a colisão procedural, RNG de gameplay ou economia, e navegar
não cria tiles visuais. O cache tem teto de 8.192 e considera zoom; o atlas tem
52 regiões em 1024×1024, com 4 MiB RGBA compartilhados.

No resultado da campanha, Enter retorna ao refúgio. M durante a pausa abandona
a tentativa e retorna diretamente. O retorno guarda moedas e incrementa as
estatísticas uma vez. Abandonar não é contado como morte. A demo mantém seu
reinício próprio, usa a profissão do save escolhido e não registra ganhos da
campanha. Sua escolha inicial de profissão também fica salva.

## Primeira vitória e Centelha

O [primeiro incremento do M7](MARCO_7_TUTORIAL_E_CENTELHA.md), implementado em
27/10/2026 e verificado, fecha a travessia local. Vencer o Guardião do
Submundo conquista a **Centelha da Insurgência**, poder aprovado pelo autor
que permitirá ferir os deuses. Agora é um avanço narrativo permanente do
personagem, sem bônus de combate; a maldição continua explicando os retornos
após a morte.

Depois da queda do chefe e antes do resultado, sete falas apresentam o poder
e o retorno voluntário ao refúgio para preparar a ascensão. O resgate e Zeus
continuam como objetivo futuro. A tela informa que a continuação está em
desenvolvimento. Os ganhos já estão liquidados antes da conversa.

Espere 1,5 s e solte o ataque: Enter/E/Espaço/clique na fala revela ou avança,
T alterna o modo de texto e Esc adia sem concluir a leitura. Após a conquista,
**V no refúgio** e **C no resultado da campanha** retomam/reveem o desfecho.
Depois da confirmação final, ele não reaparece automaticamente em outras
vitórias. Ler novamente não concede prêmios nem incrementa retornos.
Falhas de gravação mantêm o progresso em memória, aparecem na interface e
permitem **S no refúgio** para repetir o autosave.

## Reinício e loja

| Estado | Próxima arena na mesma tentativa | Nova tentativa |
| --- | --- | --- |
| Dádivas, efeitos elementais, aura, Égide e ressurreição | Mantidos, incluindo consumo/recargas | Apagados |
| Nível, XP, vida e estamina | Mantidos | Nível 1, zero XP, vida e estamina cheias |
| Atributos comprados | Mantidos | Aplicados aos atributos iniciais |
| Profissão | Mantida | Mantida, fixa para o save |
| Aliados do Necromante | Mantidos; duração congelada em menus | Apagados |
| Fúria por farming | Limpa ao avançar | Limpa |
| Desbloqueios lendário/épico | Registros permanentes no arquivo compartilhado | Disponíveis para novos personagens |
| Missão da Camponesa | Aceitação/semente mantidas no perfil pessoal | Mantidas após morte/abandono; conclusão libera Fazendeiro globalmente |
| Ferramenta do Fazendeiro | Ancinho/foice selecionados são mantidos | Começa com ancinho; uma forja continua beneficiando ambas |
| Baús aleatórios | Locais à arena | Novos sorteios |
| Recompensa da rota | Uma por sala concluída | Novamente disponível; dádiva/cura são temporárias |
| Evento de hordas | Fixo para cada sala da tentativa | Novo sorteio |
| Raridade da arma forjada | Mantida | Mantida permanentemente no save |
| Fragmentos de Forja | Guardados imediatamente no perfil | Mantidos no save |
| Região que já concedeu fragmento | Registrada no perfil | Continua bloqueada mesmo após gastar |
| Óbolos da travessia | Acumulados | Guardados no perfil ao retornar |
| Eventos de Caronte e saldo guardado | Mantidos no perfil | Mantidos, inclusive ao fechar |
| Centelha e leitura do desfecho | Pessoais, após a vitória do Submundo | Permanecem no save; não mudam atributos neste incremento |
| Dicas aprendidas e preferência H | Pessoais por save | Mantidas; F1 continua disponível |

Vida máxima dá +10% da vida base por nível; força dá +5% do dano base da arma;
agilidade dá +3% da velocidade base. Cada profissão tem bases diferentes; os
percentuais se somam, sem multiplicar compras anteriores, e o dano mantém frações.
Cada atributo tem até cinco níveis. Vida, força, agilidade e Armadura custam
5, 9, 13, 17, 21 Óbolos por nível. Reserva de estamina custa 6, 11, 16, 21, 26;
recuperação custa 7, 12, 17, 22, 27. São bônus cumulativos, limitados. Cada tentativa recebe um snapshot das
compras; não mistura esses níveis com os limites das dádivas.

Uma compra só é aplicada após a gravação do perfil. Falhar ao salvar não consome
moedas nem sobe o nível. A interface informa saldo, nível atual, comparação atual/próximo, preço e resultado.
Setas ou Anterior/Próxima mudam entre as duas páginas; 1–3/clique compram na página atual.

No altar, N ou clique no texto do Necromante gasta **1.000 Almas compartilhadas**
para desbloquear a classe em novos personagens. O personagem atual continua
com sua profissão; Óbolos não são gastos. Saldo e acesso ficam em um arquivo
compartilhado, com compra confirmada por gravação. Desbloqueios antigos são preservados.
R no refúgio consulta o resumo da última tentativa e o histórico recente do personagem.
Almas, relatório e migração: [ALMAS_E_RELATORIOS.md](ALMAS_E_RELATORIOS.md).
Regras e valores das classes:
[CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md).

Almas por derrota: 1 comum, 2 rápido, 5 tank, 20 mini-boss e 100 Guardião.
São guardadas no retorno por morte/vitória/abandono e podem ser usadas por qualquer
save. Farming sob Fúria reduz o ganho de inimigos. O resumo mostra o valor obtido.
A demo registra métricas, mas não ganha moedas.

## Altar, conversas e missão persistente

O [M3 implementado](MARCO_3_BASE_VISUAL.md) traz a conversa piloto de Caronte
com retrato, nome e caixa inferior que mantém o cenário visível. Primeiro
Enter/E/clique revela a fala; o seguinte avança. T alterna texto gradual/instantâneo,
preferência mantida nesta sessão. Esc cancela sem concluir o evento; o presente
de 5 Óbolos continua único. Textos ficam em JSON e eventos selecionam as conversas.
A Camponesa recebeu encontro/diário no M5. O **lote de diálogos do M6, de
23/10/2026**, entrega expressões de Caronte/Camponesa e retrato próprio do Ferreiro,
com painel/leitura compartilhados. Esta revisão passou em **302 testes unitários,
OpenGL e execução nativa do pacote Windows**. O [lote de dano/morte](MARCO_6_DANO_E_MORTE.md)
passou em 320 testes, OpenGL e pacote Windows. Cenários/rotas foram implementados
em 25/10/2026, com 336 testes de lógica/0 falhas, integração OpenGL e execução
nativa do pacote Windows aprovados. O M6 geral continua em andamento, com revisão humana.

O Ferreiro comenta a profissão, arma ativa, material e raridade; a resposta muda
após a tentativa de melhoria. **Espaço ou clique na fala** revela/avança, **T**
alterna leitura, **1/E ou botão** compra, **2/Enter/Esc** continua a travessia.
Ler nunca gasta o fragmento. A forja só altera o personagem após o commit existente.
A Camponesa mantém uma linha por estado e a proteção de 1,5 s com soltura do mouse
para aceitar por clique; J abre o diário com a fala já revelada. As telas seguras
continuam congelando o combate. Economia, dano e persistência da forja são os mesmos.

O M4 implementou reserva de estamina (+5% por nível, máximo 125), recuperação
(+8% por nível, máximo 14/s) e Armadura (+3 pontos percentuais de redução de dano
por nível, máximo 15%). As três têm cinco níveis. A regeneração não funciona
durante defesa e mantém a penalidade da exaustão. Armadura soma com a resistência
existente, limitada a 75% no total; Amparo multiplica o dano restante enquanto ativo.
Compras são pessoais, em Óbolos, confirmadas por gravação.
Armadura substitui o alcance de coleta permanente: níveis antigos de COLLECTION
são convertidos para ARMOR. Se ambas as chaves existirem, ARMOR prevalece,
sem somar níveis; ambas são validadas. Perfis sem essas compras começam em zero
e preservam seu progresso. Ímã e Rastro continuam ampliando a coleta temporariamente.
[Entrega e verificações](MARCO_4_DADIVAS_E_ALTAR.md).

A dificuldade já é fixa ao criar o save, confirmada com a profissão no M2.
O **M5 foi implementado em 23/10/2026**, com 290 testes unitários, integração
OpenGL e pacote Windows aprovados. A Camponesa das Cinzas tem 10% de chance base
por tentativa, ou 40% para o Apostador. Há um sorteio independente após liberar
a passagem do Estige; o encontro opcional ocorre em segurança, após eventuais
recompensa e ferreiro, sem avançar o combate.

Aceite a promessa, derrote um Carrasco para obter uma semente garantida e vença
o Guardião do Submundo. A semente independe da chance/limite do Fragmento de Forja.
Aceitação e semente persistem no personagem após morte, abandono e recarga.
A conclusão libera novos Fazendeiros em todos os saves, sem trocar a profissão
de personagens existentes ou gastar moedas. J no refúgio consulta o diário;
Tab mostra o objetivo aceito durante a tentativa.

O perfil concluído permite recuperar o acesso quando a gravação global falha.
Excluir o personagem apaga a missão incompleta; uma conclusão pendente precisa
ter o desbloqueio preservado antes da exclusão. Outros encontros, compras e
regiões continuam planejados. Regras e capturas:
[MARCO_5_FAZENDEIRO.md](MARCO_5_FAZENDEIRO.md). Ritmo/equilíbrio exigem playtests humanos.

## Economia provisória em Óbolos

- 5 Óbolos ao concluir a primeira conversa com Caronte, uma vez por perfil.
- 1 por 10 abates, até 10 por tentativa; o limite não afeta a geração de inimigos.
- Mais 10 ao derrotar o Guardião.
- 3 em um baú com esse prêmio.

A tabela descreve as bases de Média. Fácil aplica ×0,85 e Difícil ×1,25 aos
ganhos da tentativa, somando bases antes da conversão para inteiros. Caronte
continua em 5; saldo existente e preços não mudam. Recompensas de Cinzas/Sombras
também são elegíveis. Detalhes: [MARCO_2_DIFICULDADES_E_ECONOMIA.md](MARCO_2_DIFICULDADES_E_ECONOMIA.md).

Os ganhos da travessia são guardados ao retornar, mesmo por morte ou abandono.
Morrer imediatamente sem abates ou baús não concede moeda. Fechar durante a
travessia perde os ganhos não guardados. Ajustar a economia com partidas completas;
o perfil atual não oferece bônus ilimitados ou lojas dentro do combate.

## Baús

O primeiro sorteio ocorre após 18–30 segundos de simulação na arena; os próximos
a cada 25–40 segundos. Há até três baús simultâneos por arena. Posições são
sorteadas perto do jogador e precisam ser livres e conectadas ao seu caminho.
Baús distantes são retirados e não bloqueiam movimentos.

Use F dentro de 24 unidades para abrir uma vez. Prêmios: 30 de vida, 12 XP base,
40 de estamina ou 3 Óbolos, com chances iguais na campanha. Atena multiplica a
XP para 30 e a mensagem mostra o ganho efetivo. A demo usa os três
suprimentos. Vida/estamina cheias convertem o prêmio em XP. Recompensas não
ultrapassam os máximos de vida/estamina; XP abre as escolhas normais de dádivas.
Pausa e escolha de dádiva congelam o relógio dos baús. Não há baús na sala do chefe.

## Arquivo e verificação

No Windows, os oito arquivos ficam em `%LOCALAPPDATA%\ParadiseInHell\`:
`profile.json` e `profile-2.json` até `profile-8.json`. Cada um guarda versão,
saldo, profissão, níveis permanentes, retornos, mortes, vitórias, eventos e
Fragmentos de Forja e regiões já premiadas próprios, além da raridade permanente
da arma e da missão pessoal da Camponesa. Almas, desbloqueios do Necromante/Fazendeiro
e histórico ficam no arquivo compartilhado. Saves anteriores sem
forgeMaterials leem zero; a migração do limite regional está em
[FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md).
O M7 mantém `version: 1`, acrescentando `narrativeVersion: 1` e os campos
opcionais `tutorialSeen`/`tutorialEnabled`. Sem os campos de tutorial, o personagem
começa sem lições aprendidas e com dicas ativadas. Os eventos pessoais
`INSURGENCE_SPARK_OBTAINED` e `UNDERWORLD_EPILOGUE_READ` distinguem conquista
e leitura. Perfis antigos sem versão narrativa e com uma vitória recebem a
Centelha na migração, com leitura disponível por V/C. Uma versão narrativa
desconhecida é preservada; perfis já versionados não inferem conquistas do contador.
Excluir o slot limpa história, lições e preferência, sem alterar os dados
compartilhados. Falhas de autosave também retêm esses fatos em memória para S.

Não guarda a tentativa em andamento. A cópia anterior válida usa o sufixo `.bak`
do respectivo arquivo. Principal corrompido pode recuperar essa cópia, que pode
estar um salvamento atrás. Perfil de versão desconhecida é preservado.

Uma falha de autosave mantém as moedas na memória e permite S no refúgio para
repetir a gravação. Falha de leitura mantém os arquivos intactos e permite jogar
na sessão; compras permanentes ficam indisponíveis. Os testes e o executável
com --smoke-test usam um perfil em memória e não alteram o perfil real.
A entrega do M5 passou em 290 testes unitários, OpenGL e verificação nativa do
pacote Windows com Java incluído e PATH sem Java externo, sem saves reais.
Os 336 testes de lógica do lote de cenários passaram sem falhas, assim como a
integração OpenGL e o pacote Windows nativo. O executável foi verificado com Java
incluído, PATH sem Java externo, áudio OpenAL nativo silenciado e saves em memória.
A leitura em partidas e o aceite artístico do M6 permanecem pendentes. O primeiro
incremento do M7 já implementa tutorial e desfecho, preservando o ciclo de retorno;
372 testes de lógica passaram sem falhas, com 36 novos (18 de persistência,
11 do guia e sete da história), e a integração OpenGL foi aprovada. A execução
nativa do pacote Windows também foi aprovada em 27/10, com Java incluído, PATH
sem Java externo, áudio OpenAL nativo silenciado e perfis em memória. Os 336
testes acima são o histórico do lote de cenários. Playtests humanos continuam pendentes.

Testes cobrem saves independentes, profissão fixa, bônus percentuais, custos,
limites, retorno único, reset de dádivas, persistência,
cópia anterior, versão desconhecida, falhas de gravação, alcance e prêmios dos baús.
A verificação OpenGL percorre diálogos, compra dois atributos, volta após morte
e abandono, recarrega o perfil, verifica a expiração da mensagem de Caronte
e abre um baú real. Tabelas de atributos e ímã:
[BALANCEAMENTO_E_SAVES.md](BALANCEAMENTO_E_SAVES.md). Os testes aceleram estados;
partidas completas ainda precisam validar ritmo e economia.

O segundo NPC implementado é o Ferreiro Errante, encontro raro e seguro entre
salas. Fragmentos, limite regional e raridade/poder da arma persistem no save. Confira
[FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md). Cinzas tem baú garantido;
Sombras oferece dádiva extra Rara ou superior, em uma etapa segura:
[ROTAS_DO_SUBMUNDO.md](ROTAS_DO_SUBMUNDO.md). Outros encontros e inventário geral
continuam pendentes. A primeira região completa ainda precisa de validação do
tutorial/desfecho, revisão sonora/artística e playtests. Vídeo/remapeamento,
retomada da tentativa e novas regiões continuam em aberto no M7 maior.
