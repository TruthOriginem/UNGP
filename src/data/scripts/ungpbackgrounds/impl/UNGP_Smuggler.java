package data.scripts.ungpbackgrounds.impl;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import data.scripts.ungpbackgrounds.UNGP_BaseBackgroundPlugin;

public class UNGP_Smuggler extends UNGP_BaseBackgroundPlugin {
    @Override
    public float getInheritCreditsFactor() {
        return 0.75f;
    }

    @Override
    public float getInheritBlueprintsFactor() {
        return 0f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL_POINTS, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.RESOURCES, Commodities.DRUGS, 20f));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.SHIP,
                                             "cerberus_Standard", 1f));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.BULK_TRANSPORT, 1));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.ORDNANCE_EXPERTISE, 1));
        addCycleBonus(12, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 3));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.POLARIZED_ARMOR, 1));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.DERELICT_CONTINGENT, 1));
    }
}
