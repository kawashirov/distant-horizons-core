package com.seibel.lod.core.config.types;

public enum ConfigEntryAppearance {
    ALL(true, true),
    ONLY_SHOW(true, false),
    ONLY_IN_FILE(true, false);

    public final boolean showInGui;
    public final boolean showInFile; // If this is false then it wouldn't save the option
    ConfigEntryAppearance(boolean showInGui, boolean showInFile) { // If both are false then the config won't touch the option
        this.showInGui = showInGui;
        this.showInFile = showInFile;
    }
}
