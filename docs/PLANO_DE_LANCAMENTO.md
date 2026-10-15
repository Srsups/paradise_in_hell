# Plano de conclusão e lançamento

Avaliação inicial em 07/10/2026; prioridades revisadas em **13/10/2026** após os
playtests do autor. Primeiro marco escolhido: **demo para Windows**, mantendo mira
e tiro pelo mouse. Os sprites anteriores eram placeholders de terceiros.

O plano atual de incrementos está em [ROTEIRO_DE_MELHORIAS.md](ROTEIRO_DE_MELHORIAS.md).
Dificuldade fica fixa ao criar o save; Fazendeiro Épico será liberado por desafio
de um NPC sorteado por run. Este documento conserva o histórico implementado
e os critérios de publicação; o roteiro detalha as novas entregas planejadas.

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

- Marco 1 verificado em 14/10/2026: 211 testes de regressão aprovados, incluindo HUD, áudio por emissor, padrões do chefe, métricas e histórico antigo.
- Verificação com OpenGL real em janela invisível aprovada: menu, escolhas,
  profissões, oito armas, XP por tipo, metas de abate, geração contínua,
  pausa, redimensionamento, vitória da demo, morte, reinício, caminhos,
  coleta do XP restante, quatro padrões do chefe, salto com perseguição contínua e dash de esquiva, segunda fase, dano de todas as
  armas, aura, ressurreição, vitória do chefe, refúgio, diálogos, compras,
  perfil recarregado, reset das dádivas, baús, saves independentes, profissão fixa,
  expiração da mensagem de Caronte, ímã de XP, bônus percentuais, escala linear
  dos inimigos, mini-boss sem investida, material persistente, ferreiro, poderes
  das armas raras, quarto save, socos em um alvo, limite regional após gasto e
  nova run, distribuição das raridades, forja permanente após morte/vitória/recarga,
  recompensas de rotas, variação anunciada de hordas, oito saves, compra de classe lendária,
  explosões, maldições em área, aliados, defesa/revide, sinergias, Fúria dos Deuses,
  contato por silhueta, ataques/s, espera e liberação do ataque nas escolhas,
  consulta paginada por Tab, Almas compartilhadas, débito lendário de 1.000,
  relatórios/causa da morte/dano por origem, histórico e recarga sem crédito duplicado, arqueiros com as oito armas,
  mira travada/flechas/escudo/revide, XP/Almas próprios, ataque com defesa simultânea,
  exclusão/cancelamento/reutilização nas duas páginas, identidade dos relatórios,
  backup sem restaurar personagem excluído, ritmo/limites dos rápidos, volumes/mute,
  painel de áudio congelando as arenas, oito sons de arma, sinais de defesa/proteção/dano,
  avisos sem repetição e descarte das telas.
- ZIP Windows gerado em lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip.
- Executável extraído iniciou menu, demo, campanha, chefe, refúgio, seleção de saves, ferreiro e recompensas de rotas com PATH sem Java e diretório de
  trabalho fora do pacote, usando seu runtime incluído; código de saída zero.
- Áudio nativo: 24 efeitos e música em streaming verificados com OpenAL Soft em dispositivo nulo, sem reprodução pelos alto-falantes.
- Atlas novo, WAVs originais e instruções presentes no pacote; três placeholders antigos ausentes.
- Capturas de interface em build/smoke foram revisadas.

O teste de PATH não equivale à homologação em outra máquina ou em uma instalação
limpa do Windows. O teste acelera o relógio da demo, as metas da campanha e a vida
do chefe para verificar resultados. O autor já realizou partidas práticas e
relatou chefe fácil, HUD carregado e necessidade de variedade/acabamento.
A matriz completa de profissões, dificuldades e progressão continua pendente;
o feedback recebido orienta o [roteiro atualizado](ROTEIRO_DE_MELHORIAS.md).

## Critério para divulgar a demo publicamente

- [x] Recolher o primeiro parecer do autor em partidas práticas e registrar prioridades.
- [x] Aplicar M1: HUD essencial/barra de XP, som do Carrasco somente visível e revisão do Guardião.
- [ ] Validar o ritmo do Guardião revisado em partidas completas com perfis novos/evoluídos.
- [ ] Aplicar/validar os modos Fácil, Média e Difícil fixos na criação do save.
- [ ] Jogar a matriz de profissões/rotas e ajustar começo, meio e fim da curva.
- [ ] Validar o catálogo ampliado e a dádiva regional, evitando combinações dominantes.
- [x] Acrescentar sons/efeitos provisórios de tiro, impacto, XP, dash, dano e vitória/morte; revisão com jogadores pendente.
- [x] Acrescentar duas trilhas provisórias e controles de volume/mute com preferências salvas; composição e mixagem finais pendentes.
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

## Próximos incrementos após os playtests

Concluir e apresentar bem a primeira região, começando pelos problemas percebidos
na travessia atual. O [roteiro detalhado](ROTEIRO_DE_MELHORIAS.md) é a referência
de ordem, propostas e critérios de aceite.

| Ordem | Incremento planejado | Resultado esperado |
| --- | --- | --- |
| M1 — implementado; playtests pendentes | HUD, áudio visível do Carrasco e Guardião | Barras/Tab, máscaras/culling, 500 de vida, rajada reforçada, recuperações menores e novas métricas |
| M2 — implementado; playtests pendentes | Dificuldades fixas e economia | 222 testes/OpenGL, três modos, migração, frações e crédito único verificados |
| M3 | Base visual e diálogo de Caronte | Fontes, atlas/animação, molduras e conversa piloto reutilizáveis |
| M4 | Dádivas e altar | Mais escolhas, poder regional e três novas compras candidatas |
| M5 | NPC de desafio e Fazendeiro Épico | Desbloqueio narrativo e troca ancinho/foice com funções distintas |
| M6 | Arte/animação em todo o conteúdo | Profissões/inimigos próprios, efeitos por sprites e conversas polidas |
| M7 | Submundo completo, variedade e expansão | Região testada e pipeline pronto para Ascensão/Olimpo |

