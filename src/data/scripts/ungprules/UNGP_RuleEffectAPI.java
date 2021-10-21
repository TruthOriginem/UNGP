package data.scripts.ungprules;

import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

/**
 *
 */
public interface UNGP_RuleEffectAPI {

    /**
     * Will be called each time player loads game or changes the difficulty
     * 每次加载游戏存档/改变专家等级都会调用
     *
     * @param difficulty
     */
    void updateDifficultyCache(int difficulty);

    /**
     * Get the values depending on the difficulty level, these values should be saved in the object of the rule class.
     * 根据专家等级获得指定值，这个值应该被储存在规则类中
     *
     * @param index
     * @param difficulty
     * @return
     */
    float getValueByDifficulty(int index, int difficulty);

//    String getDescriptionParams(int index);

    /**
     * Shown in the rule's picker interface.
     * 会在规则选择界面展示
     *
     * @param index
     * @param difficulty
     * @return
     */
    String getDescriptionParams(int index, int difficulty);


    /**
     * Implement some global effects
     */
    void applyGlobalStats();

    void unapplyGlobalStats();

    URule getRule();
}
