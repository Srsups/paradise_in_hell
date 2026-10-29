package br.com.srsups.paradiseinhell;

/** Personal, monotonic quest progress; the completed reward is account-wide. */
public enum FarmerQuest {
    NOT_STARTED("Encontre a Camponesa das Cinzas após liberar a passagem do Estige."),
    ACCEPTED("Derrote um Carrasco do Submundo para recuperar a Semente das Cinzas."),
    SEED_FOUND("Semente recuperada. Derrote o Guardião do Tártaro para concluir a promessa."),
    COMPLETED("Promessa cumprida: Fazendeiro épico disponível para novos personagens.");

    public final String description;
    FarmerQuest(String description) { this.description = description; }
}
