# Cutscenes e diálogos — direção futura e abertura existente

Atualizado em **27/10/2026**. Após o piloto, o autor definiu **Castlevania:
Symphony of the Night (1997) e Metal Slug** como referências para o estilo visual
e a encenação das próximas cutscenes e diálogos. Também pediu gatilhos espaciais,
sequências comandadas por eventos e uso de geometria 3D texturizada quando a
cena precisar desse efeito. **Esta é uma orientação para trabalhos futuros:
não refazer a abertura agora.**

O [piloto da abertura](PILOTO_DA_ABERTURA.md) permanece com suas ilustrações
animadas em camadas, modelo, persistência, apresentação e atlas implementados.
Essa foi a direção aprovada para produzir o piloto. Zeus clássico e esposa
grega adulta continuam como referências de identidade; nome e detalhes de
personalidade da esposa seguem abertos.
Os 412 testes de lógica passaram sem falhas ou erros; OpenGL e a execução
nativa do pacote Windows do piloto foram aprovados em 27/10/2026.

## Direção para as próximas cutscenes e diálogos

As cenas devem ser encenadas em tempo real no cenário, com personagens em
sprites, poses e ações próprias. **Symphony of the Night orienta a conversa
encenada e sua continuidade com a sala; Metal Slug orienta a expressividade,
o movimento e o humor físico.** Câmera, retratos, texto, efeitos e som ajudam
a contar a cena. O enquadramento deve funcionar na perspectiva deste jogo.

| Elemento | Direção registrada |
| --- | --- |
| Personagens na sala | Caminhar até marcas, virar para o interlocutor, respirar, gesticular, reagir e executar ações ligadas à fala |
| Animação | Sequências de quadros com antecipação, ação e recuperação; manter pés/pivôs consistentes e transições legíveis |
| Conversas | Retrato expressivo, nome, texto legível e atuação do sprite ao mesmo tempo; preservar a leitura manual e a revelação instantânea |
| Objetos de cena | Props com estados e animações próprios, como um objeto que sai da mão, cai e se quebra |
| Câmera e cenário | Enquadramento da ação, profundidade e efeitos pontuais de perspectiva com planos ou geometria 3D texturizados |
| Integração | Uma condição da sala inicia a sequência, o diretor assume os atores e depois devolve o controle ao jogador |

Os vídeos fornecidos foram examinados por amostragem de quadros. As observações
abaixo são qualitativas, não medições de sincronismo de áudio.

| Arquivo de referência | Conteúdo observado | O que aproveitar |
| --- | --- | --- |
| `C:\Users\Usuario\Downloads\videoplayback.mp4` — 45,05 s, 640×360 | Conversa de Richter e Drácula na sala do trono: retratos e texto acima da atuação, alternância de falantes e mudança de pose ao fim do trecho | Nos primeiros segundos, continuidade entre sala e diálogo; durante a conversa, manter interlocutores visíveis; perto do final, ligar fala e gesto |
| `C:\Users\Usuario\Downloads\videoplayback (1).mp4` — 52,11 s, 640×360 | Sequência de Metal Slug com personagens reagindo, criatura/máquina animada e cortes entre dois espaços | Gestos e silhuetas expressivos, ações encadeadas, movimento dos elementos do cenário e ritmo de comédia visual |

Identificação das fontes locais para futuras revisões:

```text
videoplayback.mp4
SHA-256: 1df98125fcbe64a81b0dea6fdd77a42c5faa07c485c95c2008a3a53c6db6b350
videoplayback (1).mp4
SHA-256: 4c9c5420cb47f9966a969a01072eeed3009a52ae26f217a5bf54e11ee7cef719
```

A amostragem confirma escolhas de apresentação. Ela não revela o código dos
jogos originais. Os exemplos de gatilho por pixel, término de áudio e economia
de memória trazidos pelo autor serão tratados como **referências de projeto**;
a arquitetura abaixo é uma proposta nossa para Java/libGDX, sem afirmar que
cada trecho dos jogos foi implementado exatamente dessa maneira.

## Gatilhos e direção de cena — implementação futura

Usar um diretor com estados explícitos: `GAMEPLAY → CUTSCENE → GAMEPLAY`,
com preparação/finalização internas. Cada gatilho terá ID, sala, linha ou região
em coordenadas do mundo, condições narrativas e política de repetição
(por personagem, tentativa ou entrada na sala). Cruzar o limite consome o
gatilho uma vez enquanto a sequência está ativa.

Detectar o segmento entre a posição anterior e a atual do personagem. Isso
permite atravessar a marca andando ou com dash, sem exigir igualdade com um
pixel da tela. Definir também o comportamento ao entrar na sala já dentro da
região do gatilho. Redimensionar a janela não altera a condição espacial.

