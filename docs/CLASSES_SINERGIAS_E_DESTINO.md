# Classes, sinergias e Fúria dos Deuses

Incremento do protótipo de campanha. Regras confirmadas pelo autor: quatro novas
classes, Necromante lendário por 1.000 Almas compartilhadas, menor chance de reviver inimigos
fortes, exclusão dos chefes de andar e punição por continuar farmando depois de
liberar a saída. Os demais números abaixo são iniciais para playtests.

## Personagens e desbloqueio

Há oito saves, com quatro cartões por página. Setas esquerda/direita, PageUp/PageDown
ou os botões Anterior/Próxima trocam a página; 1–4 escolhem o cartão da página atual.
As nove profissões têm três páginas. A escolha pertence ao personagem e fica
fixa após salvar; a primeira página mantém Caçador, Ferreiro, Vigilante e Apostador.

| Nova profissão | Vida | Dano direto | Intervalo | Velocidade / dash | Habilidade |
| --- | --- | --- | --- | --- | --- |
| Feiticeira | 90 | 6 | 0,75 s | 60 / 200 | Varinha dispara feitiços com explosão ao acertar |
| Sacerdote | 105 | 3 | 0,80 s | 56 / 190 | Projétil aplica maldição em área de raio 32 |
| Necromante — lendária | 85 | 4 | 0,65 s | 58 / 195 | Cajado dispara almas; inimigos mortos podem virar aliados |
| Guerreiro | 150 | 7 | 0,70 s | 50 / 175 | Espada em área e escudo frontal pelo botão direito |

As novas classes têm sorte 1×. Dádivas e atributos permanentes usam as mesmas
fórmulas percentuais das classes anteriores. O elenco inicial já recebeu visuais
e animações próprios; aliados usam a animação dos inimigos em verde. O acabamento
das novas apresentações continua sujeito à revisão artística.

No altar do refúgio, **L → N ou clique no texto do Necromante** compra o desbloqueio
por **1.000 Almas do saldo compartilhado**. Moeda e `necromancerUnlocked`
são gravados juntos em `shared-progress.json`; falha não gasta.

O personagem atual mantém a profissão. Todos os novos saves podem escolher a
classe após essa compra, sem novo pagamento. Óbolos continuam individuais.
Ganhos de Almas vêm dos abates, mini-boss e chefe e são guardados após retornar.
Desbloqueios pagos anteriormente e personagens já Necromantes preservam o acesso.

O preço foi reduzido de 10.000 Óbolos para 1.000 Almas por decisão do autor.
A duração real do desbloqueio ainda precisa de playtests. Economia, migração e
relatórios: [ALMAS_E_RELATORIOS.md](ALMAS_E_RELATORIOS.md).
Use um slot vazio ou a opção Excluir na seleção de saves para reiniciar um personagem,
com confirmação. Almas e acessos lendário/épico permanecem compartilhados.

## Fazendeiro Épico e desafio da Camponesa

Implementado no M5 em **23/10/2026**, com 290 testes unitários, OpenGL e pacote
Windows aprovados; balanceamento humano pendente. É a nona profissão, com vida 120, movimento 57, dash 195 e sorte
1×. A categoria Épica pertence à profissão, separada da raridade de armas/dádivas.

| Ferramenta | Dano base | Intervalo | Alcance comum / raro | Ataque |
| --- | --- | --- | --- | --- |
| Ancinho | 9 | 0,70 s | 52 / 64 | Estocada estreita na direção da mira |
| Foice | 6 | 0,85 s | 34 / 44 | Corte circular de 360° |

**Q** troca ferramentas somente em combate ativo. A recarga é compartilhada:
trocar não concede golpe extra, reinicia procs ou duplica bônus. Dano e cadência
seguem as bases da ferramenta ativa. Uma única forja permanente melhora ambas,
com os bônus gerais de raridade e maior alcance. A ferramenta continua entre
salas e volta ao ancinho em cada tentativa. A profissão permanece fixa no save.

