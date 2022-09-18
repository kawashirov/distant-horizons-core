package com.seibel.lod.core.config;

import com.seibel.lod.core.config.types.ConfigEntry;

// TODO: Make this intergrate with the config system
public class AppliedConfigState<T> {
    final ConfigEntry<T> entry;
    T activeValue;

    public AppliedConfigState(ConfigEntry<T> entryToWatch) {
        this.entry = entryToWatch;
        activeValue = entryToWatch.get();
    }

    public boolean pollNewValue() {
        T newValue = entry.get();
        if (newValue.equals(activeValue)) {
            return false;
        }
        activeValue = newValue;
        return true;
    }

    public T get() {
        return activeValue;
    }
}
