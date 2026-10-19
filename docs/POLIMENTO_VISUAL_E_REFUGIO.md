# Polimento de movimento e refúgio

Revisão de **15/10/2026** sobre [a entrega de armas integradas](ANIMACOES_E_EQUIPAMENTOS.md),
com base no parecer do autor sobre as diagonais, saltos entre quadros e repouso.
Parte adicional do M6 antecipada antes das novas dádivas/altar do M4.

## Resultado

- As oito profissões têm cinco vistas desenhadas: baixo, cima, esquerda,
  cima-esquerda e baixo-esquerda. Direita e suas duas diagonais são espelhamentos
  horizontais das vistas da esquerda. Não se gira o corpo para produzir diagonais.
- Diagonais inferiores mostram rosto e peito em três quartos, distinguindo-se
  do perfil. A associação das diagonais do Necromante foi refeita. O Guerreiro
  carrega uma espada e um único escudo, inclusive no perfil.
- Cada classe tem quatro quadros próprios de respiração com pés apoiados.
  O repouso aparece nas arenas, no refúgio e nos cartões de escolha de profissão.
- Caminhadas usam seis posições a 0,135 s/quadro (ciclo de 0,81 s);
  respiração usa 0,40 s/quadro (ciclo de 1,60 s). Viradas preservam a fase dos passos.
  Mudanças de direção/ação misturam a pose anterior e a nova por 0,08 s.
- Ataques têm quatro poses e duração visual entre 0,20 e 0,48 s, calculada a
  partir da cadência real. Isso reduz a pressa visual da Feiticeira. O relógio
  da imagem não resolve disparos nem muda os ataques/s do jogador.
- Armas continuam incorporadas à imagem do personagem. Foram revistos os
  ataques/diagonais, incluindo os frontais de arco, pistola, varinha e cajado.
  Os ícones isolados continuam na forja.
- A conjuração do Guardião usa cinco vistas corretas e as três espelhadas:
  rosto, braços e saída do poder acompanham a direção do ataque.
- Caronte em corpo inteiro usa o mesmo homem idoso de manto azul e remo do
  retrato, substituindo o esqueleto no refúgio. Altar de pedra/bronze com chama
  azul e arco da travessia com névoa violeta têm quatro quadros cada.
- O refúgio desativa disparo, animação de ataque e consumo da recarga de tiro,
  inclusive segurando o mouse ao caminhar ou clicando em conversas.

## Alinhamento e colisão

O preparo técnico detecta as faixas transparentes das folhas, mantém escala
comum por sequência e registra cabeça/tronco contra uma referência da direção.
Ajusta somente a posição dos pixels recortados; não desenha novas poses.
Pernas e extensões das armas não determinam o alinhamento do corpo.
O [CSV de registro](../build/smoke/polish-registration.csv) contém translações
e coluna de origem de cada quadro.

Dois quadros da diagonal inferior do Sacerdote ainda trocavam o talismã de mão,
mesmo após retoque. O preparo descarta essas duas poses e usa a sequência de
origem **0, 1, 4, 5, 4, 1**, com as quatro poses coerentes, em seis posições
de reprodução. Essa seleção está explícita no manifesto e no código do preparo.

EquippedArt conserva máscaras de alfa no tronco/pernas. As três vistas da
direita espelham tanto textura quanto máscara; não alteram a região compartilhada.
Durante a mistura, a colisão segue a pose predominante.
Navegação, dano, áreas dos golpes, recargas, recompensas e saves conservam suas
regras. PlayerAnimation apenas consulta movimento, mira e estado de ataque.
Tab/pausa/áudio conservam o congelamento da simulação nas arenas.

## Origens e preparo

**20 PNGs finais** em tools/art-sources/polish: oito caminhadas, oito folhas
com repouso/ataque, uma conjuração do chefe e três elementos do refúgio.
Gerados com **imagegen, ferramenta integrada image_gen**, sem CLI/API.
Sete versões intermediárias ficam identificadas por v1/v2; a imagem de referência
das classes preserva a identidade estabelecida na entrega anterior.

- [Manifesto com caminhos finais e prompts exatos](../tools/art-sources/polish/manifest.json).
- [Primeira geração](../tools/art-sources/polish/prompts.json).
- [Repouso/ataque e primeiros reparos](../tools/art-sources/polish/actions-and-repairs-prompts.json).
- [Reparos de direção](../tools/art-sources/polish/direction-repairs-prompts.json)
  e [talismã](../tools/art-sources/polish/priest-hand-repair-prompt.json).
- [Empacotador](../tools/EquippedAtlasBaker.java) e
  [registro/seleção/prévias](../tools/PolishedFrames.java).

O atlas equipado passa a **1.002 regiões**: 926 quadros de corpo empacotados,
64 avisos/impactos e 12 quadros do refúgio. Os espelhamentos disponibilizam
**1.280 poses de corpo** ao renderer. Duas páginas, 2048×2048 e 2048×256,
ocupam **18 MiB RGBA**, sem mipmaps, compartilhadas por Main/PilotArt.
Originais e versões intermediárias ficam fora do pacote para jogadores.

```powershell
javac -d build/art-tools tools/EquippedAtlasBaker.java tools/PolishedFrames.java
java -cp build/art-tools EquippedAtlasBaker
.\gradlew.bat core:test lwjgl3:smokeTest lwjgl3:packageWinX64 --console=plain --warn
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```

## Verificação e revisão visual

**241 testes unitários e integração OpenGL aprovados.** Cobertura de mapeamento
das vistas, ciclo de repouso das oito classes, cadência/continuidade dos passos,
mistura nas viradas e relógio visual de ataque. A integração confere as máscaras
das 1.280 poses pixel a pixel, espelhamento das diagonais, variação dos quatro
quadros de repouso por classe e ausência de combate no refúgio/conversas.
Os perfis usados nos testes ficam em memória/arquivos temporários.

Prévias geradas pelo preparo e pela integração:

- [Caminhadas animadas](../build/smoke/polish-walk.gif),
  [repouso animado](../build/smoke/polish-idle.gif) e
  [ataques animados](../build/smoke/polish-attack.gif).
- [Oito direções das classes](../build/smoke/equipped-eight-directions.png)
  e [quadros de repouso](../build/smoke/polish-idle-classes.png).
- [Conjuração nas oito direções](../build/smoke/polish-boss-cast-directions.png).
- [Refúgio com as três sprites](../build/smoke/polish-refuge.png).

O [pacote Windows](../lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip)
foi atualizado e aprovado na verificação nativa: **48.856.855 bytes**. Inclui as
novas páginas, atlas e instruções do refúgio; executou com o Java incluído,
PATH sem Java e diretório de trabalho externo. O diagnóstico de áudio validou
24 efeitos nativos e música em streaming por OpenAL nulo.

Revisar as viradas e o repouso em uma partida com cada classe, principalmente
Caçador, Ferreiro, Guerreiro e Necromante nas diagonais inferiores.
O próximo marco de gameplay segue sendo **M4: novas dádivas, primeira dádiva
regional e novas melhorias do altar**. Poses de dano/morte, novos NPCs/cenários
e arte dos conteúdos futuros continuam nos marcos seguintes.
