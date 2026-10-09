# Paradise in Hell

Jogo 2D de sobrevivência de hordas inspirado na mitologia grega, feito em Java e libGDX.
Você escolhe uma profissão, mira com o mouse e combina arma e dádivas para atravessar o submundo.

**Estado: demo 0.1.0 e protótipo inicial de campanha para Windows, em desenvolvimento.** A demo termina após cinco
minutos de sobrevivência ou com a morte do jogador. Ainda precisa de áudio,
balanceamento com jogadores e revisão artística antes de uma publicação pública.
O plano de conclusão está em [docs/PLANO_DE_LANCAMENTO.md](docs/PLANO_DE_LANCAMENTO.md).

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
- Pergunta do narrador e quatro profissões: Caçador (arco), Ferreiro (espada),
  Vigilante (pistola) e Apostador (socos), com ataques diferentes.
- Cinco etapas de dificuldade, com inimigos comuns, rápidos e resistentes.
- Doze tipos de dádivas: dano, cadência, vida, velocidade, resistência, experiência,
  cura, cura por abate, Égide, aura de Poseidon, ressurreição e ímã de XP.
- Escolhas ponderadas ao subir de nível, com dádivas Comuns, Raras, Épicas e
  Mitológicas. A sorte favorece raridades superiores; limites de repetição são respeitados.
- Mapa procedural com obstáculos, área inicial segura e cache limitado.
- XP por inimigo: comum 1, rápido 2, tank 5; coleta, números de dano e barras.
- Navegação compartilhada para contornar estruturas, com colisão nos quatro
  cantos e verificação do caminho percorrido.
- Pausa, menu, reinício e resultado com tempo, abates e nível.
- Atlas novo gerado para a demo; os três placeholders antigos não entram no pacote.

Outras armas/profissões, novos chefes, economia final, áudio, suporte nativo
a controles e integração Steamworks continuam no roadmap.

## Protótipo da campanha

No menu, clique em **Campanha: protótipo** ou pressione **C** e escolha um dos
**quatro saves** com clique ou **1, 2, 3, 4**. Em um save vazio, o narrador pergunta
sua profissão; essa escolha é definitiva para o personagem. Para jogar com outra
profissão, crie um personagem em outro save vazio. Saldo, compras e eventos são
independentes. O perfil anterior continua disponível no Save 1.

Após escolher ou continuar um personagem, entre no **Refúgio do Estige**.
**C** conversa com Caronte, **L** abre o altar, **P** abre os saves e **Enter**
inicia uma tentativa. Também é possível mover-se e usar **E** perto dos locais.
A demo também permite escolher o save e usa sua profissão, sem aplicar compras
ou registrar recompensas da campanha.

O arco dispara flechas, a espada
atinge uma área curta na direção do mouse e a pistola dispara mais rápido com
dano menor. O **Apostador** luta com socos em um alvo próximo e tem **sorte 4×**.
Cada profissão tem bases próprias de vida, velocidade e dano.
Dádivas de força (+20%), vida (+20%), velocidade (+10%) e cadência (+12%)
somam percentuais sobre essas bases. O dano mantém frações, inclusive na pistola.
Nomes, balanceamento e arte das profissões são provisórios. Tabelas e fórmulas:
[docs/BALANCEAMENTO_E_SAVES.md](docs/BALANCEAMENTO_E_SAVES.md).

As dádivas são classificadas pela força do efeito. No primeiro sorteio com todas
as categorias disponíveis, as chances base são 70% Comum, 23% Rara, 6% Épica e
1% Mitológica. O Apostador favorece as categorias superiores. Números, catálogo
e regras de sorte: [docs/SORTE_E_RARIDADE.md](docs/SORTE_E_RARIDADE.md).

A **Colheita de Deméter** atrai cristais de XP: até três níveis, cada um adicionando
32 unidades de alcance e 40 de velocidade de atração. É uma dádiva temporária,
preservada entre salas e apagada na próxima tentativa.

Vida e dano dos inimigos crescem linearmente com o tempo total da tentativa e a
dificuldade da sala, incluindo o chefe. Os valores são definidos no nascimento
do inimigo ou na entrada da sala do chefe; inimigos já feridos não recuperam
vida por esse crescimento. Trocar de sala mantém o relógio da dificuldade.

