package br.com.srsups.paradiseinhell;

/** Cosmetic clock only. Its owner resolves damage, movement, rewards and resurrection. */
public final class ActorReaction {
    public enum State { NONE, HURT, DEATH }
    public static final float HURT_SECONDS = .18f, DEATH_SECONDS = .65f;
    private final float hurtSeconds, deathSeconds;
    private State state = State.NONE;
    private float elapsed;

    public ActorReaction() { this(HURT_SECONDS, DEATH_SECONDS); }
    public ActorReaction(float hurtSeconds, float deathSeconds) {
        if (!Float.isFinite(hurtSeconds) || hurtSeconds <= 0
            || !Float.isFinite(deathSeconds) || deathSeconds <= 0)
            throw new IllegalArgumentException("Invalid reaction duration");
        this.hurtSeconds = hurtSeconds;
        this.deathSeconds = deathSeconds;
    }
    /** Repeated hits do not restart an ongoing recoil or interrupt a death. */
    public boolean hurt() {
        if (state != State.NONE) return false;
        state = State.HURT;
        elapsed = 0;
        return true;
    }
    /** Starts once per life, taking priority over any nonlethal reaction. */
    public boolean die() {
        if (state == State.DEATH) return false;
        state = State.DEATH;
        elapsed = 0;
        return true;
    }
    public void advance(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid reaction time");
        if (state == State.NONE) return;
        float duration = state == State.HURT ? hurtSeconds : deathSeconds;
        elapsed = Math.min(duration, elapsed + delta);
        if (state == State.HURT && elapsed >= duration) {
            state = State.NONE;
            elapsed = 0;
        }
    }
    public State state() { return state; }
    /** A completed death holds its final pose until the owner removes or clears it. */
    public boolean active() { return state != State.NONE; }
    public boolean deathFinished() { return state == State.DEATH && elapsed >= deathSeconds; }
    public float progress() {
        return state == State.NONE ? 0 : elapsed / (state == State.HURT ? hurtSeconds : deathSeconds);
    }
    public int frame(int count) {
        if (count < 1) throw new IllegalArgumentException("Invalid reaction frame count");
        return Math.min(count - 1, (int)(progress() * count));
    }
    /** Explicit new-life reset; normal time advancement never revives a dead actor. */
    public void clear() { state = State.NONE; elapsed = 0; }
}
