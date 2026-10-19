# Marco 3 — Base visual e conversa piloto

Implementado em **15/10/2026**. Este documento registra a **entrega inicial do M3**.
A pedido do autor, o elenco atual foi ampliado depois do piloto, na mesma data.
O estado vigente, incluindo retrato do protagonista, classes, inimigos, armas,
baús e tela inteira de dádivas, está em [EXPANSAO_VISUAL.md](EXPANSAO_VISUAL.md).
Os números e limites abaixo são o registro histórico do piloto.

## Entrega jogável

- Noto Sans no corpo de texto das telas; Cinzel no título do menu e nos nomes
  do diálogo. Arquivos de fonte incluídos no jogo, com os acentos portugueses.
- Menu, cartões de dádivas e caixa de conversa usam a paleta de pedra/bronze/azul
  do Estige e molduras compartilhadas em UiTheme.
- Caçador e inimigo comum têm quatro quadros de caminhada em quatro direções.
  Parar conserva a direção; virar conserva a fase da passada. Uma margem de
  histerese evita alternância de direção por pequenas variações diagonais.
- Flecha, impacto do arco, pulso de XP e rastro de dash usam regiões de sprites.
  As imagens são apresentação; dano, alcance, recargas e janelas continuam nos modelos.
- Ícones de vida, estamina e XP nas barras; símbolo de dádiva no cartão.
- Caronte tem retrato ilustrado; a fala do protagonista usa o sprite piloto como
  retrato provisório. A caixa inferior deixa o cenário do refúgio visível.
- Primeiro Enter/E/clique completa o texto; o seguinte avança uma fala.
  T alterna texto gradual/instantâneo, mantendo a preferência nesta sessão do jogo.
  Esc fecha sem registrar conclusão; reabrir começa pela primeira fala.
  O presente de 5 Óbolos continua concedido uma única vez, ao concluir a conversa.

## Assets e reprodução

[Atlas do piloto](../core/assets/visual/pilot.png) e
[regiões nomeadas](../core/assets/visual/pilot.atlas): 1024×512 RGBA, 53 regiões.
Inclui 32 quadros de atores, 16 quadros de efeitos, quatro ícones e um retrato.

As cinco imagens de origem estão em tools/art-sources/m3: hunter.png, common.png,
effects.png, icons.png e charon.png. Foram geradas com a ferramenta integrada
**image_gen**, pela habilidade imagegen, sem CLI/API. As imagens de jogos anexadas
orientaram a composição da interface; não foram usadas como alvos de edição.
[Conjunto exato de prompts](../tools/art-sources/m3/prompts.json).

A ferramenta [PilotAtlasBaker.java](../tools/PilotAtlasBaker.java) faz apenas
o preparo técnico: recorte por alfa de cada célula, mesma escala por personagem,
pivô central nos pés, redução para quadros de 32×32 e empacotamento.
As fontes e origens permanecem locais para permitir reproduzir os arquivos:

```powershell
java tools/PilotAtlasBaker.java
java tools/FontBaker.java
```

[Cinzel](https://github.com/google/fonts/tree/main/ofl/cinzel) e
[Noto Sans](https://github.com/google/fonts/tree/main/ofl/notosans) vêm do
Google Fonts. As licenças OFL acompanham os PNG/FNT distribuídos.
TTFs de origem e ferramentas offline ficam fora dos recursos do jogo.
O bake usa AWT somente na ferramenta; não adiciona biblioteca nativa ao runtime.
[Origens e comando](../tools/font-sources/README.md).

## Organização e colisões

Main carrega PilotArt sob demanda, mantém uma única textura entre telas e a
descarta ao encerrar. Entidades tomam regiões emprestadas; não criam texturas.
UiTheme centraliza a criação das fontes e estilos; cada tela descarta suas fontes.
Máscaras são extraídas uma vez por quadro do atlas e reutilizam as formas imutáveis.
Cada entidade mantém suas próprias instâncias posicionáveis, evitando sobrescrever
a máscara de outro inimigo ao consultar contato.

O piloto usa o mesmo quadro, posição e escala para desenhar e testar contato.
Acessórios e brilho dos efeitos não fazem parte dos quadros de corpo.
Navegação segue separada das máscaras. Ataques e avisos de chefes conservam as
geometrias que já orientam dano e esquiva.

CaronteDialogue lê [charon.json](../core/assets/dialogues/charon.json) via recurso
de classe, inclusive dentro do JAR. Os eventos de primeiro encontro, suborno e
vitória continuam escolhidos pelo perfil. DialoguePlayback só controla revelação
e avanço; não tem acesso à moeda. Não há run ativa avançando durante o refúgio.

## Validação e limites

228 testes de regras aprovados. Integração OpenGL verifica português, quebras de
texto, revelação por clique, cancelamento, conclusão/crédito único, modo instantâneo,
movimento/animação, geração real de inimigos, pausa/Tab e descarte do atlas.
Os 32 quadros têm todas as posições de pixels comparadas com as máscaras de contato.
Capturas de menu, conversa, combate, efeitos e dádivas em build/smoke/m3-*.png,
revisadas em 960×540 e com diálogo em 800×600.

O pacote Windows foi conferido com seu Java incluído, diretório de trabalho
externo, atlas/fontes/JSON, 24 efeitos de áudio nativos e música em streaming via
OpenAL nulo. ZIP atualizado em lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip
(43.432.272 bytes). Os testes usaram perfis em memória/temporários, preservando saves reais.
As regras e assets do restante do elenco continuam no estágio anterior.
Ataques corpo a corpo, magia, escudos, chefes e perigos ainda têm formas provisórias.
Expressões adicionais, retrato ilustrado do protagonista e animações próprias de
ataque, dano e morte entram na expansão artística do M6.
O texto instantâneo é uma preferência de sessão, não persistida em arquivo.

## Próximo incremento

M4: ampliar dádivas, criar a primeira dádiva exclusiva do Submundo e ampliar o altar.
A expansão do elenco atual antecipou parte do M6. Revisar sua leitura na escala
real antes de produzir os próximos conteúdos de M4/M5.
[Roteiro atualizado](ROTEIRO_DE_MELHORIAS.md).

