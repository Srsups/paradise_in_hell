# Marco 4 — novas dádivas, Eco do Estige e altar

Implementado em **22/10/2026**. Primeiro lote do M4 do
[roteiro](ROTEIRO_DE_MELHORIAS.md): seis novas dádivas gerais, uma regional e
três compras permanentes. O catálogo tem **22 dádivas** e o altar, **seis compras**.
Os números são iniciais para playtests; as verificações automáticas comprovam
as regras e a integração, sem comprovar equilíbrio ou diversão.

## Novas dádivas

Todos os níveis e recargas abaixo são temporários, apagados na nova tentativa.
DPS significa dano atual da arma multiplicado pelos ataques por segundo da classe,
incluindo forja, bônus percentuais e a condição de pouca vida quando ativa.

| Dádiva | Categoria / limite | Efeito por nível | Condição e recarga |
| --- | --- | --- | --- |
| Amparo de Héstia | Rara / 1 | Recebe 30% menos dano durante 2 s | Após dano efetivo; recarga de 8 s desde a ativação. O golpe inicial não é reduzido por esse poder |
| Furor de Ares | Rara / 2 | +25% do dano base enquanto vivo com até 35% da vida máxima | Sai ao curar acima do limiar; não altera permanentemente a base |
| Fôlego de Íris | Rara / 2 | Recupera 6 de estamina por abate | Uma recuperação a cada 2 s, global entre todos os alvos |
| Rastro de Deméter | Rara / 1 | +96 de raio e +120 de velocidade de atração de XP durante 1,5 s | Dash bem-sucedido; recarga de 3 s |
| Emboscada de Ártemis | Rara / 2 | Bônus de 45% do DPS no primeiro alvo de um ataque | Após dash, janela de 2 s e recarga de 2 s |
| Convergência de Hefesto | Épica / 2 | Explosão de 30% do DPS em raio 28 | Arma acerta alvo que já queimava/estava amaldiçoado; recarga global de 1,5 s |
| Eco do Estige | Épica / 2 | Pulso de 60% do DPS em raio 44 | Fica na origem do dash, explode após 0,35 s; recarga de 3 s |

Rastro soma seu bônus ao ímã, sem renovar a duração em cada
dash dentro da recarga. Fôlego restaura até a capacidade real e pode recompensar
abates enquanto se defende; regeneração passiva continua bloqueada pelo escudo.
Abates não recuperam vida/estamina de um jogador morto.

Furor soma sua fração à Força e às compras de dano, sobre a base da classe
com a raridade da arma. Cada nível acrescenta a mesma quantidade enquanto a
condição estiver ativa, preservando a regra de bônus percentuais do personagem.

Amparo não é ativado por golpe bloqueado, Égide ou proteção de ressurreição.
A redução multiplica o dano restante após a resistência normal e não se renova
a cada golpe durante sua recarga.

Emboscada é consumida no disparo/golpe, inclusive se ele errar. Um objeto de bônus
acompanha o projétil ou todos os alvos da mesma espada: perfuração, ricochete e
golpes em grupo não replicam o bônus. O dano adicional não dispara outros procs.
No máximo, o primeiro alvo recebe o orçamento daquele ataque.

Convergência exige um estado **anterior** ao acerto. Não explode no primeiro golpe
que apenas aplica a maldição/queimadura; o próximo acerto elegível pode detoná-la.
A explosão e o Eco consultam paredes e targetable, ignorando mortos e o chefe
fora da arena. Aplicam dano uma vez e não chamam onHit: não há cadeia de explosões.
Pulsos e flashes têm quantidades limitadas; sair da sala descarta seus efeitos locais.

No nível 1, Emboscada pode adicionar até 22,5% do DPS sustentado de um alvo,
Eco até 20% por alvo e Convergência até 20% por alvo, antes de erros, bloqueios,
deslocamento e condições de ativação. São referências de orçamento, não uma
promessa de dano em partida. A classe rápida não recebe um proc em cada bala.

## Obtenção regional e sorte

