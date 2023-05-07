package ungp.scripts.ui;

public class SettingEntry<T> {
    private T curValue;

    private T defaultValue;

    public SettingEntry(T defaultValue) {
        this.defaultValue = defaultValue;
        this.curValue = defaultValue;
    }

    public T get() {
        return curValue;
    }

    /**
     * @param curValue set to default value if passed in null.
     */
    public void set(T curValue) {
        if (curValue == null) {
            reset();
        } else {
            this.curValue = curValue;
        }
    }

    public void reset() {
        curValue = defaultValue;
    }
}
