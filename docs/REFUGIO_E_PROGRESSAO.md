# Refúgio, atributos permanentes e baús

Este incremento fecha o ciclo inicial da campanha: preparar no refúgio, jogar,
voltar após morte/vitória/abandono e começar de novo. O autor confirmou compras
permanentes e baús aleatórios. Óbolos, fontes, custos e números são provisórios.

## Refúgio e Caronte

Escolha um dos quatro saves antes de começar. Em um save vazio, o narrador pergunta
a profissão e abre o Refúgio do Estige. A profissão fica fixa: outra exige um
personagem em outro save vazio. O perfil anterior é preservado no Save 1.
C conversa, L abre o altar, P abre os saves e Enter parte. E interage perto dos
locais; as mesmas ações têm botões. O refúgio é seguro e tem movimento; conversas
e loja ficam em painéis.

Caronte explica a maldição e a diferença entre dádivas e compras. Concluir sua
primeira conversa entrega cinco Óbolos uma única vez. Ele oferece a conversa do
suborno após uma morte e comenta a primeira vitória contra o Guardião. Concluir
essas conversas registra eventos; cancelar permite retomá-las depois.
As confirmações de conversa e compra desaparecem após 3,5 segundos; mensagens
sobre falhas de gravação permanecem até a resolução. Textos originais provisórios
estão em CaronteDialogue.java.

No resultado da campanha, Enter retorna ao refúgio. M durante a pausa abandona
a tentativa e retorna diretamente. O retorno guarda moedas e incrementa as
estatísticas uma vez. Abandonar não é contado como morte. A demo mantém seu
reinício próprio, usa a profissão do save escolhido e não registra ganhos da
campanha. Sua escolha inicial de profissão também fica salva.

## Reinício e loja

| Estado | Próxima arena na mesma tentativa | Nova tentativa |
| --- | --- | --- |
| Dádivas, aura, Égide e ressurreição | Mantidos, incluindo consumo/recargas | Apagados |
| Nível, XP, vida e estamina | Mantidos | Nível 1, zero XP, vida e estamina cheias |
| Atributos comprados | Mantidos | Aplicados aos atributos iniciais |
| Profissão | Mantida | Mantida, fixa para o save |
| Baús | Locais à arena | Novos sorteios |
| Raridade da arma | Mantida | Comum |
| Fragmentos de Forja | Guardados imediatamente no perfil | Mantidos no save |
| Região que já concedeu fragmento | Registrada no perfil | Continua bloqueada mesmo após gastar |
| Óbolos da travessia | Acumulados | Guardados no perfil ao retornar |
| Eventos de Caronte e saldo guardado | Mantidos no perfil | Mantidos, inclusive ao fechar |

Vida máxima dá +10% da vida base por nível; força dá +5% do dano base da arma;
agilidade dá +3% da velocidade base. Cada profissão tem bases diferentes; os
percentuais se somam, sem multiplicar compras anteriores, e o dano mantém frações.
Cada atributo tem até cinco níveis. Custos por nível: 5, 9, 13, 17,
21 Óbolos. São bônus cumulativos, limitados. Cada tentativa recebe um snapshot das
compras; não mistura esses níveis com os limites das dádivas.

Uma compra só é aplicada após a gravação do perfil. Falhar ao salvar não consome
moedas nem sobe o nível. A interface informa saldo, nível atual, preço e resultado.

## Economia provisória

- 5 Óbolos ao concluir a primeira conversa com Caronte, uma vez por perfil.
- 1 por 10 abates, até 10 por tentativa; o limite não afeta a geração de inimigos.
- Mais 10 ao derrotar o Guardião.
- 3 em um baú com esse prêmio.

Os ganhos da travessia são guardados ao retornar, mesmo por morte ou abandono.
Morrer imediatamente sem abates ou baús não concede moeda. Fechar durante a
travessia perde os ganhos não guardados. Ajustar a economia com partidas completas;
o perfil atual não oferece bônus ilimitados ou lojas dentro do combate.

## Baús

O primeiro sorteio ocorre após 18–30 segundos de simulação na arena; os próximos
a cada 25–40 segundos. Há até três baús simultâneos por arena. Posições são
sorteadas perto do jogador e precisam ser livres e conectadas ao seu caminho.
Baús distantes são retirados e não bloqueiam movimentos.

Use F dentro de 24 unidades para abrir uma vez. Prêmios: 30 de vida, 12 XP,
40 de estamina ou 3 Óbolos, com chances iguais na campanha. A demo usa os três
suprimentos. Vida/estamina cheias convertem o prêmio em XP. Recompensas não
ultrapassam os máximos de vida/estamina; XP abre as escolhas normais de dádivas.
Pausa e escolha de dádiva congelam o relógio dos baús. Não há baús na sala do chefe.

## Arquivo e verificação

No Windows, os quatro arquivos ficam em `%LOCALAPPDATA%\ParadiseInHell\`:
`profile.json`, `profile-2.json`, `profile-3.json` e `profile-4.json`. Cada um guarda versão,
saldo, profissão, níveis permanentes, retornos, mortes, vitórias, eventos e
Fragmentos de Forja e regiões já premiadas próprios. Saves anteriores sem
forgeMaterials leem zero; a migração do limite regional está em
[FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md).
Não guarda a tentativa em andamento. A cópia anterior válida usa o sufixo `.bak`
do respectivo arquivo. Principal corrompido pode recuperar essa cópia, que pode
estar um salvamento atrás. Perfil de versão desconhecida é preservado.

Uma falha de autosave mantém as moedas na memória e permite S no refúgio para
repetir a gravação. Falha de leitura mantém os arquivos intactos e permite jogar
na sessão; compras permanentes ficam indisponíveis. Os testes e o executável
com --smoke-test usam um perfil em memória e não alteram o perfil real.

Testes cobrem saves independentes, profissão fixa, bônus percentuais, custos,
limites, retorno único, reset de dádivas, persistência,
cópia anterior, versão desconhecida, falhas de gravação, alcance e prêmios dos baús.
A verificação OpenGL percorre diálogos, compra dois atributos, volta após morte
e abandono, recarrega o perfil, verifica a expiração da mensagem de Caronte
e abre um baú real. Tabelas de atributos e ímã:
[BALANCEAMENTO_E_SAVES.md](BALANCEAMENTO_E_SAVES.md). Os testes aceleram estados;
partidas completas ainda precisam validar ritmo e economia.

O segundo NPC implementado é o Ferreiro Errante, encontro raro e seguro entre
salas. Fragmentos de Forja persistem; raridade/poder da arma não. Confira
[FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md). Outros encontros,
inventário geral e recompensas exclusivas de rota continuam pendentes. A primeira região completa ainda precisa desse
conteúdo, áudio, arte e playtests.