Somente o Submundo atual (ID submundo-prototype) oferece Eco do Estige, tanto
no Estige quanto em Cinzas, Sombras e no espaço de nível do chefe. A demo e
regiões com outro ID não o sorteiam. O cartão e o Tab mostram a origem.
Exclusividade restringe a oferta, sem remover um poder adquirido ao avançar.
A travessia para outra região ainda é conteúdo futuro; hoje a run termina no Guardião.

Convergência só entra na oferta para o Sacerdote ou para quem possui Brasa de
Héstia. As demais novas dádivas gerais não exigem uma arma específica.
Continuam válidos sorte por classe, escolha sem duplicatas, limites de rank,
cura excluída com vida cheia, filtro de recompensa Rara e proteção de entrada
de 1,5 s com liberação do ataque.

Os pesos das categorias seguem 70/23/6/1 para sorte 1. No primeiro cartão do
Submundo, Atena divide as Épicas com Égide, Poseidon e Eco: **1,5%** normalmente;
com Convergência elegível, **1,2%**. Para o Apostador: aproximadamente **7,45%**
ou **5,96%**, respectivamente. Esses valores mudam com a elegibilidade; não
são a chance de aparecer no conjunto de três cartões.

## Altar e persistência

O altar tem duas páginas de três cartões. Setas ou Anterior/Próxima mudam a
página; 1–3 ou clique compram o cartão da página atual. Cada cartão mostra nível,
efeito, valor atual/próximo e preço. Os três atributos antigos mantêm seus custos.

| Compra | Ganho por nível | Máximo (5 níveis) | Custos dos níveis 1 a 5 |
| --- | --- | --- | --- |
| Vitalidade | +10% da vida base da classe | +50% | 5 / 9 / 13 / 17 / 21 Óbolos |
| Força | +5% do dano base, respeitando a forja | +25% | 5 / 9 / 13 / 17 / 21 |
| Agilidade | +3% da velocidade base | +15% | 5 / 9 / 13 / 17 / 21 |
| Reserva de estamina | +5% da capacidade base de 100 | 125 de estamina | 6 / 11 / 16 / 21 / 26 |
| Recuperação de estamina | +8% da regeneração base de 10/s | 14/s normal; um terço na exaustão | 7 / 12 / 17 / 22 / 27 |
| Armadura | +3 pontos percentuais de redução de dano | 15% de redução | 5 / 9 / 13 / 17 / 21 |

Armadura substitui o alcance de coleta permanente e reduz dano de contato,
projéteis e ataques em área, inclusive dos encontros. Seus pontos percentuais
somam com a resistência existente, limitada a 75% no total. Amparo continua
multiplicando o dano restante por 0,7 quando ativo: com Armadura máxima,
um golpe de 100 causa 85 normalmente e 59,5 sob Amparo.
Ímã e Rastro mantêm seus efeitos temporários; o raio inicial volta a 36.
A reserva não muda custo/recarga do dash nem o custo de bloquear.
Barra de estamina e seus segmentos de custo acompanham a capacidade real.
Baús e Fôlego respeitam o novo máximo.

São compras pessoais em Óbolos, com limite de cinco níveis e gravação antes de
aplicar/deduzir. Almas continuam compartilhadas e usadas pelo desbloqueio do
Necromante; a loja não altera esses saldos.

Perfis versão 1 sem ENDURANCE, RECOVERY e ARMOR recebem zero nesses campos,
exceto quando há níveis antigos de COLLECTION: eles são convertidos em ARMOR,
sem cobrança nem perda de níveis. Se as duas chaves existirem, ARMOR prevalece;
ambas são validadas e os níveis não se somam. A próxima gravação escreve somente
ARMOR. Os três atributos originais continuam obrigatórios, e os novos rejeitam
tipo, valor negativo ou nível fora do limite. Moedas, profissão, dificuldade,
raridade, limite regional de fragmentos e identidade são preservados.
Cada run conserva um snapshot das compras; novas tentativas apagam os poderes
temporários. A troca por Armadura e a retirada do emblema do título do altar
foram solicitadas pelo autor na revisão de 22/10/2026.

## Código e apresentação

