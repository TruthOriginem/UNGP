package data.scripts.ungpbackgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.inherit.UNGP_InheritData;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

import static data.scripts.campaign.UNGP_Settings.d_i18n;

public abstract class UNGP_BaseBackgroundPlugin implements UNGP_BackgroundPluginAPI {
    protected enum BackgroundBonusType {
        SKILL, // item = skill id, param1 = skill level
        CARGO_STACK, // item = id / special data, param1 = size(float), param2 = CargoItemType
        SHIP // item = variant id, param1 = quantity
    }

    protected class BackgroundBonus {
        protected BackgroundBonusType type;
        protected Object item;
        protected Object param1;
        protected Object param2;

        public BackgroundBonus(BackgroundBonusType type, Object item, Object param1) {
            this.type = type;
            this.item = item;
            this.param1 = param1;
        }

        public BackgroundBonus(BackgroundBonusType type, Object item, Object param1, Object param2) {
            this.type = type;
            this.item = item;
            this.param1 = param1;
            this.param2 = param2;
        }

        public CargoStackAPI createCargoStack() {
            CargoStackAPI stack = Global.getFactory().createCargoStack(param2 == null ? CargoAPI.CargoItemType.RESOURCES : (CargoAPI.CargoItemType) param2, item, null);
            stack.setSize((Float) param1);
            return stack;
        }
    }

    protected Map<Integer, List<BackgroundBonus>> cycleBonusMap = new LinkedHashMap<>();

    public UNGP_BaseBackgroundPlugin() {
        initCycleBonus();

    }

    @Override
    public void initCycleBonus() {

    }

    /**
     * @param cycle
     * @param bonus Bonus Type:
     *              SKILL, // item = skill id, param1 = skill level
     *              CARGO_STACK, // item = id / special data, param1 = size(float), param2 = CargoItemType
     *              SHIP // item = variant id, param1 = quantity
     */
    protected void addCycleBonus(int cycle, BackgroundBonus bonus) {
        List<BackgroundBonus> bonusList = cycleBonusMap.get(cycle);
        if (bonusList == null) {
            bonusList = new LinkedList<>();
            cycleBonusMap.put(cycle, bonusList);
        }
        bonusList.add(bonus);
    }

    @Override
    public float getInheritCreditsFactor() {
        return 0;
    }

    @Override
    public float getInheritBlueprintsFactor() {
        return 0;
    }

    @Override
    public void addPostDescTooltip(TooltipMakerAPI tooltip, @Nullable UNGP_InheritData pickedInheritData) {

    }

    @Override
    public void addInheritCreditsAndBPsTooltip(TooltipMakerAPI tooltip, @Nullable UNGP_InheritData pickedInheritData) {
        tooltip.addPara(d_i18n.get("inheritCredits") + ": %s", 0f, Misc.getHighlightColor(), (int) (getInheritCreditsFactor() * 100f) + "%");
        tooltip.addPara(d_i18n.get("inheritBPs") + ": %s", 0f, Misc.getPositiveHighlightColor(), (int) (getInheritBlueprintsFactor() * 100f) + "%");
    }

    @Override
    public Color getOverrideNameColor() {
        return null;
    }

