package data.scripts.ungpbackgrounds.impl;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import data.scripts.ungpbackgrounds.UNGP_BaseBackgroundPlugin;

public class UNGP_PoorScientist extends UNGP_BaseBackgroundPlugin {
    @Override
    public float getInheritBlueprintsFactor() {
        return 0.5f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.SENSORS, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             Commodities.BETA_CORE, 3f, CargoAPI.CargoItemType.RESOURCES));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.ENERGY_WEAPON_MASTERY, 1));
        addCycleBonus(5, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.ENERGY_WEAPON_MASTERY, 2));
        addCycleBonus(7, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             Commodities.ALPHA_CORE, 1f, CargoAPI.CargoItemType.RESOURCES));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.FLUX_REGULATION, 1));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.NEURAL_LINK, 1));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.AUTOMATED_SHIPS, 1));
    }
}
