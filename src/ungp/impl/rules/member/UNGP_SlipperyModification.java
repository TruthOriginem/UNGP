package ungp.impl.rules.member;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.RefitScreenListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignListenerTag;
import ungp.api.rules.tags.UNGP_CampaignTag;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 手滑改装：玩家的每艘舰船保存装配时，如果装配中存在内置 S 插，会在下一次舰队状态刷新
 * 或进港维修时随机获得一定数量的 D-插件。每艘舰船只生效一次。
 * <p>
 * RefitScreen.saveCurrentVariant 结束时装配界面仍持有自己的装配副本，之后关闭界面或切换舰船
 * 还会再保存一次；如果收到监听通知就立刻改写 member 的 variant，改动会被界面副本覆盖。
 * 因此监听器只负责把舰船记为待结算，真正添加 D-插件是在之后的安全时点进行：
 * 每天一次的舰队状态刷新（advanceInCampaign 的 isOneDayPassed）或玩家进港（打开市场界面）。
 * <p>
 * “存在内置 S 插”同时包含普通船插的 S 强化（getSMods）和舰船自带内置船插的 S 强化
 * （getSModdedBuiltIns）。
 */
public class UNGP_SlipperyModification extends UNGP_BaseRuleEffect
        implements UNGP_CampaignTag, UNGP_CampaignListenerTag<UNGP_SlipperyModification.SlipperyListener> {
    private static final int PENDING_SLOT = 0;
    private static final int TRIGGERED_SLOT = 1;

    private int minDMods = 0;
    private int maxDMods = 1;

    /**
     * 同时覆盖两种监听：装配保存（记录待结算）和进港（结算）。
     * 用自定义接口是为了让 UNGP_CampaignListenerTag 能按类型精确移除监听器。
     */
    public interface SlipperyListener extends RefitScreenListener, ColonyInteractionListener {
    }

    public class Listener implements SlipperyListener {
        @Override
        public void reportFleetMemberVariantSaved(FleetMemberAPI member, MarketAPI dockedAt) {
            if (!isPlayerFleetMember(member)) return;
            if (member.getVariant() == null || !hasSMods(member.getVariant())) return;

            String memberId = member.getId();
            if (memberId == null) return;

            Set<String> pending = getDataInCampaign(PENDING_SLOT);
            Set<String> triggered = getDataInCampaign(TRIGGERED_SLOT);
            if (pending == null || triggered == null) return;
            if (triggered.contains(memberId) || pending.contains(memberId)) return;

            pending.add(memberId);
            saveDataInCampaign(PENDING_SLOT, pending);
        }

        @Override
        public void reportPlayerOpenedMarket(MarketAPI market) {
            // 进港维修：结算所有待结算的舰船
            settlePending();
        }

        @Override
        public void reportPlayerClosedMarket(MarketAPI market) {
        }

        @Override
        public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
        }

        @Override
        public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        }
    }

    @Override
    public SlipperyListener getListener() {
        return new Listener();
    }

    @Override
    public Class<SlipperyListener> getClassOfListener() {
        return SlipperyListener.class;
    }

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        minDMods = Math.max(0, (int) getValueByDifficulty(0, difficulty));
        maxDMods = Math.max(minDMods, (int) getValueByDifficulty(1, difficulty));
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        int min;
        int max;
        switch (difficulty) {
            case GAMMA, BETA -> {
                min = 0;
                max = 1;
            }
            case ALPHA -> {
                min = 1;
                max = 1;
            }
            default -> {
                min = 1;
                max = 2;
            }
        }
        if (index == 0) return min;
        if (index == 1) return max;
        return 0f;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) {
            int min = (int) getValueByDifficulty(0, difficulty);
            int max = (int) getValueByDifficulty(1, difficulty);
            return min + "~" + max;
        }
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyGlobalStats() {
        if (getDataInCampaign(PENDING_SLOT) == null) {
            saveDataInCampaign(PENDING_SLOT, new HashSet<String>());
        }
        if (getDataInCampaign(TRIGGERED_SLOT) == null) {
            saveDataInCampaign(TRIGGERED_SLOT, new HashSet<String>());
        }
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        // 每天一次的舰队状态刷新时结算
        if (params == null || !params.isOneDayPassed()) return;
        settlePending();
    }

    /**
     * 把待结算的舰船结算掉：仍在舰队且仍带内置 S 插的获得 D-插件；已移除 S 插的本次不触发，
     * 也不占用唯一一次机会；已不在舰队的直接清掉。
     */
    private void settlePending() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;

        Set<String> pending = getDataInCampaign(PENDING_SLOT);
        Set<String> triggered = getDataInCampaign(TRIGGERED_SLOT);
        if (pending == null || triggered == null || pending.isEmpty()) return;

        // 还停在装配界面时先不结算：界面保存后仍会用工作副本覆盖 member 的 variant
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (campaignUI != null && campaignUI.getCurrentCoreTab() == CoreUITabId.REFIT) return;

        boolean changed = false;

        Set<String> fleetMemberIds = new HashSet<>();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member != null && member.getId() != null) fleetMemberIds.add(member.getId());
        }
        // 已经不在舰队里的舰船不再结算
        if (pending.retainAll(fleetMemberIds)) changed = true;

        List<String[]> gains = new ArrayList<>();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            String memberId = member.getId();
            if (memberId == null || !pending.contains(memberId)) continue;

            pending.remove(memberId);
            changed = true;
            // S 插在结算前被移除，本次不惩罚，也不占用那一次机会
            if (!hasSMods(member.getVariant())) continue;

            triggered.add(memberId);
            List<String> addedDMods = applyDMods(member);
            if (!addedDMods.isEmpty()) {
                gains.add(new String[]{getShipName(member), joinDModNames(addedDMods)});
            }
        }

        if (changed) {
            saveDataInCampaign(PENDING_SLOT, pending);
            saveDataInCampaign(TRIGGERED_SLOT, triggered);
        }
        if (!gains.isEmpty()) showGains(gains);
    }

    private static boolean isPlayerFleetMember(FleetMemberAPI member) {
        if (member == null) return false;
        FleetDataAPI fleetData = member.getFleetData();
        return fleetData != null && fleetData.getFleet() == Global.getSector().getPlayerFleet();
    }

    private static boolean hasSMods(ShipVariantAPI variant) {
        return variant != null
                && (!variant.getSMods().isEmpty() || !variant.getSModdedBuiltIns().isEmpty());
    }

    private List<String> applyDMods(FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant();
        List<String> before = getDModIds(variant);
        int target = minDMods + (maxDMods > minDMods ? RANDOM.nextInt(maxDMods - minDMods + 1) : 0);
        if (target <= 0) return Collections.emptyList();

        DModManager.addDMods(variant, false, target, RANDOM);
        List<String> added = getDModIds(variant);
        added.removeAll(before);
        if (added.isEmpty()) return Collections.emptyList();

        if (!variant.getHullSpec().isDHull()
                && Global.getSettings().getHullSpec(Misc.getDHullId(variant.getHullSpec())) != null) {
            try {
                // 与原版战利品回收一致：有 D-插件后切换到 D 船体，舰船名称、价值与修复流程都会按 D 船处理
                DModManager.setDHull(variant);
            } catch (RuntimeException e) {
                Global.getLogger(UNGP_SlipperyModification.class)
                        .warn("Failed to switch to D hull for " + member.getHullSpec().getHullId(), e);
            }
        }
        member.updateStats();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null) playerFleet.getFleetData().setSyncNeeded();
        return added;
    }

    private static List<String> getDModIds(ShipVariantAPI variant) {
        List<String> ids = new ArrayList<>();
        for (String id : variant.getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (spec != null && spec.hasTag(Tags.HULLMOD_DMOD)) ids.add(id);
        }
        return ids;
    }

    private static String joinDModNames(List<String> ids) {
        List<String> names = new ArrayList<>(ids.size());
        for (String id : ids) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            names.add(spec != null ? spec.getDisplayName() : id);
        }
        return String.join(" / ", names);
    }

    private static String getShipName(FleetMemberAPI member) {
        String shipName = member.getShipName();
        return shipName != null ? shipName : member.getHullSpec().getHullName();
    }

    /**
     * 结算消息：标题行 + 每艘舰船一行，列出该舰船本次获得的 D-插件名称。
     * 标题行与每行格式分别取自规则 CSV 的 extra1、extra2，便于中英文分文件维护。
     */
    private void showGains(List<String[]> gains) {
        MessageIntel message = createMessage();
        String header = rule.getExtra1();
        if (header != null && !header.isEmpty()) message.addLine(header);

        String lineFormat = rule.getExtra2();
        if (lineFormat == null || lineFormat.isEmpty()) lineFormat = "%s: %s";
        for (String[] gain : gains) {
            message.addLine(lineFormat, Misc.getTextColor(),
                    new String[]{gain[0], gain[1]},
                    Misc.getHighlightColor(), Misc.getNegativeHighlightColor());
        }
        showMessage(message);
    }

    /**
     * 规则被卸下时只丢弃"待结算"，保留"已受罚"。
     * TRIGGERED 记的是这艘船在整场战役里已经吃过的惩罚：如果连它一起清掉，
     * 卸下规则再装回来，同一艘船会再吃一次 D-插件，来回切换就能把整支舰队刷满 D 槽。
     */
    @Override
    public void cleanUp() {
        clearDataInCampaign(PENDING_SLOT);
    }
}
