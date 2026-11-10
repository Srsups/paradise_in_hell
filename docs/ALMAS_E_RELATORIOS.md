# Almas compartilhadas e resumo das tentativas

Implementado em 12/10/2026. Os valores são iniciais para playtests.

## Moedas e desbloqueio

**Almas** têm um único saldo para os oito saves. No altar, **L → N/clique**
gasta **1.000 Almas** para liberar o Necromante em novos personagens.
O débito e o desbloqueio são salvos juntos. Uma falha de gravação não gasta Almas.
O personagem atual mantém sua profissão; um novo Necromante exige outro save vazio.

Óbolos, atributos permanentes, arma, materiais e fatos narrativos continuam
pertencendo a cada personagem. Óbolos não compram o desbloqueio do Necromante
e não são convertidos em Almas. O saldo inicial de Almas é zero.

| Inimigo derrotado | Almas base |
| --- | --- |
| Comum | 1 |
| Arqueiro do Estige (meta comum) | 3 |
| Rápido | 2 |
| Tank | 5 |
| Carrasco / mini-boss | 20 |
| Guardião de andar | 100 |

Almas são recompensas determinísticas por derrota, sem multiplicador de sorte.
As metas continuam separadas por tipo e não mudam por esta moeda.
Continuar farmando sob Fúria reduz as Almas dos inimigos, com o mesmo multiplicador
de redução da XP e mínimo de 1 base; o bônus do Guardião não recebe essa penalidade.
No M2, aplicar depois o modo: Fácil ×0,85, Média ×1, Difícil ×1,25.
Acumular centésimos inteiros na tentativa e converter o total para inteiros,
descartando somente a fração final no retorno. O mínimo base não arredonda cada
abate no Fácil para 1 nem cada abate no Difícil para 2.
O relatório registra a dificuldade; histórico antigo sem campo assume Média.
Regras: [MARCO_2_DIFICULDADES_E_ECONOMIA.md](MARCO_2_DIFICULDADES_E_ECONOMIA.md).
A tabela tem constantes próprias em SoulRewards e ArcherAttack, independentes dos valores de XP.

Os ganhos são guardados ao encerrar a campanha por **morte, vitória ou abandono**.
A demo registra seu resumo, mas concede zero Almas e zero Óbolos.
Desde 29/10/2026, fechar durante o combate retoma o último checkpoint da campanha.
Salvar e sair nos pontos seguros não concede moedas nem conta como retorno;
morte, vitória e abandono confirmado encerram a tentativa. Ganhos posteriores ao
último checkpoint ainda precisam ser conquistados novamente.
[Contrato da retomada](MARCO_7_CHECKPOINTS.md).

A rota direta com as metas mínimas concede ao menos 140 Almas dos inimigos + 100 do chefe,
na Média, antes da Fúria. Cada arqueiro dentro da meta comum acrescenta 2 Almas sobre esse exemplo.
Com um Carrasco e nenhum arqueiro, são 260. Abates extras e salas opcionais
alteram esse total. Aproximadamente quatro vitórias desse exemplo atingem 1.000;
a duração real e a acessibilidade precisam de partidas completas.

## Resumo e histórico

**R no resultado ou no refúgio** abre o relatório. Esquerda/direita trocam páginas;
acima/baixo consultam tentativas recentes do mesmo personagem. R ou Esc volta.

O relatório mostra:

- Profissão, arma/raridade, modo, resultado, tempo, nível e abates.
- Dano efetivo causado e recebido, causa da morte e ressurreições usadas.
- Óbolos e Almas obtidos.
- Salas visitadas: tempo, abates, vida de entrada/saída, dano e momento de cumprir a meta.
- Dádivas escolhidas, raridades e quantidade de escolhas.
- Dano por arma, aura, explosão, choque, queimadura, maldição, aliados e revide.

Dano efetivo considera a vida realmente retirada. Golpes bloqueados,
invulnerabilidade e dano excedente à vida do alvo não entram nos totais.
Curas não subtraem dano recebido. A causa da morte é o último golpe que retirou
vida; ataques posteriores contra um jogador já morto não substituem essa origem.
As contagens continuam entre salas e congelam com a mesma simulação do combate.

O histórico mantém as **20 últimas tentativas de todos os saves**, incluindo demo
e abandono. A consulta filtra o personagem atual. Relatórios antigos saem do
histórico, sem retirar Almas ou desbloqueios. Uma nova run começa com métricas vazias.

## Exportação local para playtests — 04/11/2026

