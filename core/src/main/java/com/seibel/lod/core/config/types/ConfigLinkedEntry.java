package com.seibel.lod.core.config.types;

import com.seibel.lod.core.config.types.enums.ConfigEntryAppearance;

/**
 * Creates a UI element that copies everything from another element.
 * This only effects the UI
 *
 * @author coolGi
 */
public class ConfigLinkedEntry extends AbstractConfigType<AbstractConfigType, ConfigLinkedEntry> {
    public ConfigLinkedEntry(AbstractConfigType value) {
        super(ConfigEntryAppearance.ONLY_IN_GUI, value);
    }

    /** Appearance shouldn't be changed */
    @Override
    public void setAppearance(ConfigEntryAppearance newAppearance) {}

    /** Value shouldn't be changed after creation */
    @Override
    public void set(AbstractConfigType newValue) {}


    public static class Builder extends AbstractConfigType.Builder<AbstractConfigType, Builder> {
        /** Appearance shouldn't be changed */
        @Override
        public Builder setAppearance(ConfigEntryAppearance newAppearance) {
            return this;
        }

        public ConfigLinkedEntry build() {
            return new ConfigLinkedEntry(this.tmpValue);
        }
    }
}
