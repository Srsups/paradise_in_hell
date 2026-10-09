# Plano de conclusão e lançamento

Avaliação do repositório em 07/10/2026. Primeiro marco escolhido: **demo para Windows**,
mantendo mira e tiro pelo mouse. Os sprites anteriores eram placeholders de terceiros.

O marco seguinte é a primeira região da campanha. O autor escolheu tentativas
completas de aproximadamente 30–40 minutos, com história e desbloqueios persistentes
entre mortes. As decisões e propostas estão em [DESIGN_DO_JOGO.md](DESIGN_DO_JOGO.md).

## Diagnóstico

O projeto já tinha movimento, combate, experiência e transições entre telas.
O README anterior misturava implementação e intenção: anunciava combate automático
e muitas melhorias, mas o código dependia do mouse e oferecia apenas Égide e Poseidon.
Também indicava JDK 17 e módulo desktop, enquanto o projeto usava JDK 21 e lwjgl3.

Minha avaliação: vale continuar em Java/libGDX. A prioridade é completar e testar
uma experiência pequena antes de ampliar classes, armas e metaprogressão.
A mitologia precisa aparecer nas decisões de jogo: nomes de deuses, sozinhos,
não sustentam um diferencial. Dádivas com combinações e consequências são um
caminho promissor a explorar depois de validar a demo.

## Primeiro marco implementado

- [x] Corrigir runtime da distribuição de Java 17 para Java 21.
- [x] Atualizar documentação e comandos para core/lwjgl3.
- [x] Corrigir desenho após descarte de uma tela e liberar recursos nas transições.
- [x] Validar coordenadas de spawn e tratar tentativas sem posição válida.
- [x] Limitar projéteis, inimigos, objetos distantes e cache do mapa.
- [x] Manter terreno determinístico ao voltar a uma região retirada do cache.
- [x] Encerrar a demo em cinco minutos com vitória ou morte e estatísticas.
- [x] Introduzir dificuldade crescente e três variantes de inimigos.
- [x] Oferecer doze dádivas, com efeitos, descrições e limites de repetição.
- [x] Preservar todas as escolhas quando várias subidas de nível são acumuladas.
- [x] Impedir resistência negativa que transformaria dano em cura.
- [x] Incluir pausa, reinício, instruções e interface adaptável ao tamanho da janela.
- [x] Criar atlas novo e excluir os placeholders antigos do pacote.
- [x] Adicionar testes de regressão e verificação gráfica em janela invisível.

A arte nova foi gerada com IA. As variantes de inimigos compartilham a animação,
com cores, dimensões e atributos diferentes. Isso é suficiente para o protótipo;
arte final deve melhorar a leitura e distinguir comportamentos.

### Verificação desta entrega

- Build Gradle concluído e 80 testes de regressão aprovados.
- Verificação com OpenGL real em janela invisível aprovada: menu, escolhas,
  profissões, quatro armas, XP por tipo, metas de abate, geração contínua,
  pausa, redimensionamento, vitória da demo, morte, reinício, caminhos,
  coleta do XP restante, três padrões do chefe, segunda fase, dano de todas as
  armas, aura, ressurreição, vitória do chefe, refúgio, diálogos, compras,
  perfil recarregado, reset das dádivas, baús, saves independentes, profissão fixa,
  expiração da mensagem de Caronte, ímã de XP, bônus percentuais, escala linear
  dos inimigos, mini-boss sem investida, material persistente, ferreiro, poderes
  das armas raras, quarto save, socos em um alvo, limite regional após gasto e
  nova run, distribuição das raridades e descarte das telas.
- ZIP Windows gerado em lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip.
- Executável extraído iniciou menu, demo, campanha, chefe, refúgio, seleção de saves e ferreiro com PATH sem Java e diretório de
  trabalho fora do pacote, usando seu runtime incluído; código de saída zero.
