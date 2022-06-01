package data.scripts.ungpbackgrounds.impl;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import data.scripts.ungpbackgrounds.UNGP_BaseBackgroundPlugin;

public class UNGP_Seeker extends UNGP_BaseBackgroundPlugin {
    @Override
    public float getInheritCreditsFactor() {
        return 0.25f;
    }

    @Override
    public float getInheritBlueprintsFactor() {
        return 0.75f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.NAVIGATION, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(Items.MODSPEC, HullMods.EFFICIENCY_OVERHAUL), 1));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.SALVAGING, 1));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SHIP, "shepherd_Starting", 2));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.MAKESHIFT_EQUIPMENT, 1));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.CARGO_STACK,
                                             CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(Items.MODSPEC, HullMods.AUGMENTEDENGINES), 1));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.HULL_RESTORATION, 1));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL, Skills.DERELICT_CONTINGENT, 1));
    }
}
