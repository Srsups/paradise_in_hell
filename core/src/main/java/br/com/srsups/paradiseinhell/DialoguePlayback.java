package br.com.srsups.paradiseinhell;

/** One input reveals or advances exactly one line. No timers auto-advance a conversation. */
public final class DialoguePlayback {
    public enum Action { REVEALED, ADVANCED, FINISHED }
    private final CaronteDialogue.Conversation conversation;
    private int line;
    private float revealed;
    private boolean instant;
    public DialoguePlayback(CaronteDialogue.Conversation conversation,boolean instant) {
        this.conversation=java.util.Objects.requireNonNull(conversation);this.instant=instant;
        if(conversation.lines().isEmpty())throw new IllegalArgumentException("Empty conversation");
    }
    public CaronteDialogue.Line current() { return conversation.lines().get(line); }
    public int line() { return line; }
    public boolean instant() { return instant; }
    public void instant(boolean value) { instant=value; if(value)reveal(); }
    private int length() { return current().text().codePointCount(0,current().text().length()); }
    public boolean complete() { return instant || revealed >= length(); }
    public void advance(float delta) {
        if (!Float.isFinite(delta) || delta < 0)throw new IllegalArgumentException("Invalid text time");
        revealed=Math.min(length(),revealed+delta*42);
    }
    private void reveal() { revealed=length(); }
    public String visibleText() {
        String text=current().text(); int count=complete()?length():Math.min(length(),(int)revealed);
        return text.substring(0,text.offsetByCodePoints(0,count));
    }
    public Action accept() {
        if(!complete()){reveal();return Action.REVEALED;}
        if(line+1==conversation.lines().size())return Action.FINISHED;
        line++;revealed=0;return Action.ADVANCED;
    }
}