A **Camponesa das Cinzas** tem **10% de chance base por run**, ou **40% para o
Apostador**. Um sorteio independente ocorre após liberar a passagem do Estige;
o encontro opcional vem depois de eventuais recompensa e ferreiro, em segurança,
sem avançar o relógio de combate.

A aceitação é gravada antes de mudar o estado. Depois, derrotar um Carrasco
concede uma semente garantida, independente do Fragmento de Forja e de seu limite
regional. Vencer o Guardião do Submundo com a semente conclui o desafio, sem precisar
reencontrar a NPC. Aceitação e semente persistem entre mortes, abandonos e sessões,
mas pertencem apenas ao personagem que aceitou.

Concluir libera o Fazendeiro para **novos personagens em todos os saves**; não
muda classes existentes nem gasta Óbolos/Almas. A conclusão no perfil permite
recuperar o acesso se a gravação global falhar. A exclusão apaga missões incompletas;
uma conclusão pendente deve ter o acesso preservado antes de apagar o personagem.
Detalhes: [MARCO_5_FAZENDEIRO.md](MARCO_5_FAZENDEIRO.md).
J no refúgio consulta o diário; Tab mostra o objetivo da missão aceita na tentativa.

O [M4](MARCO_4_DADIVAS_E_ALTAR.md) ampliou o catálogo para 22 dádivas, com seis
geralistas novas e Eco do Estige exclusivo do Submundo. Novas regiões terão opções
próprias quando forem produzidas.

## Armas e forja permanente

DPS direto é `danoAtual / intervaloAtual`. As habilidades usam esse valor para
crescer com os atributos da arma e da profissão. A forja conserva o bônus geral
de dano ×1,25 e taxa de ataque ×1,10 entre runs, além do poder específico:

| Arma | Comum | Rara |
| --- | --- | --- |
| Varinha | Explosão de raio 18; 25% do DPS nos outros alvos próximos | Raio 32 |
| Maldição | Raio 32 no impacto; 60% do DPS por segundo em cada alvo por 3 s | Duração 5 s |
| Cajado | Chance de reviver definida pela força do inimigo; aliado dura 60 s | Chance ×1,25; duração 75 s |
| Espada e escudo | Espada de alcance 36; defesa frontal | Alcance 48; 35% de chance de revidar um bloqueio |

Explosões têm recarga própria de 0,8 s e respeitam obstáculos. A maldição alcança
alvos a até 32 unidades do impacto, com linha livre; funciona mesmo se o alvo direto
morrer, sem espalhamento recursivo. Dano e cadência atuais, incluindo forja, definem
o DPS do efeito aplicado. A maldição reaplicada
renova a duração e preserva o maior dano ativo, sem acumular infinitas cópias.
Ela pode afetar o mini-boss e o guardião; não se confunde com reviver esses inimigos.
Durante o salto do guardião, ele fica fora do alcance de armas, aura e efeitos;
a duração da maldição/queimadura continua passando sem causar dano no ar.

O Guerreiro segura o **botão direito**, apontando com o mouse, para defender um cone
frontal de 120°. A defesa bloqueia contato, investida, giro e projéteis, gasta
12 de estamina por bloqueio e drena 8/s enquanto mantida. Nesse período, pode
atacar com o botão esquerdo, mantendo dano/cadência e defesa. Não inicia dash,
anda a 55% da velocidade e não regenera estamina enquanto
a defesa permanece ativa. Abaixo de 12 de estamina, a defesa cai.

