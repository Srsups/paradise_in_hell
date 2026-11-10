# M7 — Primeira rodada de balanceamento do Submundo

Incremento de **04/11/2026**. A primeira região passou a ter uma base técnica
reproduzível e exportação dos relatórios para orientar partidas completas.
O lote corrige uma regressão matemática da varinha; o equilíbrio entre profissões,
a duração ideal e a economia continuam sujeitos aos playtests.

## Correção da Feiticeira

A explosão secundária da varinha tinha recarga de 0,8 s, maior que o intervalo
inicial de 0,75 s. Ao acelerar os disparos, algumas cadências passavam a alternar
entre dois e três tiros para ativar a explosão. Assim, melhorar a cadência podia
reduzir o dano de área em determinadas faixas.

`PlayerState.trySpell()` limita a recarga ao intervalo atual do ataque. A explosão
passa a causar `getDano() / 6` em cada vizinho válido, conservando raio e linha
de visão. A varinha Comum inicial produz 1 de dano secundário por disparo,
ou aproximadamente 1,33 DPS por vizinho em acertos contínuos. Esse é o mesmo
DPS secundário médio da regra anterior na base, agora com progressão estável.
Força e melhoria permanente da arma aumentam o dano por impacto; Cadência
aumenta sua frequência. A varinha Rara continua ampliando o raio para 32.

Essa normalização também reduz o crescimento excessivo em grupos da regra
anterior, que usava uma fração do DPS como dano por ativação. Portanto, há uma
mudança de potência em builds aceleradas, além da correção da recarga. No cenário
de arma Rara sem altar, o DPS secundário ideal por vizinho muda aproximadamente:

| Cadência temporária | Regra anterior | Regra atual |
| --- | ---: | ---: |
| Sem Cadência | 2,017 | 1,833 |
| Nível 5 | 5,163 | 2,933 |
| Nível 6 | 3,977 | 3,153 |

A queda anterior do nível 5 para o 6 desaparece; a contribuição de dano em grupos
dessas builds fica menor. Isso precisa de revisão em partidas, sobretudo na
Feiticeira com arma Rara. O dano direto e os raios de explosão não mudam.

O efeito não acrescenta esse dano ao alvo diretamente atingido. O alvo vizinho
precisa estar no raio e com linha de visão. Mira, obstáculos e a presença de
alvos agrupados continuam influenciando o resultado em uma partida.

## Baseline controlado para desenvolvimento

Executar a partir da raiz do projeto:

```powershell
.\gradlew.bat core:balanceBaseline
```

São gerados em `build/balance-baseline/`:

| Arquivo | Conteúdo |
| --- | --- |
| `classes.csv` | Armas, atributos, dano direto e métricas especiais em cenários controlados |
| `hordes.csv` | Piso ideal de fornecimento das metas por sala e variação |
| `README.md` | Condições da medição, limites e orientação para as partidas |

O comparativo de classes tem 360 combinações: nove profissões e dez armas
(as duas ferramentas do Fazendeiro), personagem novo/evoluído, três dificuldades,
três inimigos e tempos de combate de 0/120 s. O perfil `NEW` não tem altar,
forja ou dádivas. `DEVELOPED_R3_RARE` usa nível 3 nos seis atributos permanentes
e arma Rara, sem dádivas. Esse segundo perfil é um cenário de comparação;
não pressupõe que a forja seja obtida facilmente em uma tentativa.

O tempo ideal de eliminação considera o primeiro acerto em t=0, acerto garantido
e o intervalo real de `PlayerState`. Não inclui voo, deslocamento, mira,
obstáculos, ameaça recebida, XP ou acertos sob pressão. Maldição, explosão,
alcance/ângulo do corpo a corpo, defesa e chance de ressuscitar aparecem
separadamente. DPS direto não constitui uma classificação das profissões.

O fornecimento de hordas usa `EnemyWaves` e os métodos reais de tempo/metas de
`RunSession`: 1.000 sementes fixas por sala/variação, atualização a 60 Hz e morte
imediata dos inimigos gerados. Cada sala é isolada com tempo total inicial zero,
conservando seu deslocamento de dificuldade e multiplicador de intervalo.
Arqueiros usam a conversão e os limites reais; continuam contando como comuns.
Todas as criaturas da última onda entram na conta, inclusive as excedentes à meta.

