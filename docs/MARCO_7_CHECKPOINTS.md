# M7 — Retomada da campanha por checkpoints

Incremento de **29/10/2026**, com checkpoints entre salas e áreas seguras,
conforme a opção confirmada pelo autor. Implementação, testes de lógica,
integração OpenGL e execução nativa do pacote Windows aprovados.

## Uso pelo jogador

- A campanha grava o início de cada sala antes de entrar em combate.
- Recompensa da sala, Ferreiro Errante, Camponesa das Cinzas e escolha de destino
  também são pontos seguros. **Salvar e sair [F5]** grava e volta ao menu.
  Se F5 estiver associado a um controle personalizado, o botão continua disponível
  e o atalho fica desativado para evitar conflitos.
- Selecionar o personagem com tentativa ativa abre a retomada. **Enter** continua,
  **Esc** volta ao menu e **Del** pede confirmação para abandonar.
- Fechar durante o combate retorna ao último checkpoint. A sala e seus inimigos
  são recriados; os ganhos posteriores ao checkpoint precisam ser conquistados novamente.
- Salvar e sair conserva a tentativa aberta. Morte, vitória ou abandono encerram
  a tentativa, recolhem as recompensas e removem sua opção de retomada.
- A demo de cinco minutos mantém seu fluxo próprio e não substitui o checkpoint
  de campanha do personagem.

O refúgio continua guardando compras, missão, arma e eventos pessoais. A retomada
de uma tentativa existente ocorre antes de oferecer outra partida pelo refúgio.
Um checkpoint incompatível com o conteúdo atual bloqueia a continuação e permite
descartar somente a tentativa após confirmação, preservando o progresso permanente.

## Estado preservado

O snapshot inclui profissão/dificuldade, vida e estamina, nível/XP, dádivas e
recargas, ferramenta do Fazendeiro, raridade da arma, ressurreição já consumida,
aliados e suas durações, tempo e estatísticas, metas e salas concluídas, frações
de moedas, variações e decisões de encontros/recompensas já sorteadas.
Os bônus permanentes capturados no começo da tentativa são restaurados uma vez.

Forja e aceitação da missão atualizam o perfil e o checkpoint no mesmo commit.
Fatos pessoais conquistados durante o combate, como semente e limite regional
do Fragmento de Forja, são reconciliados ao retomar. O limite continua sendo
um fragmento por região e personagem, considerando todas as suas tentativas.

Não são guardados o instante exato do combate, posição do jogador, inimigos,
projéteis, perigos ou baús locais. Mapa e entidades são reconstruídos. O RNG
persistido conserva as decisões da tentativa, sem prometer repetição quadro a
quadro dos spawns ou dos movimentos de combate.

## Formato e etapas

CampaignProfile e SharedProgress passam a escrever **versão 2**, com leitura de
arquivos **versão 1**. Não é necessário criar outro personagem. Os arquivos
continuam em `%LOCALAPPDATA%\ParadiseInHell\`; os nomes e os oito slots são mantidos.

RunCheckpoint é um envelope imutável, versão 1, dentro do perfil. Valida UUIDs,
sequência e revisão, profissão/dificuldade e fingerprint da região. Cada etapa
tem condições próprias:

| Etapa | Ponto restaurado |
| --- | --- |
| ROOM_START | Sala ainda não simulada, inclusive sala do Guardião |
| ARENA_REWARD | Recompensa pendente, com as escolhas raras já sorteadas |
| BLACKSMITH | Encontro com o Ferreiro aberto |
| FARMER | Encontro com a Camponesa aberto, incluindo missão aceita |
| ROUTE | Escolha do próximo destino, sem recompensa/encontro pendente |

Pontos com jogador morto ou subida de nível pendente são rejeitados. A entrada
na próxima sala e a criação de sua tela só acontecem depois de gravar o estado.
Uma transição com falha fica pausada e **Enter/S** ou o botão de retry repete
a gravação do estado já decidido. Fechar nessa situação conserva somente o
último checkpoint que chegou a ser gravado.

## Recompensas e interrupções da gravação

Salvar e sair não chama a liquidação da tentativa. No encerramento definitivo:

1. O perfil grava ganhos pessoais, remoção do checkpoint e recibo pendente juntos.
2. O arquivo compartilhado consome o recibo e grava Almas, relatório e maior
   sequência concluída daquele personagem juntos.
3. O perfil confirma que o recibo foi processado.

Essa sequência permite repetir uma etapa após falha ou recarga sem recolher
moedas outra vez. O registro de sequências consumidas é separado do histórico
visual limitado a 20 relatórios. Pendências precisam ser concluídas antes de
iniciar outra tentativa ou excluir o save. **S no refúgio** tenta as gravações
pendentes novamente; a entrada na campanha também retoma essa recuperação.

Os commits terminais espelham perfil/recibo em sua cópia `.bak`, impedindo que
um backup de tentativa ativa a reabra depois da morte. O commit compartilhado
espelha saldo e registro de sequência no mesmo estado. Formatos de versão futura
são preservados; não há tentativa de sobrescrevê-los como arquivos antigos.

## Verificação e próximos passos

**495 testes de lógica passaram, sem falhas, erros ou testes ignorados**: base
anterior de 457 mais nove de estado do jogador, dez de estado da tentativa e
19 de persistência. Cobrem versões antigas/futuras, dados inválidos, recargas,
poderes consumidos, RNG, recibos repetidos, falhas de IO e backups terminais.

**Integração OpenGL aprovada**, com recarga real de JSON e telas nas resoluções
960×540 e 800×600. Verifica todos os pontos seguros, escolhas e benefícios sem
repetição, encontro já sorteado, abandono confirmado, morte, recuperação de
perfil antigo contra o registro compartilhado, falhas de gravação e retry.
F5 remapeado não salva acidentalmente; o botão continua funcionando.
Marcador: `CHECKPOINT_SMOKE_OK`.

**Pacote Windows aprovado** com Java incluído, PATH sem Java externo, diretório
de trabalho externo, saves em memória, áudio OpenAL nativo silenciado e três
modos de exibição. O executável verifica o JSON, desenha as telas de retomada/
erro e liquida a tentativa de diagnóstico. Marcador: `PACKAGED_CHECKPOINT_SMOKE_OK`.

Capturas em `build/smoke/`: `m8-checkpoint-resume`, `m8-checkpoint-route`,
`m8-checkpoint-reward`, `m8-checkpoint-abandon` e `m8-checkpoint-save-error`,
também com sufixo `-800`. A automação não substitui partidas completas nem
garante recuperação de perda física do disco ou interrupção da energia.

Depois da verificação, o próximo incremento continua sendo **balanceamento do
Submundo em partidas completas**: classes novas/evoluídas, três dificuldades,
rotas, duração e economia. Revisar também retomada após sessões longas e uso em
hardware Windows real. M7 e aceite artístico humano do M6 permanecem abertos.

As próximas cutscenes seguem a direção SOTN/Metal Slug, com gatilhos de sala,
estados e eventos de animação/diálogo. A abertura piloto atual é preservada.
Ascensão, efeitos jogáveis da Centelha, Olimpo e Zeus continuam posteriores à
validação da primeira região.

Relacionados: [roteiro](ROTEIRO_DE_MELHORIAS.md),
[balanceamento e saves](BALANCEAMENTO_E_SAVES.md),
[campanha técnica](CAMPANHA_TECNICA.md) e
[cutscenes](CUTSCENES_E_ABERTURA.md).
