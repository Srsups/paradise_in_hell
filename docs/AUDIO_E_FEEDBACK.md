# Áudio provisório e leitura do combate

Implementado em 12/10/2026. Sons e trilhas são provisórios; a audição com jogadores,
alto-falantes e fones continua necessária para revisar timbre, mixagem e fadiga.

## Controles e persistência

**O** abre áudio no menu, no refúgio em estado livre, nas hordas e no chefe.
Nas arenas, é um painel sobre a tela existente: entidades, colisão, timers,
fúria, dádivas, efeitos e aliados continuam congelados enquanto ele estiver aberto.
Fechar preserva a pausa/seleção anterior e não avança a espera das dádivas.
A música continua tocando durante a pausa e o painel.

- Clique/arraste os controles de Efeitos sonoros e Música, de 0 a 100%.
- Acima/baixo escolhe o controle; esquerda/direita ajusta em 5 pontos percentuais.
- M alterna silenciar tudo; T testa um bloqueio; S repete uma gravação pendente.
- Esc/O ou Voltar fecha e preserva os volumes escolhidos.

Valores iniciais: efeitos 75%, música 30%, silenciar desligado.
Ajustes são aplicados imediatamente. Arrastar grava ao soltar, evitando uma
gravação por quadro; teclado/cliques gravam ao ajustar e sair também tenta salvar.
Silenciar conserva os dois volumes, para restaurá-los ao desativar.

Arquivo: `%LOCALAPPDATA%\ParadiseInHell\audio-settings.json`, com cópia válida
`.bak`. Preferências são compartilhadas por todos os personagens e não são
apagadas ao excluir um save. Não alteram moeda, profissão ou relatórios.
Falha de gravação mantém a configuração em memória e mostra a opção de tentar
novamente. Falha de leitura/versão desconhecida preserva os arquivos e mantém
os controles funcionando apenas naquela sessão.

## Sons e música

Os 24 efeitos cobrem as oito armas, impacto, defesa, proteção, revide, dash, XP,
dano recebido, avisos do Guardião/Carrasco/arqueiro, trava vermelha da queda,
subida de nível, baú, passagem liberada, ressurreição, morte e vitória.

Ataque soa quando a ação acontece; impacto requer vida realmente retirada pelo
golpe da arma. Ataques contra o Guardião no ar não criam um falso impacto.
Defesa, dano e proteção usam o resultado real do cálculo de combate.
Avisos são disparados na mudança de estado, sem repetir a cada quadro desenhado.
Morte/vitória soam no primeiro encerramento, não ao redesenhar o resultado.
XP soa na coleta, e dash somente quando iniciado.

O mixer mantém até 8 efeitos simultâneos, com intervalos mínimos por categoria.
Avisos importantes podem substituir ataques/XP de prioridade menor.
Coletas em grupo e impactos repetidos não geram centenas de vozes.
Ajustar o volume atualiza também efeitos ativos; silenciar interrompe esses efeitos.
Trocar de tela limpa vozes, e encerrar libera sons e música uma única vez.

Duas trilhas instrumentais curtas, em loop:

| Trilha | Duração | Contexto |
| --- | --- | --- |
| Refúgio | 24 s | Menu, refúgio e telas seguras |
| Combate | 16 s | Hordas e Guardião |

A trilha de combate continua ao trocar diretamente entre arenas, sem reiniciar
a cada tela do mesmo contexto. Apenas uma trilha toca por vez.
Sem dispositivo funcional, o jogo mantém os controles e o combate utilizáveis.

## Efeitos visuais

Ataques, impactos e rastros usam cores próprias por arma:
arco dourado, espada laranja, pistola branca, socos coral, feitiço azul,
maldição magenta, almas verdes e espada/escudo ciano.
A geometria e o dano dos ataques permanecem iguais.

