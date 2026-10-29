package br.com.srsups.paradiseinhell;

/** Base stats belong to the character chosen when creating a save. Provisional balance. */
public enum Profession {
    CACADOR("Caçador", "Você resolvia problemas mantendo uma boa distância.", WeaponType.BOW, 100, 60, 200, 1),
    FERREIRO("Ferreiro", "Você preferia resolver as coisas no braço.", WeaponType.SWORD, 130, 54, 180, 1),
    VIGILANTE("Vigilante", "Você já lidava com gente que não respeitava limites.", WeaponType.PISTOL, 85, 66, 220, 1),
    APOSTADOR("Apostador", "Você confiava na sorte e resolvia o resto com os punhos.", WeaponType.FISTS, 110, 62, 205, 4),
    FEITICEIRA("Feiticeira", "Você achava que toda crise precisava de um bom feitiço.", WeaponType.WAND, 90, 60, 200, 1),
    SACERDOTE("Sacerdote", "Você distribuía bênçãos. Agora mudou de departamento.", WeaponType.CURSE, 105, 56, 190, 1),
    NECROMANTE("Necromante", "Você fazia amizades até depois do expediente da vida.", WeaponType.SOUL, 85, 58, 195, 1),
    GUERREIRO("Guerreiro", "Você levava um escudo para discussões difíceis.", WeaponType.SHIELD, 150, 50, 175, 1),
    FAZENDEIRO("Fazendeiro", "Você plantava problemas para colher soluções. Profissão épica.", WeaponType.RAKE, 120, 57, 195, 1);

    public final String title, description;
    public final WeaponType weapon;
    public final float baseHealth, baseSpeed, baseDashSpeed, luckMultiplier;
    Profession(String title, String description, WeaponType weapon, float health, float speed, float dashSpeed, float luck) {
        this.title = title; this.description = description; this.weapon = weapon;
        baseHealth = health; baseSpeed = speed; baseDashSpeed = dashSpeed; luckMultiplier = luck;
    }
}
