package data.scripts.ungprules.tags;

import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;


/**
 * 在应用生效规则之前，对生效规则列表做出调整
 */
public interface UNGP_TweakBeforeApplyCombatTag {
    /**
     * @param activeRules
     * @param originalActiveRules Unmodifiable
     */
    void tweakBeforeApply(List<URule> activeRules, List<URule> originalActiveRules);
}
