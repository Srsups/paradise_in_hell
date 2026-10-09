# Tecnologia para concluir o jogo

Avaliação em 07/10/2026. Recomendação: continuar com Java 21, Gradle e libGDX
para o alvo Windows. Essa é uma recomendação técnica para este projeto,
não uma decisão de migração tomada pelo autor.

## Motivos

O projeto já tem combate, progressão, renderização e distribuição Windows com
runtime próprio, verificados em testes. Esses resultados permitem investir no
conteúdo que falta. A demo não comprova desempenho da campanha inteira;
esse desempenho terá de ser medido com a quantidade pretendida de inimigos,
projéteis, poderes e efeitos.

libGDX é um framework Java com suporte a Windows e ferramentas para jogos 2D.
A página oficial apresenta jogos comerciais e o projeto continua recebendo
atualizações. A versão usada aqui permanece 1.13.1; uma atualização de biblioteca
deve ser uma mudança própria, com verificação de compatibilidade.
[Projeto libGDX](https://libgdx.com/).

Para este roguelite, considero o fluxo de produção de conteúdo o risco maior:
criar fases, ajustar inimigos, escrever diálogos e testar combinações. A campanha
já começou a separar esses dados do código e da apresentação. O objetivo é
editar conteúdo sem repetir lógica e sem construir um editor próprio.

## Alternativa considerada

Godot oferece editor de cenas, recursos de dados, ferramentas de animação e
um conjunto dedicado a 2D. Considero uma boa alternativa caso montar cenários,
interfaces e encontros manualmente vire o principal obstáculo.
[Recursos oficiais do Godot](https://godotengine.org/features/).

Migrar exigiria portar entrada, combate, colisão, progressão e telas, além de
refazer e validar distribuição e ferramentas. Não há medida neste projeto
que indique uma limitação da linguagem justificando esse custo agora.
Uma ferramenta visual pode melhorar produtividade; trocar apenas a linguagem
não oferece, por si só, esse fluxo de trabalho.

## Como tornar Java sustentável aqui

- Manter regras e estado separados da renderização e dos recursos gráficos.
- Colocar arenas, inimigos, itens e diálogos em definições de dados validadas.
- Usar ferramentas existentes para conteúdo: Tiled para mapas, Scene2D para
  interfaces maiores, atlas e editores de partículas conforme a necessidade.
- Concluir uma primeira região com chefe e NPCs antes de ampliar o catálogo.
- Medir tempo de quadro, memória e pausas de coleta de lixo em sessões longas.
  Otimizar alocações e colisões quando as medidas indicarem um problema.
- Reusar sistemas e limitar o escopo; evitar escrever uma engine ou editor
  abrangente para viabilizar o jogo.

Tiled, Skin Composer, Texture Packer e editores de partículas aparecem no
[catálogo oficial de ferramentas libGDX](https://libgdx.com/dev/tools/).
Nenhuma dessas ferramentas foi instalada nesta entrega.

## Quando reconsiderar

Reavaliar após a primeira região se produzir uma nova arena, conversa ou
habilidade exigir repetidamente mudanças grandes no código, ou se a criação
visual continuar consumindo a maior parte do trabalho. Nesse caso, vale fazer
um protótipo pequeno em Godot e comparar o tempo de produção e a manutenção.

A mudança de alvo de distribuição ou entrada de colaboradores com outras
necessidades também pede nova avaliação. Para a meta atual, continuar com a
base funcionando é a opção que considero mais econômica em trabalho.
