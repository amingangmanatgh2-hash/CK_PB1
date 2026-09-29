package ckpb1.client.core.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Multi-choice module setting (cycles through a fixed list of modes). */
public final class ModeSetting extends Setting<String> {

    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String description, String defaultMode, String... moreModes) {
        super(name, description);
        List<String> all = new ArrayList<>();
        all.add(defaultMode);
        all.addAll(Arrays.asList(moreModes));
        this.modes = all;
        this.index = 0;
    }

    public List<String> modes() {
        return modes;
    }

    @Override
    public String get() {
        return modes.get(index);
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    @Override
    public void set(String value) {
        if (value == null) {
            return;
        }
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).equalsIgnoreCase(value)) {
                index = i;
                return;
            }
        }
    }

    public void cycleForward() {
        index = (index + 1) % modes.size();
    }

    public void cycleBackward() {
        index = (index - 1 + modes.size()) % modes.size();
    }

    @Override
    public Object toJson() {
        return get();
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof String s) {
            set(s);
        }
    }

    @Override
    public String display() {
        return get();
    }
}
