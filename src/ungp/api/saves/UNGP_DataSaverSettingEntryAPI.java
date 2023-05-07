package ungp.api.saves;

public interface UNGP_DataSaverSettingEntryAPI<T> {

    T confirmValue();

    void reset();

    void advance(float amount);

    void cancelConfirm();

    T getValue();
}
