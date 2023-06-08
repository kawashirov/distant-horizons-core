package com.seibel.lod.core.config.types.enums;

/**
 * Allows config entries (including options and categories) to only be shown in the file, or only in the ui
 * (remember that if you make it only visible in the ui then the option won't save on game restart)
 *
 * @author coolGi
 */
public enum ConfigEntryAppearance
{
    /** Defeat option */
    ALL(true, true),
    /** Will only show the option in the UI. The option will be reverted on game restart */
    ONLY_IN_GUI(true, false),
    /** Only show the option in the file. There would be no way to access it using the UI */
    ONLY_IN_FILE(true, false);

    /** Sets whether the option should show in the UI */
    public final boolean showInGui;
    /** Sets whether to save an option, <br> If set to false, the option will be reset on game restart */
    public final boolean showInFile;
	
    ConfigEntryAppearance(boolean showInGui, boolean showInFile) { // If both are false then the config won't touch the option, but it would still be accessable if explicitly called 
        this.showInGui = showInGui;
        this.showInFile = showInFile;
    }
}
