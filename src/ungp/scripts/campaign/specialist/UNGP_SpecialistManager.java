package ungp.scripts.campaign.specialist;

import ungp.scripts.campaign.UNGP_InGameData;

public class UNGP_SpecialistManager {
    /**
     * 缓存当前存档是否是专家模式
     * -1: 未初始化
     * 0: 不是专家模式
     * 1: 是专家模式
     */
    private static int CACHED_SPECIALIST_MODE = -1;

    /**
     * 重置缓存
     */
    public static void resetCache() {
        CACHED_SPECIALIST_MODE = -1;
    }

    /**
     * 当前存档是否是专家模式
     *
     * @return
     */
    public static boolean isSpecialistMode() {
        if (CACHED_SPECIALIST_MODE == -1) {
            CACHED_SPECIALIST_MODE = UNGP_InGameData.getDataInSave().isHardMode() ? 1 : 0;
        }
        return CACHED_SPECIALIST_MODE == 1;
    }

    /**
     * 更新缓存的专家模式状态
     */
    public static void updateCachedSpecialistMode(boolean mode) {
        CACHED_SPECIALIST_MODE = mode ? 1 : 0;
    }
}