Na Margem do Estige, cumpra **as três metas**: 40 comuns, 30 rápidos e 8 tanks.
As hordas podem misturar tipos ou reunir inimigos do mesmo tipo. A geração não
tem um orçamento total por fase nem termina pelo relógio. Inimigos desaparecidos
por distância não contam como mortos e não dão XP.

Ao cumprir as metas, a passagem é liberada sem exigir eliminar os inimigos vivos.
As hordas continuam até você pressionar **E ou Enter** para sair. O XP restante é
recolhido e todas as escolhas de nível são resolvidas antes da troca de sala.
A interface não exige localizar inimigos fora da tela e não tem indicadores deles.

Escolha ir diretamente à sala do chefe ou passar por um combate opcional nas
Cinzas ou nas Sombras. Cada combate tem suas metas, reiniciadas ao entrar.
Vida, XP, nível, profissão, arma, dádivas, limites, estamina e recargas continuam.
Após morte ou vitória, **Enter no resultado retorna ao refúgio**. Abandonar com
M durante a pausa retorna diretamente. Uma nova tentativa começa com vida cheia,
nível 1 e sem XP ou dádivas; mantém a profissão e os atributos comprados.

Na sala do chefe, enfrente o **Guardião do Tártaro**: marcas explosivas no chão,
investida com trajetória marcada e rajadas de projéteis. Os ataques têm avisos
visuais e a luta fica mais intensa abaixo de metade da vida. Use a arma escolhida,
suas dádivas e o dash; derrotá-lo encerra o trecho jogável com vitória.
Entrar na sala, esperar ou apertar Enter não concede vitória.

Nome, sprite e valores do chefe são provisórios. As regras e sugestões de teste
estão em [docs/CHEFE_DO_SUBMUNDO.md](docs/CHEFE_DO_SUBMUNDO.md).
Caronte reconhece mortes e a vitória contra o Guardião. Sua primeira conversa
completa entrega 5 **Óbolos**, moeda provisória. No altar, compre até cinco níveis
de vida máxima (+10%), dano (+5%) e velocidade (+3%) sobre as bases da profissão.
Preços: 5, 9, 13, 17 e 21.
Compras, saldo, profissão e eventos são salvos entre sessões.
A confirmação de uma conversa ou compra desaparece após 3,5 segundos.

Durante o combate, baús surgem em locais alcançáveis. Aproxime-se e use **F**:
podem dar 30 de vida, 12 XP, 40 de estamina ou 3 Óbolos. Vida/estamina já cheias
convertem a recompensa em XP. Óbolos dos baús e dos abates são guardados no retorno:
1 por 10 abates, limitado a 10 por tentativa, mais 10 ao derrotar o chefe.
A demo usa baús de suprimentos e atributos iniciais independentes da campanha.

No Windows, os saves ficam em `%LOCALAPPDATA%\ParadiseInHell\`:
`profile.json`, `profile-2.json`, `profile-3.json` e `profile-4.json`, cada um com sua cópia anterior
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
transforma a arma de Comum em Rara nesta tentativa: +25% de dano base, +10% de
cadência e um efeito por arma — perfuração, ricochete ou maior alcance da espada/socos.
Novas tentativas restauram a arma comum e preservam o material restante.
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
| Escolher save / profissão | Clique ou teclas 1, 2, 3, 4 |
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
segunda fase, dano das quatro armas, aura e ressurreição durante a luta, refúgio,
diálogos e expiração das mensagens, compras permanentes, reset da tentativa,
recarga do perfil, saves independentes, profissão fixa, ímã, bônus percentuais,
escala de inimigos, mini-boss, limite regional salvo, forja, efeitos das armas raras,
socos em um alvo, sorte, raridades ponderadas e baús.

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

Essa opção abre uma janela invisível, renderiza menu, demo, campanha, sala do chefe, refúgio, seleção de saves e ferreiro e encerra automaticamente.
O teste completo do ciclo de jogo continua sendo `:lwjgl3:smokeTest`.

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
