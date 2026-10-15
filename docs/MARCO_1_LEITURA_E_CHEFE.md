# Marco 1 — HUD, áudio visível e Guardião

Implementado em **14/10/2026**, após os playtests do autor.
A base técnica está verificada; a diversão e a dificuldade final precisam de
novas partidas completas. O M2 foi concluído na sequência: [dificuldades e economia](MARCO_2_DIFICULDADES_E_ECONOMIA.md).
O próximo incremento do roteiro é M3.

## HUD e controles

Hordas e chefe compartilham barras compactas de vida, fôlego/estamina e XP,
nível e disponibilidade do dash. A barra de fôlego indica segmentos de custo
do dash e a recarga aparece quando necessária. Defender comunica que é preciso
soltar o escudo antes do dash. XP fracionária e níveis acumulados não são descartados.

Metas comuns/rápidos/tanks têm contagem e progresso compactos. Quando concluídas,
somem e dão lugar à passagem. Um marcador dourado surge em posição livre/conectada;
**E/Enter continua válido de qualquer posição**, sem exigir procurar inimigos
ou alcançar o marcador. A demo conserva seu cronômetro de cinco minutos.

Tab concentra profissão/arma/raridade, vida, dano, ataques/s, movimento atual,
sorte, estamina, nível, XP numérica, tempo, abates, moedas da tentativa, evento
e aliados, além das páginas de dádivas. Congela a simulação e a espera de escolha.
Instruções iniciais ficam oito segundos; interação, recompensa, Fúria e falhas de
gravação são contextuais. No chefe, barra/fase e avisos aparecem quando úteis.

Fontes, sprites e efeitos finais continuam no M3/M6. Esta entrega reduz e organiza
as informações usando os recursos provisórios existentes.

## Carrasco e visibilidade

A área de jogo vem da câmera e seu zoom dentro do viewport. A máscara de opacidade
do quadro é testada contra essa área: margens transparentes não contam como corpo.
Parte visível conta; completamente fora, não. Letterboxing não altera o mundo
visível; redimensionamento/zoom e desenho em pausa revalidam a regra.

Uma nova preparação visível emite BOSS_WARNING associado ao Carrasco.
Sair, morrer ou liberar a tela encerra somente as vozes desse emissor.
Reaparecer não cria um aviso atrasado. A recarga por tipo, prioridade e teto de
oito vozes permanecem globais. Sons do jogador, de outros emissores e música
continuam independentes; o Guardião conserva os avisos da queda quando no ar.
Dano e projéteis do mini-boss continuam sendo simulados fora da tela.

## Ajuste do Guardião

| Parâmetro | Antes | Agora |
| --- | --- | --- |
| Vida base | 420 | 500 |
| Recuperação fase 1 / 2 | 1,2 / 0,9 s | 0,95 / 0,65 s |
| Investida fase 1 / 2 | 190 / 250 | 200 / 275 unidades/s |
| Rajada fase 1 / 2 | 5 / 7 projéteis | 5 / 9 projéteis |
| Velocidade dos projéteis fase 1 / 2 | 110 / 135 | 115 / 150 unidades/s |
| Dano de projétil fase 1 / 2 | 12 / 12 | 12 / 14 |
| Aviso de marcas/investida/rajada | 0,95 / 0,75 s | Mantido |
| Aviso da queda / trava vermelha | 3 / 1 s | Mantido |

O primeiro ciclo ensina marcas, investida, rajada e queda. Depois, blocos
embaralhados apresentam todos os padrões, sem ataques consecutivos iguais.
O sorteio aceita semente nos testes. A fase 2 tem leque de 14° entre projéteis;
a primeira mantém 17°. Não acumulam duas rajadas: teto de nove projéteis ativos.
A queda limpa os projéteis anteriores. Cada projétil guarda o dano da fase
do lançamento, mesmo se o próximo ataque já for de outra fase.

Marcas, contato, área/dano da queda, crescimento, perseguição contínua,
pressão na caminhada e dash mantêm suas regras. Vida/dano seguem o snapshot
da escala de sala e tempo. O objetivo é aumentar pressão com um reforço moderado
da vida; não comprovar um equilíbrio final por simulação.

## Relatório e compatibilidade

R ganhou uma página para cada sala de chefe com métricas:
tempo nas fases 1/2, DPS médio efetivo, vida de entrada, dano recebido e quantidade
de cada padrão iniciado. Tempos incluem introdução, preparação e recuperação.
Contagem de preparações não significa acertos ou ataques concluídos.
O dano continua sendo medido pela vida realmente retirada, por origem.

RunStatistics observa somente o modelo sem recursos gráficos e copia os dados
ao encerrar sala/run, liberando a referência. BossFightSummary é imutável e
limitado aos quatro padrões. Room recebe bossFight opcional no JSON versão 1.
Históricos antigos, inclusive as antigas três escolhas de Atena, permanecem
legíveis; Almas, personagens e preferências não são recalculados.

## Verificação da implementação

- 211 testes de regras aprovados, incluindo XP fracionária/overflow, disponibilidade
  do dash, transparência/visibilidade, vozes por emissor, ordem justa/reproduzível,
  recuperação/avisos, dano capturado no lançamento e relatório antigo/novo.
- Verificação com OpenGL real aprovada: HUD, Tab, barra de XP, passagem acessível,
  Carrasco invisível/visível/retorno, parada seletiva, bordas, zoom, letterboxing,
  chefe e página de métricas.
- Capturas revisadas em build/smoke/m1-*.png.
- Distribuição Windows validada com runtime incluído, diretório de trabalho externo
  e diagnóstico nativo de áudio: 24 efeitos e música em streaming via OpenAL nulo.
  Esse teste não substitui ouvir o jogo em hardware real.

## Roteiro para as próximas partidas

Jogar primeiro uma rota direta e uma opcional. Comparar profissão de alcance
curto, distância, Guerreiro e Necromante, com personagem novo/evoluído,
com/sem forja e Atena. Registrar se as duas fases aparecem, quais padrões são
difíceis de ler, quanto dura a luta e se alguma combinação exige um poder específico.

Avaliar também se metas/saída/dash ficam claros com o HUD reduzido, se Tab contém
os detalhes esperados e se sons do Carrasco param quando ele desaparece.
Usar os relatórios para os números do M2, sem encurtar o segundo vermelho da queda.

Referências: [roteiro](ROTEIRO_DE_MELHORIAS.md), [chefe](CHEFE_DO_SUBMUNDO.md),
[balanceamento](BALANCEAMENTO_E_SAVES.md), [áudio](AUDIO_E_FEEDBACK.md),
[Almas e relatórios](ALMAS_E_RELATORIOS.md).

