# Marco 7 — remapeamento de controles

Incremento de controles do M7, implementado e aprovado em **424 testes de
lógica, sem falhas ou erros, integração OpenGL e execução nativa do pacote
Windows**. O resultado anterior de 412 testes pertence ao piloto da abertura.
A revisão visual do M6, os playtests de equilíbrio e as demais entregas do M7
continuam abertos.

Este lote permite editar os comandos de movimento e combate sem criar outro
personagem. A preferência é global, independente dos oito saves, da profissão,
da dificuldade, de Óbolos, Almas e desbloqueios. A abertura existente e a
direção futura de cutscenes permanecem como registradas anteriormente.

## Entradas e pausa

| Local | Entrada | Comportamento |
| --- | --- | --- |
| Menu | F3 ou botão CONTROLES | Abre ControlsSettingsScreen e retorna ao menu |
| Refúgio | F3 | Sobrepõe ControlsPanel, preservando conversa/altar e texto revelado |
| Arena | F3 | Sobrepõe o painel, sem mover, atacar, avançar inimigos ou relógios |
| Sala do Guardião | F3 | Sobrepõe o painel, preservando padrão do chefe e estado da luta |

Os quadros de abertura e fechamento do painel são consumidos antes da
simulação. Fechar não força a saída de uma pausa, consulta de dádivas,
conversa ou escolha que já estivesse aberta. Os comandos segurados ao aplicar
ou fechar ficam bloqueados para gameplay até a soltura física, evitando
disparo, defesa ou dash involuntários na volta.

## Editar e salvar

O painel tem duas páginas, com até seis ações por página. Cada ação oferece
um comando principal e um alternativo. Clique em uma associação para
substituí-la, ou em Adicionar para preencher o segundo comando. Solte os
controles e aguarde a proteção de 0,2 segundo antes de pressionar a nova tecla
ou botão do mouse. O clique que abre a captura não vira a nova associação.

| Ação no painel | Controle |
| --- | --- |
| Trocar página | Setas esquerda/direita ou Anterior/Próxima |
| Cancelar captura | Esc |
| Remover associação durante a captura | Delete ou Backspace; deve restar pelo menos um comando para a ação |
| Aplicar e salvar rascunho | Enter ou SALVAR |
| Restaurar configuração inicial no rascunho | RESTAURAR PADRÃO; precisa salvar para aplicar |
| Repetir gravação que falhou | S no painel, fora da captura |
| Fechar | Esc fora da captura, F3 ou VOLTAR |

Editar não altera os controles ativos até Salvar. Sair descarta as alterações
não aplicadas. Comandos duplicados, conflitos com outra ação e teclas reservadas
mostram uma mensagem; a associação anterior é conservada. A captura continua
disponível para escolher outra entrada ou cancelar.

Se a gravação falhar, os controles aplicados continuam nesta sessão e o painel
informa a falha. S repete a tentativa de gravação. Se a leitura inicial falhar
ou o arquivo pertencer a uma versão incompatível, usar o padrão e permitir
edições apenas na sessão, preservando o arquivo original.

## Ações e configuração inicial

| Ação | Principal | Alternativo |
| --- | --- | --- |
| Mover para cima | W | Seta cima |
| Mover para baixo | S | Seta baixo |
| Mover para a esquerda | A | Seta esquerda |
| Mover para a direita | D | Seta direita |
| Atacar | Mouse esquerdo | — |
| Dash | Shift esquerdo | Shift direito |
| Defender | Mouse direito | — |
| Interagir / atravessar | E | Enter |
| Abrir baú | F | — |
| Trocar ferramenta do Fazendeiro | Q | — |
| Ver dádivas em combate | Tab | — |

A mira continua acompanhando o mouse. Atacar e defender podem usar tecla ou
botão do mouse; a profissão continua determinando o efeito da ação. O refúgio
mantém ataques desativados. Enter no refúgio conserva o atalho global para
partir, e os menus, diálogos, escolhas e confirmações mantêm seus controles
próprios. As dicas e os avisos de jogabilidade consultam as associações ativas.

Esc, F1, F2, F3, O, H, C, L, P, R, J, V, I e M são reservados aos controles de
interface e atalhos. Enter pode pertencer somente à ação Interagir. Delete e
Backspace removem associações durante a captura. Teclas de sistema e códigos
sem suporte são recusados.

São aceitos teclado e os cinco botões usuais do mouse, quando disponíveis:
esquerdo, direito, central e dois laterais. Este lote não implementa gamepad,
combinações de teclas, roda do mouse ou remapeamento dos menus. Remapear
controles também não acrescenta retomada de tentativa nem escala livre do HUD.

## Modelo, persistência e leitura do input

[ControlAction](../core/src/main/java/br/com/srsups/paradiseinhell/ControlAction.java)
enumera 11 ações de jogabilidade. Cada
[InputBinding](../core/src/main/java/br/com/srsups/paradiseinhell/InputBinding.java)
identifica uma tecla ou botão e fornece rótulo e consulta física.
[ControlSettings](../core/src/main/java/br/com/srsups/paradiseinhell/ControlSettings.java)
mantém dados imutáveis, uma ou duas associações por ação, validação de conflitos
e serialização estrita. O JSON tem versão 1, array bindings com action e inputs,
e cada input contém type KEY/MOUSE e code inteiro. Campos desconhecidos ou
duplicados, tipos incorretos, ações ausentes/repetidas e códigos inválidos são
rejeitados. Uma versão futura é reconhecida antes de validar o schema atual.

