# Paradise in Hell

Jogo 2D de sobrevivência de hordas inspirado na mitologia grega, feito em Java e libGDX.
Você escolhe uma profissão, mira com o mouse e combina arma e dádivas para atravessar o submundo.

**Estado: demo 0.1.0 e protótipo inicial de campanha para Windows, em desenvolvimento.** A demo termina após cinco
minutos de sobrevivência ou com a morte do jogador. Já inclui áudio e efeitos provisórios;
ainda precisa de balanceamento com jogadores, revisão sonora e artística antes da publicação.
O plano de conclusão está em [docs/PLANO_DE_LANCAMENTO.md](docs/PLANO_DE_LANCAMENTO.md).

O feedback dos playtests de 13/10/2026 foi consolidado no
[roteiro de melhorias](docs/ROTEIRO_DE_MELHORIAS.md). O **marco 1 está implementado**:
HUD essencial com barra de XP, áudio do Carrasco conforme visibilidade e Guardião
mais exigente. Detalhes: [docs/MARCO_1_LEITURA_E_CHEFE.md](docs/MARCO_1_LEITURA_E_CHEFE.md).
O **marco 2 também está implementado**: Fácil, Média e Difícil ficam fixas na criação
do personagem. Saves antigos assumem Média; inimigos e moedas usam multiplicadores
por modo. Detalhes: [docs/MARCO_2_DIFICULDADES_E_ECONOMIA.md](docs/MARCO_2_DIFICULDADES_E_ECONOMIA.md).
O **M3 e a expansão visual do elenco atual estão implementados**: fontes
Noto Sans/Cinzel, oito profissões com armas próprias, seis visuais de inimigos,
baús animados, retrato do protagonista e poderes em sprites. A caminhada tem
oito direções a partir de cinco vistas desenhadas e três espelhadas, seis posições
na caminhada e transição curta ao virar. As oito profissões têm respiração em repouso,
inclusive na seleção de classe. Caronte, altar e travessia têm sprites animadas no refúgio,
onde os ataques ficam desativados. As armas
fazem parte dos quadros de caminhada/ataque; seus ícones isolados ficam na forja.
A escolha de dádiva cobre a tela inteira e cada uma das 15 dádivas tem seu ícone.
Caronte e protagonista usam retratos no diálogo: Enter/E/clique revela ou avança
uma fala; T alterna texto gradual/instantâneo.
[Entrega inicial](docs/EXPANSAO_VISUAL.md) e
[revisão de armas e ataques](docs/ANIMACOES_E_EQUIPAMENTOS.md) e
[polimento de movimento e refúgio](docs/POLIMENTO_VISUAL_E_REFUGIO.md).
Arqueiro, Carrasco e Guardião têm poses próprias de ataque; avisos usam imagens
animadas. O Guardião respira na espera, corre, dá ombrada e aterrissa apoiado no punho.

Parte do M6 foi antecipada para esta revisão artística. Próximo: novas dádivas e
altar (M4), Fazendeiro Épico por desafio de NPC (M5) e acabamento do Submundo.
Poses de dano/morte e arte dos próximos conteúdos continuam planejadas.

## Direção da versão completa

A campanha planejada acompanha um mortal comum tentando resgatar a esposa,
raptada por Zeus. A jornada passa pelo submundo até chegar ao Olimpo, com humor,
NPCs, itens, escolhas de poderes e chefes.

Cada tentativa bem-sucedida deve durar aproximadamente **30–40 minutos**.
A morte reinicia a travessia, preservando avanços de história e desbloqueios.
Os retornos são explicados por uma maldição de Zeus: o castigo que deveria
prolongar o sofrimento do protagonista acaba lhe dando novas chances de enfrentá-lo.
Derrotar Zeus conclui o objetivo principal da campanha.

As decisões confirmadas, propostas e o próximo marco — uma região completa —
estão em [docs/DESIGN_DO_JOGO.md](docs/DESIGN_DO_JOGO.md).

## O que existe na demo

