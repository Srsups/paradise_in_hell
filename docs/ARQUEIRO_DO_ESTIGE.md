# Arqueiro do Estige e validação da travessia

Protótipo implementado em 12/10/2026. Valores iniciais; o equilíbrio depende de
partidas completas com jogadores.

## Comportamento e leitura

O arqueiro é uma variação de **COMMON**, contando para a meta de comuns. As metas
de rápidos e tanks continuam independentes. Ele usa a animação provisória dos
inimigos, com tom violeta, arco desenhado, linha de mira e círculo no destino.

Procura uma linha de tiro livre, contornando estruturas pelo campo de navegação
compartilhado. Só inicia a mira dentro da tela, com margem de 20 unidades, e a
até 160 unidades do jogador. Para de andar e marca a posição atual do jogador:
o aviso dura **1 segundo** e fica vermelho no último quarto de segundo. O destino
fica travado desde o começo; andar ou dar dash não arrasta o círculo.

Depois do aviso, uma flecha segue em linha reta. Não atravessa paredes nem segue
o jogador. Saiu do círculo, ainda verifique o percurso da flecha: outros pontos
da mesma linha também podem ser atingidos. O escudo bloqueia pela frente; o revide
da arma Rara atinge o arqueiro que disparou. Retaguarda continua vulnerável.

Durante 2 segundos de recuperação, o arqueiro pode recuar se o jogador se
aproximar a menos de 72 unidades. Se uma parede impedir o recuo, tenta deslocar-se
lateralmente em um trecho livre. Sua velocidade é inferior à de todas as profissões.
Se a linha de tiro estiver bloqueada, volta a buscar passagem sem atravessar paredes.
Sair da margem visível ou obstruir a linha durante o aviso cancela o disparo.

## Números iniciais

| Atributo | Valor base |
| --- | --- |
| Vida | 14 |
| Velocidade | 28 unidades/s |
| Contato | 6 de dano |
| Flecha | 10 de dano; 180 unidades/s |
| Aviso / recuperação | 1 s / 2 s |
| Alcance de mira | 160 unidades |
| XP / Almas | 3 / 3 |
| Força para necromancia | 1,8 × raiz da escala de vida |

Vida, contato e flecha recebem a escala de sala/tempo e Fúria no nascimento.
Recompensas recebem a redução de Fúria no abate, com mínimo 1. Sorte não multiplica
Almas. A demo concede XP e nenhum saldo de moedas, como os demais inimigos.
O arqueiro pode virar aliado do Necromante: segue o comportamento atual de aliados
de contato, sem conservar a habilidade de disparar flechas nesta versão.

A partir de 15 segundos totais de combate na campanha, cada spawn comum elegível
tem **20%** de chance de ser arqueiro. Na demo, a introdução ocorre aos 60 segundos.
O sorteio respeita os pesos dos comuns da sala; não reduz a disponibilidade de
rápidos/tanks. Máximo de **2 arqueiros por onda** e **6 ativos por arena**.
Esses limites controlam pressão e simulação; a geração continua após suas mortes.

Cada arqueiro mantém no máximo 2 flechas, com vida máxima de 3 segundos.
Colisão verifica pequenos trechos percorridos e o raio da ponta contra paredes e
a máscara visível do jogador. Um impacto consome a flecha, inclusive se bloqueado.
Morte, remoção por distância, saída e descarte da tela eliminam suas flechas.
Pausa, Tab e escolhas de dádivas congelam aviso, recuperação, flechas e tempo de run.
Relatórios identificam dano como “Arqueiro do Estige / Flecha” ou contato.

Não foi alterado o formato dos saves, a profissão de personagens ou o limite
regional do Fragmento de Forja.

## Verificação automática

Testes de regras verificam aviso, destino travado, desvio, dano escalado,
colisão sem atravessar paredes em um quadro, alvo antes de uma parede,
navegação ao redor de estruturas, recuo seguro, cancelamento fora da tela,
pausa, limite/duração das flechas, escudo, chances e limites de spawn.
Metas continuam separadas; derrota concede 3 Almas na campanha e zero na demo.

A verificação OpenGL usa as oito armas para atingir e derrotar arqueiros,
confere cristal de 3 XP, um único crédito, dano pela máscara do jogador,
mira travada após movimento, pausa/Tab/redimensionamento, defesa/revide e limpeza.
Capturas: `build/smoke/archer-warning.png`, `archer-arrow.png`, `archer-shield.png`.

## Próximo passo: partidas completas

Após o relato de rápidos acumulando, eles passaram a ter recuperação de 1 s,
limites de 2 por onda/24 ativos e ciclos defasados. O Guerreiro agora ataca enquanto
defende. Repetir partidas sem Hermes para avaliar a pressão, especialmente quando
rápidos e arqueiros aparecem juntos. A seleção permite esvaziar um slot com
confirmação para criar outro personagem, preservando moedas globais.

Executar esta matriz manualmente. **Ainda não realizada por jogadores.** Usar
atributos comparáveis; registrar compras, raridade e dádivas quando diferentes.
A demo e a travessia do Submundo são experiências distintas: esta matriz é da campanha.

| Profissão | Direto ao Guardião | Rota opcional | Ponto principal |
| --- | --- | --- | --- |
| Caçador | Pendente | Pendente | Precisão, alcance e tempo disponível para esquiva |
| Ferreiro | Pendente | Pendente | Aproximação sem dano inevitável; alcance da espada |
| Vigilante | Pendente | Pendente | Cadência e sobrevivência com menor vida |
| Apostador | Pendente | Pendente | Alcance dos socos, pressão à distância e sorte |
| Feiticeira | Pendente | Pendente | Explosão em grupos e comparação com alvos isolados |
| Sacerdote | Pendente | Pendente | Área/DPS da maldição e tempo para se reposicionar |
| Necromante | Pendente | Pendente | Aliados contra arqueiros e ritmo do desbloqueio |
| Guerreiro | Pendente | Pendente | Legibilidade da defesa frontal, estamina e retaguarda |

Alternar Cinzas e Sombras nas rotas opcionais. Depois comparar arma Comum e Rara.
Não presumir que a mesma quantidade de ataques ou dano nominal significa equilíbrio.

Ao terminar, usar **R** e registrar profissão, compras/forja, rota, resultado,
tempo por sala, tempo até a saída, vida, dano recebido, causa da morte, níveis
e dádivas. Registrar separadamente impressões que o relatório não mede:

- O primeiro aviso foi entendido sem ler instruções?
- O círculo e a flecha continuam visíveis entre hordas, aura e maldições?
- Foi possível alcançar um arqueiro com espada/socos sem depender da sorte?
- A defesa funcionou na direção esperada? A flecha nunca atravessou estruturas?
- A passagem foi liberada em um ritmo satisfatório e o chefe exigiu decisões?
- A rota opcional compensou o risco? Surgiu vontade de tentar outra construção?

Ajustar frequência/dano/aviso com base nesses relatos e nos relatórios; depois
ajustar metas, recompensas e Guardião. Ainda precisam de medição desempenho e
memória em sessões longas. Novas regiões ficam para depois de validar esta travessia.

