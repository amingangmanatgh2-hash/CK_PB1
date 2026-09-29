package ckpb1.client.core.setting;

/** On/off module setting. */
public final class BoolSetting extends Setting<Boolean> {

    private boolean value;

    public BoolSetting(String name, String description, boolean defaultValue) {
        super(name, description);
        this.value = defaultValue;
    }

    @Override
    public Boolean get() {
        return value;
    }

    public boolean isOn() {
        return value;
    }

    @Override
    public void set(Boolean value) {
        this.value = value != null && value;
    }

    public void toggle() {
        this.value = !this.value;
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Boolean b) {
            value = b;
        }
    }

    @Override
    public String display() {
        return value ? "ON" : "OFF";
    }
}
