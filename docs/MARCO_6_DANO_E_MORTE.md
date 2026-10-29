# Marco 6 — reações de dano e animações de morte

Incremento de 23/10/2026, após o [lote de diálogos](MARCO_6_DIALOGOS_E_NPCS.md).
Este lote acrescenta poses próprias de impacto e queda ao elenco de combate.
O M6 geral continua em andamento; cenários, novos conteúdos e revisão em partidas
permanecem no [roteiro](ROTEIRO_DE_MELHORIAS.md).

## Comportamento

- As nove profissões têm reação de dano e queda, incluindo ancinho e foice do
  Fazendeiro. Roupa e arma continuam integradas ao corpo.
- Comum, rápido, tank, arqueiro, Carrasco e Guardião têm suas próprias reações.
- O impacto dura 0,18 s e usa três quadros. Não impede andar, atacar ou defender.
  Golpes sucessivos não reiniciam uma reação já em andamento.
- O dano real inicia a reação. Bloqueio, proteção e cura não geram uma pose de
  ferimento. Dano seguido de cura no mesmo passo continua sendo reconhecido.
- Carrasco e Guardião reagem ao impacto durante a recuperação; o Guardião também
  pode reagir na introdução. Seus padrões e avisos de ataque têm prioridade.
- Inimigos derrotados saem imediatamente do combate. A animação de queda usa
  quatro quadros em 0,65 s e depois desaparece em 0,25 s. Há no máximo 48 corpos
  visuais; novas quedas substituem as mais antigas quando necessário.
- XP, abates, Almas, semente, material e tentativa de necromancia são resolvidos
  no abate. O corpo visual não colide, ataca, bloqueia projéteis ou concede outra
  recompensa. Sair da sala descarta essas imagens.
- A morte final do protagonista tem uma apresentação de 0,65 s; a derrota do
  Guardião, de 1 s. A tentativa já está encerrada e liquidada durante a apresentação.
  Nenhum tempo extra entra no combate ou no relatório. Ressurreição mantém a run.

As animações de combate ficam congeladas em pausa, Tab, escolhas e configurações
de áudio. As máscaras das poses de dano acompanham seus pixels de corpo; armas,
brilhos e corpos derrotados não criam contato adicional.

## Implementação

[ActorReaction](../core/src/main/java/br/com/srsups/paradiseinhell/ActorReaction.java)
é um relógio visual puro, com prioridade de morte e reprodução única. Não altera
vida, dano, movimento ou recargas. O estado de morte segura o último quadro até
o proprietário remover a apresentação; apenas um reset explícito inicia outra vida.

[ReactionArt](../core/src/main/java/br/com/srsups/paradiseinhell/ReactionArt.java)
carrega o atlas compartilhado por PilotArt e conserva as oito direções lógicas:
cinco vistas desenhadas e três espelhadas. Cada corpo vivo possui suas próprias
posições mutáveis de hitbox. As formas de alfa e texturas são compartilhadas.
As poses originais de caminhada, repouso e ataque continuam em seus atlas.

[DeathVisuals](../core/src/main/java/br/com/srsups/paradiseinhell/DeathVisuals.java)
armazena somente ator, direção, posição, escala e idade. Não retém inimigos,
callbacks de recompensa ou projéteis. A lista tem limite e tempo de vida definidos.

A finalização da run preserva a liquidação imediata em Main. A apresentação final
recebe uma imagem congelada da arena e metadados do corpo; pode descartar os
objetos do combate antes de mostrar o resultado. Seus recursos temporários são
liberados ao trocar de tela ou fechar o jogo.

## Arte e reprodução

Foram selecionadas **16 folhas PNG**, geradas com a skill **imagegen** pela
ferramenta integrada **image_gen**, com transparência real e as imagens atuais
como referência de identidade. Dez folhas cobrem as oito profissões anteriores
e os dois conjuntos do Fazendeiro; seis cobrem comum, rápido, tank, arqueiro,
Carrasco e Guardião. Cada folha tem grade **7×5**: três colunas de dano e quatro
de morte, nas vistas baixo, cima, esquerda, cima-esquerda e baixo-esquerda.
Armas e roupa fazem parte de cada desenho. O último quadro mostra o corpo
deitado; a transparência de desaparecimento é controlada pela apresentação.

O [atlas](../core/assets/visual/m6-reactions.atlas) e a
[página PNG](../core/assets/visual/m6-reactions.png) têm **560 regiões de 64×64**
em uma página **2048×2048**, equivalente a **16 MiB RGBA** na memória gráfica.
As cinco vistas desenhadas geram **896 poses de reação** com os espelhamentos
das três vistas da direita. ReactionArt compartilha as texturas e formas de
alfa entre arenas/corpos; cada entidade mantém suas próprias posições de máscara.

