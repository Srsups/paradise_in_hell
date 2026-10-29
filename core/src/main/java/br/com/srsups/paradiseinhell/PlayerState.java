package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;

/** Mutable state of one attempt. Contains no input, textures, screens or GL resources. */
public final class PlayerState {
    private final Profession profession;
    private WeaponType weapon;
    private final BoonState boons=new BoonState();
    public BoonState boons(){return boons;}
    private float staminaCapacityBonus,staminaRecoveryBonus;
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
    public float baseDps() { return getWeapon().damage / getWeapon().interval; }
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
        return (offense+boons.powerBonus()) * (float) Math.sqrt(vidaMaxima / profession.baseHealth / modDanoRecebido);
    }

    public PlayerState() { this(Profession.CACADOR); }
    public PlayerState(Profession profession) {
        this.profession = java.util.Objects.requireNonNull(profession);
        weapon = profession.weapon;
        vida = vidaMaxima = profession.baseHealth;
        tempoEntreTiros = profession.weapon.interval;
    }
    public Profession getProfession() { return profession; }
    public WeaponType getWeapon() { return weapon; }
    /** Both tools belong to the same character and share the unexpired attack cooldown. */
    public boolean switchTool() {
        if (profession != Profession.FAZENDEIRO) return false;
        weapon = weapon == WeaponType.RAKE ? WeaponType.SCYTHE : WeaponType.RAKE;
        refreshAttackInterval();
        return true;
    }
    public WeaponRarity getWeaponRarity() { return rarity; }
    public boolean canForgeWeapon() { return rarity == WeaponRarity.COMMON; }
    public void forgeWeapon() {
        if (!canForgeWeapon()) throw new IllegalStateException("Weapon already forged");
        rarity = WeaponRarity.RARE;
        refreshAttackInterval();
    }
    public float getLuckMultiplier() { return profession.luckMultiplier; }
    public float meleeReach() {
        if (getWeapon() == WeaponType.FISTS) return rarity == WeaponRarity.RARE ? 34 : 24;
        if (getWeapon() == WeaponType.RAKE) return rarity == WeaponRarity.RARE ? 64 : 52;
        if (getWeapon() == WeaponType.SCYTHE) return rarity == WeaponRarity.RARE ? 44 : 34;
        return swordReach();
    }
    public float swordReach() { return rarity == WeaponRarity.RARE ? 48 : WeaponAttack.SWORD_REACH; }

    public void advance(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        boons.advance(delta);
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
        if (!dashing && !guarding) estaminaAtual = Math.min(getEstaminaMaxima(),
            estaminaAtual + staminaRegeneration() * (exausto ? 1f/3 : 1) * delta);
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
        boons.dash(dps());
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
    private long damageRevision;
    /** Visual observers see real damage even if healing occurs in the same combat tick. */
    public long damageRevision(){return damageRevision;}
    public float getEstaminaAtual() { return estaminaAtual; }
    public void recuperarEstamina(float amount) { estaminaAtual = Math.min(getEstaminaMaxima(), estaminaAtual + Math.max(0, amount)); }
    public float getEstaminaMaxima() { return 100+100*staminaCapacityBonus; }
    public float staminaRegeneration(){return 10*(1+staminaRecoveryBonus);}
    public float normalWalkingSpeed(){return profession.baseSpeed*modVelocidade;}
    public void aumentarCapacidadeEstamina(float percent){
        float before=getEstaminaMaxima();staminaCapacityBonus+=percent;estaminaAtual+=getEstaminaMaxima()-before;
    }
    public void aumentarRecuperacaoEstamina(float percent){staminaRecoveryBonus+=percent;}
    public void onKill(){
        if(estaMorto())return;
        curar(curaPorAbate);recuperarEstamina(boons.staminaOnKill());
    }
    public boolean isExausto() { return exausto; }
    public void sofrerDano(float quantidade) { sofrerDano(quantidade,"Dano não identificado"); }
    private void sofrerDano(float quantidade,String cause) {
        if (escudoAtivo || protecaoRessurreicao > 0) return;
        float before=Math.max(0,vida);
        vida -= quantidade * modDanoRecebido * boons.receivedMultiplier();
        if(Math.max(0,vida)<before)damageRevision++;
        if(statistics!=null) statistics.receive(Math.max(0,before-Math.max(0,vida)),cause);
        if(quantidade>0)boons.afterDamage();
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
    public float getDano() { return getWeapon().damage * rarity.damageMultiplier * (1 + damageBonus+boons.damageBonus(vida,vidaMaxima)); }
    public void aumentarDano(float percentual) { damageBonus += percentual; }
    public float getAttackInterval() { return tempoEntreTiros; }
    public float attacksPerSecond() { return 1 / getAttackInterval(); }
    public float attacksPerSecondAfterCadence() {
        return 1 / Math.max(getWeapon().minimumInterval,
            getWeapon().interval / ((1 + attackRateBonus + .12f) * rarity.attackRateMultiplier));
    }
    public void aumentarCadencia() {
        attackRateBonus += 0.12f;
        refreshAttackInterval();
    }
    private void refreshAttackInterval() {
        tempoEntreTiros = Math.max(getWeapon().minimumInterval,
            getWeapon().interval / ((1 + attackRateBonus) * rarity.attackRateMultiplier));
    }
    public void aumentarVidaMaxima(float percentual) {
        float bonus = profession.baseHealth * percentual;
        vidaMaxima += bonus; vida += bonus;
    }
    public void aumentarAtracaoXp() { magnetRank = Math.min(3, magnetRank + 1); }
    public float getRaioAtracaoXp() { return 36 + 32 * magnetRank+(boons.collectionActive()?96:0); }
    public float getVelocidadeAtracaoXp() { return 120 + 40 * magnetRank+(boons.collectionActive()?120:0); }
    public boolean possuiImaXp() { return magnetRank > 0; }
    public void aumentarVelocidadeMovimento(float percentual) { modVelocidade += percentual; }
    public void aumentarResistencia(float percentual) { modDanoRecebido = Math.max(0.25f, modDanoRecebido - percentual); }
    public float damageReduction() { return 1 - modDanoRecebido; }
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

    /** Exact attempt state. No input, animation, resource or statistics owner is retained. */
    public JsonValue checkpointState() {
        JsonValue state=new JsonValue(JsonValue.ValueType.object);
        SnapshotJson.put(state,"version",1);SnapshotJson.put(state,"profession",profession.name());
        SnapshotJson.put(state,"weapon",weapon.name());SnapshotJson.put(state,"rarity",rarity.name());
        state.addChild("boons",boons.checkpointState());
        SnapshotJson.put(state,"staminaCapacityBonus",staminaCapacityBonus);SnapshotJson.put(state,"staminaRecoveryBonus",staminaRecoveryBonus);
        SnapshotJson.put(state,"vida",vida);SnapshotJson.put(state,"vidaMaxima",vidaMaxima);
        SnapshotJson.put(state,"damageBonus",damageBonus);SnapshotJson.put(state,"attackRateBonus",attackRateBonus);
        SnapshotJson.put(state,"nivel",nivel);SnapshotJson.put(state,"xpParaProximoNivel",xpParaProximoNivel);
        SnapshotJson.put(state,"curaPorAbate",curaPorAbate);SnapshotJson.put(state,"magnetRank",magnetRank);
        SnapshotJson.put(state,"xpAtual",xpAtual);SnapshotJson.put(state,"modVelocidade",modVelocidade);
        SnapshotJson.put(state,"modDanoRecebido",modDanoRecebido);SnapshotJson.put(state,"modXpGanho",modXpGanho);
        SnapshotJson.put(state,"tempoEntreTiros",tempoEntreTiros);SnapshotJson.put(state,"cooldownTiro",cooldownTiro);
        if(walkingSpeedLimit==Float.POSITIVE_INFINITY)state.addChild("walkingSpeedLimit",new JsonValue(JsonValue.ValueType.nullValue));
        else SnapshotJson.put(state,"walkingSpeedLimit",walkingSpeedLimit);
        SnapshotJson.put(state,"estaminaAtual",estaminaAtual);SnapshotJson.put(state,"dashTimer",dashTimer);
        SnapshotJson.put(state,"dashCooldownTimer",dashCooldownTimer);SnapshotJson.put(state,"timerFlash",timerFlash);
        SnapshotJson.put(state,"dashing",dashing);SnapshotJson.put(state,"exausto",exausto);
        SnapshotJson.put(state,"temRessurreicao",temRessurreicao);SnapshotJson.put(state,"auraDePoseidonAtiva",auraDePoseidonAtiva);
        SnapshotJson.put(state,"danoAura",danoAura);SnapshotJson.put(state,"raioAura",raioAura);
        SnapshotJson.put(state,"habilidadeEgideAtiva",habilidadeEgideAtiva);SnapshotJson.put(state,"escudoAtivo",escudoAtivo);
        SnapshotJson.put(state,"timerEscudo",timerEscudo);SnapshotJson.put(state,"protecaoRessurreicao",protecaoRessurreicao);
        SnapshotJson.put(state,"shockRank",shockRank);SnapshotJson.put(state,"burnRank",burnRank);SnapshotJson.put(state,"impactRank",impactRank);
        SnapshotJson.put(state,"shockCooldown",shockCooldown);SnapshotJson.put(state,"burnCooldown",burnCooldown);
        SnapshotJson.put(state,"impactCooldown",impactCooldown);SnapshotJson.put(state,"spellCooldown",spellCooldown);
        SnapshotJson.put(state,"retaliationCooldown",retaliationCooldown);SnapshotJson.put(state,"guarding",guarding);
        SnapshotJson.put(state,"guardX",guardX);SnapshotJson.put(state,"guardY",guardY);SnapshotJson.put(state,"damageRevision",damageRevision);
        return state;
    }
    public static PlayerState fromCheckpointState(JsonValue state) {
        SnapshotJson.object(state,"version","profession","weapon","rarity","boons","staminaCapacityBonus","staminaRecoveryBonus",
            "vida","vidaMaxima","damageBonus","attackRateBonus","nivel","xpParaProximoNivel","curaPorAbate","magnetRank","xpAtual",
            "modVelocidade","modDanoRecebido","modXpGanho","tempoEntreTiros","cooldownTiro","walkingSpeedLimit","estaminaAtual",
            "dashTimer","dashCooldownTimer","timerFlash","dashing","exausto","temRessurreicao","auraDePoseidonAtiva","danoAura","raioAura",
            "habilidadeEgideAtiva","escudoAtivo","timerEscudo","protecaoRessurreicao","shockRank","burnRank","impactRank",
            "shockCooldown","burnCooldown","impactCooldown","spellCooldown","retaliationCooldown","guarding","guardX","guardY","damageRevision");
        SnapshotJson.integer(state,"version",1,1);
        PlayerState player=new PlayerState(SnapshotJson.enumValue(state,"profession",Profession.class));
        player.weapon=SnapshotJson.enumValue(state,"weapon",WeaponType.class);
        player.rarity=SnapshotJson.enumValue(state,"rarity",WeaponRarity.class);
        if(player.profession==Profession.FAZENDEIRO?player.weapon!=WeaponType.RAKE&&player.weapon!=WeaponType.SCYTHE:player.weapon!=player.profession.weapon)
            throw SnapshotJson.invalid("arma da profissão");
        player.boons.restoreCheckpointState(SnapshotJson.required(state,"boons"));
        player.staminaCapacityBonus=SnapshotJson.nonNegative(state,"staminaCapacityBonus");
        player.staminaRecoveryBonus=SnapshotJson.nonNegative(state,"staminaRecoveryBonus");
        player.vidaMaxima=SnapshotJson.number(state,"vidaMaxima",player.profession.baseHealth,Float.MAX_VALUE);
        player.vida=SnapshotJson.number(state,"vida",-Float.MAX_VALUE,player.vidaMaxima);
        player.damageBonus=SnapshotJson.nonNegative(state,"damageBonus");player.attackRateBonus=SnapshotJson.nonNegative(state,"attackRateBonus");
        player.nivel=SnapshotJson.integer(state,"nivel",1,Integer.MAX_VALUE);
        player.xpParaProximoNivel=SnapshotJson.integer(state,"xpParaProximoNivel",10,Integer.MAX_VALUE);
        int expectedThreshold=10;
        for(int level=1;level<player.nivel&&expectedThreshold<Integer.MAX_VALUE;level++)expectedThreshold=(int)(expectedThreshold*1.5f);
        if(player.xpParaProximoNivel!=expectedThreshold)throw SnapshotJson.invalid("progressão de nível");
        player.curaPorAbate=SnapshotJson.integer(state,"curaPorAbate",0,Upgrade.VAMPIRISMO.maxRank);
        player.magnetRank=SnapshotJson.integer(state,"magnetRank",0,3);
        player.xpAtual=SnapshotJson.nonNegative(state,"xpAtual");
        player.modVelocidade=SnapshotJson.number(state,"modVelocidade",Float.MIN_VALUE,Float.MAX_VALUE);
        player.modDanoRecebido=SnapshotJson.number(state,"modDanoRecebido",.25f,1);
        player.modXpGanho=SnapshotJson.number(state,"modXpGanho",1,Float.MAX_VALUE);
        player.tempoEntreTiros=SnapshotJson.number(state,"tempoEntreTiros",player.weapon.minimumInterval,player.weapon.interval);
        float expectedInterval=Math.max(player.weapon.minimumInterval,player.weapon.interval/((1+player.attackRateBonus)*player.rarity.attackRateMultiplier));
        if(Math.abs(player.tempoEntreTiros-expectedInterval)>Math.max(.000001f,expectedInterval*.00001f))throw SnapshotJson.invalid("intervalo da arma");
        float maximumCooldown=player.profession==Profession.FAZENDEIRO?Math.max(WeaponType.RAKE.interval,WeaponType.SCYTHE.interval):player.profession.weapon.interval;
        player.cooldownTiro=SnapshotJson.number(state,"cooldownTiro",0,maximumCooldown);
        player.walkingSpeedLimit=SnapshotJson.required(state,"walkingSpeedLimit").isNull()?Float.POSITIVE_INFINITY:
            SnapshotJson.number(state,"walkingSpeedLimit",Float.MIN_VALUE,Float.MAX_VALUE);
        float maximumStamina=player.getEstaminaMaxima();
        if(!Float.isFinite(maximumStamina))throw SnapshotJson.invalid("capacidade de estamina");
        player.estaminaAtual=SnapshotJson.number(state,"estaminaAtual",0,maximumStamina);
        player.dashTimer=SnapshotJson.number(state,"dashTimer",-Float.MAX_VALUE,.15f);
        player.dashCooldownTimer=SnapshotJson.number(state,"dashCooldownTimer",0,1);
        player.timerFlash=SnapshotJson.number(state,"timerFlash",0,.15f);
        player.dashing=SnapshotJson.flag(state,"dashing");player.exausto=SnapshotJson.flag(state,"exausto");
        player.temRessurreicao=SnapshotJson.flag(state,"temRessurreicao");player.auraDePoseidonAtiva=SnapshotJson.flag(state,"auraDePoseidonAtiva");
        player.danoAura=SnapshotJson.number(state,"danoAura",Float.MIN_VALUE,Float.MAX_VALUE);
        player.raioAura=SnapshotJson.number(state,"raioAura",Float.MIN_VALUE,Float.MAX_VALUE);
        player.habilidadeEgideAtiva=SnapshotJson.flag(state,"habilidadeEgideAtiva");player.escudoAtivo=SnapshotJson.flag(state,"escudoAtivo");
        player.timerEscudo=SnapshotJson.number(state,"timerEscudo",0,10);
        player.protecaoRessurreicao=SnapshotJson.number(state,"protecaoRessurreicao",0,2);
        player.shockRank=SnapshotJson.integer(state,"shockRank",0,Upgrade.CHOQUE.maxRank);
        player.burnRank=SnapshotJson.integer(state,"burnRank",0,Upgrade.QUEIMADURA.maxRank);
        player.impactRank=SnapshotJson.integer(state,"impactRank",0,Upgrade.IMPACTO.maxRank);
        player.shockCooldown=SnapshotJson.number(state,"shockCooldown",0,.8f);
        player.burnCooldown=SnapshotJson.number(state,"burnCooldown",0,.8f);
        player.impactCooldown=SnapshotJson.number(state,"impactCooldown",0,.8f);
        player.spellCooldown=SnapshotJson.number(state,"spellCooldown",0,.8f);
        player.retaliationCooldown=SnapshotJson.number(state,"retaliationCooldown",0,.6f);
        player.guarding=SnapshotJson.flag(state,"guarding");
        player.guardX=SnapshotJson.number(state,"guardX",-1,1);player.guardY=SnapshotJson.number(state,"guardY",-1,1);
        player.damageRevision=SnapshotJson.longInteger(state,"damageRevision",0,Long.MAX_VALUE);
        if(player.dashing&&player.dashTimer<=0||player.guarding&&(player.weapon!=WeaponType.SHIELD||player.dashing||player.exausto||player.estaminaAtual<12)
            ||player.escudoAtivo&&!player.habilidadeEgideAtiva||Math.abs(player.guardX*player.guardX+player.guardY*player.guardY-1)>.0001f)
            throw SnapshotJson.invalid("estado de combate");
        if(player.shockRank==0&&player.shockCooldown>0||player.burnRank==0&&player.burnCooldown>0||player.impactRank==0&&player.impactCooldown>0)
            throw SnapshotJson.invalid("recarga sem poder");
        if(!Float.isFinite(player.getDano())||!Float.isFinite(player.dps())||!Float.isFinite(player.normalWalkingSpeed())
            ||!Float.isFinite(player.staminaRegeneration())||!Float.isFinite(player.xpGain(Integer.MAX_VALUE))||!Float.isFinite(player.powerRatio()))
            throw SnapshotJson.invalid("atributo derivado");
        return player;
    }
}
