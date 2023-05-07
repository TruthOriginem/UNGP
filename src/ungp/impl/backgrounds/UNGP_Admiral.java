package ungp.impl.backgrounds;

import com.fs.starfarer.api.impl.campaign.ids.Skills;
import ungp.api.backgrounds.UNGP_BaseBackgroundPlugin;

public class UNGP_Admiral extends UNGP_BaseBackgroundPlugin {
    @Override
    public float getInheritCreditsFactor() {
        return 0.5f;
    }

    @Override
    public float getInheritBlueprintsFactor() {
        return 0.5f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.TACTICAL_DRILLS, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 1));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.COORDINATED_MANEUVERS, 1));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.OFFICER_TRAINING, 1));
        addCycleBonus(12, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 2));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.OFFICER_MANAGEMENT, 1));
        addCycleBonus(20, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 3));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.SUPPORT_DOCTRINE, 1));
    }
}
