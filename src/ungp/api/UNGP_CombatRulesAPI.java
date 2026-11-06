package ungp.api;

import ungp.scripts.campaign.UNGP_InGameData;

/**
 * 专家模式战斗规则的公共 API。
 * Public API of the specialist mode's combat rules.
 * <p>
 * 可以用它在生涯中设置“下一场战斗不生效战斗类专家规则”。该设置保存在存档中，
 * 只会被下一场真正的生涯战斗消耗一次；模拟战与自动战斗不会消耗它。
 * <p>
 * 注意：只屏蔽战斗插件（UNGP_SpecialistCombatPlugin）在战斗开始时应应用的战斗规则。
 * 已在生涯层生效、或经技能/成员 Buff/战斗监听器等通道进入战斗的生涯规则效果不会被撤销。
 * <p>
 * Note: this only blocks the combat rules that UNGP_SpecialistCombatPlugin would apply at
 * battle start. Campaign-level effects already applied, or effects that reach the battle
 * through skills, fleet member buffs or battle listeners, are not reverted.
 */
public class UNGP_CombatRulesAPI {
    private UNGP_CombatRulesAPI() {
    }

    /**
     * 设置后，当前生涯中的下一场战斗将自动屏蔽所有战斗类专家规则。
     * 若当前不在生涯中（例如主菜单的战斗任务），调用不会产生任何效果。
     * <p>
     * Block all combat specialist rules in the next campaign battle. The mark is saved
     * in the campaign and consumed by that battle only.
     */
    public static void blockInNextBattle() {
        UNGP_InGameData inGameData = UNGP_InGameData.getDataInSaveOrNull();
        if (inGameData != null) {
            inGameData.setBlockCombatRulesInNextBattle(true);
        }
    }

    /**
     * 取消由 {@link #blockInNextBattle()} 设置的屏蔽。
     * Cancel the block set by {@link #blockInNextBattle()}.
     */
    public static void cancelBlockInNextBattle() {
        UNGP_InGameData inGameData = UNGP_InGameData.getDataInSaveOrNull();
        if (inGameData != null) {
            inGameData.setBlockCombatRulesInNextBattle(false);
        }
    }

    /**
     * @return 是否已设置屏蔽下一场战斗的战斗类专家规则
     */
    public static boolean isBlockedInNextBattle() {
        UNGP_InGameData inGameData = UNGP_InGameData.getDataInSaveOrNull();
        return inGameData != null && inGameData.isBlockCombatRulesInNextBattle();
    }
}