Na tela do relatório, **E** ou **Exportar histórico** cria uma pasta em
`%LOCALAPPDATA%\ParadiseInHell\exports\run-history-<id>\`, com `history.json`,
`attempts.csv` e `rooms.csv`. Inclui até 20 relatórios globais disponíveis e o
relatório aberto se ele estiver fora desse conjunto. O modo/resultado distingue
campanha, demo, morte, vitória e abandono. Não é um histórico vitalício.

JSON preserva os dados originais. Os CSVs permitem comparar rotas, dano efetivo,
moedas por minuto, tempo até a passagem e tempo restante na sala após liberá-la,
além das fases/padrões iniciados do chefe quando disponíveis. Campos ausentes
permanecem vazios: os relatórios atuais não guardam atributos iniciais do altar,
variação da sala ou revisão de balanceamento. Esses dados precisam de anotação
manual nas partidas; não podem ser reconstruídos a partir dos atributos finais.

Exportar só lê snapshots e grava uma pasta própria, sem alterar os saldos,
recompensas, histórico ou arquivos de save. Falha mostra erro e pode ser repetida.
Não há envio automático desses arquivos. As medições e a rodada inicial estão no
[marco de balanceamento do Submundo](MARCO_7_BALANCEAMENTO_SUBMUNDO.md).

## Métricas do chefe no marco 1

Em 14/10/2026, as salas de chefe passaram a guardar bossFight opcional:
tempo nas duas fases e contagem por padrão iniciado. R oferece uma página
adicional para cada sala com métricas, incluindo DPS médio efetivo calculado
pelo dano da sala e pelo tempo do encontro. Introdução/recuperação entram no tempo.

A medição não registra eventos por quadro nem guarda recursos gráficos.
A contagem não é de acertos: uma preparação que começou é registrada, mesmo
se o chefe for derrotado antes de concluir o ataque. Morte/abandonar e vitória
guardam o resumo existente, preservando o crédito único de Almas.
Relatórios antigos sem bossFight permanecem válidos; nomes/IDs e saldo não mudam.
Detalhes: [MARCO_1_LEITURA_E_CHEFE.md](MARCO_1_LEITURA_E_CHEFE.md).

## Persistência e compatibilidade

No Windows, o novo arquivo é:

`%LOCALAPPDATA%\ParadiseInHell\shared-progress.json`

Ele escreve versão 2 e lê/migra a versão 1, conservando saldo, desbloqueios e
histórico. O registro da maior sequência concluída por personagem é separado
dos 20 relatórios visíveis. A cópia válida usa `shared-progress.json.bak`.
Os oito arquivos pessoais conservam seus nomes e também escrevem formato 2,
lendo a versão 1. Os snapshots de relatório continuam no formato 1; exportar
não migra esses documentos. Compra global não altera a profissão do personagem.

Desbloqueios pagos nas versões anteriores são migrados gratuitamente. Um perfil
já criado como Necromante também preserva o acesso. Não é necessário pagar outra
vez; o desbloqueio passa a existir no arquivo compartilhado.

Gravações usam arquivo temporário e substituição ao final. Um principal corrompido
pode ser recuperado pela cópia; versões desconhecidas ou ambos os arquivos
ilegíveis são preservados. Falha de autosave mantém Almas e resumo na memória;
**S no refúgio** tenta gravar novamente. Compras ficam indisponíveis após falha de
leitura. Fechar com uma falha ainda pendente perde os ganhos apenas em memória.

Cada tentativa possui um ID; retorno e relatório são processados uma vez.
Na campanha com checkpoint, o perfil grava o retorno e um recibo pendente antes
de o arquivo compartilhado consumir esse recibo e guardar Almas, relatório e
sequência concluída juntos. O perfil então confirma o consumo. A repetição de
uma etapa não concede moeda outra vez, mesmo após a saída do relatório dos 20
registros visíveis. Pendências são recuperadas antes de nova tentativa ou exclusão.
Backups terminais não reabrem tentativas encerradas. Reabrir resultados, exportar,
consultar histórico e repetir a gravação não executam uma nova liquidação.

## Exclusão e reutilização de personagens

Excluir na seleção de saves reseta somente o perfil pessoal, após confirmação.
O saldo de Almas e o acesso lendário permanecem compartilhados. Relatórios já
creditados não concedem nem devolvem moeda ao excluir o personagem.

Perfis e relatórios novos incluem um characterId. O histórico consultado por R
filtra slot e identidade, evitando mostrar as tentativas do personagem excluído
quando aquele slot é reutilizado. Os relatórios antigos podem permanecer entre os
20 registros globais até serem substituídos, mas não entram na consulta do novo
personagem. Criar outro personagem no slot vazio gera outra identidade.

O campo é opcional no formato versão 1: perfis e relatórios anteriores usam a
identidade legada vazia e preservam sua consulta; antes de gravar o primeiro
checkpoint, o perfil legado recebe uma identidade persistente. Cópias e compras
conservam a identidade. Backups anteriores do
perfil pessoal são invalidados no reset; os demais arquivos não são modificados.

## Responsabilidades e verificação

RunStatistics agrega números por sala e origem, sem guardar inimigos ou eventos
por quadro. RunReport é um snapshot imutável. DamageSink acompanha todas as
fontes reais de dano; CombatHit preserva o nome da origem após remover projéteis.
SharedProgress/SharedProgressService controlam moeda, compra e histórico.
SaveSlots fornece um store compartilhado junto aos oito stores pessoais.

Testes verificam ganhos de dois saves, compra única por 1.000, recarga, moedas
independentes, migração, falhas de leitura/gravação, backup, limites do histórico,
ausência de crédito duplicado, métricas reais e exclusão de overkill/bloqueios.
A verificação gráfica abre as páginas, volta ao resultado/refúgio, consulta uma
tentativa antiga, troca de personagem e recarrega o saldo e os relatórios.
O executável empacotado também renderiza um resumo usando dados em memória.

