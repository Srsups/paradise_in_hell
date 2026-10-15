package br.com.srsups.paradiseinhell;

/** A readable source survives removal of the attacking projectile/entity. */
public record CombatHit(float damage, float x, float y, Kind kind, String label) {
    public enum Kind { CONTACT, PROJECTILE, GROUND }
    public CombatHit(float damage,float x,float y,Kind kind) {
        this(damage,x,y,kind,switch(kind) {case CONTACT->"Contato";case PROJECTILE->"Projétil";case GROUND->"Ataque em área";});
    }
}