- Atlas novo e instruções presentes no pacote; três placeholders antigos ausentes.
- Capturas de interface em build/smoke foram revisadas.

O teste de PATH não equivale à homologação em outra máquina ou em uma instalação
limpa do Windows. O teste acelera o relógio da demo, as metas da campanha e a vida
do chefe para verificar resultados. Partidas completas e balanceamento continuam
pendentes.

## Critério para divulgar a demo publicamente

- [ ] Jogar partidas completas e ajustar o começo, meio e fim da curva de dificuldade.
- [ ] Validar se as doze dádivas geram escolhas interessantes e evitar combinações dominantes.
- [ ] Acrescentar efeitos de tiro, impacto, XP, dash, dano e vitória/morte.
- [ ] Acrescentar música e controles de volume, com preferências salvas.
- [ ] Revisar animação, alinhamento, contraste do personagem, ícone e identidade do menu.
- [ ] Testar em máquina sem Java instalado e com uma GPU diferente.
- [ ] Testar sessões repetidas, Alt+Tab, minimizar, diferentes resoluções e escalas do Windows.
- [ ] Recolher feedback de pelo menos cinco jogadores que não conheçam o projeto.
- [ ] Documentar hardware testado, desempenho e problemas conhecidos.
- [ ] Conferir avisos e licenças das bibliotecas/runtime distribuídos.

Sugestão de playtest: observar uma primeira partida sem ensinar os controles,
depois pedir respostas sobre clareza do objetivo, sensação do tiro/dash, variedade
das escolhas e vontade de tentar novamente. Registrar tempo até a morte, nível,
dádivas escolhidas e trechos de dificuldade excessiva. Esses dados devem orientar
o próximo ajuste; não há avaliação de balanceamento apenas por compilação.

## Próximo incremento de conteúdo

