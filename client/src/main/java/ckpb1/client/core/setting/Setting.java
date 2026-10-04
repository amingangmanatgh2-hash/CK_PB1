package ckpb1.client.core.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class of CK_PB1 module settings. Settings serialize themselves into
 * the per-profile module config (JSON) via {@link #toJson()} / {@link #fromJson(Object)}.
 */
public abstract class Setting<T> {

    public final String name;
    public final String description;

    protected Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract T get();

    public abstract void set(T value);

    /** Serialized representation of the current value. */
    public abstract Object toJson();

    /** Restores the value; must tolerate unknown/corrupt input. */
    public abstract void fromJson(Object json);

    /** Display text of the current value used by the Click GUI. */
    public abstract String display();

    @SuppressWarnings("unchecked")
    protected static List<String> stringList(Object json) {
        List<String> out = new ArrayList<>();
        if (json instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    String s = String.valueOf(o);
                    if (!s.isBlank()) {
                        out.add(s);
                    }
                }
            }
        } else if (json instanceof String s) {
            for (String part : s.split("[,\n]")) {
                if (!part.isBlank()) {
                    out.add(part.trim());
                }
            }
        }
        return out;
    }

    protected static double toDouble(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    protected static void putIf(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
