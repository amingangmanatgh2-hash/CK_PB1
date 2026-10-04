package ckpb1.client.core.setting;

import java.util.ArrayList;
import java.util.List;

/** Editable string list setting (e.g. player target lists, shop commands). */
public final class ListSetting extends Setting<List<String>> {

    private final List<String> values = new ArrayList<>();
    private final List<String> defaults;

    public ListSetting(String name, String description, List<String> defaults) {
        super(name, description);
        this.defaults = new ArrayList<>(defaults);
        this.values.addAll(defaults);
    }

    @Override
    public List<String> get() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean containsIgnoreCase(String value) {
        for (String v : values) {
            if (v.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public void add(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String trimmed = value.trim();
        if (!containsIgnoreCase(trimmed)) {
            values.add(trimmed);
        }
    }

    public boolean remove(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase(value.trim())) {
                values.remove(i);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        values.clear();
    }

    public void resetToDefaults() {
        values.clear();
        values.addAll(defaults);
    }

    @Override
    public void set(List<String> value) {
        values.clear();
        if (value != null) {
            for (String v : value) {
                if (v != null && !v.isBlank()) {
                    values.add(v.trim());
                }
            }
        }
    }

    @Override
    public Object toJson() {
        return new ArrayList<>(values);
    }

    @Override
    public void fromJson(Object json) {
        set(stringList(json));
    }

    @Override
    public String display() {
        return values.isEmpty() ? "(empty)" : values.size() + " item(s)";
    }
}
