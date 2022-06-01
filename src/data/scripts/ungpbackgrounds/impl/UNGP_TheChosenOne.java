package data.scripts.ungpbackgrounds.impl;

import data.scripts.ungpbackgrounds.UNGP_BaseBackgroundPlugin;

import java.awt.*;

public class UNGP_TheChosenOne extends UNGP_BaseBackgroundPlugin {
    @Override
    public float getInheritCreditsFactor() {
        return 1f;
    }

    @Override
    public float getInheritBlueprintsFactor() {
        return 1f;
    }

    @Override
    public void initCycleBonus() {
        addCycleBonus(2, new BackgroundBonus(BackgroundBonusType.SKILL_POINTS, 1));
        addCycleBonus(3, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 1));
        addCycleBonus(4, new BackgroundBonus(BackgroundBonusType.SKILL_POINTS, 1));
        addCycleBonus(5, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 1));
        addCycleBonus(9, new BackgroundBonus(BackgroundBonusType.SKILL_POINTS, 1));
        addCycleBonus(10, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 2));
        addCycleBonus(16, new BackgroundBonus(BackgroundBonusType.SKILL_POINTS, 1));
        addCycleBonus(17, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 2));
        addCycleBonus(25, new BackgroundBonus(BackgroundBonusType.SKILL_POINTS, 1));
        addCycleBonus(26, new BackgroundBonus(BackgroundBonusType.STORY_POINTS, 3));
    }

    @Override
    public Color getOverrideNameColor() {
        return new Color(0, 255, 178);
    }
}
