# Chefe do submundo — protótipo

O Guardião do Tártaro é o primeiro combate de chefe implementado. Nome, identidade
narrativa, sprite e números são provisórios. Sua função é testar a transição das
hordas para uma luta com padrões legíveis; vencer encerra o trecho atual,
sem concluir o resgate ou a história de Zeus.

## Acesso e regras

No menu, escolha a campanha e seu save; responda ao narrador se for um personagem
novo. Parta do refúgio e cumpra as três
metas do Estige: 40 comuns, 30 rápidos e 8 tanks. Saia com E/Enter, resolva as
dádivas e escolha a sala do chefe, diretamente ou após um combate opcional.
Cinzas oferece um baú de recuperação/moeda; Sombras oferece uma dádiva extra
Rara ou superior. Eventos de hordas são anunciados na escolha de caminho:
[ROTAS_DO_SUBMUNDO.md](ROTAS_DO_SUBMUNDO.md).

A arena é fechada, sem obstáculos interiores. A vida, profissão, arma e dádivas
vêm da mesma tentativa. A arma forjada também é restaurada nas próximas runs;
dádivas continuam temporárias. O chefe tem **500 pontos de vida base**, multiplicados
pela escala linear de tempo total e dificuldade da sala no momento da entrada.
A vitória exige zerar a vida resultante, indicada no HUD.
Tab consulta as dádivas escolhidas e pausa a luta; Esc ou Tab fecha a lista.
Dano e ataques por segundo são consultados em Tab. O HUD conserva barras de
vida/fôlego/XP, nível, dash, vida do chefe/fase e aviso somente durante preparação. Contato corporal e projéteis
consideram a silhueta do quadro desenhado; áreas sinalizadas conservam seus raios.
Não há prazo de vitória ou comando para pular a luta. Esc pausa; M durante a
pausa abandona a tentativa. Ao morrer, a ressurreição disponível restaura metade
da vida e oferece dois segundos de proteção. Sem ela, aparece a derrota.

## Padrões

O primeiro ciclo apresenta marcas → investida → rajada → **Queda do Tártaro**,
intercalados por recuperação. Depois, cada bloco embaralha os quatro ataques,
sem repetir um padrão consecutivamente entre blocos. Cada bloco contém todos
os padrões; o sorteio pode receber uma semente nos testes.

| Padrão | Aviso e esquiva | Fase 1 | Fase 2 |
| --- | --- | --- | --- |
| Marcas do Tártaro | Círculos travam a posição no início do aviso; saia da área antes da explosão | Uma marca, 22 de dano | Duas marcas, 28 de dano |
| Investida | Faixa indica o percurso fixo; saia para o lado | 200 unidades/s, 18 de dano | 275 unidades/s, 24 de dano |
| Rajada | Arco e direção indicam o leque; desvie entre os projéteis | 5 projéteis a 115 unidades/s, 12 de dano | 9 projéteis a 150 unidades/s, 14 de dano |
| Queda do Tártaro | Chefe salta para fora da tela; círculo crescente persegue, trava vermelho no último segundo e explode na aterrissagem | Marcador a 45 unidades/s; 36 de dano | Marcador a 130 unidades/s; caminhada limitada a 24 unidades/s durante o vermelho; 48 de dano |

Os danos da tabela são valores base: todos recebem o multiplicador da sala e
do tempo na entrada, incluindo contato e projéteis. Fórmulas e exemplos:
[BALANCEAMENTO_E_SAVES.md](BALANCEAMENTO_E_SAVES.md).

### Queda do Tártaro

O salto dura **3 segundos** nas duas fases. O círculo começa com diâmetro 16,
o tamanho do jogador, e cresce continuamente até diâmetro 69: **1,5× o tamanho
desenhado do chefe**, de 46 unidades. Nos dois primeiros segundos, acompanha o
jogador com velocidade constante limitada, inclusive durante o dash: não se
teletransporta nem fica preso à posição do personagem. Para em cima do alvo
quando o alcança e respeita os limites de aterrissagem da arena.

No último **1 segundo**, fica vermelho e seu centro para de se mover; o círculo
continua crescendo. Uma linha interna e o HUD indicam a contagem até o impacto.
O chefe aterrissa no centro marcado e causa dano uma única vez em toda a área.
A colisão usa o mesmo raio desenhado, considerando também o corpo do jogador.
A animação de impacto dura 0,45 s, seguida pela recuperação habitual.

Na fase 1, é possível sair andando. Na fase 2, a pressão da queda limita a caminhada
a 24 unidades/s somente durante o aviso vermelho, mantendo o dash e sua estamina.
Para escapar de uma marca centrada no personagem, use **Shift + direção ao surgir
o vermelho**. O Guerreiro deve soltar a defesa para iniciar o dash. A caminhada
volta ao normal na aterrissagem ou ao sair da tela; a penalidade não vai para o save.

