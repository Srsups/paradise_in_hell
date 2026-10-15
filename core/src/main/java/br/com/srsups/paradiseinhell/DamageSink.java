package br.com.srsups.paradiseinhell;

@FunctionalInterface
public interface DamageSink {
    void deal(CombatTarget target, float amount, DamageSource source);
    DamageSink DIRECT = (target, amount, source) -> target.hurt(amount);
}

