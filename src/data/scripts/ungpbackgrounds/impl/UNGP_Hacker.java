package data.scripts.ungpbackgrounds.impl;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import data.scripts.ungpbackgrounds.UNGP_BaseBackgroundPlugin;

public class UNGP_Hacker extends UNGP_BaseBackgroundPlugin {

    @Override
    public float getInheritCreditsFactor() {
        return 0.5f;
    }

    @Override
    public float getInheritBlueprintsFactor() {
        return 0.25f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.NAVIGATION, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(Items.MODSPEC, HullMods.INTEGRATED_TARGETING_UNIT), 1f));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.SHIP,
                                             "omen_PD", 1f));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.ELECTRONIC_WARFARE, 1));
        addCycleBonus(5, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(Items.MODSPEC, HullMods.ECCM), 1f));
        addCycleBonus(6, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(Items.MODSPEC, HullMods.HARDENED_SHIELDS), 1f));
        addCycleBonus(7, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.RESOURCES, Commodities.ALPHA_CORE, 1f));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.CYBERNETIC_AUGMENTATION, 1));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.PHASE_CORPS, 1));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.NEURAL_LINK, 1));
    }
}
