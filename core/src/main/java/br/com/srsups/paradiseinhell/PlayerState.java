package br.com.srsups.paradiseinhell;

/** Mutable state of one attempt. Contains no input, textures, screens or GL resources. */
public final class PlayerState {
    private final Profession profession;
    private WeaponRarity rarity = WeaponRarity.COMMON;
    private float vida, vidaMaxima, damageBonus, attackRateBonus;
    private int nivel = 1, xpParaProximoNivel = 10, curaPorAbate, magnetRank;
    private float xpAtual, modVelocidade = 1, modDanoRecebido = 1, modXpGanho = 1;
    private float tempoEntreTiros, cooldownTiro;
    private float estaminaAtual = 100, dashTimer, dashCooldownTimer, timerFlash;
    private boolean dashing, exausto, temRessurreicao, auraDePoseidonAtiva;
    private float danoAura = 5, raioAura = 32;
    private boolean habilidadeEgideAtiva, escudoAtivo;
    private float timerEscudo, protecaoRessurreicao;

    public PlayerState() { this(Profession.CACADOR); }
    public PlayerState(Profession profession) {
        this.profession = java.util.Objects.requireNonNull(profession);
        vida = vidaMaxima = profession.baseHealth;
        tempoEntreTiros = profession.weapon.interval;
    }
    public Profession getProfession() { return profession; }
    public WeaponType getWeapon() { return profession.weapon; }
    public WeaponRarity getWeaponRarity() { return rarity; }
    public boolean canForgeWeapon() { return rarity == WeaponRarity.COMMON; }
    public void forgeWeapon() {
        if (!canForgeWeapon()) throw new IllegalStateException("Weapon already forged");
        rarity = WeaponRarity.RARE;
        refreshAttackInterval();
    }
    public float getLuckMultiplier() { return profession.luckMultiplier; }
    public float meleeReach() {
        if (profession.weapon == WeaponType.FISTS) return rarity == WeaponRarity.RARE ? 34 : 24;
        return swordReach();
    }
    public float swordReach() { return rarity == WeaponRarity.RARE ? 48 : WeaponAttack.SWORD_REACH; }

    public void advance(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        protecaoRessurreicao = Math.max(0, protecaoRessurreicao - delta);
        cooldownTiro = Math.max(0, cooldownTiro - delta);
        dashCooldownTimer = Math.max(0, dashCooldownTimer - delta);
        timerFlash = Math.max(0, timerFlash - delta);
        if (dashing && (dashTimer -= delta) <= 0) dashing = false;
        if (exausto && estaminaAtual >= 20) exausto = false;
        if (!exausto && estaminaAtual <= 5) exausto = true;
        if (!dashing) estaminaAtual = Math.min(100, estaminaAtual + (exausto ? 10f / 3 : 10) * delta);
        if (habilidadeEgideAtiva && (timerEscudo -= delta) <= 0) {
            escudoAtivo = !escudoAtivo;
            timerEscudo = escudoAtivo ? 2 : 10;
        }
    }
    public boolean tryShoot() {
        if (cooldownTiro > 0) return false;
        cooldownTiro = tempoEntreTiros;
        return true;
    }
    public void tryDash() {
        if (dashCooldownTimer > 0 || estaminaAtual < 20 || exausto) return;
        dashing = true;
        dashTimer = 0.15f;
        dashCooldownTimer = 1;
        estaminaAtual -= 20;
    }
    public float movementSpeed() { return (dashing ? profession.baseDashSpeed : profession.baseSpeed * (exausto ? 0.6f : 1)) * modVelocidade; }
    public boolean flashing() { return timerFlash > 0; }
    public float getEstaminaAtual() { return estaminaAtual; }
    public void recuperarEstamina(float amount) { estaminaAtual = Math.min(100, estaminaAtual + Math.max(0, amount)); }
    public float getEstaminaMaxima() { return 100; }
    public boolean isExausto() { return exausto; }
    public void sofrerDano(float quantidade) {
        if (escudoAtivo || protecaoRessurreicao > 0) return;
        vida -= quantidade * modDanoRecebido;
        timerFlash = 0.15f;
    }
    public boolean estaMorto() { return vida <= 0; }
    public void ganharXP(int quantidade) { xpAtual += quantidade * modXpGanho; }
    public boolean podeSubirDeNivel() { return xpAtual >= xpParaProximoNivel; }
    public void subirDeNivel() {
        if (!podeSubirDeNivel()) throw new IllegalStateException("Not enough XP");
        nivel++;
        xpAtual -= xpParaProximoNivel;
        xpParaProximoNivel = (int) (xpParaProximoNivel * 1.5f);
    }
    public float getDano() { return profession.weapon.damage * rarity.damageMultiplier * (1 + damageBonus); }
    public void aumentarDano(float percentual) { damageBonus += percentual; }
    public float getAttackInterval() { return tempoEntreTiros; }
    public void aumentarCadencia() {
        attackRateBonus += 0.12f;
        refreshAttackInterval();
    }
    private void refreshAttackInterval() {
        tempoEntreTiros = Math.max(profession.weapon.minimumInterval,
            profession.weapon.interval / ((1 + attackRateBonus) * rarity.attackRateMultiplier));
    }
    public void aumentarVidaMaxima(float percentual) {
        float bonus = profession.baseHealth * percentual;
        vidaMaxima += bonus; vida += bonus;
    }
    public void aumentarAtracaoXp() { magnetRank = Math.min(3, magnetRank + 1); }
    public float getRaioAtracaoXp() { return 36 + 32 * magnetRank; }
    public float getVelocidadeAtracaoXp() { return 120 + 40 * magnetRank; }
    public boolean possuiImaXp() { return magnetRank > 0; }
    public void aumentarVelocidadeMovimento(float percentual) { modVelocidade += percentual; }
    public void aumentarResistencia(float percentual) { modDanoRecebido = Math.max(0.25f, modDanoRecebido - percentual); }
    public void aumentarInteligencia(float percentual) { modXpGanho += percentual; }
    public void curar(int valor) { vida = Math.min(vida + valor, vidaMaxima); }
    public boolean possuiRessurreicao() { return temRessurreicao; }
    public void ganharRessurreicao() { temRessurreicao = true; }
    public void usarRessurreicao() {
        if (!temRessurreicao) throw new IllegalStateException("No resurrection available");
        temRessurreicao = false;
        vida = vidaMaxima / 2;
        protecaoRessurreicao = 2;
    }
    public void ativarCuraPorAbate(int valor) { curaPorAbate = valor; }
    public int getCuraPorAbate() { return curaPorAbate; }
    public float getVida() { return vida; }
    public float getVidaMaxima() { return vidaMaxima; }
    public int getNivel() { return nivel; }
    public float getXpAtual() { return xpAtual; }
    public int getXpParaProximoNivel() { return xpParaProximoNivel; }
    public void ativarAuraDePoseidon() {
        if (auraDePoseidonAtiva) { danoAura += 4; raioAura += 8; }
        auraDePoseidonAtiva = true;
    }
    public boolean possuiAura() { return auraDePoseidonAtiva; }
    public float getRaioAura() { return raioAura; }
    public float getDanoAura() { return danoAura; }
    public boolean isEscudoAtivo() { return escudoAtivo; }
    public void ativarHabilidadeEgide() { habilidadeEgideAtiva = true; }
}
