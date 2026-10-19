# Fontes de origem

Baixadas em 15/10/2026 do repositório oficial Google Fonts:

- [Cinzel variável](https://github.com/google/fonts/tree/main/ofl/cinzel): Cinzel.ttf e Cinzel-OFL.txt.
- [Noto Sans variável](https://github.com/google/fonts/tree/main/ofl/notosans): NotoSans.ttf e NotoSans-OFL.txt.

As cópias locais permitem refazer o bake sem novo download. Executar, na raiz:

```powershell
java tools/FontBaker.java
```

O jogo distribui somente os arquivos PNG/FNT e as licenças em core/assets/ui/fonts.
TTFs e AWT são utilizados pela ferramenta offline; não são dependências do runtime.
A fonte de corpo inclui os acentos portugueses e 198 glifos. Cinzel tem 200.
Símbolos ausentes nessas fontes não recebem fallback de fontes do Windows;
usar texto suportado ou acrescentar uma fonte licenciada ao pipeline ao precisar.

