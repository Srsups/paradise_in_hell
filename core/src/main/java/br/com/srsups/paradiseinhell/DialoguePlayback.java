package br.com.srsups.paradiseinhell;

/** One input reveals or advances exactly one line. No timers auto-advance a conversation. */
public final class DialoguePlayback {
    public enum Action { REVEALED, ADVANCED, FINISHED }
    private final java.util.List<DialogueLine> lines;
    private int line;
    private float revealed;
    private boolean instant;
    public DialoguePlayback(CaronteDialogue.Conversation conversation,boolean instant) {
        this(java.util.Objects.requireNonNull(conversation).lines().stream()
            .map(line->new DialogueLine(line.speaker(),line.text(),line.portrait())).toList(),instant);
    }
    public DialoguePlayback(java.util.List<DialogueLine> lines,boolean instant) {
        this.lines=java.util.List.copyOf(lines);this.instant=instant;
        if(this.lines.isEmpty())throw new IllegalArgumentException("Empty conversation");
    }
    public DialogueLine current() { return lines.get(line); }
    public int line() { return line; }
    public int lineCount() { return lines.size(); }
    public boolean instant() { return instant; }
    public void instant(boolean value) { if(instant||value)reveal();instant=value; }
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
        if(line+1==lines.size())return Action.FINISHED;
        line++;revealed=0;return Action.ADVANCED;
    }
}

