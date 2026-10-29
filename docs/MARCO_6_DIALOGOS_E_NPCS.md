# Marco 6 — diálogos e retratos dos NPCs

Lote de diálogos/NPCs implementado em **23/10/2026**, seguindo o
[roteiro de melhorias](ROTEIRO_DE_MELHORIAS.md). Caronte, Ferreiro Errante e
Camponesa das Cinzas usam uma apresentação compartilhada, com retrato e expressão
por fala. Este lote está concluído; o **M6 geral continua em andamento**. O avanço
seguinte está no [lote de dano/morte](MARCO_6_DANO_E_MORTE.md); cenários futuros e
arte de conteúdos ainda não implementados permanecem no roteiro.

**Direção futura registrada em 27/10/2026:** próximas conversas/cutscenes devem
seguir as referências Symphony of the Night e Metal Slug, com retratos e
atuação dos sprites no cenário, gestos e objetos sincronizados por eventos.
O [planejamento de cutscenes](CUTSCENES_E_ABERTURA.md) distingue leitura,
conclusão de voz e marcadores de animação. Este lote e a abertura existente
continuam como implementados; a nova direção não altera seus controles agora.

## Conversas e ações

O painel mostra retrato, nome do interlocutor, texto e controles. A expressão
vem da fala atual, com quatro opções por NPC: neutra, divertida, preocupada e
satisfeita. O retrato do protagonista já existente acompanha suas respostas.
A revelação gradual usa 42 caracteres Unicode por segundo; uma entrada revela
a fala inteira, e outra avança. O texto não avança sozinho. **T** alterna entre
revelação gradual e instantânea, com o mesmo modo ao passar de uma conversa para
outra durante a sessão.

| Local | Ler a fala | Ação explícita |
| --- | --- | --- |
| Caronte no refúgio | Enter, E ou clique revelam/avançam; T alterna texto | Concluir a última fala registra o evento existente; Esc fecha sem concluí-lo |
| Ferreiro Errante | Espaço ou clique **na área da fala** revelam/avançam; T alterna texto | 1/E ou botão Melhorar compra; 2/Enter/Esc ou botão Continuar parte |
| Camponesa das Cinzas | Enter/E ou clique revelam o texto; T alterna texto | E/Enter ou botão Aceitar registra a promessa após a proteção de entrada; Esc/Continuar sai |
| Diário da promessa no refúgio | J abre a etapa atual; T alterna texto | Enter/E ou botão retorna ao refúgio; Esc fecha |

A Camponesa mantém a espera de **1,5 segundo** para aceitar e exige soltar o
botão de ataque antes de reconhecer um novo clique. Revelar uma fala não aceita
a missão no mesmo comando. Após aceitar, a confirmação também tem sua proteção
de entrada. O diário consulta a missão do M5, sem iniciar ou concluir objetivos.

A forja diferencia arma já Rara, falta de material, disponibilidade da melhoria,
compra concluída e falha de gravação. O Ferreiro Errante reconhece a profissão
Ferreiro, e as falas usam o nome da ferramenta ativa. O painel de compra mantém
custo, estoque, raridade, efeito e permanência visíveis. Ler com Espaço ou clicar
no diálogo não compra a melhoria; os comandos de compra continuam explícitos.

Este lote preserva a economia, os saves, o dano e a raridade. Concluir a primeira
conversa de Caronte mantém o presente único de 5 Óbolos. A forja continua custando
um Fragmento para Comum → Rara, com gravação antes de gastar/aplicar e melhoria
permanente entre tentativas. Apresentar uma fala de sucesso não concede a melhoria:
ela depende da operação de compra. A missão, semente e desbloqueio do Fazendeiro
mantêm as regras do [M5](MARCO_5_FAZENDEIRO.md). Probabilidades de encontros não mudaram.

## Conteúdo e modelos

As conversas ficam fora das telas, em três arquivos:

- [Caronte](../core/assets/dialogues/charon.json): eventos existentes e expressões
  nas falas de Caronte/protagonista.
- [Ferreiro Errante](../core/assets/dialogues/blacksmith.json): disponibilidade,
  profissão Ferreiro, falta de material, arma Rara, sucesso e falha; `{weapon}`
  recebe o nome da arma ativa.
- [Camponesa](../core/assets/dialogue/camponesa.json): oferta, aceitação e mensagens
  das etapas da promessa, com retratos correspondentes.

[DialogueLine](../core/src/main/java/br/com/srsups/paradiseinhell/DialogueLine.java)
guarda interlocutor, texto e retrato imutáveis.
[DialoguePortrait](../core/src/main/java/br/com/srsups/paradiseinhell/DialoguePortrait.java)
define IDs estáveis, sem carregar texturas. Na ausência do campo opcional de
retrato, o modelo usa a expressão neutra do interlocutor; IDs inválidos são
rejeitados. Isso mantém compatibilidade com as falas anteriores.

[CaronteDialogue](../core/src/main/java/br/com/srsups/paradiseinhell/CaronteDialogue.java),
[BlacksmithDialogue](../core/src/main/java/br/com/srsups/paradiseinhell/BlacksmithDialogue.java)
e [CamponesaDialogue](../core/src/main/java/br/com/srsups/paradiseinhell/CamponesaDialogue.java)
selecionam o conteúdo conforme fatos já registrados. Consultar esses modelos não
concede moedas, compra arma nem altera objetivos.
[DialoguePlayback](../core/src/main/java/br/com/srsups/paradiseinhell/DialoguePlayback.java)
controla revelação e avanço de uma fala por entrada, preservando caracteres Unicode.
[DialoguePanel](../core/src/main/java/br/com/srsups/paradiseinhell/DialoguePanel.java)
é o componente visual compartilhado: retrato com proporção preservada, nome,
quebra de texto, progresso da conversa e controles. As telas continuam responsáveis
por entrada, compras, conclusão de evento e descarte de seus próprios recursos.