Enquanto está no ar, o chefe fica fora do alcance de armas, aura, maldições,
queimadura, choque e aliados. As durações de efeitos continuam passando; aliados
seguem o jogador enquanto aguardam o retorno do chefe. A queda atravessa a defesa
frontal do escudo, como as marcas no chão, mas respeita resistência, Égide e
proteção de ressurreição. A fase do salto é definida no início do ataque.

Projéteis causam 12/14 de dano base nas fases 1/2, capturado ao lançar. Contato causa 10 durante recuperação e ataques
exceto investida/queda, que usam seus próprios impactos, com intervalo próprio;
aproximar durante o aviso não causa dano de contato.
Espada e socos alcançam a borda do corpo do chefe. Resistência, Égide, aura de Poseidon
e cura por abate usam os atributos existentes da tentativa. Feitiços, maldições,
choque, queimadura e aliados também atacam o chefe. Ele não pode ser ressuscitado
ou empurrado. O escudo do Guerreiro bloqueia ataques frontais e pode revidar
quando Raro; marcas no chão e a queda atravessam a defesa. A fúria por farming é limpa ao
avançar e não cresce na luta. Regras: [CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md).

A segunda fase começa com metade da vida ou menos. Os próximos ataques usam
avisos de 0,75 s para marcas, investida e rajada e recuperação de 0,65 s,
contra 0,95 s e 0,95 s da fase inicial. A queda conserva seus 3 s de aviso e 1 s vermelho.
O HUD mostra fase, vida e padrão. O chefe usa temporariamente a animação existente
do atlas, ampliada e tingida. Avisos sonoros, sons de combate e trilha provisória
foram adicionados; arte própria e revisão sonora continuam pendentes.
Regras e origem: [AUDIO_E_FEEDBACK.md](AUDIO_E_FEEDBACK.md).

## Ajuste implementado após os playtests

Em 14/10/2026, o M1 respondeu ao relato de chefe fácil: vida 420 → 500,
recuperação 1,2/0,9 → 0,95/0,65 s, investida 190/250 → 200/275,
rajada 5/7 → 5/9, velocidade 110/135 → 115/150 e dano de projétil
12/12 → 12/14. Marcas, contato e dano da queda conservam seus valores.

Os avisos de preparação não foram encurtados. A queda mantém seu último segundo
vermelho e a pressão com dash preservado. Nova rajada substitui projéteis da
rajada anterior; a queda limpa os projéteis para sua esquiva. O teto é nove ativos.
Cada projétil conserva o dano de sua fase de origem, inclusive ao cruzar 50% da vida.

O relatório R ganhou página por sala de chefe: tempo de fase 1/2, DPS efetivo médio,
vida de entrada, dano recebido e contagem de padrões iniciados. A contagem inclui
preparações interrompidas; não representa acertos. Dados antigos sem a medição
continuam legíveis. O tempo da fase inclui introdução/recuperação, além dos ataques.

Ajuste e verificação: [MARCO_1_LEITURA_E_CHEFE.md](MARCO_1_LEITURA_E_CHEFE.md).
Ainda medir diversão e dificuldade em partidas com classes de curto/longo alcance,
escudo e aliados, com/sem altar, forja, Atena e rotas. Os três modos estão no [M2](MARCO_2_DIFICULDADES_E_ECONOMIA.md), com vida/dano
multiplicados sem encurtar os avisos.

## Verificação e próximo ajuste

Os testes puros cobrem avisos, alvo travado, esquiva, colisão da investida,
limites, segunda fase, expiração de projéteis e fim dos perigos após a derrota.
A queda tem testes de crescimento, perseguição limitada, trava do centro,
aterrissagem única, área de dano, exclusão de alvos no ar e retorno do ciclo.
As oito profissões, inclusive com bônus máximos de velocidade, são verificadas:
a caminhada sob pressão não sai da área e o dash no aviso vermelho permite escapar.
A verificação gráfica testa as oito armas contra o chefe, preservação de poderes,
aura, pausa, redimensionamento, ressurreição e resultados de vitória/derrota.
Também verifica a queda na tela real: chefe fora da arena, arma/aura sem acertar
a posição vazia, movimento contínuo do círculo, pausa do crescimento, trava vermelha,
dano caminhando na fase 2 e esquiva com dash. Capturas ficam em `build/smoke/`.
Metas e vida são aceleradas em parte dessa verificação.

Para avaliar dificuldade, jogar tentativas completas com cada profissão, primeiro
pela rota direta e depois com um combate opcional. Observar se os avisos são
compreensíveis, se a espada consegue atacar e recuar, se o dash ajuda e se a luta
continua interessante sem uma dádiva específica. Registrar duração, vida ao entrar,
nível e poderes escolhidos. Os testes automáticos não comprovam esse balanceamento.

O resultado retorna ao refúgio com Enter e guarda a recompensa em Óbolos.
Refúgio, Caronte, compras, perfil e recompensas de rota já estão implementados.
Novos encontros/desafios e a revisão da travessia seguem o roteiro atualizado.
Consulte [REFUGIO_E_PROGRESSAO.md](REFUGIO_E_PROGRESSAO.md).
