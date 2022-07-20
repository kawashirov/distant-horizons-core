package com.seibel.lod.core.enums.override;

/**
 * PRIMARY, <br>
 * SECONDARY, <br>
 * CORE, <br>
 *
 * @author James Seibel
 * @version 2022-7-18
 */
public enum EApiOverridePriority
{
	/**
	 * The default Override priority and the one generally suggested
	 * for developers who want to create an override. <br>
	 * The highest priority.
	 */
	PRIMARY,
	
	/**
	 * Overrides with this priority are only used if there isn't
	 * an override with PRIMARY priority. <br>
	 * Could be used to allow creating overrides that other mod developers
	 * could override.
	 */
	SECONDARY,
	
	/**
	 * Only Distant Horizons classes should use the CORE priority,
	 * attempting to set an override with CORE priority will result in an error. <br>
	 * This is the lowest priority and will be overridden by all other priorities.
	 */
	CORE,
}
