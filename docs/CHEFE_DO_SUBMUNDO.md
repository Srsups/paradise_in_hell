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

A arena é fechada, sem obstáculos interiores. A vida, profissão, arma e dádivas
vêm da mesma tentativa. O chefe tem **420 pontos de vida base**, multiplicados
pela escala linear de tempo total e dificuldade da sala no momento da entrada.
A vitória exige zerar a vida resultante, indicada no HUD.
Não há prazo de vitória ou comando para pular a luta. Esc pausa; M durante a
pausa abandona a tentativa. Ao morrer, a ressurreição disponível restaura metade
da vida e oferece dois segundos de proteção. Sem ela, aparece a derrota.

## Padrões

Os ataques seguem marcas → investida → rajada, intercalados por recuperação.

| Padrão | Aviso e esquiva | Fase 1 | Fase 2 |
| --- | --- | --- | --- |
| Marcas do Tártaro | Círculos travam a posição no início do aviso; saia da área antes da explosão | Uma marca, 22 de dano | Duas marcas, 28 de dano |
| Investida | Faixa indica o percurso fixo; saia para o lado | 190 unidades/s, 18 de dano | 250 unidades/s, 24 de dano |
| Rajada | Arco e direção indicam o leque; desvie entre os projéteis | 5 projéteis a 110 unidades/s | 7 projéteis a 135 unidades/s |

Os danos da tabela são valores base: todos recebem o multiplicador da sala e
do tempo na entrada, incluindo contato e projéteis. Fórmulas e exemplos:
[BALANCEAMENTO_E_SAVES.md](BALANCEAMENTO_E_SAVES.md).

Projéteis causam 12 de dano base. Contato causa 10 durante ataques e recuperação,
com intervalo próprio; aproximar durante o aviso não causa dano de contato.
Espada e socos alcançam a borda do corpo do chefe. Resistência, Égide, aura de Poseidon
e cura por abate usam os atributos existentes da tentativa.

A segunda fase começa com metade da vida ou menos. Os próximos ataques usam
avisos de 0,75 s e recuperação de 0,9 s, contra 0,95 s e 1,2 s da fase inicial.
O HUD mostra fase, vida e padrão. O chefe usa temporariamente a animação existente
do atlas, ampliada e tingida; arte própria e feedback de som continuam pendentes.

## Verificação e próximo ajuste

Os testes puros cobrem avisos, alvo travado, esquiva, colisão da investida,
limites, segunda fase, expiração de projéteis e fim dos perigos após a derrota.
A verificação gráfica testa as quatro armas contra o chefe, preservação de poderes,
aura, pausa, redimensionamento, ressurreição e resultados de vitória/derrota.
Metas e vida são aceleradas em parte dessa verificação.

Para avaliar dificuldade, jogar tentativas completas com cada profissão, primeiro
pela rota direta e depois com um combate opcional. Observar se os avisos são
compreensíveis, se a espada consegue atacar e recuar, se o dash ajuda e se a luta
continua interessante sem uma dádiva específica. Registrar duração, vida ao entrar,
nível e poderes escolhidos. Os testes automáticos não comprovam esse balanceamento.

O resultado retorna ao refúgio com Enter e guarda a recompensa em Óbolos.
Refúgio, Caronte, compras e perfil já estão implementados; outros encontros e
recompensas de rota vêm depois dos playtests. Consulte [REFUGIO_E_PROGRESSAO.md](REFUGIO_E_PROGRESSAO.md).
