package ungp.impl.backgrounds;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import ungp.api.backgrounds.UNGP_BaseBackgroundPlugin;

public class UNGP_Capitalist extends UNGP_BaseBackgroundPlugin {
    @Override
    public float getInheritCreditsFactor() {
        return 1f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.CREW_TRAINING, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.RESOURCES, Commodities.ORGANS, 10f));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL_POINTS, 1));
        addCycleBonus(6, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.RESOURCES, Commodities.ORGANS, 20f));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.BULK_TRANSPORT, 1));
        addCycleBonus(10, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                              CargoAPI.CargoItemType.RESOURCES, Commodities.ORGANS, 30f));
        addCycleBonus(12, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 2));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.INDUSTRIAL_PLANNING, 1));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.HULL_RESTORATION, 1));
    }
}
