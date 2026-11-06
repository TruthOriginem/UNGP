package ungp.impl.rules.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.UnderworldCustomBounty;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignListenerTag;
import ungp.api.rules.tags.UNGP_CampaignTag;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 赏金：玩家可接的赏金任务（人物赏金与酒馆悬赏）报酬提高。
 * <p>
 * 账本记的是"赏金 id → 已经加上的 credits"，id 由赏金自身能跨存档稳定的字段拼出来：
 * 人物赏金用目标人物 id，酒馆悬赏用任务 id。不能用 intel 对象本身当 key ——
 * 它进了 persistentData 之后，读档时能否还原成同一个实例取决于 XStream 的引用表，
 * 一旦失配 containsKey 就会落空：同一份赏金被重复加成，卸载规则时又会减到错误的对象上。
 * <p>
 * 触发时机只有三个：规则生效时一次、每天一次、玩家打开核心界面时一次（赏金面板在 INTEL 里）。
 * 不做逐帧扫描，赏金的报酬在任务进行中不会变化。
 */
public class UNGP_BountyHunter extends UNGP_BaseRuleEffect
        implements UNGP_CampaignTag, UNGP_CampaignListenerTag<UNGP_BountyHunter.BountyListener> {
    private static final int BONUS_SLOT = 0;
    private static final String PERSON_PREFIX = "person:";
    private static final String MISSION_PREFIX = "mission:";

    private float flatBonus;
    private float bonusPercent;

    /**
     * 只关心"核心界面被打开"。用独立接口是为了让 UNGP_CampaignListenerTag 能按类型精确移除监听器。
     */
    public interface BountyListener extends CoreUITabListener {
    }

    public class Listener implements BountyListener {
        @Override
        public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
            // 任何核心界面打开时顺手刷新一次，代价是一次 intel 列表遍历，远低于逐帧扫描
            if (tab != null) updateBounties();
        }
    }

    @Override
    public BountyListener getListener() {
        return new Listener();
    }

    @Override
    public Class<BountyListener> getClassOfListener() {
        return BountyListener.class;
    }

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        flatBonus = getValueByDifficulty(0, difficulty);
        bonusPercent = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(10000f, 40000f, 50000f);
        if (index == 1) return 10f;
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyGlobalStats() {
        updateBounties();
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        // 人物赏金由原版定期创建，玩家不打开界面也会出现，每天兜底一次
        if (params == null || !params.isOneDayPassed()) return;
        updateBounties();
    }

    private int getBonus(float reward) {
        return Math.round(reward * bonusPercent * 0.01f + flatBonus);
    }

    private void updateBounties() {
        Map<String, Integer> bonuses = loadBonuses();
        List<IntelInfoPlugin> bounties = listBounties();

        // 已经结束、下架或不再可接的赏金从账本里丢掉，避免账本随战役无限增长
        Set<String> alive = new HashSet<>();
        for (IntelInfoPlugin bounty : bounties) {
            if (!isActive(bounty)) continue;
            String key = ledgerKey(bounty);
            if (key != null) alive.add(key);
        }
        bonuses.keySet().retainAll(alive);

        for (IntelInfoPlugin bounty : bounties) {
            if (!isActive(bounty) || !isRewardable(bounty)) continue;
            String key = ledgerKey(bounty);
            if (key == null || bonuses.containsKey(key)) continue;
            float reward = getReward(bounty);
            if (reward <= 0f) continue;
            int bonus = getBonus(reward);
            setReward(bounty, reward + bonus);
            bonuses.put(key, bonus);
        }
        saveDataInCampaign(BONUS_SLOT, bonuses);
    }

    /**
     * 读取账本。旧存档里存的是 Map{intel 对象 → bonus}，这里顺手迁移成 id 账本；
     * 认不出 id 的旧条目直接丢弃（宁可不回滚，也不要减错赏金）。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> loadBonuses() {
        Object raw = getDataInCampaign(BONUS_SLOT);
        if (raw instanceof Map<?, ?> stored) {
            if (stored.isEmpty()) {
                Map<String, Integer> empty = new HashMap<>();
                saveDataInCampaign(BONUS_SLOT, empty);
                return empty;
            }
            if (stored.keySet().iterator().next() instanceof String) {
                return (Map<String, Integer>) stored;
            }
            Map<String, Integer> migrated = new HashMap<>();
            for (Map.Entry<?, ?> entry : stored.entrySet()) {
                if (!(entry.getValue() instanceof Number number)) continue;
                String key = entry.getKey() instanceof IntelInfoPlugin intel ? ledgerKey(intel) : null;
                if (key != null) migrated.put(key, number.intValue());
            }
            saveDataInCampaign(BONUS_SLOT, migrated);
            return migrated;
        }
        Map<String, Integer> fresh = new HashMap<>();
        saveDataInCampaign(BONUS_SLOT, fresh);
        return fresh;
    }

    private static List<IntelInfoPlugin> listBounties() {
        IntelManagerAPI intelManager = Global.getSector().getIntelManager();
        if (intelManager == null) return Collections.emptyList();
        List<IntelInfoPlugin> bounties = new ArrayList<>();
        bounties.addAll(intelManager.getIntel(PersonBountyIntel.class));
        bounties.addAll(intelManager.getCommQueue(PersonBountyIntel.class));
        bounties.addAll(intelManager.getIntel(BaseCustomBounty.class));
        bounties.addAll(intelManager.getCommQueue(BaseCustomBounty.class));
        return bounties;
    }

    /**
     * 赏金自己的稳定标识。拿不到（目标人物或任务 id 还没生成）时返回 null，这次跳过。
     */
    private static String ledgerKey(IntelInfoPlugin bounty) {
        if (bounty instanceof PersonBountyIntel personBounty) {
            PersonAPI person = personBounty.getPerson();
            return person == null ? null : PERSON_PREFIX + person.getId();
        }
        if (bounty instanceof BaseCustomBounty customBounty) {
            String missionId = customBounty.getMissionId();
            return missionId == null ? null : MISSION_PREFIX + missionId;
        }
        return null;
    }

    /**
     * 用 isAssignableFrom 而不是精确类比较：别的 mod 继承这两种酒馆悬赏时也能吃到加成。
     */
    private static boolean isRewardable(IntelInfoPlugin bounty) {
        Class<?> bountyClass = bounty.getClass();
        return PersonBountyIntel.class.isAssignableFrom(bountyClass)
                || MilitaryCustomBounty.class.isAssignableFrom(bountyClass)
                || UnderworldCustomBounty.class.isAssignableFrom(bountyClass);
    }

    private static boolean isActive(IntelInfoPlugin bounty) {
        if (bounty.isEnding() || bounty.isEnded()) return false;
        return !(bounty instanceof BaseCustomBounty contactBounty)
                || contactBounty.getCurrentStage() == BaseCustomBounty.Stage.BOUNTY;
    }

    private static float getReward(IntelInfoPlugin bounty) {
        if (bounty instanceof PersonBountyIntel personBounty) return personBounty.getBountyCredits();
        return ((BaseCustomBounty) bounty).getCreditsReward();
    }

    private static void setReward(IntelInfoPlugin bounty, float reward) {
        if (bounty instanceof PersonBountyIntel personBounty) {
            personBounty.setBountyCredits(reward);
        } else {
            ((BaseCustomBounty) bounty).setCreditReward(Math.round(reward));
        }
    }

    @Override
    public void unapplyGlobalStats() {
        // 缓存重建时会对所有规则调用 unapply，包括从没生效过的那些。
        // 没有账本就直接返回，避免给未生效的规则在存档里创建空槽。
        if (getDataInCampaign(BONUS_SLOT) == null) return;
        Map<String, Integer> bonuses = loadBonuses();
        if (!bonuses.isEmpty()) {
            for (IntelInfoPlugin bounty : listBounties()) {
                if (!isActive(bounty)) continue;
                String key = ledgerKey(bounty);
                Integer bonus = key == null ? null : bonuses.get(key);
                if (bonus == null) continue;
                setReward(bounty, getReward(bounty) - bonus);
            }
        }
        clearDataInCampaign(BONUS_SLOT);
    }

    @Override
    public void cleanUp() {
        unapplyGlobalStats();
    }
}
