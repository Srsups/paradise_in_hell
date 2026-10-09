# Ferreiro Errante, material persistente e mini-boss

Implementado no protótipo da campanha em 09/10/2026. O autor definiu materiais
entre runs, chances base de 3% para o ferreiro e 1,5% para o material, até um drop
por **andar/região por save**, e mini-boss com giro/pedras, sem ataque de movimento.
A correção do limite persiste mesmo após gastar o material ou começar outra run.

O Apostador recebe sorte 4×: 12% para o encontro e 6% para o drop. Frequência de
mini-boss, custos, atributos da arma rara e efeitos são valores iniciais para playtests.
Recompensas próprias de Cinzas/Sombras continuam propostas, sem alteração neste
incremento.

## Mini-boss entre as hordas

O **Carrasco das Sombras** surge uma vez por sala após 45 segundos locais de
combate, em posição livre, alcançável e próxima. Se não houver posição válida,
a tentativa de colocação continua nos quadros seguintes. A geração normal de
hordas permanece ativa. O mini-boss usa a navegação dos inimigos e persegue
durante recuperação; fica parado durante os avisos e ataques.

| Atributo | Valor base |
| --- | --- |
| Vida | 180, contra 30 do tank e 420 do chefe |
| Velocidade de perseguição | 24 unidades/s |
| Contato durante recuperação | 20, com intervalo de 1 s |
| Giro de espada | 28 de dano, raio 42, um acerto por giro |
| Rajada de pedras | 5 pedras, 20 de dano cada, velocidade 115 |
| XP por derrota | 20 |

Vida e dano recebem o snapshot da escala linear da sala e tempo total ao nascer.
Não são recalculados enquanto o jogador o enfrenta. O giro tem aviso de 1 s;
a rajada, 1,1 s, com direção fixada no aviso. Ataques duram 0,45 s e recuperação
2,2 s. Círculo e leque mostram o perigo; as pedras têm duração máxima de 3 s.
Giro e pedras respeitam estruturas. Não há investida, teleporte ou salto ofensivo.

A derrota conta um abate geral, sem substituir metas de comuns/rápidos/tanks.
Gera um cristal de 20 XP e encerra seus perigos. Pausa e dádivas congelam o
encontro. A saída permanece dependente somente das metas existentes; não obriga
caçar um mini-boss fora da tela. Sair descarta o encontro.

Sprite ampliado/tingido e formas dos ataques são provisórios; não foi criada
arte final ou áudio neste incremento.

## Fragmento de Forja

Cada derrota tem **1,5% de chance base** enquanto o save não obteve material
naquela região; com o Apostador, a chance é **6%**. O limite é **uma unidade por
andar/região por save**, acumulando todas as runs. Um sorteio sem drop não consome
o direito: o jogador pode conseguir seu primeiro fragmento na terceira tentativa.
Depois disso, novas salas, morte, abandono, nova tentativa e gasto no ferreiro
não permitem outro fragmento no mesmo Submundo.

O limite pertence ao ID estável da região, associado ao seu guardião. Ao vencer
e avançar para outra região, haverá um limite independente. O protótipo contém
somente o Submundo: derrotar seu chefe ainda encerra o trecho jogável.
O mini-boss continua surgindo uma vez **por sala**, mesmo quando não pode dropar
material, e sempre oferece XP. Os sorteios são independentes do encontro com o ferreiro.

O fragmento é creditado e salvo **imediatamente na derrota**, sem exigir coleta
manual ou retorno ao refúgio. Morte, abandono e fechamento posterior não removem
um material já salvo. Os materiais pertencem ao save, não são compartilhados
entre personagens e permanecem até serem gastos.

Campos opcionais `forgeMaterials` e `claimedMaterialRegions` no perfil versão 1.
O primeiro guarda estoque; o segundo registra regiões já premiadas, mesmo com
estoque zero. Saves anteriores sem estoque leem zero. Saves antigos com fragmentos
e sem registro reservam o Submundo, preservando todo o estoque e os outros dados.
Um fragmento já gasto em um save antigo sem registro não pode ser identificado. Autosave falho mantém o recurso
na memória e informa o erro; S no refúgio repete a gravação. Se o jogador fechar
antes de resolver uma falha, ganhos ainda não salvos podem ser perdidos.

## Encontro com o ferreiro

Há **um único sorteio por tentativa**, ao começar: **3%** para as profissões
originais e **12%** para o Apostador. Se selecionado, o
Ferreiro Errante aparece em um encontro seguro entre a primeira sala concluída
e a escolha de destino. Tempo parado, troca de sala e reabrir a interface não
geram sorteios adicionais. Pode aparecer sem que o personagem tenha material.

Fala inicial provisória: “Não conserto casamentos. Armas, talvez.”
A interface mostra profissão, arma, raridade, material, custo e benefício.

- **1 ou E / clique:** forjar por um Fragmento de Forja.
- **2, Enter ou Esc / clique:** continuar para a escolha de destino.

O encontro não avança tempo/dificuldade nem restaura vida automaticamente.

## Arma rara durante a tentativa

A primeira forja transforma **Comum → Rara**, com:

- Dano base da arma ×1,25.
- Taxa de ataque ×1,10.
- Arco: perfura um alvo adicional.
- Pistola: ricocheteia uma vez para outro alvo vivo em até 100 unidades, com
  caminho livre. Sem alvo válido, o tiro termina.
- Espada: alcance aumenta de 36 para 48, mantendo o cone de 110°.
- Socos: alcance aumenta de 24 para 34, mantendo o cone de 80° e um alvo por ataque.
  O Apostador continua usando apenas os punhos; a forja fortalece seus golpes.

Dádivas e compras continuam percentuais sobre a base da arma forjada. A ordem
das melhorias não muda o resultado; frações são mantidas. Um projétil não acerta
o mesmo alvo duas vezes. O bônus de dano/cadência e alcance também funcionam
na sala do chefe; perfuração/ricochete dependem de outros alvos.

O custo é gravado antes de aplicar a raridade. Falha de gravação preserva material
e arma. Repetir o botão não gasta novamente. Nesta versão há um nível de raridade
e uma compra por tentativa. A profissão não muda.

Raridade e poder continuam nas salas seguintes; morte, vitória ou abandono
encerram o benefício. A próxima tentativa começa Comum, com os atributos
permanentes e materiais restantes do save.

## Verificação e playtests

Testes puros cobrem limites exatos das probabilidades, sorteio único, limite por
região, inclusive após gasto, morte, troca de sala e recarga; saves independentes, compatibilidade do perfil, falha de compra,
ordem dos bônus, limites dos projéteis e geometria da espada. O modelo do mini-boss
testa avisos sem dano, imobilidade durante ataques, esquiva, giro único, pedras,
paredes, escala e limpeza após derrota.

A verificação OpenGL força resultados aleatórios em fixtures em memória, sem
mudar probabilidades do jogo ou o perfil real. Exercita mini-boss junto a inimigos,
pausa, derrota por tiro, drop salvo, interface/forja, quatro efeitos reais, arma
rara contra o chefe e reset. Capturas ficam em `build/smoke/`.

Jogar salas completas com as quatro profissões deve validar a leitura dos ataques
em meio à horda. Medir frequência dos materiais/encontro em sessões longas,
valor da arma rara e duração do duelo antes de ampliar raridades ou mini-bosses.
