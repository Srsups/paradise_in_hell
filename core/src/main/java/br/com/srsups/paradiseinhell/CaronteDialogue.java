package br.com.srsups.paradiseinhell;

import java.util.List;

/** Original provisional dialogue; completion flags select later conversations. */
public final class CaronteDialogue {
    public record Line(String speaker, String text) {}
    public record Conversation(CampaignProfile.Event event, List<Line> lines) {}
    private CaronteDialogue() {}
    public static Conversation select(CampaignProfile profile) {
        if (!profile.has(CampaignProfile.Event.CARONTE_MET)) return new Conversation(CampaignProfile.Event.CARONTE_MET, List.of(
            new Line("Caronte", "Zeus roubou sua esposa e ainda lhe deu uma maldição de retorno? Ele nunca pensa nas filas que cria."),
            new Line("Protagonista", "Então posso morrer, voltar e tentar chegar ao Olimpo de novo?"),
            new Line("Caronte", "Pode. Mas as dádivas ficam na travessia. O que comprar no altar continua com você."),
            new Line("Caronte", "Tome cinco óbolos para começar. Vou cobrar de Zeus. Ele deve estar acostumado a pagar pelos próprios erros.")));
        if (profile.deaths() > 0 && !profile.has(CampaignProfile.Event.BRIBE_JOKE))
            return new Conversation(CampaignProfile.Event.BRIBE_JOKE, List.of(
                new Line("Protagonista", "Te dou uma moeda se me devolver vivo."),
                new Line("Caronte", "Você já vai ser devolvido vivo."),
                new Line("Protagonista", "Então me devolve perto do Olimpo."),
                new Line("Caronte", "Uma moeda compra uma travessia, não uma mudança de endereço.")));
        if (profile.victories() > 0 && !profile.has(CampaignProfile.Event.GUARDIAN_RECOGNIZED))
            return new Conversation(CampaignProfile.Event.GUARDIAN_RECOGNIZED, List.of(
                new Line("Caronte", "O Guardião perdeu? Vou precisar de outro funcionário para carimbar os mortos."),
                new Line("Protagonista", "Diga que ele está de licença por motivos de saúde."),
                new Line("Caronte", "Ótimo. Uma licença sem prazo. Muito apropriada para o submundo.")));
        return new Conversation(null, List.of(new Line("Caronte",
            "Já foram " + profile.attempts() + " retornos. Suas compras ficam; as dádivas mudam. Prepare-se e tente outra vez.")));
    }
}