Bloqueio ganha faísca/arco e a indicação DEFESA; revide ganha ligação com o alvo.
Proteção é distinguida de bloqueio e de dano. Ao perder vida, uma borda vermelha
curta reforça o flash do personagem; ela não altera a câmera nem as hitboxes.
A indicação de proteção anterior desaparece quando há dano real.
Dash deixa rastro curto, cristais geram um pulso na coleta e ressurreição tem anel.

Até 96 pulsos por arena, com limite de 16 rastros de projéteis por quadro.
Sinais de defesa/proteção têm prioridade sobre rastros repetitivos.
Rótulos não se sobrepõem em cópias do mesmo tipo. Efeitos expiram em frações de
segundo e são descartados na saída; não guardam referências a inimigos ou texturas.

## Origem dos arquivos

Todos os WAVs foram sintetizados por código para este projeto, sem gravações,
samples, músicas ou pacotes externos. O gerador versionado é
[tools/generate_audio.py](../tools/generate_audio.py); ele usa apenas a biblioteca
padrão de Python e resultados determinísticos. Executar:

```powershell
python tools/generate_audio.py
```

Saída: `core/assets/audio/`, PCM16 mono a 22.050 Hz. Os recursos são carregados
de dentro do JAR, não por caminhos relativos à instalação.
Esta é uma base técnica; trilhas completas, mixagem final e identidade sonora
ainda precisam de trabalho artístico.

## Visibilidade sonora do Carrasco implementada

Correção do M1 em 14/10/2026. GameScreen só pede BOSS_WARNING ao iniciar uma
nova preparação com o corpo visível. VisibleArea consulta o retângulo da câmera,
zoom e viewport, e SpriteHitbox.visibleWithin verifica células opacas do quadro
desenhado; margens transparentes fora da tela não tornam o inimigo audível.

GameAudio associa vozes a um emissor. stopEmitter encerra somente as vozes do
Carrasco ao sair da visão, morrer ou liberar a arena. O desenho revalida a visão
também durante pausa/redimensionamento. Retorno à tela não repete avisos antigos.
Prioridade, limite de oito vozes e recarga por tipo de som permanecem globais;
nenhuma fila de sons atrasados ou mapa crescente de emissores é criado.

Música, jogador e outros emissores continuam independentes. Dano recebido mantém
seu sinal próprio; o Guardião mantém os avisos quando está no ar. Culling sonoro
não altera simulação, dano ou duração dos projéteis. Testes reais incluem invisível,
retorno, parada seletiva, borda, zoom e letterboxing, além da validação das máscaras.

Os sinais visuais por formas ainda serão substituídos por sprites nos M3/M6.
Regras e resultados: [MARCO_1_LEITURA_E_CHEFE.md](MARCO_1_LEITURA_E_CHEFE.md).

## Verificação

Testes de regras cobrem volumes/mute, gravação/backup, arquivos inválidos,
preservação após excluir personagem, limites/prioridade de vozes, descarte,
falha de dispositivo, duração/conteúdo dos WAVs e feedback ligado a dano efetivo.
A verificação OpenGL usa saída de áudio registrada, testa os controles reais,
sons das oito armas, defesa/dano/proteção, pausa e alerta do chefe sem duplicação.
Capturas ficam em `build/smoke/audio-*.png` e `feedback-*.png`.

O pacote tem **--audio-smoke-test**, além do diagnóstico gráfico existente.
`tools/verify_windows_package.ps1` executa ambos com Java incluído, diretório de
trabalho externo e dispositivo nulo do OpenAL Soft. O diagnóstico de áudio usa
o decodificador/mixer nativo para carregar/reproduzir os 24 efeitos e verifica
que a música em streaming avança. Ele não reproduz som pelos alto-falantes e
não substitui uma avaliação auditiva em hardware real.

Antes de fechar o marco, jogar uma travessia e avaliar se avisos de perigo são
distintos, se tiros/coleta cansam durante hordas longas e se cores permanecem
legíveis junto a aura, maldição e vários inimigos. Música/efeitos devem ajustar
separadamente, inclusive silenciar, e manter os valores após fechar/reabrir.

