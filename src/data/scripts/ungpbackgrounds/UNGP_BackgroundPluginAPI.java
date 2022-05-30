package data.scripts.ungpbackgrounds;


import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.campaign.inherit.UNGP_InheritData;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Used for Total background, better extends {@link UNGP_BaseBackgroundPlugin}
 */
public interface UNGP_BackgroundPluginAPI {

    void initCycleBonus();

    /**
     * @return 0~1
     */
    float getInheritCreditsFactor();

    /**
     * @return 0~1
     */
    float getInheritBlueprintsFactor();

    /**
     * Would be added after detailed description
     *
     * @param tooltip
     * @param pickedInheritData
     */
    void addPostDescTooltip(TooltipMakerAPI tooltip, @Nullable UNGP_InheritData pickedInheritData);

    /**
     * Should be edited by base class, modders won't need to change this
     *
     * @param tooltip
     * @param pickedInheritData
     */
    void addInheritCreditsAndBPsTooltip(TooltipMakerAPI tooltip, @Nullable UNGP_InheritData pickedInheritData);

    /**
     * Added after {@link #addPostDescTooltip(TooltipMakerAPI, UNGP_InheritData)}
     *
     * @param tooltip
     * @param pickedInheritData
     * @param showLimit         some bonus might be limited, show them while needed
     */
    void addBonusTooltip(TooltipMakerAPI tooltip, @Nullable UNGP_InheritData pickedInheritData, boolean showLimit);

    void addAfterConfirmTooltip(TooltipMakerAPI tooltip, UNGP_InheritData pickedInheritData);

    /**
     * Do other things after inherit confirmation.
     *
     * @param pickedInheritData
     */
    void afterConfirm(UNGP_InheritData pickedInheritData);

    /**
     * Background could be locked.
     *
     * @param pickedInheritData
     * @return
     */
    boolean isUnlocked(UNGP_InheritData pickedInheritData);

    Color getOverrideNameColor();
}