### Situação do protótipo anterior à revisão

| Situação | Entrega | Evidência de conclusão |
| --- | --- | --- |
| Implementado no protótipo; playtests pendentes | Arqueiro do Estige | Mira fixa de 1 s, flechas bloqueáveis, metas/recompensas e oito armas verificadas; validar leitura e equilíbrio em partidas |
| Implementado no protótipo | Chefe do submundo | Quatro padrões sinalizados, queda com trava vermelha e pressão na caminhada da fase 2, segunda fase e resultado por derrota do chefe; balanceamento pendente |
| Protótipo implementado; playtests pendentes | Sinergias entre dádivas | Choque, queimadura, impacto, maldição e aura combinam; três construções candidatas a validar |
| Implementado no protótipo | Oito profissões/armas | Quatro novas classes, Necromante lendário por 1.000 Almas compartilhadas e Guerreiro com defesa/revide; arte e valores pendentes |
| Implementado no protótipo | Fúria dos Deuses | Farming após saída liberada gera tolerância, aviso e penalidades graduais; avanço limpa pressão |
| Implementado no protótipo | Categorias das dádivas | Comum/Rara/Épica/Mitológica com probabilidades ponderadas pela sorte; equilíbrio pendente |
| Implementado no protótipo | Rotas com recompensas e variação | Cinzas: baú; Sombras: dádiva Rara ou superior; eventos fixos e anunciados na rota |
| Base implementada; revisão artística/auditiva pendente | Áudio e feedback | 24 sons, duas trilhas, volumes persistentes; sinais de dano, defesa, proteção e armas distintos |
| Implementado no protótipo | Perfil e atributos permanentes | Óbolos, profissão, compras e eventos de Caronte sobrevivem ao retorno e à recarga do perfil |
| Planejado nos M4/M5 | Fazendeiro Épico, dádivas regionais e mais melhorias permanentes | Novos pedidos incorporados; valores e desbloqueio em elaboração |

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
1,5% base por mini-boss e forja permanente com efeitos por arma. O limite corrigido
é uma unidade por região por save, sem rearmar após gasto ou nova run. O Apostador
usa socos e sorte 4×; material/encontro ficam em 6%/12%, com dádivas superiores
mais frequentes. Tabelas: [SORTE_E_RARIDADE.md](SORTE_E_RARIDADE.md). O Carrasco usa giro
e pedras, entre hordas, sem investida. Cinzas e Sombras agora oferecem recompensas
próprias em etapas seguras, e cada sala sorteia um evento que altera a composição
das hordas, anunciado na rota. Travessias completas orientam os ajustes de risco,
recompensa e dificuldade do roteiro atual. Choque, queimadura e impacto, quatro novas
classes e Fúria dos Deuses já têm protótipos funcionais: validar três combinações,
a duração do desbloqueio lendário por Almas e o tempo até a fúria antes de expandir regiões.
Regras: [CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md). Consulte [ROTAS_DO_SUBMUNDO.md](ROTAS_DO_SUBMUNDO.md). Consulte [FERREIRO_E_MINI_BOSS.md](FERREIRO_E_MINI_BOSS.md).
Consulte [CHEFE_DO_SUBMUNDO.md](CHEFE_DO_SUBMUNDO.md),
[CAMPANHA_TECNICA.md](CAMPANHA_TECNICA.md) e a
[avaliação da tecnologia](DECISAO_DE_TECNOLOGIA.md).

O resumo das tentativas está disponível com R no resultado/refúgio. O histórico
guarda as 20 últimas runs; permite comparar salas, dádivas, dano por origem e
causas de morte. O Necromante agora custa 1.000 Almas compartilhadas, com ganhos
de abates/mini-boss/chefe e migração dos acessos antigos. Valores e regras:
[ALMAS_E_RELATORIOS.md](ALMAS_E_RELATORIOS.md).
A próxima avaliação deve usar esses dados em partidas completas; o inimigo
com ataque à distância foi implementado: [ARQUEIRO_DO_ESTIGE.md](ARQUEIRO_DO_ESTIGE.md).
Foi aplicado o primeiro ajuste com base no relato de rápidos acumulando:
limites por onda/arena e ciclos de arrancada/recuperação, sem exigir bônus de
velocidade para reposicionar em caminho livre. Guerreiro ataca com a defesa ativa;
slots podem ser excluídos com confirmação e profissões perderam prefixos numéricos.
Áudio/feedback também ganharam base funcional: [AUDIO_E_FEEDBACK.md](AUDIO_E_FEEDBACK.md).
Atena recebeu +150% de XP, categoria Épica e limite de uma escolha, com testes
de retorno da escolha e preservação de relatórios antigos. O feedback seguinte
motivou o M1, implementado em 14/10/2026. O M2 também foi implementado:
[dificuldade fixa por save e economia](MARCO_2_DIFICULDADES_E_ECONOMIA.md).
O próximo incremento é M3, base visual e conversa piloto. A matriz de partidas
continua medindo rotas, builds e progresso do altar, agora também por dificuldade.
O Fazendeiro amplia a matriz quando implementado; a arte e narrativa seguem
os mesmos critérios de travessia e produção de conteúdo.

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