- BoonState pertence ao PlayerState: níveis temporários, janelas e recargas.
- WeaponBonus é compartilhado por todos os impactos de um ataque.
- CombatEffects resolve dano, pulsos e paredes por DamageSink.
- GameScreen e BossRoomScreen integram os mesmos poderes.
- RunSession aplica os benefícios de abate uma vez, retirando a duplicação nas telas.
- Relatórios distinguem Arma, Emboscada, Convergência e Eco por DamageSource;
  os novos nomes e os níveis das dádivas sobrevivem à leitura do histórico.
- Tab comporta 22 opções em quatro páginas. Origens e descrições cabem nos cartões.

Os ícones foram gerados com **imagegen, ferramenta integrada image_gen**:
[PNG de origem](../tools/art-sources/m4/boon-icons-original.png) e
[prompt exato/modo/caminhos](../tools/art-sources/m4/prompts.json).
Há sete ícones de dádivas. O oitavo ícone, criado como emblema de estamina,
permanece no atlas para preservar o recurso original, sem ser exibido no título do altar.
[MilestoneFourAtlasBaker.java](../tools/MilestoneFourAtlasBaker.java) faz somente
recorte, escala e empacotamento, preservando a arte/alfa gerados.
O [atlas](../core/assets/visual/m4.atlas) e a
[página](../core/assets/visual/m4.png) têm oito regiões, 256×128 e **128 KiB RGBA**.
ExpandedArt compartilha e descarta o recurso com o restante da apresentação.

Poderes no mundo combinam os sprites existentes de proteção, magnetismo, fogo,
raio, água e ressurreição. O Eco mostra seu centro/raio antes do impacto.
As mudanças não acrescentam contadores ao HUD de combate.

## Verificação e playtest

**263 testes unitários e integração OpenGL aprovados.** Incluem:

- Defesa após dano, pouca vida, bônus relativo às oito classes, recargas globais,
  projétil perfurante, dano em área sem recursão, paredes e corpos fora de alcance.
- Exclusividade do Eco, pré-requisito da Convergência, limites, reset e transição de sala.
- Migração de perfis antigos, campos malformados, limites/preços, falha de gravação,
  snapshots, regeneração durante defesa, teto de estamina e redução de dano por Armadura.
- Compras por mouse/teclado nas duas páginas, ícones distintos dos dois atlas,
  Emboscada no combate real das oito classes, Eco em hordas e chefe,
  pausa com Tab e releitura dos novos tipos de dano nos relatórios.

Os testes usam saves em memória/temporários. O build Windows incorpora atlas,
página e instruções atualizadas, com Java incluído.

O [ZIP Windows atualizado](../lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip)
foi aprovado na execução nativa com PATH sem Java e
diretório externo. O JAR contém o atlas/página M4 e os sete ícones de dádiva;
originais de arte ficam fora da distribuição. O teste nativo de áudio verificou
24 efeitos e música em streaming por OpenAL nulo.

Comandos:

```powershell
java tools/MilestoneFourAtlasBaker.java
.\gradlew.bat core:test lwjgl3:smokeTest lwjgl3:packageWinX64 --console=plain --warn
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```

Capturas:

- [Altar: atributos básicos](../build/smoke/m4-altar-base.png) e
  [estamina/Armadura](../build/smoke/m4-altar-stamina.png).
- [Escolhas novas](../build/smoke/m4-new-boon-choices.png) e
  [Tab, página quatro](../build/smoke/m4-boons-page-four.png).
- [Catálogo de 22 ícones](../build/smoke/visual-catalog-boons.png).
- [Eco: preparação](../build/smoke/m4-echo-warning.png),
  [impacto](../build/smoke/m4-echo-impact.png) e
  [contra o Guardião](../build/smoke/m4-boss-echo.png).

Comparar três linhas de build: risco/defesa (Furor + Amparo), dash
(Rastro + Emboscada + Eco) e estados elementais (queimadura/maldição +
Convergência). Jogar com personagem novo/evoluído e nas três dificuldades;
medir duração do chefe, dano por origem, mortes, escolhas e ganhos em Óbolos.
O M5 seguinte é o NPC de desafio/Fazendeiro, ainda com decisões de missão/encontro
a confirmar. Não foi implementado neste marco.
