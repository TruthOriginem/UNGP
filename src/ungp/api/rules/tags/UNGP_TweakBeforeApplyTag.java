package ungp.api.rules.tags;

import java.util.List;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;


/**
 * 在应用生效规则之前，对生效规则列表做出调整
 */
public interface UNGP_TweakBeforeApplyTag {
    /**
     * @param activeRules
     * @param originalActiveRules Unmodifiable
     */
    void tweakBeforeApply(List<URule> activeRules, List<URule> originalActiveRules);
}