[ControlSettingsService](../core/src/main/java/br/com/srsups/paradiseinhell/ControlSettingsService.java)
carrega uma vez, aplica a confirmação e mantém gravações falhas pendentes.
[FileControlSettingsStore](../core/src/main/java/br/com/srsups/paradiseinhell/FileControlSettingsStore.java)
guarda `control-settings.json` junto às demais preferências, fora da instalação,
em UTF-8, com arquivo temporário, troca atômica quando suportada e backup
validado `.bak`. Falha no arquivo principal tenta o backup. Versão futura
no principal bloqueia esse fallback; a sessão assume o padrão sem sobrescrever
os arquivos. Um backup de versão futura também é protegido contra gravação,
inclusive quando o principal é válido e a chamada vai diretamente ao store.
Trocar/excluir um personagem não grava ou apaga essas preferências.

[GameControls](../core/src/main/java/br/com/srsups/paradiseinhell/GameControls.java)
centraliza consultas de comando segurado, novo pressionamento, estado físico e
rótulos. O bloqueio de retorno é por entrada física e termina ao soltar o
comando. Os guardas de escolha verificam o estado físico do ataque, inclusive
quando o gameplay está bloqueado, para continuar exigindo soltura antes de
confirmar uma dádiva. A navegação dos menus não passa por esse remapeamento.

## Validação

[ControlSettingsTest](../core/src/test/java/br/com/srsups/paradiseinhell/ControlSettingsTest.java)
acrescenta 12 testes de lógica para aliases, imutabilidade, conflitos, teclas
reservadas, parser, versões futuras com campos novos, persistência, backup,
falha/retry e bloqueio físico. **424 testes de lógica passaram, sem falhas ou
erros**: base histórica de 412 mais os 12 novos. `:lwjgl3:testClasses` compilou.

**Integração OpenGL aprovada**, com `:lwjgl3:smokeTest` encerrado com código
zero e marcadores `M7_CONTROLS_OK` e `DEMO_SMOKE_OK`.
[ControlSmoke](../lwjgl3/src/test/java/br/com/srsups/paradiseinhell/lwjgl3/ControlSmoke.java)
percorreu as entradas reais, as duas páginas, captura/cancelamento/remoção/
restauração, salvar e falhar/repetir, os estados preservados e a execução de
movimento/ataque/dash com novas entradas. Verificou conflito sem alterar o
rascunho, rejeição do clique segurado na captura e bloqueio do ataque ao voltar,
inclusive durante uma escolha de dádiva. Também mediu os rótulos das
associações e produziu capturas em 960×540 e 800×600.

| Estado | 960×540 | 800×600 |
| --- | --- | --- |
| Controles pelo menu | [captura](../build/smoke/m7-controls-menu.png) | [captura](../build/smoke/m7-controls-menu-800.png) |
| Segunda página | [captura](../build/smoke/m7-controls-page-two.png) | [captura](../build/smoke/m7-controls-page-two-800.png) |
| Falha de gravação | [captura](../build/smoke/m7-controls-save-error.png) | [captura](../build/smoke/m7-controls-save-error-800.png) |
| Sobre combate | [captura](../build/smoke/m7-controls-combat.png) | [captura](../build/smoke/m7-controls-combat-800.png) |
| Sobre refúgio | [captura](../build/smoke/m7-controls-refuge.png) | [captura](../build/smoke/m7-controls-refuge-800.png) |
| Sobre o chefe | [captura](../build/smoke/m7-controls-boss.png) | [captura](../build/smoke/m7-controls-boss-800.png) |

As capturas dos controles pelo menu e da falha de gravação em 960×540, da
segunda página em 800×600 e do menu principal em 960×540 foram inspecionadas
visualmente, com textos legíveis. Demais capturas permitem revisão posterior.

**Execução nativa do pacote Windows aprovada**: `:lwjgl3:packageWinX64` e o
[script de verificação](../tools/verify_windows_package.ps1) encerraram com
código zero, com `WINDOWS_PACKAGE_OK`. O executável incluiu Java, com PATH sem
Java externo, perfis/progresso e preferências em memória e OpenAL em dispositivo
null, misturando 24 efeitos e música por streaming sem tocar nos alto-falantes.
O diagnóstico oculto do launcher renderiza ControlsSettingsScreen depois da
abertura no quadro 29 e emite `PACKAGED_CONTROLS_SMOKE_OK`.

A revisão humana dos controles continua. Os smokes preservam saves,
preferências e configurações físicas do usuário. Esse resultado não encerra o
M7 amplo, a escala livre do HUD, a retomada de tentativa ou os playtests.

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :lwjgl3:smokeTest
.\gradlew.bat :lwjgl3:packageWinX64
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```