O [contrato](../tools/art-sources/m6-reactions/contract.json), os
[prompts das profissões](../tools/art-sources/m6-reactions/classes-prompts.json),
os [prompts dos inimigos](../tools/art-sources/m6-reactions/enemies-prompts.json)
e suas [revisões](../tools/art-sources/m6-reactions/enemies-prompts-revisions.json)
ficam em `tools/art-sources/m6-reactions/`. A folha final do rápido usa o prompt
revisado que exige as cinco linhas; o primeiro resultado tinha quatro e não foi
selecionado. O [manifesto principal](../tools/art-sources/m6-reactions/manifest.json)
lista os 16 originais selecionados, hashes SHA-256, escala e pivôs por direção;
o [registro dos inimigos](../tools/art-sources/m6-reactions/manifest-enemies.json)
conserva o histórico das gerações. Originais e prompts ficam fora do pacote.

[ReactionAtlasBaker.java](../tools/ReactionAtlasBaker.java) realiza somente recorte
nas divisões transparentes, registro do pivô, redução por vizinho mais próximo e
empacotamento. Preserva cores e alfa gerados, sem pintar poses. Mede as silhuetas
nos atlas atuais e nos 15 quadros de dano de cada ator; o fator de escala é
**único por ator e igual nos quadros de morte**. O corpo deitado não é ampliado
para preencher a altura de um personagem em pé.

| Referência atual | Altura alvo no novo recorte de 64×64 |
| --- | --- |
| Comum e rápido: 26 pixels no atlas de 32×32 | 32,5 pixels |
| Tank: 28 pixels no atlas de 32×32 | 35 pixels |
| Oito profissões anteriores | 41–43 pixels, conforme o corpo atual |
| Fazendeiro com ancinho / foice | 44 / 42 pixels |
| Arqueiro / Carrasco / Guardião | 41 / 41 / 40 pixels |

Nos três inimigos básicos, a conversão compensa a escala de desenho 1,6 do novo
recorte: o pivô vertical usa `32 + (péAtual / alturaAtual - 0,5) × 64 / 1,6`.
Os pés ficam em 49,5 pixels, ou 48,25 na vista de costas do rápido, em vez de
assumir a base dos atores equipados. Nos demais atores, pé e centro horizontal
vêm do quadro vivo de referência da mesma direção, em geral com pé perto de 55.
Isso mantém escala e apoio compatíveis ao entrar na reação. As medidas estão em
[calibration.csv](../tools/art-sources/m6-reactions/calibration.csv), e os
recortes/deslocamentos em [registration.csv](../tools/art-sources/m6-reactions/registration.csv).

Reprodução independente com JDK 21, usando os atlas atuais como referência:

```powershell
javac -d build/art-tools tools/ReactionAtlasBaker.java
java -cp build/art-tools ReactionAtlasBaker
```

A [folha de revisão frontal](../build/smoke/m6-reactions-contact-sheet.png)
mostra os 16 atores, cada um com três quadros de dano e quatro de morte.
O M6 geral continua em andamento, com cenários, futuros encontros/interações
de NPCs e revisão humana das transições em partidas completas.

## Validação

**320 testes unitários, integração OpenGL e execução nativa do pacote Windows
aprovados.** A verificação gráfica carrega as 896 poses, confere seus espelhamentos
e compara as máscaras de dano com os pixels visíveis. As três criaturas básicas
usam o alfa completo; os corpos equipados mantêm o recorte central que exclui armas.

O fluxo real verifica abates únicos, XP e moedas, limite/expiração de corpos,
reação de aura em Carrasco/Guardião, pausa/Tab/áudio, troca de ferramenta do
Fazendeiro e ressurreição. No encerramento, confirma liquidação imediata, arena
descartada, relógios parados e liberação da imagem congelada e da tela de resultado
ao concluir ou fechar o jogo. A troca de ferramenta conserva a transição visual
existente; a reação usa a ferramenta da pose predominante.

Capturas da integração:

- [Início das quedas de comum/arqueiro](../build/smoke/m6-enemy-death.png).
- [Queda do protagonista, pausada](../build/smoke/m6-player-death.png).
- [Queda do Guardião](../build/smoke/m6-guardian-death.png).
- [Todos os quadros frontais](../build/smoke/m6-reactions-contact-sheet.png).

O ZIP atualizado fica em `lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip`.
Foi executado com o Java incluído, sem Java no PATH e com áudio nativo pelo
dispositivo OpenAL silencioso. O teste não utiliza os saves reais do jogador.
O aceite humano continua incluindo uma travessia com cada profissão para avaliar
suavidade, leitura dos perigos e continuidade das silhuetas.