Ataques pelas costas, marcas no chão e o impacto da Queda do Tártaro atravessam a defesa.
Na queda da fase 2, a caminhada fica limitada a 24 unidades/s durante o segundo
vermelho; o dash conserva sua velocidade. Solte a defesa e use Shift + direção
no aviso vermelho para sair da área. Égide e proteção de
ressurreição mantêm suas regras. Um projétil bloqueado não pode esconder uma marca
sobreposta. O revide raro causa 150% do dano atual ao inimigo responsável pelo ataque
bloqueado, com recarga de 0,6 s. É contra-ataque direto; não cria um projétil amigo.
As probabilidades são protótipo e ainda precisam de feedback visual/sonoro mais forte.

As escolhas de dádivas têm 1,5 s de espera e exigem soltar o ataque antes de clicar.
Tab consulta as dádivas adquiridas e seus níveis, pausando a tentativa. O HUD
mostra ataques por segundo e Celeridade exibe a taxa atual e a taxa após a escolha.

## Necromancia

| Inimigo na escala base | Índice de força | Chance com cajado comum |
| --- | --- | --- |
| Comum | 1 | 35% |
| Rápido | 1,4 | 25% |
| Tank | 4 | 8,75% |
| Carrasco | 18 | aproximadamente 1,94% |
| Guardião de andar | Excluído | 0% |

A força dos inimigos comuns é seu índice × raiz do multiplicador de vida recebido
ao nascer. O Carrasco usa 18 × raiz do seu multiplicador de vida. Chance =
`35% / max(1, força)`, multiplicada por 1,25 com arma Rara, limitada a 50%.
Assim, inimigos mais fortes pela sala, pelo tempo ou pela fúria também são
mais difíceis de reviver. Cada cadáver recebe no máximo uma tentativa de ressurreição.

Há no máximo seis aliados, com vida útil de 60/75 segundos de combate. Eles seguem
o jogador, perseguem um inimigo próximo e atacam a cada 0,9 s, respeitando estruturas.
Seu dano usa `min(40, 4 × raiz(força)) × bônusDeRaridade`, capturado ao ressuscitar.
Os aliados não sofrem dano nesta primeira versão: a duração e o limite controlam
seu acúmulo. Este comportamento também precisa de playtests.

Os aliados continuam nas próximas salas, incluindo a sala do guardião, e são
reposicionados na entrada segura. Enquanto o guardião está no ar, seguem o jogador
e aguardam um alvo acessível. Conversas, menus e pausa congelam sua duração.
Ao terminar a tentativa, todos desaparecem; eles não são armazenados no perfil.
Não dão XP, Óbolos ou metas por desaparecer. Os inimigos que eles matam dão suas
recompensas normais uma única vez e podem gerar novos aliados. Guardiões nunca
participam do sorteio de reviver.

## Dádivas e três combinações para testar

Este primeiro lote ampliou de 12 para 15; o M4 posterior chegou a **22 dádivas**. As três novas são Raras, com três níveis:

| Dádiva | Efeito por nível | Recarga |
| --- | --- | --- |
| Faísca de Zeus | Pulso de 35% do DPS × nível; encadeia em até nível + 1 alvos | 0,8 s |
| Brasa de Héstia | Queima com 35% do DPS × nível por segundo durante 3 s | 0,8 s |
| Martelo de Hefesto | Empurra 12 + 6 × nível unidades, com resistência dos inimigos pesados | 0,8 s |

Os efeitos têm recargas independentes da velocidade da arma. Tiros adicionais
não multiplicam a frequência de choque, queimadura ou impacto. Queimaduras renovam
duração sem acumular cópias. Raios buscam alvos a até 90 unidades de cada salto,
sem repetir o mesmo alvo e sem atravessar estruturas. Alvos queimando ou amaldiçoados
recebem 25% a mais no pulso elétrico.

A aura de Poseidon também pode disparar os três efeitos respeitando suas recargas.
Com a aura, a força de empurrão aumenta em 35%. Estruturas interrompem o empurrão;
o guardião não pode ser deslocado. Dano contínuo e aliados não geram novos procs
elementais a cada tick: isso impede cadeias recursivas.

Combinações candidatas, ainda sem comprovação de viabilidade em partidas completas:

