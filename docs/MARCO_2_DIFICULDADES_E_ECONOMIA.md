# Marco 2 — Dificuldade fixa e economia

Implementado em **14/10/2026**. Multiplicadores iniciais para playtests;
verificar as regras não comprova equilíbrio ou diversão.

## Criação e persistência

O narrador pergunta a profissão; a tela seguinte permite escolher Fácil,
Média ou Difícil. **Criar personagem/Enter** confirma as duas escolhas.
**Voltar/Esc** descarta a seleção sem ocupar o slot. A seleção padrão é Média.
A profissão e a dificuldade ficam fixas no personagem. Criar outro save
ou excluir e recriar um slot permite outras escolhas.

ProfileService grava uma cópia com as duas escolhas antes de trocar o perfil
ativo. Uma falha mantém a tela e as escolhas para tentar novamente;
um arquivo ilegível é preservado. Não há cobrança na criação do personagem.
O Necromante continua exigindo o desbloqueio compartilhado de **1.000 Almas**.

Campo JSON **difficulty**, com IDs EASY/MEDIUM/HARD, no perfil e relatório.
Campos ausentes em versões anteriores assumem **Média**, preservando identidade,
saldo, atributos, eventos, forja e histórico. Campo presente inválido é rejeitado.
A dificuldade aparece no cartão do save e no resumo detalhado; o HUD de combate
mantém suas informações essenciais.

## Inimigos

| Modo | Vida | Dano | Óbolos/Almas elegíveis |
| --- | --- | --- | --- |
| Fácil | ×0,85 | ×0,75 | ×0,85 |
| Média | ×1 | ×1 | ×1 |
| Difícil | ×1,25 | ×1,20 | ×1,25 |

GameDifficulty compõe esses valores com EnemyDifficulty (tempo/sala) e Fúria.
Aplica a comuns, rápidos, tanks, arqueiros (contato/flecha), Carrasco e Guardião.
Atributos são capturados ao criar o inimigo/chefe; inimigos vivos não recuperam vida.
Velocidades, ondas, metas, avisos e janelas de dash conservam as regras do M1.
A demo também usa o modo do personagem, mantendo cinco minutos e zero moedas.

## Recompensas e frações

Óbolos pessoais: somar valores base de abates, baús e bônus de chefe,
aplicar o percentual uma vez ao total. A base de abates mantém o teto de 10;
o multiplicador pode levar essa parcela a 12 no Difícil.
Cinzas dá 5 base; reserva de Sombras com categorias esgotadas dá 8 base.
Os cartões/avisos de baús mostram o incremento inteiro efetivo, incluindo
frações acumuladas pelos ganhos anteriores da tentativa.

Almas compartilhadas: cada recompensa base recebe a redução de Fúria existente
(mínimo de 1 base para inimigos) e então o percentual do modo. Guardião mantém
100 base sem penalidade de Fúria. O acumulador usa **centésimos inteiros**,
evitando erro de ponto flutuante e arredondamento por abate.

Exemplo: quatro comuns no Difícil geram 1,25 + 1,25 + 1,25 + 1,25 = **5 Almas**;
no Fácil, 0,85 × 4 = 3,40, mostrando **3** inteiras.
As frações ficam na tentativa e combinam com outros prêmios até o retorno.
A conversão descarta a fração final; ela não é transferida para a tentativa seguinte.
Abandonar ou morrer guarda apenas o ganho realizado; bônus do Guardião exige derrota.
Relatório e regravação não voltam a multiplicar ou creditar os ganhos.

O presente único de Caronte continua em **5 Óbolos** em qualquer modo.
Saldo existente, preços do altar e desbloqueio lendário permanecem nominais.
XP, Atena, sorte, chances de raridade/ferreiro/material e limite regional de fragmentos
mantêm seus sistemas próprios.

## Verificação

- 222 testes de regras aprovados: três modos, persistência/migração, criação com
  falha, arquivo ilegível, composição com Fúria, frações, fontes de moeda e crédito único.
- Integração com OpenGL: seleção/cancelamento/confirmação, recarga, três modos,
  inimigos/arqueiro/mini-boss/Guardião, demo, relatório, reutilização de slot,
  redimensionamento e nova tentativa. Falha de gravação seguida de nova confirmação.
- Capturas em build/smoke/m2-*.png revisadas: seleção, saves, relatório,
  redimensionamento e falha de gravação.
- Pacote Windows atualizado: lwjgl3/build/construo/dist/paradise-in-hell-gdx-winX64.zip.
  Verificação aprovada com Java incluído, diretório de trabalho externo, assets,
  24 efeitos nativos e streaming de música via dispositivo OpenAL nulo.
  As verificações usaram perfis em memória/temporários, preservando os saves do jogador.

## Playtests e próximo marco

Comparar travessias com personagem novo/evoluído, por profissão e rota, nos três
modos. Usar dificuldade, tempo por sala, fases do chefe, dano por origem e moedas
dos relatórios para ajustar números. Média é a referência do M1.
Confirmar se Fácil melhora a acessibilidade sem tornar o altar lento e se a
recompensa de Difícil compensa o risco.

Próximo incremento: **M3**, base visual reutilizável e diálogo piloto de Caronte.
O Fazendeiro e o desafio do NPC continuam em M5, conforme o
[roteiro](ROTEIRO_DE_MELHORIAS.md).

