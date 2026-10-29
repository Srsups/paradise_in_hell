# Marco 7 — tutorial contextual e Centelha da Insurgência

Primeiro incremento implementado e verificado em **27/10/2026**. Este lote
apresenta os objetivos no momento de uso e fecha a primeira travessia do Submundo.
O M7 maior continua em aberto: opções de vídeo/escala, remapeamento, regras para
interromper/retomar uma tentativa, variedade e playtests precedem novas regiões.
O M6 continua em revisão humana de apresentação.

## Conquista narrativa e retorno

O autor aprovou a **Centelha da Insurgência**: seu poder permitirá ferir os
deuses e abre o próximo arco da jornada. Neste incremento ela é uma conquista
permanente da história do personagem, **sem efeito nos atributos ou no combate**.
Não aumenta dano, vida ou sorte e não substitui arma, dádiva ou melhoria da forja.
A aplicação jogável contra deuses será definida na expansão.

Uma vitória real contra o Guardião do Submundo conquista a Centelha. Morte,
abandono, demo e simples entrada na sala não a concedem. A maldição de Zeus
continua explicando o retorno após a morte; a Centelha tem outra função. Sua
aquisição não encerra o resgate nem define como Zeus poderia revogar a maldição.

A tentativa é liquidada uma vez antes da apresentação: moedas, conquistas,
relatório e missão mantêm seu fluxo. Depois da queda do Guardião e antes do
resultado, [UnderworldEndingScreen](../core/src/main/java/br/com/srsups/paradiseinhell/UnderworldEndingScreen.java)
apresenta sete falas de Caronte e protagonista. Eles reconhecem a vitória,
explicam a Centelha e escolhem **voltar voluntariamente ao refúgio** para preparar
a ascensão. Zeus e o resgate da esposa continuam como objetivo. O rodapé informa
que a continuação da campanha está em desenvolvimento; a Ascensão não foi
acrescentada como região jogável neste lote.

| Ação no desfecho | Controle |
| --- | --- |
| Revelar a fala; depois avançar uma fala | Enter, E, Espaço ou clique dentro da fala |
| Alternar texto gradual/instantâneo | T |
| Adiar sem concluir a leitura | Esc |
| Retomar ou rever no refúgio, após conquistar a Centelha | V |
| Retomar ou rever no resultado da campanha | C |

A entrada espera **1,5 s** e exige soltar o botão esquerdo antes de confirmar.
Esc adia a leitura sem marcar sua conclusão. Retomar/rever começa a conversa;
não guarda a posição da fala. Confirmar a última fala registra a conclusão e
segue ao resultado. Depois de lido, o desfecho não reaparece automaticamente em
outras vitórias. Rever não concede outra Centelha, moedas ou retorno adicional.

[UnderworldStory](../core/src/main/java/br/com/srsups/paradiseinhell/UnderworldStory.java)
carrega [underworld-ending.json](../core/assets/dialogues/underworld-ending.json)
sem recursos gráficos. Expõe `title()`, `subtitle()`, `nextObjective()`,
`continuationNotice()` e `lines()` imutável. O parser valida os campos, tipos,
duplicações, limites e retratos compatíveis. São usados os retratos existentes;
não há novo atlas. Caronte conserva as prioridades e o presente único de cinco
Óbolos; sua fala da vitória mantém a piada da licença e comenta a Centelha.

## Dicas e guia de jogo

Uma dica contextual aparece por vez no rodapé, por até **sete segundos de
leitura ativa**. O personagem aprende ao executar a ação correspondente ou ao
completar esse tempo de leitura. São dez tópicos: refúgio, movimento, ataque,
dash, XP, dádivas, metas, passagem, Guardião e retorno. Uma ação pode registrar
aprendizado mesmo antes de sua dica aparecer. As dicas não mudam a dificuldade,
não concedem prêmios e cedem espaço a avisos prioritários de combate.