- Movimento com WASD ou setas, dash, estamina e exaustão.
- Mira manual e ataques contínuos enquanto o botão esquerdo é pressionado.
- Pergunta do narrador e oito profissões: Caçador, Ferreiro, Vigilante, Apostador,
  Feiticeira, Sacerdote, Necromante lendário e Guerreiro com escudo.
- Cinco etapas de dificuldade, com inimigos comuns, rápidos e resistentes.
- Quinze tipos de dádivas: dano, cadência, vida, velocidade, resistência, experiência,
  cura, cura por abate, Égide, aura de Poseidon, ressurreição, ímã de XP, choque, queimadura e impacto.
- Escolhas ponderadas ao subir de nível, com dádivas Comuns, Raras, Épicas e
  Mitológicas. A sorte favorece raridades superiores; limites de repetição são respeitados.
- Mapa procedural com obstáculos, área inicial segura e cache limitado.
- XP por inimigo: comum 1, arqueiro 3, rápido 2, tank 5; coleta, números de dano e barras.
- Arqueiro do Estige: mira fixa avisada por 1 segundo, flechas bloqueadas por paredes e defesa frontal.
- Navegação compartilhada para contornar estruturas, com colisão nos quatro
  cantos e verificação do caminho percorrido.
- Pausa, menu, reinício e resultado com tempo, abates e nível.
- Oito visuais de profissões, inimigos distintos e golpes/poderes em sprites.
- Baús com abertura animada, retratos de diálogo e dádivas com ícones em tela inteira.
- Atlas gerados para o projeto; os três placeholders antigos não entram no pacote.

Outras armas/profissões, novos chefes, economia final, identidade sonora final, suporte nativo
a controles e integração Steamworks continuam no roadmap.

## HUD de combate

Vida, fôlego/estamina e XP usam barras compactas, com nível e disponibilidade do dash.
Metas ficam no canto oposto e são substituídas pelo acesso à passagem quando concluídas.
Um marcador dourado indica a saída; E/Enter continua funcionando em qualquer posição.
Durante o chefe, aparecem barra de vida/fase e avisos contextuais. O cronômetro fica
na demo de cinco minutos. Dano, ataques/s, sorte, moedas e detalhes são consultados
em Tab, que pausa a simulação. Instruções iniciais desaparecem após oito segundos.

O Carrasco só inicia um aviso sonoro quando seu corpo aparece na tela, inclusive
parcialmente. Sair da área visível interrompe somente aquela voz; voltar não repete
um aviso antigo. Zoom, letterboxing e máscaras do quadro são considerados.

## Áudio e leitura do combate

**O** abre áudio no menu, no refúgio e nas arenas, preservando a pausa e a run.
Há volumes independentes de efeitos/música, silenciar tudo e teste de som.
Clique/arraste ou use setas; **M** silencia, **T** testa, **Esc/O** volta.
As preferências são compartilhadas, salvas fora da instalação e mantidas ao
excluir personagens. Enquanto o painel está aberto, a simulação fica congelada.

O protótipo tem **24 sons** para armas, impactos, bloqueio, revide, dash, XP e
perigos/resultados, além de **duas trilhas instrumentais** em loop.
Foram sintetizados para o projeto, sem gravações externas. São provisórios.
O mixer limita vozes e prioriza avisos. Rastros/impactos diferenciam armas,
DEFESA e PROTEGIDO distinguem respostas a ataques, e dano real acende uma
borda vermelha breve. Colisão e regras de combate permanecem as mesmas.

Origem, controles e verificações: [docs/AUDIO_E_FEEDBACK.md](docs/AUDIO_E_FEEDBACK.md).

## Protótipo da campanha

No menu, clique em **Campanha: protótipo** ou pressione **C** e escolha um dos
**oito saves**, em duas páginas: **setas ou Anterior/Próxima** trocam a página;
clique ou **1, 2, 3, 4** escolhem o cartão atual. Em um save vazio, o narrador pergunta
sua profissão; essa escolha é definitiva para o personagem. Para jogar com outra
profissão, crie um personagem em outro save vazio. Saldo, compras e eventos são
independentes. O perfil anterior continua disponível no Save 1.
**Excluir** em um cartão pede confirmação e deixa o slot vazio para outro
personagem. **Esc/Cancelar** preserva; **Enter/Excluir personagem** confirma.
Almas e desbloqueio do Necromante permanecem compartilhados. **DEL** alterna
o modo de exclusão por teclado, com 1–4 para o slot da página.
Os cartões de profissão exibem nomes sem prefixos numéricos.

