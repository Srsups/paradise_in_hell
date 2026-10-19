# Ferreiro Errante, material persistente e mini-boss

Implementado no protótipo da campanha em 09/10/2026. O autor definiu materiais
entre runs, chances base de 3% para o ferreiro e 1,5% para o material, até um drop
por **andar/região por save**, e mini-boss com giro/pedras, sem ataque de movimento.
A correção do limite persiste mesmo após gastar o material ou começar outra run.

O Apostador recebe sorte 4×: 12% para o encontro e 6% para o drop. Frequência de
mini-boss, custos, atributos da arma rara e efeitos são valores iniciais para playtests.
Cinzas e Sombras agora têm recompensas próprias e eventos de hordas:
[ROTAS_DO_SUBMUNDO.md](ROTAS_DO_SUBMUNDO.md).

## Mini-boss entre as hordas

O **Carrasco das Sombras** surge uma vez por sala após 45 segundos locais de
combate, em posição livre, alcançável e próxima. Se não houver posição válida,
a tentativa de colocação continua nos quadros seguintes. A geração normal de
hordas permanece ativa. O mini-boss usa a navegação dos inimigos e persegue
durante recuperação; fica parado durante os avisos e ataques.

| Atributo | Valor base |
| --- | --- |
| Vida | 180, contra 30 do tank e 500 do chefe |
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

O Carrasco recebeu corpo próprio na expansão de 15/10/2026, ampliado para oito
direções e ataques corporais de giro/lançamento na revisão seguinte:
capuz de ferro, avental alaranjado e silhueta musculosa. Giro e pedras usam
sprites; círculos e leques agora são imagens animadas, com áreas/tempos preservados.
Poses de dano/morte e a arte de mundo do Ferreiro Errante continuam pendentes.
Na forja, o ícone isolado mostra a arma que será melhorada.
[Entrega visual](ANIMACOES_E_EQUIPAMENTOS.md).
O playtest de 13/10/2026 identificou avisos do Carrasco fora da tela; a correção
de visibilidade sonora foi implementada no M1 em 14/10/2026 e foi preservada.
Regras e critérios: [ROTEIRO_DE_MELHORIAS.md](ROTEIRO_DE_MELHORIAS.md) e
[AUDIO_E_FEEDBACK.md](AUDIO_E_FEEDBACK.md).

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

## Arma rara permanente

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

O custo e a raridade são gravados juntos antes de aplicar a melhoria ao jogador.
Falha de gravação preserva material e arma. Repetir o botão não gasta novamente.
Nesta versão há uma melhoria **permanente por personagem**, Comum → Rara.
O ferreiro ainda pode aparecer depois dela, mas informa que a melhoria disponível
já foi obtida e permite continuar sem gasto. A profissão não muda.

Raridade, bônus e poder continuam nas salas seguintes e após morte, vitória,
abandono e fechamento. Cada nova tentativa da campanha começa com a raridade
salva, enquanto dádivas, XP e recargas reiniciam. O refúgio e a seleção de saves
mostram a raridade. A demo isolada mantém seus atributos iniciais.

O campo opcional `weaponRarity` guarda a melhoria no perfil versão 1. Saves antigos
que registraram mais regiões premiadas do que fragmentos em estoque permitem
identificar um gasto na forja e recuperar automaticamente a arma Rara.
Saves sem esse histórico começam Comum; compras antigas não identificáveis não
podem ser reconstruídas. Estoque, região premiada, moedas e outros dados são preservados.

## Verificação e playtests

Testes puros cobrem limites exatos das probabilidades, sorteio único, limite por
região, inclusive após gasto, morte, troca de sala e recarga; saves independentes, compatibilidade do perfil, falha de compra,
ordem dos bônus, limites dos projéteis e geometria da espada. O modelo do mini-boss
testa avisos sem dano, imobilidade durante ataques, esquiva, giro único, pedras,
paredes, escala e limpeza após derrota.

A verificação OpenGL força resultados aleatórios em fixtures em memória, sem
mudar probabilidades do jogo ou o perfil real. Exercita mini-boss junto a inimigos,
pausa, derrota por tiro, drop salvo, interface/forja, quatro efeitos reais, arma
rara contra o chefe e preservação na tentativa seguinte. Também confirma a
raridade gravada no perfil, sem repetir o gasto. Capturas ficam em `build/smoke/`.

Jogar salas completas com as oito profissões deve validar a leitura dos ataques
em meio à horda. Medir frequência dos materiais/encontro em sessões longas,
valor da arma rara e duração do duelo antes de ampliar raridades ou mini-bosses.

Os poderes Raros das novas armas incluem explosão ampliada, maldição prolongada,
necromancia melhorada e revide do escudo. O Carrasco pode ser revivido como aliado
com chance muito menor que um inimigo comum; isso não repete XP ou drop de material.
O guardião do andar fica excluído. Valores: [CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md).
