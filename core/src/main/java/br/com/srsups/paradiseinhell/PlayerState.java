package br.com.srsups.paradiseinhell;

/** Mutable state of one attempt. Contains no input, textures, screens or GL resources. */
public final class PlayerState {
    private final Profession profession;
    private RunStatistics statistics;
    public void setStatistics(RunStatistics value) { statistics=value; }
    private WeaponRarity rarity = WeaponRarity.COMMON;
    private float vida, vidaMaxima, damageBonus, attackRateBonus;
    private int nivel = 1, xpParaProximoNivel = 10, curaPorAbate, magnetRank;
    private float xpAtual, modVelocidade = 1, modDanoRecebido = 1, modXpGanho = 1;
    private float tempoEntreTiros, cooldownTiro;
    private float walkingSpeedLimit = Float.POSITIVE_INFINITY;
    private float estaminaAtual = 100, dashTimer, dashCooldownTimer, timerFlash;
    private boolean dashing, exausto, temRessurreicao, auraDePoseidonAtiva;
    private float danoAura = 5, raioAura = 32;
    private boolean habilidadeEgideAtiva, escudoAtivo;
    private float timerEscudo, protecaoRessurreicao;
    private int shockRank, burnRank, impactRank;
    private float shockCooldown, burnCooldown, impactCooldown, spellCooldown, retaliationCooldown;
    private boolean guarding;
    private float guardX = 1, guardY;
    public void addShock() { shockRank++; }
    public void addBurn() { burnRank++; }
    public void addImpact() { impactRank++; }
    public int shockRank() { return shockRank; }
    public int burnRank() { return burnRank; }
    public int impactRank() { return impactRank; }
    public float baseDps() { return profession.weapon.damage / profession.weapon.interval; }
    public float dps() { return getDano() / getAttackInterval(); }
    public boolean tryShock() { if (shockRank == 0 || shockCooldown > 0) return false; shockCooldown = 0.8f; return true; }
    public boolean tryBurn() { if (burnRank == 0 || burnCooldown > 0) return false; burnCooldown = 0.8f; return true; }
    public boolean tryImpact() { if (impactRank == 0 || impactCooldown > 0) return false; impactCooldown = 0.8f; return true; }
    public boolean trySpell() { if (spellCooldown > 0) return false; spellCooldown = 0.8f; return true; }
    public boolean guarding() { return guarding; }
    public float guardAngle() { return (float) Math.toDegrees(Math.atan2(guardY, guardX)); }
    public void setGuard(boolean held, float dx, float dy) {
        guarding = held && getWeapon() == WeaponType.SHIELD && !dashing && !exausto && estaminaAtual >= 12;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0.001f) { guardX = dx / len; guardY = dy / len; }
    }
    /** Returns true only when the shield actually absorbs the hit. */
    public boolean receive(CombatHit hit, float x, float y) {
        if (escudoAtivo || protecaoRessurreicao > 0) return false;
        float dx = hit.x() - x, dy = hit.y() - y, len = (float) Math.sqrt(dx * dx + dy * dy);
        boolean front = len < 0.001f || (dx * guardX + dy * guardY) / len >= 0.5f;
        if (guarding && hit.kind() != CombatHit.Kind.GROUND && front && estaminaAtual >= 12) {
            estaminaAtual -= 12;
            if (estaminaAtual < 12) guarding = false;
            return true;
        }
        sofrerDano(hit.damage(),hit.label()); return false;
    }
    public boolean tryRetaliation(java.util.random.RandomGenerator random) {
        if (getWeapon() != WeaponType.SHIELD || rarity != WeaponRarity.RARE || retaliationCooldown > 0) return false;
        retaliationCooldown = 0.6f;
        return random.nextDouble() < 0.35;
    }
    /** Compare power to this class's base, including defenses and independent damage sources. */
    public float powerRatio() {
        float offense = dps() / baseDps() + 0.22f * shockRank + 0.22f * burnRank + 0.12f * impactRank;
        if (auraDePoseidonAtiva) offense += danoAura / baseDps();
        return offense * (float) Math.sqrt(vidaMaxima / profession.baseHealth / modDanoRecebido);
    }

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
        shockCooldown = Math.max(0, shockCooldown - delta);
        burnCooldown = Math.max(0, burnCooldown - delta);
        impactCooldown = Math.max(0, impactCooldown - delta);
        spellCooldown = Math.max(0, spellCooldown - delta);
        retaliationCooldown = Math.max(0, retaliationCooldown - delta);
        if (guarding) {
            estaminaAtual = Math.max(0, estaminaAtual - 8 * delta);
            if (estaminaAtual < 12) guarding = false;
        }
        protecaoRessurreicao = Math.max(0, protecaoRessurreicao - delta);
        cooldownTiro = Math.max(0, cooldownTiro - delta);
        dashCooldownTimer = Math.max(0, dashCooldownTimer - delta);
        timerFlash = Math.max(0, timerFlash - delta);
        if (dashing && (dashTimer -= delta) <= 0) dashing = false;
        if (exausto && estaminaAtual >= 20) exausto = false;
        if (!exausto && estaminaAtual <= 5) exausto = true;
        if (!dashing && !guarding) estaminaAtual = Math.min(100, estaminaAtual + (exausto ? 10f / 3 : 10) * delta);
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
    public boolean canDash() { return !guarding && !dashing && dashCooldownTimer <= 0 && estaminaAtual >= 20 && !exausto; }
    public float dashCooldownRemaining() { return dashCooldownTimer; }
    public void tryDash() {
        if (!canDash()) return;
        dashing = true;
        dashTimer = 0.15f;
        dashCooldownTimer = 1;
        estaminaAtual -= 20;
    }
    public void setWalkingSpeedLimit(float limit) {
        if (Float.isNaN(limit) || limit <= 0) throw new IllegalArgumentException("Invalid walking speed limit");
        walkingSpeedLimit = limit;
    }
    public float movementSpeed() {
        float speed = (dashing ? profession.baseDashSpeed : profession.baseSpeed * (exausto ? 0.6f : 1))
            * modVelocidade * (guarding ? 0.55f : 1);
        return dashing ? speed : Math.min(speed, walkingSpeedLimit);
    }
    public boolean isDashing() { return dashing; }
    public boolean flashing() { return timerFlash > 0; }
    public float getEstaminaAtual() { return estaminaAtual; }
    public void recuperarEstamina(float amount) { estaminaAtual = Math.min(100, estaminaAtual + Math.max(0, amount)); }
    public float getEstaminaMaxima() { return 100; }
    public boolean isExausto() { return exausto; }
    public void sofrerDano(float quantidade) { sofrerDano(quantidade,"Dano não identificado"); }
    private void sofrerDano(float quantidade,String cause) {
        if (escudoAtivo || protecaoRessurreicao > 0) return;
        float before=Math.max(0,vida);
        vida -= quantidade * modDanoRecebido;
        if(statistics!=null) statistics.receive(Math.max(0,before-Math.max(0,vida)),cause);
        timerFlash = 0.15f;
    }
    public boolean estaMorto() { return vida <= 0; }
    public float xpGain(int base) { return base * modXpGanho; }
    public void ganharXP(int quantidade) { xpAtual += xpGain(quantidade); }
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
    public float attacksPerSecond() { return 1 / getAttackInterval(); }
    public float attacksPerSecondAfterCadence() {
        return 1 / Math.max(profession.weapon.minimumInterval,
            profession.weapon.interval / ((1 + attackRateBonus + .12f) * rarity.attackRateMultiplier));
    }
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
        if(statistics!=null) statistics.resurrect();
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