## Retratos e atlas

Foram geradas **três folhas transparentes em grade 2×2**, uma por NPC, com
quatro expressões cada. Mantêm identidade, roupas, ângulo e iluminação entre
expressões. O Ferreiro Errante tem aparência própria, distinta da profissão
Ferreiro do jogador; o protagonista conserva seu retrato anterior.

| NPC | Origem selecionada | Regiões |
| --- | --- | --- |
| Caronte | [Folha de expressões](../tools/art-sources/m6-dialogue/charon-expressions-original.png) | `portrait-charon-neutral/amused/concerned/pleased` |
| Ferreiro Errante | [Folha de expressões](../tools/art-sources/m6-dialogue/blacksmith-expressions-original.png) | `portrait-blacksmith-neutral/amused/concerned/pleased` |
| Camponesa das Cinzas | [Folha de expressões](../tools/art-sources/m6-dialogue/camponesa-expressions-original.png) | `portrait-camponesa-neutral/amused/concerned/pleased` |

O [atlas](../core/assets/visual/m6-dialogue.atlas) e a
[página PNG](../core/assets/visual/m6-dialogue.png) têm **12 regiões de 256×256**,
em uma página **1024×1024**, equivalente a **4 MiB RGBA** na memória gráfica.
ExpandedArt carrega e compartilha o recurso entre as telas, descartando-o uma
única vez. DialoguePanel apenas usa os recortes; não carrega nem descarta texturas.
Sprites do mundo, armas e máscaras de colisão permanecem os já implementados.

A geração usou **imagegen pela ferramenta integrada image_gen**. Os
[prompts exatos](../tools/art-sources/m6-dialogue/prompts.json),
[manifesto com seleção/SHA-256](../tools/art-sources/m6-dialogue/manifest.json)
e [registro dos recortes](../tools/art-sources/m6-dialogue/registration.csv)
permitem revisar a origem. Caronte e Camponesa usam as imagens existentes do
projeto como referência de identidade. Os originais ficam fora da distribuição.

[DialoguePortraitAtlasBaker.java](../tools/DialoguePortraitAtlasBaker.java)
faz somente recorte uniforme do busto, registro, redução por vizinho mais próximo
e empacotamento. Preserva cores e alfa gerados; não pinta rostos ou expressões.
Reprodução com JDK 21:

```powershell
javac -d build/art-tools tools/DialoguePortraitAtlasBaker.java
java -cp build/art-tools DialoguePortraitAtlasBaker
```

## Validação e revisão visual

**302 testes unitários, sem falhas, e integração OpenGL aprovados.** A verificação
cobre leitura/avanço, expressões e Unicode, compatibilidade dos JSONs, condições
da forja, ações explícitas e preservação da missão do M5. A revisão das capturas
confirmou encaixe e legibilidade em **960×540 e 800×600**, com letterbox real na
janela menor. O painel da Camponesa tem altura de 285 unidades para acomodar
também o texto mais longo do diário da promessa concluída; as verificações de
limites continuam estritas.

| Estado | 960×540 | 800×600 |
| --- | --- | --- |
| Caronte | [Conversa](../build/smoke/m6-charon.png) | [Conversa](../build/smoke/m6-charon-800.png) |
| Resposta do protagonista | [Retrato e fala](../build/smoke/m6-protagonist.png) | [Retrato e fala](../build/smoke/m6-protagonist-800.png) |
| Ferreiro: sem material | [Condição da forja](../build/smoke/m6-blacksmith-missing.png) | [Condição da forja](../build/smoke/m6-blacksmith-missing-800.png) |
| Ferreiro: falha ao gravar | [Material preservado](../build/smoke/m6-blacksmith-save-error.png) | [Material preservado](../build/smoke/m6-blacksmith-save-error-800.png) |
| Ferreiro: compra concluída | [Arma Rara](../build/smoke/m6-blacksmith-success.png) | [Arma Rara](../build/smoke/m6-blacksmith-success-800.png) |
| Camponesa: oferta | [Promessa disponível](../build/smoke/m6-camponesa-offer.png) | [Promessa disponível](../build/smoke/m6-camponesa-offer-800.png) |
| Camponesa: falha ao gravar | [Aceitação pendente](../build/smoke/m6-camponesa-save-error.png) | [Aceitação pendente](../build/smoke/m6-camponesa-save-error-800.png) |
| Camponesa: promessa aceita | [Confirmação](../build/smoke/m6-camponesa-accepted.png) | [Confirmação](../build/smoke/m6-camponesa-accepted-800.png) |
| Camponesa: diário | [Etapa da missão](../build/smoke/m6-camponesa-journal.png) | [Etapa da missão](../build/smoke/m6-camponesa-journal-800.png) |
| Camponesa: desbloqueio global | [Promessa concluída](../build/smoke/m6-camponesa-global.png) | [Promessa concluída](../build/smoke/m6-camponesa-global-800.png) |

O [catálogo das 12 expressões](../build/smoke/m6-portrait-expressions.png) permite
comparar os retratos. As telas também exercitam cliques vindos do combate e a
diferença entre ler uma fala e confirmar uma compra/aceitação.
A execução nativa do pacote Windows **também foi aprovada**: Java incluído,
sem Java no PATH, pasta de trabalho externa ao pacote e áudio nativo pelo
dispositivo OpenAL silencioso. O diagnóstico inclui a forja e o diário da Camponesa,
verificando os textos e retratos empacotados. O ZIP atualizado está em
`lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip`.