Após escolher ou continuar um personagem, entre no **Refúgio do Estige**.
**C** conversa com Caronte, **L** abre o altar, **P** abre os saves e **Enter**
inicia uma tentativa. Também é possível mover-se e usar **E** perto dos locais.
A demo também permite escolher o save e usa sua profissão, sem aplicar compras
ou registrar recompensas da campanha.

O arco dispara flechas, a espada
atinge uma área curta na direção do mouse e a pistola dispara mais rápido com
dano menor. O **Apostador** luta com socos em um alvo próximo e tem **sorte 4×**.
A segunda página traz **Feiticeira**, com feitiços explosivos; **Sacerdote**, com
maldições contínuas; **Necromante**, com chance de reviver inimigos como aliados;
e **Guerreiro**, com espada e defesa frontal pelo botão direito. Ele pode atacar
com o botão esquerdo enquanto defende, mantendo a recarga, os custos da defesa
e a redução de movimento; solte o escudo para iniciar o dash. O Necromante é
lendário: no altar, **L → N/clique** compra seu desbloqueio por **1.000 Almas**
do saldo compartilhado entre os oito saves, permitindo escolhê-lo ao criar novos personagens.
Almas são obtidas na campanha por derrota: comum 1, arqueiro 3, rápido 2, tank 5,
mini-boss 20 e chefe 100. São guardadas ao retornar, inclusive por morte ou
abandono; a Fúria reduz ganhos por farming. Óbolos e compras continuam individuais.
Desbloqueios anteriores do Necromante são preservados gratuitamente.

**R no resultado ou no refúgio** consulta o resumo da tentativa: duração por sala,
dádivas, dano por origem, causa da morte e moedas. Setas laterais trocam páginas;
acima/baixo consultam tentativas anteriores do personagem. O histórico mantém
as 20 últimas tentativas de todos os saves. Regras:
[docs/ALMAS_E_RELATORIOS.md](docs/ALMAS_E_RELATORIOS.md).

Cada profissão tem bases próprias de vida, velocidade e dano.
A escolha da profissão, o refúgio e **Tab** mostram **ataques por segundo**. Celeridade exibe
a taxa atual e a taxa após a melhoria. O movimento continua com seus próprios bônus.
A maldição do Sacerdote afeta inimigos próximos ao impacto, com dano contínuo
proporcional ao dano e à cadência atuais da arma.
Dádivas de força (+20%), vida (+20%), velocidade (+10%) e cadência (+12%)
somam percentuais sobre essas bases. O dano mantém frações, inclusive na pistola.
Nomes, balanceamento e arte das profissões são provisórios. Tabelas e fórmulas:
[docs/BALANCEAMENTO_E_SAVES.md](docs/BALANCEAMENTO_E_SAVES.md).

Cada nova escolha de dádiva espera **1,5 segundo** e exige soltar o botão de ataque
antes de aceitar um clique, evitando escolhas acidentais durante o combate.
A escolha usa cartões largos em tela inteira, ícones distintos e informação de raridade.
**Tab** consulta as dádivas já escolhidas, com os mesmos ícones, raridade e nível; setas trocam
páginas. A lista pausa o combate e Esc ou Tab a fecha.

As dádivas são classificadas pela força do efeito. No primeiro sorteio com todas
as categorias disponíveis, as chances base são 70% Comum, 23% Rara, 6% Épica e
1% Mitológica. O Apostador favorece as categorias superiores. Números, catálogo
e regras de sorte: [docs/SORTE_E_RARIDADE.md](docs/SORTE_E_RARIDADE.md).

