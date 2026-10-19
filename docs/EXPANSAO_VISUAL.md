# Expansão visual do Submundo

Implementada em **15/10/2026**, antecipando parte do M6 antes do catálogo/altar
do M4. A entrega amplia o elenco atual; Fazendeiro, outras regiões e novos NPCs
seguem o [roteiro de melhorias](ROTEIRO_DE_MELHORIAS.md).

**Registro da primeira expansão.** A revisão seguinte substituiu as armas
separadas por personagens equipados, acrescentou diagonais e poses de ataque
e trocou os avisos geométricos por sprites:
[ANIMACOES_E_EQUIPAMENTOS.md](ANIMACOES_E_EQUIPAMENTOS.md).
Os números e limites abaixo descrevem a entrega inicial.

## Conteúdo integrado

| Pedido | Resultado atual |
| --- | --- |
| Movimento mais suave | Oito profissões com seis quadros de caminhada em quatro direções; passada contínua e transição de 0,10 s ao virar |
| Inimigos diferentes | Corpos próprios para comum, rápido, tank, Arqueiro do Estige, Carrasco e Guardião do Tártaro |
| Baús | Madeira/bronze para suprimentos e modelo escuro com gema para Cinzas; quatro quadros de abertura por modelo |
| Retrato do protagonista | Ilustração própria usada nas respostas a Caronte |
| Profissões, armas e golpes | Oito visuais de roupa, armas separadas orientadas pela mira, sequências de golpes/projéteis e efeitos de impacto |
| Dádivas animadas | Água, proteção, raio, fogo, impacto, ressurreição, cura e ímã em sprites; aquisição breve para bônus passivos |
| Escolha de dádiva | Tela opaca completa, cartões largos, ícone próprio para cada uma das 15 dádivas, descrição/raridade e estado de aquisição |

Caçador usa arco, Ferreiro espada, Vigilante pistola, Apostador socos, Feiticeira
varinha, Sacerdote talismã de maldição, Necromante cajado e Guerreiro espada/escudo.
A arma Rara recebe um tom claro; as melhorias permanentes e seus efeitos continuam
nos modelos existentes. O Guerreiro continua atacando durante a defesa.

## Animação, máscaras e efeitos

ActorAnimation separa locomoção da mira, conserva a orientação ao parar e aplica
histerese nas diagonais. Mudar de direção não reinicia a passada. A mistura das
direções dura 0,10 s; um novo giro parte da orientação predominante para evitar
saltar para uma direção ainda invisível. Não é uma simulação de oito direções.

ExpandedArt conserva formas imutáveis de alfa e cria posições independentes
por corpo. O contato usa o mesmo tamanho e posição do desenho e a direção
predominante durante a transição. Armas, brilhos e rastros ficam fora da máscara.
Os testes comparam todos os pixels dos 288 quadros: 192 de profissões e 96 de
inimigos. Carrasco e Guardião usam células de 64×64; os demais, 32×32.

WeaponVisuals, DivineVisuals e FeedbackVisuals usam regiões de textura para os
golpes, feitiços, maldição, água, defesa e respostas a acertos. O raio é orientado
entre os alvos; os efeitos de área usam o alcance do modelo. Flechas e pedras
também são imagens. Avisos táticos de mira, giro, marcas e queda conservam as
geometrias existentes. Tempos, dano, recargas e janelas de esquiva não foram
rebalanceados nesta entrega.

O corpo do Guardião desaparece durante o salto e retorna no centro marcado.
O Carrasco conserva a regra de som apenas quando visível. Aliados usam o novo
corpo comum com tonalidade verde; ainda não preservam a aparência de cada
inimigo ressuscitado.

## Escolhas e baús

BoonChoiceView é compartilhado entre nível e recompensa das Sombras. Cobre o
mundo e o HUD inteiro, incluindo margens de viewport. Aceita de uma a três
ofertas e a reserva de Óbolos quando o catálogo superior está esgotado.
O bloqueio de 1,5 s e a exigência de soltar o botão de ataque foram preservados.
Tab abre a lista com os mesmos ícones e mantém o tempo congelado. Nada muda no
sorteio, nas raridades nem nos limites de repetição.

