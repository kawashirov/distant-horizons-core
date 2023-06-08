package com.seibel.lod.core.config.listeners;

public interface IConfigListener
{
	/** Called whenever the value is set (including in core DH code) */
	void onConfigValueSet();
	
	/** Called whenever the value is changed through the UI (only when the done button is pressed) */
	void onUiModify(); // TODO
	
}