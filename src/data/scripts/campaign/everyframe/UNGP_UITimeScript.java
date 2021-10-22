package data.scripts.campaign.everyframe;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.HashMap;

/**
 * 暂时只有2s的计时
 * Transient script
 */
public final class UNGP_UITimeScript implements EveryFrameScript {
    private transient static UNGP_UITimeScript instance;
    private boolean isPaused = false;
    private long systemTime;
    private HashMap<String, IntervalUtil> intervalMap = new HashMap<>();

    public UNGP_UITimeScript() {
        reset();
    }

    public static UNGP_UITimeScript getInstance() {
        return instance;
    }

    public static void setInstance(UNGP_UITimeScript instance) {
        UNGP_UITimeScript.instance = instance;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (isPaused) return;
        if (intervalMap.isEmpty()) return;
        long curSystemTime = System.nanoTime();
        long diff = curSystemTime - systemTime;
        float elapsed = diff / 1000000000f;
        for (IntervalUtil intervalUtil : intervalMap.values()) {
            if (elapsed < intervalUtil.getIntervalDuration()) {
                intervalUtil.advance(elapsed);
            }
        }
        systemTime = curSystemTime;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
        reset();
    }

    public static float getFactor(String intervalID) {
        IntervalUtil intervalUtil = getInstance().intervalMap.get(intervalID);
        if (intervalUtil != null) {
            return Math.min(intervalUtil.getElapsed() / intervalUtil.getIntervalDuration(), 1f);
        }
        return 0f;
    }

    public void reset() {
        systemTime = System.nanoTime();
    }

    public static void addInterval(String id, IntervalUtil interval) {
        final UNGP_UITimeScript script = getInstance();
        script.intervalMap.put(id, interval);
        script.reset();
    }

    public static void removeInterval(String id) {
        final UNGP_UITimeScript script = getInstance();
        script.intervalMap.remove(id);
    }
}
