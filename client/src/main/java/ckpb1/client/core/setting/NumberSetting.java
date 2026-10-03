package ckpb1.client.core.setting;

/** Numeric (double) module setting with min/max/step and an optional suffix. */
public final class NumberSetting extends Setting<Double> {

    public final double min;
    public final double max;
    public final double step;
    public final String suffix;
    private double value;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        this(name, description, defaultValue, min, max, step, "");
    }

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
        super(name, description);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.value = clamp(defaultValue);
    }

    private double clamp(double v) {
        double clamped = Math.max(min, Math.min(max, v));
        // snap to step grid
        if (step > 0) {
            clamped = Math.round((clamped - min) / step) * step + min;
        }
        return Math.max(min, Math.min(max, clamped));
    }

    @Override
    public Double get() {
        return value;
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    @Override
    public void set(Double value) {
        this.value = clamp(value == null ? min : value);
    }

    /** Sets from a 0..1 slider position. */
    public void setNormalized(double normalized) {
        set(min + (max - min) * Math.max(0, Math.min(1, normalized)));
    }

    public double normalized() {
        if (max <= min) {
            return 0;
        }
        return (value - min) / (max - min);
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        set(toDouble(json, value));
    }

    @Override
    public String display() {
        double v = Math.round(value * 100) / 100.0;
        String s = v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
        return s + suffix;
    }
}