As colunas média/p50/p90 medem **disponibilidade de inimigos**, com percentil pelo
método de posto mais próximo. Não são tempos esperados de conclusão por jogadores.
Não há Carrasco, baú, projétil, deslocamento, dano sofrido, Fúria ou luta do chefe.
XP/Almas são potencial base fornecido antes dos multiplicadores de dificuldade.
O fornecimento ideal é igual nos três modos porque dano/vida não atrasam abates
instantâneos; as três dificuldades estão presentes no comparativo das classes.

### Primeira medição do fornecimento

Resultados de 04/11/2026, com 1.000 sementes por linha. Tempos em segundos;
excedentes são a média de abates além da soma das três metas da sala, incluindo
os gerados na última onda. A última coluna mostra somente XP base potencial.

| Sala | Variação | p50 até todas as metas | p90 até todas as metas | Abates excedentes médios | XP base potencial média |
| --- | --- | ---: | ---: | ---: | ---: |
| Estige | Hordas mistas | 94,86 | 103,42 | 15,53 | 199,85 |
| Estige | Caçada veloz | 108,21 | 120,88 | 37,57 | 257,96 |
| Estige | Marcha pesada | 112,89 | 126,41 | 47,64 | 351,75 |
| Cinzas | Hordas mistas | 43,98 | 52,83 | 11,74 | 116,58 |
| Cinzas | Caçada veloz | 54,28 | 64,15 | 25,54 | 153,53 |
| Cinzas | Marcha pesada | 55,72 | 65,53 | 28,65 | 201,44 |
| Sombras | Hordas mistas | 30,42 | 36,18 | 25,22 | 212,51 |
| Sombras | Caçada veloz | 29,58 | 34,55 | 22,54 | 221,35 |
| Sombras | Marcha pesada | 40,20 | 48,75 | 62,57 | 391,59 |

A composição altera a espera pelas metas mesmo quando todos morrem
instantaneamente. No Estige com Caçada veloz, a meta de rápidos chega na mediana
em 74,15 s, mas a de comuns apenas em 108,21 s. Na Marcha pesada, a meta de tanks
chega em 35,43 s e a de rápidos em 111,73 s. Portanto, a sensação de repetir um
tipo de inimigo pode vir da espera pelo outro tipo, além da pressão de combate.
As metas individuais são medianas separadas; não se deve somá-las.

Sombras fornece as mesmas metas do Estige mais cedo por começar com hordas
maiores e intervalo reduzido; isso não demonstra que seja uma rota mais fácil.
Marcha pesada também fornece mais XP potencial e muitos tanks excedentes, cujo
custo real de eliminação não está representado no piso. Comparar no playtest
tempo até a passagem, vida perdida e recompensa realmente coletada antes de
alterar pesos ou metas. Este lote não aplica outro enfraquecimento genérico dos
rápidos, outra alteração de pesos ou mudança de recompensas a partir dessa tabela.

Os rápidos já alternam 1 s a velocidade 70 e 1 s a 24, com limite de 24 vivos e
dois por onda. A média de 47 em perseguição livre fica abaixo das bases de
caminhada, mas não simula obstáculos, cerco, defesa ou decisões de dash.
O piso de fornecimento também não mede acúmulo: todos os inimigos somem ao nascer.

Uma análise opcional lê um histórico local sem modificá-lo:

```powershell
.\gradlew.bat core:balanceBaseline -PbalanceHistoryFile="C:\caminho\shared-progress.json"
```

Os 17 relatórios locais disponíveis na revisão inicial são da dificuldade Média,
sendo 16 da campanha e um da demo. Há oito mortes de campanha; a causa final
registrada é contato dos rápidos em cinco, tank em uma, comum em uma e marca
do chefe em uma. Isso prioriza observar os rápidos na nova rodada, mas não mede
a participação deles em todo o dano nem estabelece causalidade por dificuldade.
Eles não guardam data, revisão do balanceamento, atributos permanentes iniciais
nem variação da sala. Misturam builds e experiência do jogador; não demonstram
que uma profissão domina outra. Abandonos devem ficar separados de mortes.

## Exportar resultados pelo jogo

