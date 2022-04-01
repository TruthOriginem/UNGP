package data.scripts.ungpsaves;

import org.json.JSONObject;

public interface UNGP_DataSaverAPI {
    void saveDataToSlot(JSONObject jsonObject);

    void inheritDataFromSlot(JSONObject jsonObject);
}