    @Override
    public void addBonusTooltip(TooltipMakerAPI tooltip, @Nullable UNGP_InheritData pickedInheritData, boolean showLimit) {
        if (!cycleBonusMap.isEmpty()) {
            Color hl = Misc.getHighlightColor();
            Color gray = Misc.getGrayColor();
            float pad = 0f;

            int curCycle = 1;
            if (pickedInheritData != null) {
                curCycle = pickedInheritData.cycle;
            }
            for (Map.Entry<Integer, List<BackgroundBonus>> entry : cycleBonusMap.entrySet()) {
                int cycle = entry.getKey();
                boolean isLimited = false;
                if (curCycle < cycle) {
                    isLimited = true;
                    if (!showLimit) {
                        continue;
                    }
                }
                for (BackgroundBonus bonus : entry.getValue()) {
                    switch (bonus.type) {
                        case SKILL:
                            String skillId = (String) bonus.item;
                            SkillSpecAPI skillSpec = Global.getSettings().getSkillSpec(skillId);
                            int level;
                            if (bonus.param1 instanceof Integer) {
                                level = (int) bonus.param1;
                            } else {
                                level = 1;
                            }
                            if (!isLimited) {
                                if (level < 2) {
                                    tooltip.addPara(d_i18n.get("bg_bonus_skill"), pad, hl, skillSpec.getName());
                                } else {
                                    tooltip.addPara(d_i18n.get("bg_bonus_skill_elite"), pad, Misc.getStoryOptionColor(), skillSpec.getName());
                                }
                            } else {
                                if (level < 2) {
                                    tooltip.addPara(d_i18n.format("bg_bonus_skill", skillSpec.getName())
                                                            + d_i18n.format("bg_bonus_cycle_unlock_tip", "" + cycle), gray, pad);
                                } else {
                                    tooltip.addPara(d_i18n.format("bg_bonus_skill_elite", skillSpec.getName())
                                                            + d_i18n.format("bg_bonus_cycle_unlock_tip", "" + cycle), gray, pad);
                                }
                            }
                            break;
                        case CARGO_STACK:
                            CargoStackAPI stack = bonus.createCargoStack();
                            if (!isLimited) {
                                tooltip.addPara(d_i18n.get("bg_bonus_item"), pad, hl, stack.getDisplayName(), (int) stack.getSize() + "");
                            } else {
                                tooltip.addPara(d_i18n.format("bg_bonus_item", stack.getDisplayName(), (int) stack.getSize() + "")
                                                        + d_i18n.format("bg_bonus_cycle_unlock_tip", "" + cycle), gray, pad);
                            }
                            break;
                        case SHIP:
                            String variantId = (String) bonus.item;
                            int size;
                            if (bonus.param1 instanceof Integer) {
                                size = (int) bonus.param1;
                            } else {
                                size = 1;
                            }
                            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
                            ShipHullSpecAPI hullSpec = variant.getHullSpec();
                            if (!isLimited) {
                                tooltip.addPara(d_i18n.get("bg_bonus_ship"), pad, hl, hullSpec.getHullNameWithDashClass(), size + "");
                            } else {
                                tooltip.addPara(d_i18n.format("bg_bonus_ship", hullSpec.getHullNameWithDashClass(), size + "")
                                                        + d_i18n.format("bg_bonus_cycle_unlock_tip", "" + cycle), gray, pad);
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void afterConfirm(UNGP_InheritData pickedInheritData) {
        if (!cycleBonusMap.isEmpty()) {
            Map<String, Integer> skillGonnaLearnMap = new HashMap<>();
            int curCycle = 1;
            if (pickedInheritData != null) {
                curCycle = pickedInheritData.cycle;
            }
            MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            Iterator<Map.Entry<Integer, List<BackgroundBonus>>> iterator = cycleBonusMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<BackgroundBonus>> entry = iterator.next();
                int cycle = entry.getKey();
                if (curCycle < cycle) {
                    continue;
                }
                for (BackgroundBonus bonus : entry.getValue()) {
                    switch (bonus.type) {
                        case SKILL:
                            String skillId = (String) bonus.item;
                            int level;
                            if (bonus.param1 instanceof Integer) {
                                level = (int) bonus.param1;
                            } else {
                                level = 1;
                            }
                            Integer targetLevel = skillGonnaLearnMap.get(skillId);
                            if (targetLevel == null) {
                                targetLevel = level;
                            }
                            if (level > targetLevel) {
                                skillGonnaLearnMap.put(skillId, targetLevel);
                            }
                            skillGonnaLearnMap.put(skillId, level);
                            break;
                        case CARGO_STACK:
                            CargoStackAPI stack = bonus.createCargoStack();
                            playerFleet.getCargo().addFromStack(stack);
                            break;
                        case SHIP:
                            String variantId = (String) bonus.item;
                            int size;
                            if (bonus.param1 instanceof Integer) {
                                size = (int) bonus.param1;
                            } else {
                                size = 1;
                            }
                            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
                            for (int i = 0; i < size; i++) {
                                FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
                                member.getRepairTracker().setCR(1f);
                                playerFleet.getFleetData().addFleetMember(member);
                            }
                            break;
                    }
                }
            }
            if (!skillGonnaLearnMap.isEmpty()) {
                for (Map.Entry<String, Integer> entry : skillGonnaLearnMap.entrySet()) {
                    String skillId = entry.getKey();
                    int level = entry.getValue();
                    int curLevel = (int) playerStats.getSkillLevel(skillId);
                    if (level > curLevel) {
                        playerStats.setSkillLevel(skillId, level);
                    }
                    if (level >= curLevel) {
                        playerStats.addPoints(1);
                        if (curLevel >= 2) {
                            playerStats.addStoryPoints(1);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addAfterConfirmTooltip(TooltipMakerAPI tooltip, UNGP_InheritData pickedInheritData) {
        addBonusTooltip(tooltip, pickedInheritData, false);
    }

    @Override
    public boolean isUnlocked(UNGP_InheritData pickedInheritData) {
        return false;
    }
}
