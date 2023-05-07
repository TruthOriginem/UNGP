package ungp.impl.backgrounds;

import com.fs.starfarer.api.impl.campaign.ids.Skills;
import ungp.api.backgrounds.UNGP_BaseBackgroundPlugin;

public class UNGP_Genius extends UNGP_BaseBackgroundPlugin {
    @Override
    public float getInheritBlueprintsFactor() {
        return 1f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.HELMSMANSHIP, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.HELMSMANSHIP, 2));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.GUNNERY_IMPLANTS, 1));
        addCycleBonus(5, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.GUNNERY_IMPLANTS, 2));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.ORDNANCE_EXPERTISE, 1));
        addCycleBonus(10, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.ORDNANCE_EXPERTISE, 2));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.TARGET_ANALYSIS, 1));
        addCycleBonus(17, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.TARGET_ANALYSIS, 2));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.SYSTEMS_EXPERTISE, 1));
        addCycleBonus(26, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.SYSTEMS_EXPERTISE, 2));
    }
}