**R** no resultado/refúgio abre o relatório. **E** ou **Exportar histórico** cria
uma pasta local em `%LOCALAPPDATA%\ParadiseInHell\exports\run-history-<id>\` com:

- `history.json`: cópia dos relatórios disponíveis e condições da exportação;
- `attempts.csv`: uma linha por tentativa, com rota, dano efetivo e moedas por minuto;
- `rooms.csv`: uma linha por sala, incluindo tempo até a meta, tempo após liberá-la
  e fases/padrões iniciados do Guardião, quando registrados.

A exportação inclui até 20 registros globais de todos os personagens e o
relatório aberto se ele já não estiver nesse conjunto. Pode incluir demos e
abandonos, identificados nas linhas. Não é o total de toda a vida do jogador.
Campos históricos ausentes ficam vazios; não são preenchidos por inferência.
Exportar não concede recompensas, não liquida tentativas e não modifica os saves.
Cada operação cria uma pasta própria e mostra sucesso/erro na tela.

## Rodada humana inicial

Planejar **18 partidas completas na dificuldade Média**: uma com personagem novo
e outra com evoluído para cada uma das nove profissões. Usar a mesma rota entre
as duas observações de cada profissão. Dar prioridade às classes já disponíveis;
Necromante/Fazendeiro pendentes podem ser avaliados depois de seus desbloqueios.
Não apagar o personagem principal para realizar essa coleta: usar slots de teste
disponíveis e registrar o que ainda não pôde ser comparado.

O perfil evoluído do baseline é uma referência controlada. Nas partidas, anotar
os níveis reais dos seis atributos e a raridade inicial da arma; uma comparação
com outras bases deve ser identificada como tal. Não exigir obtenção aleatória
da forja somente para completar uma linha da planilha.

Registrar junto à exportação:

| Observação manual | Pergunta orientadora |
| --- | --- |
| Bases iniciais, arma e experiência na profissão | A diferença veio do personagem ou do domínio do jogador? |
| Variação e rota escolhidas | Faltou algum tipo para liberar a passagem? |
| Rápidos | Houve janela clara para recuperar espaço sem dádiva de velocidade? |
| Dádivas e escolha de XP | As opções mudaram decisões ou alguma foi sempre descartada? |
| Chefe | Quais avisos/ataques causaram dano; foi possível entender o erro? |
| Progressão | A próxima compra pareceu útil e o custo adequado ao retorno? |
| Retomada e apresentação | Checkpoints, texto e HUD mantiveram a leitura da tentativa? |

Os relatórios fornecem duração por sala, vida de entrada/saída, dano efetivo,
momento da passagem, resultado do chefe e moedas. A causa final de morte não é
uma contagem de todos os acertos de um inimigo. Consultar o tempo após liberar
a passagem para separar conclusão e permanência opcional.

Depois, repetir os problemas observados em Fácil/Difícil e nas rotas Cinzas/
Sombras. Alterar uma hipótese por rodada, registrar motivo e efeito esperado,
comparar novamente e manter os dados anteriores. A meta continua incluindo
feedback de pelo menos cinco pessoas externas ao projeto.

## Verificação e próximos passos

**515 testes de lógica passaram, sem falhas, erros ou testes ignorados**: base
anterior de 495 mais cinco de regressão da varinha, cinco de classes, três de
fornecimento e sete de exportação. O baseline foi gerado com 360 linhas de
classes e 9.000 amostras de fornecimento de hordas, além do cabeçalho dos CSVs.

**Integração OpenGL aprovada**: exportação protegida por tecla/botão, histórico
global de duas profissões e relatório fora dos 20 registros, recarga do JSON,
falha/retry em memória, navegação e ausência de alterações nos saves/moedas.
A página de dano agora usa duas colunas para não invadir o rodapé. Capturas
em `build/smoke/m7-balance-export`, `m7-balance-export-error` e
`m7-balance-damage`, também em 800×600 com sufixo `-800`.
Marcador: `BALANCE_REPORT_EXPORT_OK`.

**Pacote Windows aprovado** com Java incluído, diretório de trabalho fora da
instalação, PATH sem Java externo, áudio nativo silenciado e três modos de
exibição. A exportação do diagnóstico usa somente memória; os arquivos reais
do jogador não são alterados. Marcador: `PACKAGED_REPORT_EXPORT_SMOKE_OK`.
Essa validação verifica regras e execução; não encerra os playtests nem o M7.

O próximo incremento depende das partidas: pressão dos rápidos, profissões,
Guardião, duração das rotas e economia. Depois entram estabilidade em sessões
longas/hardware Windows real, variedade entre tentativas e o sistema futuro de
cutscenes SOTN/Metal Slug, preservando a abertura atual. Ascensão/Centelha,
Olimpo e Zeus continuam posteriores à validação da primeira região.

Relacionados: [balanceamento e saves](BALANCEAMENTO_E_SAVES.md),
[Almas e relatórios](ALMAS_E_RELATORIOS.md), [roteiro](ROTEIRO_DE_MELHORIAS.md) e
[cutscenes](CUTSCENES_E_ABERTURA.md).
