package ungp.api.saves;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import ungp.impl.saves.UNGP_BlueprintsDataSaver;
import ungp.scripts.campaign.UNGP_InGameData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Used to extend the ungp.data which could save in the savepoint. Should be overridden and initialized by writing the class path to "ungp.data/campaign/UNGP_dataSavers.csv"
 * Take {@link UNGP_BlueprintsDataSaver} as example
 * An instance of each ungp.data saver would be recorded, and each time inherit ungp.data is created, the instance would be *copied*.
 */
public interface UNGP_DataSaverAPI {

    /**
     * Used for immediately createPlugin a ungp.data saver related to current game status for inherit ungp.data.
     *
     * @param inGameData
     * @return
     */
    UNGP_DataSaverAPI createSaverBasedOnCurrentGame(UNGP_InGameData inGameData);

    /**
     * For the players who don't have savepoint.
     *
     * @return
     */
    UNGP_DataSaverAPI createEmptySaver();

    /**
     * Would be called after {@link #createEmptySaver()}, load your ungp.data from savepoint file
     *
     * @param jsonObject
     * @throws JSONException
     */
    void loadDataFromSavepointSlot(JSONObject jsonObject) throws JSONException;

    /**
     * Save your ungp.data to savepoint file.
     * The jsonObject would be saved automatically so don't call save().
     *
     * @param jsonObject
     * @throws JSONException
     */
    void saveDataToSavepointSlot(JSONObject jsonObject) throws JSONException;

    /**
     * @param root   Would add some information after inherit.
     * @param params Some basic param that you may not care.
     */
    void startInheritDataFromSaver(TooltipMakerAPI root, Map<String, Object> params);

    /**
     * Show the saver info while showing the whole inherit things.
     *
     * @param root
     * @param descKey
     */
    void addSaverInfo(TooltipMakerAPI root, String descKey);

}
