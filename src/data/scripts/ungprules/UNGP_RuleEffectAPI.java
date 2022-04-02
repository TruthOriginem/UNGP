package data.scripts.ungprules;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
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
    void updateDifficultyCache(Difficulty difficulty);
//    void updateDifficultyCache(int difficulty);

    /**
     * Get the values depending on the difficulty level, these values should be saved in the object of the rule class.
     * Better use {@link Difficulty#getLinearValue(float, float)}
     * 根据专家等级获得指定值，这个值应该被储存在规则类中
     *
     * @param index
     * @param difficulty
     * @return
     */
    float getValueByDifficulty(int index, Difficulty difficulty);
//    float getValueByDifficulty(int index, int difficulty);

    /**
     * Shown in the rule's picker interface.
     * 会在规则选择界面展示
     *
     * @param index
     * @param difficulty
     * @return
     */
    String getDescriptionParams(int index, Difficulty difficulty);
//    String getDescriptionParams(int index, int difficulty);


    /**
     * Implement some global effects
     */
    void applyGlobalStats();

    void unapplyGlobalStats();

    /**
     * Would be called when the rule was removed after reselection or other reasons like rules which may remove others.
     * Should only put some global cleansing code here.
     * 该规则被移除时会被调用，一般往里放清理全局数据的代码
     */
    void cleanUp();

    /**
     * In Specialist intel there will be an icon tip that could show some data from this rule.
     * 专家模式界面该rule的生涯提示，注意这个只会放在右边
     *
     * @param imageTooltip
     * @return If return false, will not show the tooltip
     */
    boolean addIntelTips(TooltipMakerAPI imageTooltip);

    URule getRule();
}