| Tópico | Ação observada que conclui a dica antes dos sete segundos |
| --- | --- |
| Refúgio | Concluir conversa ou iniciar a travessia |
| Movimento | Deslocar o personagem |
| Ataque | Emitir um ataque |
| Dash | Executar um dash aceito pelo modelo |
| XP | Receber experiência |
| Dádivas | Escolher uma dádiva |
| Metas | Completar todas as metas |
| Passagem | Usar a saída |
| Guardião | Derrotar o chefe |
| Retorno | Confirmar uma compra no altar |

**F1** abre/fecha o guia no refúgio, na arena e no chefe. Na campanha são duas
páginas: combate/controles e travessia/progressão; a demo tem somente a página
de combate. Setas ou os botões Anterior/Próxima trocam páginas; F1/Esc fecha.
O painel pausa a simulação e consome a entrada do quadro em que abre/fecha,
evitando mover, atacar ou interagir ao mesmo tempo. Consultar o guia não marca
tópicos como aprendidos.

**H** ativa/desativa as dicas do personagem. O guia continua acessível por F1.
Na campanha, tópicos aprendidos e preferência acompanham o save entre sessões;
as lições da demo ficam locais e não avançam os aprendizados da campanha.
Pausa, consulta por Tab, diálogos e áudio mantêm suas proteções e não avançam
o tempo de leitura de uma dica oculta. O aviso vermelho da queda e os demais
avisos do Guardião continuam prioritários, assim como Fúria, passagem e baús.
No refúgio, a leitura contextual avança somente enquanto o personagem pode
caminhar, fora de conversa e altar.

[TutorialGuide](../core/src/main/java/br/com/srsups/paradiseinhell/TutorialGuide.java)
seleciona tópicos por contexto e registra ações/tempo de leitura, sem ler entrada
ou avançar combate. [TutorialPanel](../core/src/main/java/br/com/srsups/paradiseinhell/TutorialPanel.java)
compartilha guia, rodapé e sincronização com o serviço do perfil entre as telas.

## Perfil, migração e falhas

O perfil mantém `version: 1` e acrescenta `narrativeVersion: 1`, além dos campos
opcionais `tutorialSeen` e `tutorialEnabled`. Ausência dos campos de tutorial
significa nenhum tópico aprendido e dicas ativadas. `tutorialSeen` contém IDs
de [TutorialTopic](../core/src/main/java/br/com/srsups/paradiseinhell/TutorialTopic.java);
IDs inválidos, duplicações e tipos incorretos são rejeitados. Uma versão narrativa
desconhecida recebe a mesma proteção contra sobrescrita do perfil incompatível.

| Evento pessoal | Quando é registrado |
| --- | --- |
| `INSURGENCE_SPARK_OBTAINED` | Liquidação da vitória real na região do Submundo |
| `UNDERWORLD_EPILOGUE_READ` | Confirmação da última fala, após ter conquistado a Centelha |

Perfis antigos sem `narrativeVersion` que já tinham uma vitória recebem a
Centelha na migração: antes deste esquema, as vitórias registradas pertenciam
ao Submundo. A leitura permanece pendente para consulta por V/C. Perfis que já
declaram a versão narrativa não inferem uma nova conquista só pelo contador.

Eventos, dicas e preferência são **pessoais por save**. Excluir o personagem
limpa esses dados; outro slot começa independentemente. Almas e desbloqueios
do arquivo compartilhado conservam suas regras. A Centelha não é compartilhada
entre personagens.

[ProfileService](../core/src/main/java/br/com/srsups/paradiseinhell/ProfileService.java)
recebe a identidade do personagem em `completeTutorial`, `setTutorialEnabled`
e `finishUnderworldEpilogue`, impedindo gravações no slot errado. Falhas de
autosave mantêm a conquista, a leitura e o aprendizado em memória para nova
gravação por **S no refúgio**. O resultado também mostra falhas de gravação,
inclusive quando a leitura terminou somente em memória. Fechar o processo antes de resolver a falha pode
perder o que ainda não foi gravado. Repetir a leitura ou a gravação não liquida
a tentativa outra vez.

## Validação e próximos passos

