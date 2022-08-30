package com.seibel.lod.core.config.types;

import com.seibel.lod.core.config.types.ConfigEntryAppearance;

/**
 * The class where all config options should extend
 *
 * @author coolGi
 */
public abstract class AbstractConfigType<T, S> { // The S is the class that is extending this
    public String category = "";    // This should only be set once in the init
    public String name;            // This should only be set once in the init
    protected T value;
    protected final Listener listener;

    public Object guiValue; // This is a storage variable something like the gui can use

    protected ConfigEntryAppearance appearance;

    public AbstractConfigType(ConfigEntryAppearance appearance, T value, Listener listener) {
        this.appearance = appearance;
        this.value = value;
        this.listener = listener;
    }

    public interface Listener {
        void onModify();
//        void onUiModify(); // TODO
    }

    
    /** Gets the value */
    public T get() {
        return this.value;
    }
    /** Sets the value */
    public void set(T newValue) {
        this.value = newValue;
        if (this.listener != null)
            this.listener.onModify();
    }
    
    public ConfigEntryAppearance getAppearance() {
        return appearance;
    }
    public void setAppearance(ConfigEntryAppearance newAppearance) {
        this.appearance = newAppearance;
    }


    /** Should not be needed for anything other than the gui/file handling */
    public String getCategory() {
        return this.category;
    }
    /** Should not be needed for anything other than the gui/file handling */
    public String getName() {
        return this.name;
    }
    /** Should not be needed for anything other than the gui/file handling */
    public String getNameWCategory() {
        return (this.category.isEmpty() ? "" : this.category + ".") + this.name;
    }


    // Gets the class of T
    public Class<?> getType() {
        return value.getClass();
    }

    protected static abstract class Builder<T, S> {
        protected ConfigEntryAppearance tmpAppearance = ConfigEntryAppearance.ALL;
        protected T tmpValue;
        protected Listener tmpListener;


        // Put this into your own builder
        public S setAppearance(ConfigEntryAppearance newAppearance) {
            this.tmpAppearance = newAppearance;
            return (S) this;
        }
        public S set(T newValue) {
            this.tmpValue = newValue;
            return (S) this;
        }
        public S setListener(Listener newListener) {
            this.tmpListener = newListener;
            return (S) this;
        }
    }
}
