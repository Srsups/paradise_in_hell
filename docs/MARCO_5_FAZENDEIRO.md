# Marco 5 — Camponesa das Cinzas e Fazendeiro

Implementação de **23/10/2026**, seguindo o [roteiro de melhorias](ROTEIRO_DE_MELHORIAS.md).
O encontro abre uma missão pessoal persistente e libera o Fazendeiro Épico para
novos personagens. Chances, números de combate e duração do desafio são iniciais
para playtests; testes automáticos verificam regras e persistência.

## Encontro e promessa

A Camponesa das Cinzas pode aparecer na etapa segura após concluir uma sala de
combate do Submundo e escolher sair. A chance base é **10% por tentativa**, ajustada
pela sorte: **40% para o Apostador**. Há um único sorteio por tentativa, com gerador
independente do material/ferreiro; reabrir telas não repete o sorteio. O jogador
pode continuar sem aceitar. O encontro e a leitura congelam o combate.

Ao aceitar a promessa, o próximo Carrasco derrotado no Submundo concede a
**Semente das Cinzas garantida**. É um objetivo da missão, separado do Fragmento
de Forja e de sua chance/limite regional. Com a semente, derrotar o Guardião do
Tártaro conclui a promessa e libera a profissão. Carrascos e chefes derrotados
antes da etapa correspondente não contam retroativamente.

| Etapa salva por personagem | Objetivo |
| --- | --- |
| NOT_STARTED | Encontrar a Camponesa e aceitar |
| ACCEPTED | Derrotar um Carrasco do Submundo |
| SEED_FOUND | Derrotar o Guardião do Tártaro |
| COMPLETED | Recompensa conquistada; preservar desbloqueio compartilhado |

Morte, abandono, nova tentativa e fechamento preservam as etapas já gravadas;
não é necessário achar a NPC e cumprir tudo na mesma run. Aceitar exige gravação
bem-sucedida. Ganhos de objetivos ficam na memória se o autosave falhar e podem
ser gravados novamente. A identidade do personagem impede entregar objetivos de
uma tentativa antiga a um slot reutilizado. A demo não concede progresso da missão.

O desbloqueio é compartilhado entre os oito saves, **sem gastar Óbolos ou Almas**.
Ele permite criar um novo Fazendeiro, mantendo a profissão dos personagens
existentes. A etapa COMPLETED recupera a recompensa se a gravação compartilhada
falhar; excluir o personagem concluído exige preservar primeiro esse desbloqueio.
Saves antigos sem a missão/desbloqueio começam sem a nova conquista e continuam
compatíveis. O Necromante mantém sua compra por 1.000 Almas.

**J no refúgio** abre o diário da promessa. **Tab nas arenas** inclui seu resumo;
ambos permitem consultar o próximo objetivo. A escolha de profissão passa a
**nove classes em três páginas**; os oito saves continuam em duas páginas.

## Combate do Fazendeiro

O Fazendeiro começa com 120 de vida, movimento 57, dash 195 e sorte normal (1×).
As duas ferramentas pertencem ao mesmo personagem. **Q durante o combate ativo**
alterna entre ancinho e foice; menus, pausa e escolhas não permitem trocar.

| Ferramenta comum | Dano base | Intervalo / ataques por segundo | Alcance | Uso |
| --- | --- | --- | --- | --- |
| Ancinho | 9 | 0,70 s / 1,43 | 52 | Estocada estreita na direção do mouse |
| Foice | 6 | 0,85 s / 1,18 | 34 | Um golpe de 360° ao redor do personagem |

A estocada tem meia largura de 4 unidades; as duas geometrias incluem o corpo
do alvo e respeitam estruturas. Trocar não renova a recarga de ataque nem duplica
benefícios: dano, cadência, raridade, efeitos e recargas pertencem ao mesmo
personagem. O golpe já iniciado mantém a ferramenta, alcance e animação de origem.

Compras e dádivas de dano/cadência usam a base da ferramenta ativa. A forja
Comum → Rara melhora ambas permanentemente, com os multiplicadores já existentes;
o alcance passa a 64 no ancinho e 44 na foice. A ferramenta ativa acompanha a
travessia entre salas; uma nova tentativa começa novamente com o ancinho.

## Arte e animação