**372 testes de lógica aprovados, sem falhas**, incluindo 36 novos: 18 de
persistência, 11 do guia e sete da história. **Integração OpenGL aprovada**,
com `:lwjgl3:smokeTest` encerrado com código zero e marcador `M7_STORY_OK`.
**Execução nativa do pacote Windows aprovada em 27/10/2026**, com
`WINDOWS_PACKAGE_OK` e código de saída zero no
[script de verificação](../tools/verify_windows_package.ps1). O executável foi
extraído e iniciado de outra pasta de trabalho, com Java incluído, PATH sem Java
externo, perfis em memória e 24 efeitos/música via dispositivo OpenAL nativo
silenciado. Os testes não alteraram saves reais.
Os 336 testes e a execução gráfica/nativa de 25/10 pertencem ao lote anterior
de cenários; não são aprovação deste incremento.

[TutorialSmoke](../lwjgl3/src/test/java/br/com/srsups/paradiseinhell/lwjgl3/TutorialSmoke.java)
percorre F1 no refúgio, arena e chefe, preservação de pausa/Tab/áudio, preferência
H por save e lições locais da demo. Executa ações reais de movimento, ataque,
dash, coleta e escolha de dádiva, incluindo a proteção original de 1,5 s e
soltura do ataque. Também verifica que consultar o guia não conclui tópicos nem
grava a cada quadro. Mede os textos com a fonte real, incluindo instruções
específicas de Caçador, Guerreiro e Fazendeiro.

[UnderworldEndingSmoke](../lwjgl3/src/test/java/br/com/srsups/paradiseinhell/lwjgl3/UnderworldEndingSmoke.java)
verifica vitória e liquidação antes da leitura, proteção de entrada, revelação,
conclusão, adiamento, replay sem novo crédito, falhas de autosave e retry por S.
Também cobre descarte durante a queda/desfecho, preservando conquista e leitura
pendente sem reter recursos gráficos de uma tela que não foi exibida.
Migração, exclusão de demo/morte/abandono e independência dos perfis também
fazem parte da cobertura lógica.

As capturas reais foram inspecionadas em **960×540 e 800×600**. O enquadramento
menor usa letterbox; textos, retratos, campos e controles couberam sem cortes.

| Conteúdo | 960×540 | 800×600 |
| --- | --- | --- |
| Guia no refúgio | [captura](../build/smoke/m7-guide-refuge.png) | [captura](../build/smoke/m7-guide-refuge-800.png) |
| Guia de progressão | [captura](../build/smoke/m7-guide-progression.png) | [captura](../build/smoke/m7-guide-progression-800.png) |
| Guia de combate | [captura](../build/smoke/m7-guide-combat.png) | [captura](../build/smoke/m7-guide-combat-800.png) |
| Dica de combate | [captura](../build/smoke/m7-tutorial-combat.png) | [captura](../build/smoke/m7-tutorial-combat-800.png) |
| Dica da passagem | [captura](../build/smoke/m7-tutorial-exit.png) | [captura](../build/smoke/m7-tutorial-exit-800.png) |
| Centelha — Caronte | [captura](../build/smoke/m7-centelha-charon.png) | [captura](../build/smoke/m7-centelha-charon-800.png) |
| Centelha — protagonista | [captura](../build/smoke/m7-centelha-protagonist.png) | [captura](../build/smoke/m7-centelha-protagonist-800.png) |

Reprodução a partir da raiz do projeto, com JDK 21:

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :lwjgl3:smokeTest
.\gradlew.bat :lwjgl3:packageWinX64
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```

O script usa por padrão
`lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip`; `-Archive` permite
indicar outro pacote. A execução OpenGL exige ambiente gráfico compatível.

Partidas completas ainda devem avaliar se as dicas ajudam sem poluir a tela,
se o primeiro retorno é compreensível e se a Centelha dá sentido à vitória
local. Vídeo/remapeamento, retomada de tentativa, novos encontros/regiões,
equilíbrio e revisão sonora/artística continuam no [roteiro](ROTEIRO_DE_MELHORIAS.md).