1. Sacerdote + queimadura + choque: dano contínuo e pulsos reforçados pela maldição.
2. Feiticeira + força/cadência + choque: explosões e cadeias contra grupos.
3. Guerreiro + aura + impacto + resistência: proximidade, controle de espaço e defesa.

Nada obriga uma profissão a usar essas combinações. As outras classes também recebem
os efeitos, escalados por seu DPS. Raridades e sorte seguem os pesos existentes.

## Fúria dos Deuses

Uma build forte que avança não é penalizada. Somente tempo de combate **após
cumprir todas as metas e liberar a saída** gera pressão:

1. Primeiros 20 s após liberar a saída: tolerância sem penalidade.
2. Próximos 15 s reais: aviso no HUD com contagem regressiva, mesmo para builds fortes.
3. Depois: Fúria nível 1; novos níveis surgem a cada 25 unidades de pressão, até nível 3.

| Fúria | Vida dos novos inimigos | Dano dos novos inimigos | Intervalo das hordas | XP por abate |
| --- | --- | --- | --- | --- |
| 0 | ×1 | ×1 | ×1 | ×1 |
| 1 | ×1,20 | ×1,15 | ÷1,20 | ÷1,50 |
| 2 | ×1,40 | ×1,30 | ÷1,40 | ÷2 |
| 3 | ×1,60 | ×1,45 | ÷1,60 | ÷2,50 |

Os multiplicadores de vida/dano se somam à curva linear existente por multiplicação.
Inimigos vivos não recuperam vida nem mudam seus atributos. XP é arredondada para
inteiro, com mínimo de 1, e aplicada ao cristal no momento do abate; um tank de
5 XP concede 3/3/2 nos níveis 1/2/3. Baús e recompensas garantidas mantêm seu valor.

Pressão por segundo após o aviso = `min(3, 1 + 0,6 × excesso)`. A referência
da sala é `1,5 + difficultySeconds/120`; excesso = poder + 0,15 × aliados −
referência, com mínimo zero. Poder usa DPS/baseDPS da própria classe, bônus
elementais, aura, vida máxima relativa e resistência. As classes começam com a
mesma referência 1, apesar de seus diferentes atributos base. Os valores estão
em PlayerState.powerRatio e DivineFury.

Pausa, seleção de dádivas, rotas, ferreiro e recompensas congelam o relógio.
Entrar na próxima sala limpa a pressão; a dificuldade normal de tempo total
continua. O chefe e a demo de cinco minutos não acumulam fúria.

## Verificação

Além das verificações da base abaixo, o M5 passou em 290 testes unitários,
integração OpenGL do fluxo completo e verificação nativa do pacote Windows.
Missão, falhas de gravação, exclusão segura, criação do Fazendeiro e ferramentas
foram cobertas. Equilíbrio, animações e ritmo continuam sujeitos a partidas humanas.

Os testes de regressão verificam custos, falha de gravação, migração, oito arquivos,
bloqueio de profissão, efeitos nas oito classes, recargas, dano contínuo, paredes,
chance inversa de necromancia, exclusão de guardiões, duplicação/limites/expiração
de aliados, defesa frontal/traseira/chão, estamina e aviso/escalada/reset da fúria.

A verificação OpenGL cria as quatro novas classes pelos menus, compra o desbloqueio,
recarrega perfis, atinge inimigos com as armas, observa maldição e aliados, leva
aliados ao chefe, testa defesa/revide, sinergias e pausa da fúria. Capturas ficam
em `build/smoke/`. O executável Windows também renderiza as novas classes no
diagnóstico invisível.

O próximo passo é medir travessias completas por profissão: duração, nível,
dádivas, mortes, vida ao entrar no chefe e tempo acumulado após a saída. Esses dados
podem ser consultados com R no resultado/refúgio e devem ajustar a economia do Necromante, a força dos aliados e a pressão do destino,
antes de acrescentar outra região.

