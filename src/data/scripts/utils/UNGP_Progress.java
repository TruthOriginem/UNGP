package data.scripts.utils;

public class UNGP_Progress {
    private float elapsed = 0f;
    private float targetTime = 1f;

    public UNGP_Progress(float targetTime) {
        this.targetTime = targetTime;
    }

    public void advance(float amount) {
        this.elapsed += amount;
    }

    public float getProgress() {
        float progress = elapsed / targetTime;
        return Math.min(1, progress);
    }

    public void reset() {
        elapsed = 0f;
    }
}
