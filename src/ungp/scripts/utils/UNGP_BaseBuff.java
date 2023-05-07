package ungp.scripts.utils;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class UNGP_BaseBuff implements BuffManagerAPI.Buff {
    public static final float BASE_DURATION = 0.1f;
    protected String id;
    protected float dur;

    public UNGP_BaseBuff(String id, float dur) {
        this.id = id;
        this.dur = dur;
    }

    public UNGP_BaseBuff(String id) {
        this.id = id;
        this.dur = BASE_DURATION;
    }

    @Override
    public void apply(FleetMemberAPI member) {

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isExpired() {
        return dur <= 0;
    }

    @Override
    public void advance(float days) {
        dur -= days;
    }

    public float getDur() {
        return dur;
    }

    public void setDur(float dur) {
        this.dur = dur;
    }

    public void refresh() {
        this.dur = BASE_DURATION;
    }

    public UNGP_BaseBuff init(String id, float dur) {
        return new UNGP_BaseBuff(id, dur);
    }
}
