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
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.inherit.UNGP_InheritData;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

import static data.scripts.campaign.UNGP_Settings.d_i18n;

public abstract class UNGP_BaseBackgroundPlugin implements UNGP_BackgroundPluginAPI {
    protected interface BackgroundBonusScript {
        void addBonusTooltip(TooltipMakerAPI tooltip, UNGP_InheritData pickedInheritData, boolean isLimited, boolean showLimit);

        void afterConfirm(UNGP_InheritData pickedInheritData);
    }

    protected enum BackgroundBonusType {
        SKILL, // skill id, skill level
        CARGO_STACK, // CargoItemType, id / special data, size(float)
        SHIP, //variant id, quantity
        SKILL_POINTS, // size(int)
        STORY_POINTS, // size(int)
        SCRIPT // BackgroundBonusScript
    }

    protected static class BackgroundBonus {
        protected BackgroundBonusType type;
        protected Object[] params;

        public BackgroundBonus(BackgroundBonusType type, Object... params) {
            this.type = type;
            this.params = params;
        }

        public CargoStackAPI createCargoStack() {
            CargoStackAPI stack = Global.getFactory().createCargoStack((CargoAPI.CargoItemType) params[0], params[1], null);
            stack.setSize(Float.parseFloat(params[2].toString()));
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
     * @param bonus
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
        tooltip.addPara(d_i18n.get("inheritCredits") + ": %s", 0f, Misc.getPositiveHighlightColor(), (int) (getInheritCreditsFactor() * 100f) + "%");
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
            Color base = Misc.getTextColor();
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
                boolean isActuallyLocked = isLimited || showLimit;
                Color baseColor = isLimited ? gray : base;
                Color highlightColor = isLimited ? gray : hl;
                for (BackgroundBonus bonus : entry.getValue()) {
                    switch (bonus.type) {
                        case SKILL:
                            String skillId = (String) bonus.params[0];
                            SkillSpecAPI skillSpec = Global.getSettings().getSkillSpec(skillId);
                            int level;
                            if (bonus.params[1] instanceof Integer) {
                                level = (int) bonus.params[1];
                            } else {
                                level = 1;
                            }
                            if (level < 2) {
                                tooltip.addPara(d_i18n.get("bg_bonus_skill"), pad, baseColor, highlightColor, skillSpec.getName());
                            } else {
                                tooltip.addPara(d_i18n.get("bg_bonus_skill_elite"), pad, baseColor, isLimited ? gray : Misc.getStoryOptionColor(), skillSpec.getName());
                            }
                            if (isActuallyLocked) {
                                addUnlockCycleStringToTooltipAtRight(tooltip, cycle, gray, pad);
                            }
                            break;
                        case CARGO_STACK:
                            CargoStackAPI stack = bonus.createCargoStack();
                            tooltip.addPara(d_i18n.get("bg_bonus_item"), pad, baseColor, highlightColor, stack.getDisplayName(), (int) stack.getSize() + "");
                            if (isActuallyLocked) {
                                addUnlockCycleStringToTooltipAtRight(tooltip, cycle, gray, pad);
                            }
                            break;
                        case SHIP:
                            String variantId = (String) bonus.params[0];
                            int size;
                            if (bonus.params[1] instanceof Integer) {
                                size = (int) bonus.params[1];
                            } else {
                                size = 1;
                            }
                            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
                            ShipHullSpecAPI hullSpec = variant.getHullSpec();
                            tooltip.addPara(d_i18n.get("bg_bonus_ship"), pad, baseColor, highlightColor, hullSpec.getHullNameWithDashClass(), size + "");
                            if (isActuallyLocked) {
                                addUnlockCycleStringToTooltipAtRight(tooltip, cycle, gray, pad);
                            }
                            break;
                        case SKILL_POINTS:
                            tooltip.addPara(d_i18n.get("bg_bonus_skill_point"), pad, baseColor, highlightColor, (int) bonus.params[0] + "");
                            if (isActuallyLocked) {
                                addUnlockCycleStringToTooltipAtRight(tooltip, cycle, gray, pad);
                            }
                            break;
                        case STORY_POINTS:
                            tooltip.addPara(d_i18n.get("bg_bonus_story_point"), pad, baseColor, highlightColor, (int) bonus.params[0] + "");
                            if (isActuallyLocked) {
                                addUnlockCycleStringToTooltipAtRight(tooltip, cycle, gray, pad);
                            }
                            break;
                        case SCRIPT:
                            BackgroundBonusScript script = (BackgroundBonusScript) bonus.params[0];
                            script.addBonusTooltip(tooltip, pickedInheritData, isLimited, showLimit);
                            break;
                    }
                }
                tooltip.addSpacer(0f);
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
            for (Map.Entry<Integer, List<BackgroundBonus>> entry : cycleBonusMap.entrySet()) {
                int cycle = entry.getKey();
                if (curCycle < cycle) {
                    continue;
                }
                for (BackgroundBonus bonus : entry.getValue()) {
                    switch (bonus.type) {
                        case SKILL:
                            String skillId = (String) bonus.params[0];
                            int level;
                            if (bonus.params[1] instanceof Integer) {
                                level = (int) bonus.params[1];
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
                            String variantId = (String) bonus.params[0];
                            int size;
                            if (bonus.params[1] instanceof Integer) {
                                size = (int) bonus.params[1];
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

                        case SKILL_POINTS:
                            playerStats.addPoints((Integer) bonus.params[0]);
                            break;
                        case STORY_POINTS:
                            playerStats.addStoryPoints((Integer) bonus.params[0]);
                            break;
                        case SCRIPT:
                            BackgroundBonusScript script = (BackgroundBonusScript) bonus.params[0];
                            script.afterConfirm(pickedInheritData);
                            break;
                    }
                }
            }
            if (!skillGonnaLearnMap.isEmpty()) {
                for (Map.Entry<String, Integer> entry : skillGonnaLearnMap.entrySet()) {
                    String skillId = entry.getKey();
                    int level = entry.getValue();
                    int curLevel = (int) playerStats.getSkillLevel(skillId);
                    if (curLevel > 0 && level >= curLevel) {
                        playerStats.addPoints(1);
                        if (curLevel >= 2) {
                            playerStats.addStoryPoints(1);
                        }
                    }
                    if (level > curLevel) {
                        playerStats.setSkillLevel(skillId, level);
                    }
                }
            }
        }
    }


    protected String getUnlockCycleString(int cycle) {
        return d_i18n.format("bg_bonus_cycle_unlock_tip", "" + cycle);
    }

    protected void addUnlockCycleStringToTooltipAtRight(TooltipMakerAPI tooltip, int cycle, Color gray, float pad) {
        // last para should be the unlocked entry
        tooltip.addSpacer(-tooltip.getPrev().getPosition().getHeight());
        tooltip.addPara(getUnlockCycleString(cycle), gray, pad).setAlignment(Alignment.RMID);
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