Ao iniciar, o diretor controla movimento, orientação e animação dos atores.
Suspender comandos de combate, IA, spawn, projéteis, dano, coleta, tempo da run
e Fúria dos Deuses; câmera, sprites, efeitos e objetos da cena continuam
atualizando. A pausa da aplicação e F2 congelam a sequência inteira.

No código atual, `Main.setScreen()` descarta a tela anterior. Uma cutscene
acionada na arena deverá usar um modo/overlay da própria sala ou uma posse
explícita do mundo preservado. O plano não deve tentar voltar a um GameScreen
já descartado.

Criar futuramente um `CutsceneScript` e um `CutsceneDirector` próprios.
Comandos previstos: mover, orientar, animar, exibir fala, aguardar evento,
instanciar/remover objeto, controlar câmera, tocar som e concluir. Cada ação
terá um ID e emitirá sua conclusão uma única vez. Ações paralelas, como falar
e respirar, precisam declarar quais eventos a próxima etapa aguarda.
`OpeningStory`/`OpeningPlayback` continuam específicos de `opening-v1`.

### Sincronismo de fala, animação e objetos

O exemplo abaixo registra a lógica desejada pelo autor; os nomes são
ilustrativos e não constituem API implementada ou novo roteiro para Zeus:

```text
cruzou_gatilho("sala_do_trono") → assumir_controle_da_cena
fala("dialogo_x")
fim_da_fala("dialogo_x") → animar("personagem", "beber")
marcador_da_animacao("soltar_objeto") → instanciar("calice_caindo")
fim_da_animacao("calice_caindo") → tocar_som("impacto")
sequencia_concluida → aplicar_estado_final → devolver_controle
```

Separar **fim do áudio**, **texto totalmente revelado** e **confirmação de
leitura**. A condição que libera uma ação deve ser declarada pelo roteiro.
Revelar o texto instantaneamente não deve executar toda a sequência. Uma ação
ligada ao fim da voz pode acontecer enquanto o texto ainda permanece visível;
a próxima fala pode aguardar a confirmação do jogador.

