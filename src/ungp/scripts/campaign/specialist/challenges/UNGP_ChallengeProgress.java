package ungp.scripts.campaign.specialist.challenges;

public final class UNGP_ChallengeProgress {
    public enum ProgressState {
        RUNNING,
        CANCELLED,
        COMPLETED
    }

    private String challengeId;
    private int elapsedMonth = 0;
    private float defeatedDp = 0;
    private ProgressState state = ProgressState.RUNNING;

    public UNGP_ChallengeProgress(String challengeId) {
        this.challengeId = challengeId;
    }

    public void setState(ProgressState state) {
        this.state = state;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public int getElapsedMonth() {
        return elapsedMonth;
    }

    public float getDefeatedDp() {
        return defeatedDp;
    }

    public void advanceMonth() {
        elapsedMonth++;
    }

    public void addDefeatedDp(float dp) {
        defeatedDp += dp;
    }

    public ProgressState getState() {
        return state;
    }
}
