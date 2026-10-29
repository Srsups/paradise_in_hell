package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;

/** Stable portrait identifiers; dialogue models never load textures. */
public enum DialoguePortrait {
    CHARON_NEUTRAL("portrait-charon-neutral"),
    CHARON_AMUSED("portrait-charon-amused"),
    CHARON_CONCERNED("portrait-charon-concerned"),
    CHARON_PLEASED("portrait-charon-pleased"),
    BLACKSMITH_NEUTRAL("portrait-blacksmith-neutral"),
    BLACKSMITH_AMUSED("portrait-blacksmith-amused"),
    BLACKSMITH_CONCERNED("portrait-blacksmith-concerned"),
    BLACKSMITH_PLEASED("portrait-blacksmith-pleased"),
    CAMPONESA_NEUTRAL("portrait-camponesa-neutral"),
    CAMPONESA_AMUSED("portrait-camponesa-amused"),
    CAMPONESA_CONCERNED("portrait-camponesa-concerned"),
    CAMPONESA_PLEASED("portrait-camponesa-pleased"),
    PROTAGONIST("portrait-protagonist");

    private final String regionName;
    DialoguePortrait(String regionName) { this.regionName=regionName; }
    public String regionName() { return regionName; }
    public static DialoguePortrait defaultFor(String speaker) {
        if("Caronte".equals(speaker))return CHARON_NEUTRAL;
        if("Ferreiro Errante".equals(speaker)||"Ferreiro".equals(speaker))return BLACKSMITH_NEUTRAL;
        if("Camponesa das Cinzas".equals(speaker))return CAMPONESA_NEUTRAL;
        return PROTAGONIST;
    }
    static DialoguePortrait parse(JsonValue value,DialoguePortrait fallback) {
        if(value==null)return fallback;
        if(!value.isString())throw new IllegalArgumentException("Invalid dialogue portrait");
        return valueOf(value.asString());
    }
}