Implementar a primeira região como uma travessia com arenas, caminhos, NPCs,
recompensas e um chefe. A sequência de implementação e os critérios de conclusão
estão no [design da campanha](DESIGN_DO_JOGO.md#próximo-marco-uma-região-completa).

| Prioridade | Entrega | Evidência de conclusão |
| --- | --- | --- |
| Alta | Inimigo com ataque à distância e sinalização | O jogador entende e consegue evitar o ataque |
| Implementado no protótipo | Chefe do submundo | Três padrões sinalizados, segunda fase e resultado por derrota do chefe; balanceamento pendente |
| Alta | Sinergias entre dádivas | Pelo menos três construções viáveis em playtests |
| Implementado no protótipo | Quatro profissões/armas | Arco, espada, pistola e socos com ataques próprios; Apostador tem sorte elevada; arte e valores pendentes |
| Implementado no protótipo | Categorias das dádivas | Comum/Rara/Épica/Mitológica com probabilidades ponderadas pela sorte; equilíbrio pendente |
| Média | Melhorias de VFX e feedback | Dano, invulnerabilidade e alcance são claros |
| Implementado no protótipo | Perfil e atributos permanentes | Óbolos, profissão, compras e eventos de Caronte sobrevivem ao retorno e à recarga do perfil |
| Depois | Classes e catálogo amplo de melhorias permanentes | Ampliar após validar a travessia da primeira região |

A base inicial da campanha foi implementada: estado da tentativa separado das telas,
atributos em PlayerState, arenas/região definidas em JSON e escolha entre duas rotas.
O acoplamento a GameScreen.instance foi removido; os ataques usam uma função
fornecida pela arena. Vida, XP, nível, poderes, limites, estamina e recargas
sobrevivem à troca de tela sem reter texturas descartadas.

A fase inicial libera acesso direto à sala do chefe, com duas rotas de combate
opcionais. Profissões com arco, espada, pistola e socos, XP por tipo e navegação ao redor
de estruturas estão implementados. O Guardião do Tártaro agora encerra o trecho
jogável com combate: marcas, investida, rajada e segunda fase. A identidade, arte
e dificuldade são provisórias; não há ainda conclusão da história de Zeus.
Refúgio, Caronte, altar, Óbolos provisórios, baús e perfil persistente foram
implementados. Em 09/10/2026, foram acrescentados quatro saves independentes,
profissão fixa por personagem, ímã de XP, bases distintas e bônus percentuais,
escala linear de vida/dano dos inimigos e expiração da confirmação de Caronte.
O perfil anterior é preservado no primeiro save.

O segundo NPC é o Ferreiro Errante: 3% base por tentativa, material persistente com
1,5% base por mini-boss e forja temporária com efeitos por arma. O limite corrigido
é uma unidade por região por save, sem rearmar após gasto ou nova run. O Apostador
usa socos e sorte 4×; material/encontro ficam em 6%/12%, com dádivas superiores
mais frequentes. Tabelas: [SORTE_E_RARIDADE.md](SORTE_E_RARIDADE.md). O Carrasco usa giro
e pedras, entre hordas, sem investida. O próximo incremento são recompensas
próprias nas rotas, após playtests do mini-boss e da forja. Modificadores e novas
sinergias permanecem planejados. Consulte [FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md).
Consulte [CHEFE_DO_SUBMUNDO.md](CHEFE_DO_SUBMUNDO.md),
[CAMPANHA_TECNICA.md](CAMPANHA_TECNICA.md) e a
[avaliação da tecnologia](DECISAO_DE_TECNOLOGIA.md).

## Preparação da versão comercial

- [x] Definir o formato da campanha: tentativas completas de 30–40 minutos, com progressão narrativa persistente.
- [ ] Fechar o conteúdo da 1.0: quantidade de arenas, inimigos, chefes, armas e NPCs.
- [ ] Criar configurações persistentes, modo de tela e opção de remapear controles.
- [x] Salvar metaprogressão fora da instalação: perfil, compras e eventos; retomada da tentativa continua em discussão.
- [ ] Planejar localização e usar textos externos ao código.
- [ ] Medir desempenho e memória em partidas e navegação prolongadas.
- [ ] Incluir logs de falhas e um procedimento simples de suporte.
- [ ] Criar arte de loja, capturas reais, trailer e descrição correspondente ao jogo.
- [ ] Integrar conquistas/Cloud/controles apenas se fizerem parte do escopo anunciado.
- [ ] Homologar cada sistema operacional que será listado na loja.

## Steam: passos externos ao projeto

A Valve exige que o jogo inicie corretamente em cada sistema anunciado e que
as funcionalidades da página estejam presentes no build. Página e build passam
por revisão; a documentação orienta reservar pelo menos sete dias úteis para cada
revisão. [Processo de revisão](https://partner.steamgames.com/doc/store/review_process).

O Steam Direct cobra **US$ 100 por produto**, recuperáveis após atingir o patamar
de receita ajustada definido pela Valve; não é uma taxa reembolsável sob demanda.
[Steam Direct](https://partner.steamgames.com/steamdirect).

Preparar conta Steamworks, cadastro e informações bancárias/fiscais, AppID, depots,
opção de inicialização, upload via SteamPipe e teste pelo cliente Steam.
Conferir os prazos de onboarding e de Coming Soon ao definir a data.
[Onboarding](https://partner.steamgames.com/doc/gettingstarted/onboarding) e
[Coming Soon](https://partner.steamgames.com/doc/store/coming_soon).

O atlas desta demo entra como arte pré-gerada com IA no questionário de conteúdo.
Declarar seu uso e revisar os resultados antes de enviar a página/build.
[Content Survey](https://partner.steamgames.com/doc/gettingstarted/contentsurvey).

A demo na Steam possui seu próprio fluxo de configuração associado ao jogo principal.
Preparar esse cadastro quando a demo estiver pronta para divulgação.
[Demos](https://partner.steamgames.com/doc/store/application/demos).

Nenhuma conta, cobrança, upload ou publicação foi realizada durante esta preparação.
