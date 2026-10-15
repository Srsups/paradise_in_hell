package br.com.srsups.paradiseinhell;

/** Every new choice requires time to read and a released attack button before accepting a fresh input. */
public final class ChoiceInputGuard {
    public static final float READ_SECONDS = 1.5f;
    private float remaining = READ_SECONDS;
    private boolean released;
    public void reset() { remaining = READ_SECONDS; released = false; }
    public void advance(float delta, boolean attackHeld) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        remaining = Math.max(0, remaining - delta);
        if (remaining == 0 && !attackHeld) released = true;
    }
    public boolean ready() { return remaining == 0 && released; }
    public float remaining() { return remaining; }
    public String notice() {
        return remaining > 0 ? "Leia as dádivas: escolha disponível em " + CombatNumbers.format(remaining) + "s."
            : released ? "Clique em uma opção ou use 1, 2, 3." : "Solte o botão de ataque para escolher.";
    }
}

