# Rotas do Submundo, recompensas e eventos

Implementado em 09/10/2026. O autor confirmou a continuidade do marco e corrigiu
a forja: **a melhoria da arma é permanente no save**, inclusive após morrer.
Dádivas, nível e XP continuam temporários. Valores das rotas e pesos dos eventos
são iniciais para playtests.

## Escolher o caminho

Após cumprir todas as metas do Estige, use E/Enter, recolha o XP restante e
resolva as escolhas de nível. O ferreiro sorteado ainda aparece nesse momento.
A escolha de destino mostra objetivo, evento de hordas e recompensa:

| Destino | Metas: comum / rápido / tank | Intervalo das ondas | Recompensa |
| --- | --- | --- | --- |
| Chefe | Derrotar o Guardião | Sem hordas | Vitória do trecho atual |
| Cinzas | 24 / 16 / 4 | Intervalo normal da sala ×1,25 | Baú: recuperar até 40 de vida ou ganhar 5 Óbolos |
| Sombras | 40 / 30 / 8 | Intervalo normal da sala ×0,85 | Escolher uma dádiva extra Rara ou superior |

Cinzas conserva dificuldade de sala 60; Sombras, 120. Essa dificuldade afeta
vida/dano dos inimigos e a curva normal das ondas. Multiplicar o intervalo por
1,25 dá mais tempo entre ondas; por 0,85 aumenta a frequência.
As metas precisam ser cumpridas por tipo; uma quantidade extra de rápidos
não substitui os tanks. Hordas continuam até o jogador escolher sair.

## Recompensa ao concluir

A recompensa própria aparece em uma tela segura **após sair da sala** e resolver
todos os níveis de XP pendentes, antes da próxima escolha de destino.
Não há inimigos ativos, relógio de combate ou crescimento da dificuldade nessa
tela. As salas iniciais e o chefe não recebem esse bônus extra.

**Baú de Cinzas:** escolha recuperação (até 40 de vida, sem ultrapassar o máximo)
ou 5 Óbolos, guardados ao retornar ao refúgio. F/Enter abre com recuperação;
1, 2 ou clique selecionam o benefício. Vida cheia converte a recuperação em
5 Óbolos. O baú existe mesmo que nenhum baú aleatório tenha surgido no combate.

**Dádiva de Sombras:** oferece até três efeitos distintos de categoria mínima
Rara. As categorias elegíveis mantêm seus pesos e a influência da sorte da profissão;
limites de repetição são respeitados. A escolha concede o efeito diretamente,
sem consumir XP nem inventar uma subida de nível. Use 1, 2, 3 ou clique; Enter
escolhe a primeira opção. Se todos os efeitos Raros, Épicos e Mitológicos estiverem
no limite, a recompensa vira 8 Óbolos ao retornar.

Reabrir a recompensa mantém as opções sorteadas. A sessão impede receber duas
vezes e impede passar à próxima sala antes de resolver a recompensa.
Esc nessa tela abandona a tentativa e retorna ao refúgio. As recompensas das
rotas ficam novamente disponíveis em outra run: cura e dádiva pertencem à run,
enquanto Óbolos guardados pertencem ao save. O fragmento da forja continua
respeitando seu limite permanente de uma unidade por região.

## Eventos de hordas

Cada sala de combate sorteia uma das três variações ao iniciar a tentativa,
com chances iguais. A rota mostra o resultado antes da entrada; a sala inicial
mostra o evento no HUD. Ao entrar ou reabrir uma tela, a variação permanece.

| Evento | Comum | Rápido | Tank |
| --- | --- | --- | --- |
| Hordas mistas | 50% | 35% | 15% |
| Caçada veloz | 35% | 50% | 15% |
| Marcha pesada | 40% | 25% | 35% |

As porcentagens são pesos de escolha dos tipos. A chance de onda de um único
tipo continua em 35%; as demais ondas misturam tipos. Todos os tipos conservam
chance positiva, permitindo cumprir todas as metas. A tabela descreve os pesos
brutos: os limites de 2 rápidos por onda e 24 ativos redistribuem spawns
excedentes entre comuns/tanks, mudando a composição efetiva. Rápidos alternam
1 s de arrancada e 1 s de recuperação, permitindo reposicionar sem Hermes. Os eventos não mudam o XP
por tipo, o limite de material nem os ataques de mini-boss/chefe. Outra tentativa
sorteia novamente; pode repetir um evento por acaso. A demo isolada mantém sua
distribuição gradual e não usa as recompensas das rotas.

## Forja permanente e saves

O gasto de um fragmento e a raridade Rara são gravados juntos, antes de alterar
a arma na sessão. Falha de gravação não aplica a melhoria nem consome o material.
Morte, vitória, abandono, troca de sala e fechamento mantêm raridade, bônus e
poder. A próxima campanha e o jogador no refúgio restauram a arma salva;
dádivas, XP e recargas são reconstruídos a cada run.

Neste protótipo existe uma melhoria permanente, **Comum → Rara**, por personagem:
dano base ×1,25, taxa de ataque ×1,10 e o poder da arma. Reencontrar o ferreiro
com arma Rara permite continuar e não gasta outro fragmento. Raridades de armas
adicionais ainda não foram implementadas. Cada profissão continua fixa no save,
e os oito personagens não compartilham materiais ou melhoria da arma.

O perfil versão 1 recebe o campo opcional `weaponRarity`. Saves anteriores
com registro de regiões e estoque menor que o total de regiões premiadas
evidenciam fragmento gasto: a migração recupera a arma Rara. Quando falta esse
histórico, o save usa Comum. Valores antigos de estoque, moedas, eventos e
atributos permanecem preservados.

## Conteúdo e verificação

No JSON da região, `reward` aceita NONE (padrão), RECOVERY_CHEST ou RARE_BOON.
Chefes exigem NONE. `waveIntervalMultiplier` tem padrão 1 e deve ser positivo
e finito. Essas regras são declaradas nos dados das arenas, sem depender dos
nomes Cinzas/Sombras. ID da região continua estável para o limite de material.

Os testes verificam persistência da arma nas oito profissões após
morte/vitória/abandono/recarga, compra atômica, migração, bloqueio de gastos
duplicados, recompensas depois das metas, cura limitada, conversões em Óbolos,
dádivas superiores sem duplicatas, categorias esgotadas, distribuição e estabilidade
dos eventos e configurações inválidas.

O teste OpenGL percorre ambas as recompensas pela interface real, resolve níveis
pendentes primeiro, abre o baú com F, verifica ausência de novos sorteios ao reabrir
a recompensa e chega ao chefe após Sombras. O executável Windows com runtime
incluído renderiza também essas telas em seu diagnóstico invisível.
Capturas ficam em `build/smoke/`. Travessias completas ainda devem validar ritmo,
valor da recuperação e vantagem da dádiva extra.

[Regras da forja](FERREIRO_E_MINI_BOSS.md) ·
[Sorte e raridades](SORTE_E_RARIDADE.md) ·
[Arquitetura](CAMPANHA_TECNICA.md)

Continuar combatendo após liberar a saída acumula Fúria dos Deuses; avisos precedem
as penalidades. Entrar na próxima sala limpa a pressão. Aliados do Necromante
continuam entre arenas. Consulte [CLASSES_SINERGIAS_E_DESTINO.md](CLASSES_SINERGIAS_E_DESTINO.md).
