package data.scripts.ungprules;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.UNGP_CampaignPlugin.TempCampaignParams;
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
    void refreshDifficultyCache(int difficulty);
    float getValueByDifficulty(int index, int difficulty);
    String getDescriptionParams(int index);
    String getDescriptionParams(int index, int difficulty);
    void advanceInCombat(CombatEngineAPI engine, float amount);
    void advanceInCampaign(float amount, TempCampaignParams params);

    /**
     * 每帧执行一次
     * @param amount
     * @param enemy
     */
    void applyEnemyShipInCombat(float amount, ShipAPI enemy);
    void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship);

    /**
     * 通过{@link BuffManagerAPI.Buff} 实现，这个buff存续时间是0.1秒，每帧刷新一次
     * @param member
     */
    void applyPlayerFleetMemberInCampaign(FleetMemberAPI member);
    /**
     * 只会在更新缓存执行一次
     *
     * @param stats
     */
    void applyPlayerCharacterStats(MutableCharacterStatsAPI stats);
    void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats);
    /**
     * 除了更新缓存时会更新，每帧都会更新
     */
    void applyPlayerFleetStats(CampaignFleetAPI fleet);
    void unapplyPlayerFleetStats(CampaignFleetAPI fleet);
    void applyGlobalStats();
    void unapplyGlobalStats();
    URule getRule();
}
