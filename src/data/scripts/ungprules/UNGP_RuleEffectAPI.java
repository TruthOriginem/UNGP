package data.scripts.ungprules;

import data.scripts.campaign.hardmode.UNGP_RulesManager.URule;

/**
 *
 */
public interface UNGP_RuleEffectAPI {

    /**
     * 每次加载游戏存档/改变专家等级都会调用
     *
     * @param difficulty
     */
    void updateDifficultyCache(int difficulty);
    float getValueByDifficulty(int index, int difficulty);
    String getDescriptionParams(int index);
    String getDescriptionParams(int index, int difficulty);

    void applyGlobalStats();
    void unapplyGlobalStats();
    URule getRule();
}