A **Sabedoria de Atena** é Épica e concede +150% de XP coletada (2,5×), uma vez
por tentativa. Afeta a XP recebida após a escolha, incluindo baús, e favorece
o investimento cedo na run. Frações são preservadas e consultadas em Tab; Óbolos e Almas mantêm suas
recompensas próprias. A comparação com o antigo bônus de 20% está no documento
de [balanceamento](docs/BALANCEAMENTO_E_SAVES.md#sabedoria-de-atena-e-a-curva-de-xp).

A **Colheita de Deméter** atrai cristais de XP: até três níveis, cada um adicionando
32 unidades de alcance e 40 de velocidade de atração. É uma dádiva temporária,
preservada entre salas e apagada na próxima tentativa.
Choque, queimadura e impacto têm recargas próprias e combinam com maldição/aura.
Regras das classes, forja e sinergias: [docs/CLASSES_SINERGIAS_E_DESTINO.md](docs/CLASSES_SINERGIAS_E_DESTINO.md).

Contato e acertos de projéteis usam a silhueta visível dos quadros dos inimigos,
acompanhando sua escala e evitando colisão nas margens transparentes. Durante a
transição de direção, a máscara acompanha o quadro predominante; armas e brilhos
não fazem parte do corpo.

Vida e dano dos inimigos crescem linearmente com o tempo total da tentativa e a
dificuldade da sala, incluindo o chefe. Os valores são definidos no nascimento
do inimigo ou na entrada da sala do chefe; inimigos já feridos não recuperam
vida por esse crescimento. Trocar de sala mantém o relógio da dificuldade.

Na Margem do Estige, cumpra **as três metas**: 40 comuns, 30 rápidos e 8 tanks.
O **Arqueiro do Estige** conta como comum e concede **3 XP e 3 Almas** na campanha.
Marca uma posição por **1 segundo** e dispara nela: andar ou dar dash evita a
mira travada. Flechas respeitam paredes e defesa frontal; vida e dano seguem a
escala normal. Só prepara disparos dentro da tela, com poucos arqueiros ativos.
Regras e roteiro de partidas com as oito profissões:
[docs/ARQUEIRO_DO_ESTIGE.md](docs/ARQUEIRO_DO_ESTIGE.md).

Os rápidos alternam **1 s de arrancada** e **1 s de recuperação**, com ciclos
defasados, criando espaço para reposicionar sem depender de Hermes.
Há até **2 rápidos por onda e 24 ativos**; excedentes viram comuns ou tanks,
preservando o tamanho da horda e as metas. Esses valores são iniciais para playtests.

As hordas podem misturar tipos ou reunir inimigos do mesmo tipo. A geração não
tem um orçamento total por fase nem termina pelo relógio. Inimigos desaparecidos
por distância não contam como mortos e não dão XP.

Ao cumprir as metas, a passagem é liberada sem exigir eliminar os inimigos vivos.
As hordas continuam até você pressionar **E ou Enter** para sair. O XP restante é
recolhido e todas as escolhas de nível são resolvidas antes da troca de sala.
A interface não exige localizar inimigos fora da tela e não tem indicadores deles.
Continuar farmando depois de liberar a passagem atrai a **Fúria dos Deuses**:
20 s de tolerância, depois 15 s de aviso, antes de hordas mais fortes e XP reduzida.
Builds acima da referência da sala aceleram a escalada após o aviso. Avançar limpa
essa pressão; uma build forte que segue avançando não é punida.

Escolha ir diretamente à sala do chefe ou passar por um combate opcional nas
Cinzas ou nas Sombras. **Cinzas** tem menos hordas e um baú garantido ao concluir:
recuperar até 40 de vida ou receber 5 Óbolos. **Sombras** tem hordas mais intensas
e oferece uma dádiva extra **Rara ou superior**. As recompensas são escolhidas em
segurança, uma vez por sala. Cada combate tem suas metas, reiniciadas ao entrar.

Cada sala de combate sorteia um evento para a tentativa: hordas mistas, mais rápidos
ou mais tanks. A escolha de caminho mostra o evento e a recompensa antes da entrada;
reabrir a tela mantém o resultado. Regras: [docs/ROTAS_DO_SUBMUNDO.md](docs/ROTAS_DO_SUBMUNDO.md).
Vida, XP, nível, profissão, arma, dádivas, limites, estamina e recargas continuam.
Após morte ou vitória, **Enter no resultado retorna ao refúgio**. Abandonar com
M durante a pausa retorna diretamente. Uma nova tentativa começa com vida cheia,
nível 1 e sem XP ou dádivas; mantém a profissão, os atributos comprados e a arma forjada.

Na sala do chefe, enfrente o **Guardião do Tártaro**: marcas explosivas no chão,
investida com trajetória marcada, rajadas de projéteis e **Queda do Tártaro**.
Na queda, ele salta para fora da tela enquanto um círculo cresce e persegue você
com velocidade limitada. No último segundo, o círculo trava vermelho antes do impacto.
Na fase 2, o marcador persegue mais rápido e a pressão do aviso vermelho reduz a
caminhada: use **Shift + direção ao surgir o vermelho**; o dash mantém sua velocidade.
Os ataques têm avisos visuais e a luta fica mais intensa abaixo de metade da vida. Use a arma escolhida,
suas dádivas e o dash; derrotá-lo encerra o trecho jogável com vitória.
Entrar na sala, esperar ou apertar Enter não concede vitória.

Nome, sprite e valores do chefe são provisórios. As regras e sugestões de teste
estão em [docs/CHEFE_DO_SUBMUNDO.md](docs/CHEFE_DO_SUBMUNDO.md).
Caronte reconhece mortes e a vitória contra o Guardião. Sua primeira conversa
completa entrega 5 **Óbolos**, moeda provisória. No altar, compre até cinco níveis
de vida máxima (+10%), dano (+5%) e velocidade (+3%) sobre as bases da profissão.
Preços: 5, 9, 13, 17 e 21.
Compras, Óbolos, profissão e eventos são salvos por personagem entre sessões.
Almas, acesso ao Necromante e histórico usam `shared-progress.json`, no mesmo diretório.
A confirmação de uma conversa ou compra desaparece após 3,5 segundos.

Durante o combate, baús surgem em locais alcançáveis. Aproxime-se e use **F**:
podem dar 30 de vida, 12 XP, 40 de estamina ou 3 Óbolos. Vida/estamina já cheias
convertem a recompensa em XP. Óbolos dos baús e dos abates são guardados no retorno:
1 por 10 abates, limitado a 10 por tentativa, mais 10 ao derrotar o chefe.
A demo usa baús de suprimentos e atributos iniciais independentes da campanha.

No Windows, os saves ficam em `%LOCALAPPDATA%\ParadiseInHell\`:
`profile.json` e `profile-2.json` até `profile-8.json`, cada um com sua cópia anterior
`.bak`. Fechar durante uma tentativa descarta seu estado
e os Óbolos ainda não guardados. Não há retomada de uma tentativa.
Detalhes: [docs/REFUGIO_E_PROGRESSAO.md](docs/REFUGIO_E_PROGRESSAO.md).

Um **mini-boss** aparece por sala de combate da campanha após 45 segundos:
o Carrasco das Sombras persegue normalmente e para para avisar um giro de espada
ou lançar cinco pedras. Tem 180 de vida base, recebe a escala da sala/tempo e
concede 20 XP. As hordas continuam; derrotá-lo não substitui as metas nem é
obrigatório para sair.

Cada derrota tem **1,5% de chance base** de conceder um **Fragmento de Forja**:
**uma unidade por região por save**, considerando todas as tentativas. Depois de
obter o fragmento, morrer, gastá-lo ou mudar de sala não libera outro naquela região.
Uma região diferente tem seu próprio limite; o protótipo atual contém apenas o Submundo.
O material e o registro da região são salvos imediatamente. O **Ferreiro Errante**
tem **3% de chance base por tentativa**, sorteada uma vez, e aparece em segurança
após a primeira sala concluída. Para o Apostador, material e ferreiro passam a
**6% e 12%**, mantendo o limite regional. Um fragmento
transforma a arma de Comum em Rara **permanentemente no save**: +25% de dano base, +10% de
cadência e um efeito por arma: perfuração, ricochete, alcance, explosão, maldição,
necromancia ou revide do escudo. Os aliados acompanham salas e desaparecem no retorno.
Morte, abandono, vitória, novas tentativas e fechamento preservam a arma rara
e o material restante. Neste protótipo, há uma melhoria permanente, Comum → Rara.
Falhar ao salvar a compra preserva o fragmento.
Regras e testes: [docs/FERREIRO_E_MINI_BOSS.md](docs/FERREIRO_E_MINI_BOSS.md).

Ainda faltam outros encontros, inventário geral e próximas regiões. A meta de 30–40 minutos
pertence à campanha futura; vencer este chefe não conclui a história de Zeus.

As arenas estão em [core/assets/campaign/submundo-prototype.json](core/assets/campaign/submundo-prototype.json).
As regras estão em [docs/CAMPANHA_TECNICA.md](docs/CAMPANHA_TECNICA.md).

## Jogar no Windows

Gere o pacote conforme a seção abaixo, extraia o ZIP inteiro e abra
`paradise-in-hell-gdx.exe` dentro da pasta extraída. Mantenha juntos o executável,
o JAR e as pastas `app` e `runtime`.

**O pacote Windows inclui seu próprio Java 21.** Quem joga não precisa instalar
Java, Gradle ou uma IDE. Os requisitos mínimos de hardware ainda serão medidos.

| Ação | Controle |
| --- | --- |
| Mover | WASD ou setas |
| Dash em movimento | Shift esquerdo ou direito |
| Mirar | Mouse |
| Atacar com a arma escolhida | Segurar o botão esquerdo |
| Escolher save / profissão | Clique ou teclas 1, 2, 3, 4 da página atual |
| Trocar página de saves / profissões | Setas, PageUp/PageDown ou Anterior/Próxima |
| Defender com o Guerreiro | Segurar o botão direito, mirando para o ataque |
| Escolher dádiva | Clique ou teclas 1, 2, 3 |
| Pausar / continuar | Esc |
| Abandonar durante a pausa: refúgio na campanha, menu na demo | M |
| Iniciar demo / repetir demo / retornar ao refúgio no resultado da campanha | Enter |
| Iniciar protótipo da campanha no menu | C ou clique |
| Sair após cumprir todas as metas | E ou Enter |
| Escolher caminho | Clique ou teclas 1, 2, 3 |
| Abrir baú próximo | F |
| Conversar / altar / saves no refúgio | C / L / P |
| Interagir perto de Caronte, altar ou passagem | E |
| Partir do refúgio | Enter |
| Comprar atributo no altar | Clique ou teclas 1, 2, 3 |
| Desbloquear Necromante no altar | N ou clique; custa 1.000 Almas compartilhadas |
| Voltar do resultado ao menu | Esc ou clique |

O objetivo da demo é sobreviver por **05:00 de simulação**. Pausa e seleção de
dádivas não avançam o relógio.

## Desenvolvimento

### Ferramentas

- JDK **21**, incluindo `java` e `javac`.
- Gradle **8.14.1**, fornecido pelo wrapper; não precisa instalar Gradle globalmente.
- libGDX **1.13.1**, com backend desktop LWJGL3.
- Construo **1.7.1** para empacotar o jogo com um runtime.
- Git; IntelliJ IDEA ou outra IDE com suporte a Java/Gradle, opcional.

O Gradle também possui configuração para localizar/obter um JDK 21. Na primeira
compilação, é necessária conexão para baixar as dependências.

### Clonar e executar

```powershell
git clone https://github.com/Srsups/paradise_in_hell.git
cd paradise_in_hell
.\gradlew.bat :lwjgl3:run
```

Na IDE, abra a pasta raiz como projeto Gradle, selecione JDK 21 e execute a tarefa
`:lwjgl3:run`. O ponto de entrada é
[`Lwjgl3Launcher.java`](lwjgl3/src/main/java/br/com/srsups/paradiseinhell/lwjgl3/Lwjgl3Launcher.java).
Os módulos atuais são `core` e `lwjgl3`.
A recomendação de continuar em Java/libGDX e os critérios para reconsiderá-la estão
em [docs/DECISAO_DE_TECNOLOGIA.md](docs/DECISAO_DE_TECNOLOGIA.md).

Em outros sistemas, o wrapper equivalente é `./gradlew`. Os alvos Linux e macOS
estão configurados, mas ainda não foram homologados nesta demo.

### Compilar e verificar

```powershell
.\gradlew.bat build
.\gradlew.bat :lwjgl3:smokeTest
```

`build` executa os testes de progressão, duração de projéteis e ciclo de vida das
telas, estado da tentativa, rotas, compras, gravação/recuperação de perfil e baús. `smokeTest` exige ambiente gráfico/OpenGL e verifica o jogo real em uma
janela invisível: menu, profissões, armas, escolhas, pausa, redimensionamento,
vitória, morte, reinício, XP por tipo, metas, hordas contínuas, ataques do chefe,
segunda fase, dano das oito armas, aura e ressurreição durante a luta, refúgio,
diálogos e expiração das mensagens, compras permanentes, reset da tentativa,
recarga do perfil, saves independentes, profissão fixa, ímã, bônus percentuais,
escala de inimigos, mini-boss, limite regional salvo, forja, efeitos das armas raras,
socos em um alvo, sorte, raridades ponderadas, forja permanente, recompensas de
rotas, eventos fixos na tentativa, baús, oito saves, desbloqueio lendário, explosões,
maldições, aliados entre salas, defesa/revide, sinergias, Fúria dos Deuses, Almas compartilhadas e relatórios de tentativas.
Também verifica controles de volume, mute, persistência, oito sons de ataque,
feedback de dano/defesa/proteção, congelamento do painel de áudio e avisos sem
repetição, além de exclusão/cancelamento/reutilização de saves, backups, identidade
nos relatórios, ataque com defesa nas duas arenas, ciclos/limites de rápidos e
arqueiros com as oito armas, mira travada, flechas, escudo/revide,
XP/Almas próprios, pausa e limpeza.

As capturas dessa verificação ficam em `build/smoke/`. Esses testes não substituem
uma partida completa nem comprovam diversão, desempenho mínimo ou balanceamento.

### Gerar a demo Windows

```powershell
.\gradlew.bat :lwjgl3:packageWinX64
```

Saída: `lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip`.
O pacote usa Temurin **21.0.12+8** e inclui instruções para o jogador.
A primeira geração também baixa o JDK de distribuição e o launcher.

Para verificar apenas a inicialização do executável extraído, execute:

```powershell
.\paradise-in-hell-gdx.exe --smoke-test
```

Essa opção abre uma janela invisível, renderiza menu, demo, campanha, sala do chefe, refúgio, seleção de saves, ferreiro, recompensas das rotas e novas classes, e encerra automaticamente.
O teste completo do ciclo de jogo continua sendo `:lwjgl3:smokeTest`.
`--audio-smoke-test` também carrega/reproduz todos os efeitos com OpenAL nativo
e verifica o avanço da música em streaming. O script de verificação usa um
dispositivo nulo, sem reprodução pelos alto-falantes.

O script `tools/verify_windows_package.ps1` extrai o ZIP e verifica esse executável
com um PATH sem Java, para confirmar o uso do runtime incluído.

Para desenvolvimento, `:lwjgl3:jar` gera um JAR executável em `lwjgl3/build/libs/`;
essa alternativa exige Java 21 na máquina que o executa.

### Estrutura

```text
core/src/main/java/      Regras do jogo, entidades e telas
core/src/test/java/      Testes de regressão
core/assets/            Atlas, definições JSON da campanha e placeholders antigos
lwjgl3/src/main/java/    Inicialização desktop
lwjgl3/src/test/java/    Verificação gráfica da demo
gradle/                 Wrapper e configuração do JDK
docs/                   Plano, instruções e origem da arte
```

## Arte e publicação

A origem do atlas, seu prompt e a organização das células estão em
[docs/ARTE_DA_DEMO.md](docs/ARTE_DA_DEMO.md). O atlas foi gerado com IA e deve ser
revisado artisticamente antes do lançamento. Os placeholders antigos permanecem
no repositório para referência, mas são excluídos dos recursos de distribuição.

O [plano de lançamento](docs/PLANO_DE_LANCAMENTO.md) diferencia trabalho concluído,
polimento da demo e preparação comercial. O projeto ainda não é anunciado como
uma versão 1.0 ou como aprovado pela Steam.
