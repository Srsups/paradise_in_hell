package br.com.srsups.paradiseinhell;

/** A single-use bonus shared by every target of one swing or every hit of one projectile. */
public final class WeaponBonus {
    private float remaining;
    public WeaponBonus(float damage){
        if(!Float.isFinite(damage)||damage<0)throw new IllegalArgumentException("Invalid bonus damage");
        remaining=damage;
    }
    public float take(){float result=remaining;remaining=0;return result;}
}
