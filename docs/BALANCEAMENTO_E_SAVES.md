# Atributos, dificuldade e saves

Revisão do protótipo em 09/10/2026. Os números abaixo são provisórios para
playtests; testes automáticos verificam regras, sem comprovar equilíbrio ou diversão.

## Profissão fixa por personagem

O menu oferece quatro saves. Em um save vazio, o narrador pergunta a profissão.
Depois de escolher, o personagem mantém essa profissão entre tentativas e sessões.
Para experimentar outra, crie outro personagem em um save vazio. Não há exclusão
de saves pela interface neste incremento.

No Windows, os arquivos ficam em `%LOCALAPPDATA%\ParadiseInHell\`:

| Save | Arquivo | Cópia anterior |
| --- | --- | --- |
| 1 | profile.json — preserva o perfil anterior | profile.json.bak |
| 2 | profile-2.json | profile-2.json.bak |
| 3 | profile-3.json | profile-3.json.bak |
| 4 | profile-4.json | profile-4.json.bak |

Saldo, compras, eventos e estatísticas são independentes. A demo usa a profissão
do save escolhido, com seus atributos base, sem compras ou recompensas da campanha.
Selecionar um personagem pela primeira vez também salva sua profissão na demo.

## Bases e bônus percentuais

| Profissão | Vida | Dano | Intervalo de ataque | Velocidade | Velocidade do dash |
| --- | --- | --- | --- | --- | --- |
| Caçador / arco | 100 | 5 | 0,65 s | 60 | 200 |
| Ferreiro / espada | 130 | 8 | 0,60 s | 54 | 180 |
| Vigilante / pistola | 85 | 2 | 0,25 s | 66 | 220 |
| Apostador / socos | 110 | 4 | 0,30 s | 62 | 205 |

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

Os percentuais se somam sobre a base da profissão, sem multiplicar os bônus
anteriores. Uma dádiva de força aumenta o dano do arco de 5 para 6 e o da pistola
de 2 para 2,4: ambos recebem 20%. Uma compra de força e uma dádiva dão +25% no total.
Dano e vida mantêm frações; a interface mostra até uma casa decimal.

Cadência calcula intervalo = intervaloBase / (1 + 0,12 × níveis), respeitando o
intervalo mínimo da arma. Cura imediata, cura por abate, XP e poderes como a aura
mantêm suas regras próprias; não são bônus ao atributo base da arma.

## Arma rara

Um Fragmento de Forja transforma a arma comum em rara: ×1,25 no dano base e
×1,10 na taxa de ataque. Dádivas e compras continuam percentuais sobre a nova base,
sem depender da ordem: dano = danoBase × raridade × (1 + bônus).
Intervalo = intervaloBase / ((1 + bônusCadência) × taxaRaridade), respeitando o mínimo.

Flecha rara atravessa mais um alvo; pistola ricocheteia uma vez em um alvo próximo
com caminho livre; espada mantém o cone e ganha alcance de 36 para 48.
Socos ganham alcance de 24 para 34, continuando em um alvo.
A forja ocorre uma vez por tentativa nesta versão. Material e limite de uma
unidade por região persistem no save; gastar não reabre o drop. Raridade reinicia. Regras: [FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md).

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