Planejar um canal de voz com conclusão identificável. A interface
[Music do libGDX 1.13.1](https://github.com/libgdx/libgdx/blob/1.13.1/gdx/src/com/badlogic/gdx/audio/Music.java)
oferece listener de término. A interface
[Sound da mesma versão](https://github.com/libgdx/libgdx/blob/1.13.1/gdx/src/com/badlogic/gdx/audio/Sound.java)
não oferece esse callback; o booleano atual de GameAudio.play tampouco informa
fim de fala. O diretor deverá receber eventos do controlador de voz e dos
marcadores de animação, sem inferir conclusão pelo desenho de um frame.

Volume zero deve conservar a sequência lógica. Sem dublagem, dispositivo ou
arquivo disponível, usar duração de leitura/fala declarada e confirmação de
texto. Esperas terão limites e uma saída definida para falhas ou marcadores
ausentes. Pausa congela áudio e relógio; callbacks de uma cena cancelada são
ignorados pelo ID da execução. As cenas continuam funcionando com legendas.

### Geometria 3D no cenário 2D

Planejar planos/quads ou uma malha simples com texturas 2D, mais câmera
ortográfica ou de perspectiva conforme o efeito. Isso pode servir a um salão,
piso em perspectiva, rotação ou aproximação localizada. Personagens continuam
em sprites; colisão, navegação e gatilhos mantêm suas coordenadas 2D.

O libGDX já fornece renderização de modelos via
[ModelBatch](https://libgdx.com/wiki/graphics/3d/modelbatch). O protótipo futuro
deve separar as passagens de cenário, atores/props e interface; definir
profundidade/ordenação, transparência, oclusão e restauração do estado gráfico.
Preservar escala dos pixels e nitidez durante movimentos da câmera.

**Geometria texturizada não garante economia de memória.** Comparar custo das
texturas, chamadas de desenho, carregamento e tempo de quadro com a composição
2D equivalente. Escolher o recurso pelo efeito e pelo custo medido. A
justificativa histórica sobre memória do PS1 não está validada pelos vídeos.

### Saída, pulo e persistência

Cada roteiro terá um estado final explícito: posições, orientações, poses,
objetos, câmera, HUD, controle e fatos narrativos. Conclusão e pulo confirmado
aplicam esse estado de forma idempotente; uma recompensa, quando existir,
depende da transação de progressão, não de desenhar um gesto ou ouvir um som.
Replay é somente apresentação. Fechar no meio segue a política de retomada
definida por cena, sem registrar conclusão por acidente.

Consumir a entrada usada para avançar/pular antes de devolver o controle e
restaurar áudio, câmera e atualização do mundo. A API futura de registros deve
distinguir fatos por personagem e por tentativa/sala, preservando os estados
atuais de `opening-v1` e os desbloqueios compartilhados.

## Critérios para o primeiro protótipo futuro

Quando esse trabalho entrar no roteiro, validar uma cena curta na própria
sala: cruzar um gatilho, caminhar até uma marca, conversar com retrato, executar
um gesto, criar um objeto por marcador e devolver o controle. O efeito de
geometria 3D terá um ensaio visual específico quando houver cena que o exija.

O aceite deve incluir dash sobre o gatilho, reentrada e disparo único;
áudio ausente/mudo; leitura manual/instantânea; pausa/F2/Alt+Tab; marcador
ausente; pulo em qualquer etapa; falha de save; replay sem ganhos; e retorno
à arena sem dano ou tempo de combate acumulado. Produzir uma cena curta
permitirá medir quantos quadros/poses e quanto trabalho cada minuto exige.
**Nenhum desses sistemas novos foi implementado nesta revisão documental.**

## Apresentação do piloto existente

Fundos, personagens e primeiro plano são imagens separadas, com deslocamento
suave, aproximação da câmera, respiração discreta, transições e sons. O piloto
usa SpriteBatch e uma linha do tempo própria, independente do combate. Não
exige vídeo pré-renderizado ou dublagem. Legendas acompanham o falante mesmo
com áudio silenciado.

Referências técnicas do libGDX: [animação por quadros](https://libgdx.com/wiki/graphics/2d/2d-animation)
e [ações/transformações 2D](https://libgdx.com/wiki/graphics/2d/scene2d/scene2d).
Elas explicam possibilidades do motor; o piloto atual usa OpeningPlayback e
SpriteBatch, sem depender de um sistema Scene2D de cutscenes.

O enquadramento é 16:9 com letterbox e legendas em área própria. A composição
funciona com o movimento pausado. Portais, luz localizada, som e fades sugerem
o poder de Zeus sem flashes obrigatórios de tela inteira. O humor recai sobre
sua vaidade e a burocracia do Submundo; o sequestro é uma violência de Zeus,
sem responsabilizar a esposa.

## Storyboard adotado no piloto

| Quadro | Duração | Acontecimento |
| --- | --- | --- |
| HOME — cotidiano | 6 s | O casal aparece antes da interrupção divina. O protagonista é apresentado sem impor arma ou trocar a profissão escolhida. |
| ARRIVAL — Zeus | 7 s | Zeus interrompe com pompa e anuncia que levará a esposa para o Olimpo. |
| ABDUCTION — sequestro | 7 s | A esposa recusa ser levada; o protagonista exige que Zeus a solte. |
| CURSE — maldição | 9 s | Zeus o condena a lamentar a perda repetidamente; o confronto termina e leva à transição ao Submundo. |
| STYX — despertar | 7 s | O protagonista desperta na margem do Estige, surpreso por sua jornada continuar. |
| CHARON — recepção | 9 s | Caronte comenta o problema deixado por Zeus. O protagonista pede para voltar; a cena leva ao refúgio. |

São **45 segundos na linha do tempo**, alteráveis por pausa ou avanço manual.
Falas e composição podem ser refinadas durante os testes. A maldição explica
os retornos; a **Centelha da Insurgência continua sendo conquistada somente
após o Guardião**. A abertura não entrega esse poder nem encerra o resgate.

## Controles, entradas e registro

I/ABERTURA no menu permite assistir sem criar personagem. I no refúgio revê
a cena sem registrar progresso. Na primeira entrada de campanha de um personagem
com profissão/dificuldade confirmadas, a abertura pendente aparece antes do refúgio.
Criar ou jogar pela demo não a dispara automaticamente na demo.

Espaço pausa/continua; Enter/Próximo avança depois de 1,5 s por quadro.
Esc/Pular pede confirmação, com 0,2 s de proteção e soltura do mouse; Esc na
pergunta continua assistindo. F2 abre vídeo sem avançar a cena. Uma pausa da
aplicação pausa a apresentação; Espaço permite retomá-la.

O registro pessoal fica em cinematics, separado dos eventos da Centelha,
Caronte e tutorial. UNSEEN, COMPLETED, SKIPPED e LEGACY distinguem
pendência, conclusão, pulo e personagens anteriores ao piloto. Legados sem o
campo e com tentativas anteriores recebem LEGACY, sem fingir que assistiram.
Fechar no meio não registra conclusão. Ver/pular pelo menu ou refúgio não
concede moedas ou altera conquistas. Regras completas de identidade, falha
de gravação e retry estão no [documento do piloto](PILOTO_DA_ABERTURA.md).

## Continuidade do trabalho

O autor pediu para conservar o piloto agora. Avaliações de ritmo, poses,
recortes, câmera, legendas e sons podem orientar uma revisão futura, que deverá
seguir a direção de sprites e eventos registrada acima. O próximo trabalho
de cutscenes começa por um protótipo curto encenado na sala; sua produção não
foi iniciada por esta anotação. As opções de vídeo já têm seu próprio
[incremento verificado](MARCO_7_VIDEO_E_JANELA.md); remapeamento e escala livre
do HUD continuam em aberto. O M7 maior e a revisão humana do M6 não são
encerrados pela introdução.