Os dois conjuntos do Fazendeiro têm armas **integradas ao corpo**, com seis
quadros de caminhada, quatro de respiração e quatro de ataque para cada uma das
cinco vistas: baixo, cima, esquerda, cima-esquerda e baixo-esquerda. As três vistas
da direita são espelhamentos horizontais. São **140 poses desenhadas e 224 poses
disponíveis**, preservando o elenco anterior. O ancinho estoca; a foice usa poses
próprias de giro. A troca conserva a pose da ferramenta que iniciou um golpe.

O [atlas M5](../core/assets/visual/m5.atlas) e sua
[página PNG](../core/assets/visual/m5.png) contêm **152 regiões**, em 1024×1024,
com carga RGBA de **4 MiB**. Além dos corpos, há retrato/corpo da Camponesa,
dois ícones das ferramentas para a forja e quatro quadros de efeito para cada
ataque. O retrato aparece no encontro/diário; o corpo da NPC fica disponível
para uma futura apresentação no mundo. Ícones separados não são desenhados
sobre o protagonista.

A arte foi criada com **imagegen pela ferramenta integrada image_gen**, usando
RGBA transparente. As referências do projeto orientaram estilo e proporções;
edições posteriores reduziram franjas de cor do retrato/efeitos. A procedência,
seleção dos originais e verificações SHA-256 estão no
[manifesto](../tools/art-sources/m5/manifest.json), com os
[prompts exatos](../tools/art-sources/m5/prompts.json). Os originais ficam fora
do pacote do jogo.

[MilestoneFiveAtlasBaker.java](../tools/MilestoneFiveAtlasBaker.java) realiza
somente recorte, registro do pivô, redução por vizinho mais próximo e
empacotamento. Não pinta personagens, ferramentas ou efeitos. Preserva o alfa
gerado; recortes excluem fragmentos de células vizinhas. Os deslocamentos de
registro estão em [registration.csv](../tools/art-sources/m5/registration.csv).
ExpandedArt é proprietário do FarmerArt; EquippedArt usa o mesmo atlas no jogo,
sem duplicar sua carga, e as máscaras seguem a pose/ferramenta atual.

Reprodução com JDK 21:

```powershell
javac -d build/art-tools tools/EquippedAtlasBaker.java tools/PolishedFrames.java tools/MilestoneFiveAtlasBaker.java
java -cp build/art-tools MilestoneFiveAtlasBaker
```

Revisão: [caminhada nas cinco vistas](../build/smoke/m5-farmer-walk-contact-sheet.png).
Avaliar as transições e a leitura das estocadas/giros em partidas completas,
inclusive perto de estruturas e com múltiplos poderes simultâneos.

## Verificação

Verificações concluídas na entrega de **23/10/2026**:

- **290 testes unitários aprovados**, sem falhas: missão em etapas, semente
  garantida, progresso pessoal, desbloqueio compartilhado, compatibilidade,
  falhas de gravação/retry, exclusão segura, recarga compartilhada, paredes e
  preservação de dádivas/forja entre salas.
- **OpenGL aprovado (`DEMO_SMOKE_OK`)**: fluxo completo do M5 nos menus e arenas,
  encontro/diário, criação bloqueada/liberada, ancinho, foice e forja; verificação
  de 1.504 poses do conjunto de animações.
- **Pacote Windows aprovado (`WINDOWS_PACKAGE_OK`)**: `packageWinX64` e
  `verify_windows_package.ps1`, com runtime incluído, PATH sem Java externo,
  dispositivo de áudio nulo e sem alterar saves reais.

| Captura | Conteúdo |
| --- | --- |
| [Camponesa](../build/smoke/m5-camponesa.png) | Encontro e aceitação da promessa |
| [Diário](../build/smoke/m5-journal.png) | Consulta do objetivo no refúgio |
| [Profissões](../build/smoke/m5-professions.png) | Fazendeiro na terceira página |
| [Ancinho](../build/smoke/m5-rake.png) | Estocada durante o combate |
| [Foice](../build/smoke/m5-scythe.png) | Corte circular durante o combate |
| [Forja](../build/smoke/m5-forge.png) | Melhoria permanente das ferramentas |

[ZIP Windows validado](../lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip).
Os checks verificam regras, recursos e execução; equilíbrio, suavidade percebida,
espera pelo encontro e diversão ainda dependem dos playtests humanos. Depois
dessa revisão, seguem o acabamento do M6 e o fechamento do Submundo no M7.