ChestVisuals guarda no máximo seis aberturas, com duração de 0,85 s. Abrir retira
o baú interativo e entrega a recompensa imediatamente, uma única vez; a imagem
continua somente para concluir a animação. Pausa/Tab/escolhas congelam sua
duração. Sair da arena limpa os efeitos. O baú de Cinzas aparece aberto na
tela segura; as duas recompensas existentes continuam disponíveis.

## Arquivos e reprodução

[Atlas](../core/assets/visual/expanded.png) e
[regiões](../core/assets/visual/expanded.atlas): **385 regiões, 1024×2048 RGBA**.
É uma textura adicional de aproximadamente 8 MiB, sem mipmaps. PilotArt/ Main
carregam uma única instância compartilhada e a descartam no encerramento.
Fontes e partes ainda utilizadas do piloto permanecem nos recursos existentes.

Foram gerados **20 originais** com a ferramenta integrada image_gen, pela habilidade
imagegen: oito protagonistas, seis inimigos, retrato, baús, armas, ataques, poderes
e ícones. Origens em tools/art-sources/expansion; os
[prompts exatos](../tools/art-sources/expansion/prompts.json) registram cada imagem.
Os originais não entram nos assets distribuídos.

[ExpandedAtlasBaker.java](../tools/ExpandedAtlasBaker.java) recorta e empacota as
imagens, mantendo escala comum por ator, centro e alinhamento dos pés. Nos ícones,
extrai os 16 componentes para evitar fragmentos de células vizinhas. Sequências
de efeito compartilham limites, preservando o crescimento entre quadros.

```powershell
java tools/ExpandedAtlasBaker.java
.\gradlew.bat core:test lwjgl3:smokeTest lwjgl3:packageWinX64 --console=plain --warn
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify_windows_package.ps1
```

## Verificação

**232 testes unitários aprovados**, incluindo continuidade/transição de direção
e ciclo limitado das aberturas. A integração OpenGL passa pelas oito profissões,
seis papéis de inimigo, 288 máscaras, 15 ícones distintos, armas e poderes,
recompensa única de baú, tela inteira em 960×540/800×600 e seleção com apenas
uma opção restante. As verificações anteriores de combate, áudio, pausa,
dificuldade, moedas e saves também continuam aprovadas.

O ZIP atualizado foi gerado em
[lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip](../lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip),
com **44.992.196 bytes**. Executável verificado com seu Java incluído, sem Java
no PATH e com diretório de trabalho externo. Os dois atlas expandidos estão no
JAR, as instruções foram atualizadas e os três placeholders antigos continuam
ausentes. Áudio nativo: 24 efeitos e música em streaming via OpenAL nulo; esta
verificação não reproduz som nos alto-falantes nem equivale a testar outro PC.

Capturas locais para revisão:

- [Profissões e equipamentos](../build/smoke/visual-catalog-classes.png).
- [Inimigos, mini-boss e chefe](../build/smoke/visual-catalog-enemies.png).
- [Ícones das dádivas](../build/smoke/visual-catalog-boons.png).
- [Escolha em tela inteira](../build/smoke/visual-boon-fullscreen.png).
- [Poderes em combate](../build/smoke/visual-divine-effects.png).
- [Abertura de baú](../build/smoke/visual-chest-opening.png).
- [Diálogo e retrato do protagonista](../build/smoke/m3-protagonist.png).

As capturas são artefatos de teste, regenerados pelo smokeTest. Perfis de teste
são temporários/em memória; os saves reais não são alterados.

## Acabamento restante e próximo passo

Ainda faltam poses corporais próprias de ataque, dano e morte, expressões
adicionais, arte de mundo dos NPCs/refúgio e identidade dos futuros conteúdos
do M4/M5. Esta entrega usa corpo animado, arma e efeito em camadas para os ataques.
A avaliação automática não mede a qualidade percebida do movimento nem substitui
partidas com hordas grandes. Validar legibilidade das armas e poderes, contraste
e estabilidade de direção durante uma travessia completa.

Após essa revisão, retomar **M4: novas dádivas, primeira dádiva regional e novas
compras do altar**. O M6 fica parcialmente antecipado, sem marcar toda a arte
da campanha como concluída.

