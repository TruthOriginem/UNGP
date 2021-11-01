package data.scripts.campaign.specialist.rules;

import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

import java.util.Comparator;

public class UNGP_RuleSorter implements Comparator<URule> {
    /**
     * 负面规则在前，正面在后
     * cost高的在前，低的在后
     * 根据id在排
     *
     * @param o1
     * @param o2
     * @return
     */
    @Override
    public int compare(URule o1, URule o2) {
        int compare = Boolean.compare(!o1.isBonus(), !o2.isBonus());
        if (compare == 0) {
            compare = Boolean.compare(o1.isGolden(), o2.isGolden());
            if (compare == 0) {
                compare = Boolean.compare(!o1.isMileStone(), !o2.isMileStone());
                if (compare == 0) {
                    compare = Integer.compare(Math.abs(o1.getCost()), Math.abs(o2.getCost()));
                    if (compare == 0)
                        compare = o1.getId().compareTo(o2.getId());
                }
            }
        }
        compare = -compare;
        return compare;
    }
}
