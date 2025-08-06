\<div align="center"\>

# 🏺 Paradise in Hell 🏺

*Um roguelite 2D de sobrevivência com inspiração na mitologia grega.*

\</div\>

\<p align="center"\>
\<img src="[https://github-readme-stats.vercel.app/api?username=Srsups\&show\_icons=true\&theme=radical\&hide\_border=true\&include\_all\_commits=true\&count\_private=true](https://www.google.com/search?q=https://github-readme-stats.vercel.app/api%3Fusername%3DSrsups%26show_icons%3Dtrue%26theme%3Dradical%26hide_border%3Dtrue%26include_all_commits%3Dtrue%26count_private%3Dtrue)" alt="Estatísticas do GitHub de Srsups" /\>
\<img src="[https://github-readme-stats.vercel.app/api/top-langs/?username=Srsups\&layout=compact\&theme=radical\&hide\_border=true](https://www.google.com/search?q=https://github-readme-stats.vercel.app/api/top-langs/%3Fusername%3DSrsups%26layout%3Dcompact%26theme%3Dradical%26hide_border%3Dtrue)" alt="Top Linguagens de Srsups" /\>
\</p\>

-----

## 📖 Sobre o Jogo

**Paradise in Hell** é um jogo de sobrevivência de hordas no estilo "bullet heaven" (como *Vampire Survivors*). O jogador deve sobreviver a ondas infinitas de inimigos mitológicos, coletar experiência, subir de nível e escolher entre uma variedade de dádivas dos deuses do Olimpo para se tornar cada vez mais poderoso. Cada partida é uma nova chance de construir um personagem divino e enfrentar o caos do submundo.

## 📊 Status do Projeto

**`Status:`** 🚀 Em Desenvolvimento Ativo

## ✨ Funcionalidades Atuais

O projeto já conta com uma base sólida e um ciclo de jogo completo. As seguintes funcionalidades estão implementadas:

### 🕹️ Gameplay & Core

  - **Mundo Infinito:** Mapa gerado proceduralmente que se expande conforme o jogador explora.
  - **Movimentação Complexa:** Personagem com sistema de dash, estamina e exaustão.
  - **Combate Automático:** Sistema de tiro contínuo com cooldown.
  - **Inimigos com IA:** Hordas de inimigos que aparecem fora da tela, perseguem o jogador e atacam com cooldown.
  - **Física de Colisão:** Projéteis e personagens colidem com os obstáculos do mapa.

### 📈 Progressão & Habilidades

  - **Sistema de Experiência:** Inimigos derrotados dropam orbes de XP.
  - **Tela de Level Up:** Ao subir de nível, o jogo pausa e apresenta uma tela com escolhas de melhorias aleatórias.
  - **Habilidades Passivas:** Implementação de auras de dano (`Correnteza de Poseidon`) e escudos periódicos (`Égide`).
  - **Melhorias de Atributos:** Grande variedade de upgrades que alteram status como vida, velocidade, dano, etc.

### 🛠️ Arquitetura & UI

  - **Ciclo de Jogo Completo:** Telas de Menu Principal, Jogo e Game Over com transição estável.
  - **Interface Clara:** HUD com barras de vida e estamina, nível e XP.
  - **Feedback Visual:** Números de dano flutuantes e efeito de "flash" ao receber dano.

-----

## 💻 Tecnologias Utilizadas

Este projeto é construído com um conjunto de ferramentas robustas e consagradas no desenvolvimento de jogos em Java.

-----

## ⚙️ Ambiente de Desenvolvimento: Guia de Instalação

Este guia serve para configurar um novo ambiente de desenvolvimento em qualquer máquina e continuar a programação.

#### ✅ **Pré-requisitos**

  * **☕ `Java Development Kit (JDK)`:** Versão 17 ou superior. Recomendo o **[Eclipse Temurin (Adoptium)](https://adoptium.net/temurin/releases/?version=17)**.
  * **💡 `IntelliJ IDEA`:** A versão Community (gratuita) é suficiente. Baixe no site da **[JetBrains](https://www.jetbrains.com/idea/download/)**.
  * **🐙 `Git`:** Para controle de versão. Baixe em **[git-scm.com](https://git-scm.com/downloads)**.

#### 🚀 **Passo a Passo da Configuração**

1.  **Clone o Repositório:** Abra seu terminal e execute:

    ```bash
    git clone https://github.com/Srsups/paradise_in_hell.git
    ```

2.  **Importe no IntelliJ IDEA:**

      - Abra o IntelliJ e clique em **`Open`**.
      - Navegue até a pasta `paradise_in_hell` que você clonou.
      - Selecione o arquivo `build.gradle` e clique em **`OK`**.
      - Escolha **`Open as Project`**.
      - ➡️ **Aguarde a sincronização do Gradle.** Ele vai baixar todas as dependências do libGDX.

-----

### ▶️ Executando o Jogo

Após a sincronização, você pode rodar o jogo de duas formas:

1.  **Via Gradle:** No painel do Gradle (canto direito), navegue até `desktop > Tasks > application` e dê um duplo-clique em **`run`**.
2.  **Via `DesktopLauncher`:** Navegue no painel de projeto até `desktop/src/` e encontre `DesktopLauncher.java`. Clique na seta verde ao lado do método `main` para rodar.

-----

### 🗺️ Roadmap Futuro

Para guiar os próximos passos, aqui está o nosso plano de desenvolvimento:

  - [x] **Aprofundar o Core Loop:** Sistema de Level Up com escolhas e habilidades passivas.
  - [ ] **Aprofundar o Desafio:**
      - [ ] Implementar novos tipos de inimigos (rápidos, tanques, de longa distância).
      - [ ] Adicionar um sistema de "eventos" com ondas de inimigos mais difíceis.
      - [ ] Criar um chefe que aparece em um determinado tempo de partida.
  - [ ] **Aprofundar as Ferramentas do Jogador:**
      - [ ] Implementar novas armas e habilidades ativas (`Raio de Zeus`, etc.).
    <!-- end list -->
      * [ ] Criar um sistema de "Classes" que muda o status/arma inicial.
  - [ ] **Construir a Meta-Progressão:**
      - [ ] Sistema de moedas dropadas por inimigos.
      - [ ] Loja de melhorias permanentes no menu principal.
  - [ ] **Polimento Final:**
      - [ ] Adicionar efeitos sonoros e música.
      - [ ] Melhorar os efeitos visuais (VFX).

-----
