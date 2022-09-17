package com.seibel.lod.api.items.interfaces;

/**
 * The Distant Horizons' API objects can't cover
 * every potential use case. Sometimes developers just need
 * the base Minecraft Objects.
 *
 * @author James Seibel
 * @version 2022-9-16
 */
public interface IDhApiUnsafeWrapper
{
	
	/**
	 * Returns the Minecraft object this wrapper contains. <br>
	 * <strong>Warning</strong>: This object will be Minecraft
	 * version dependent and may change without notice. <br> <br>
	 *
	 * In order to cast this object to something usable, you may want
	 * to use <code>obj.getClass()</code> when in your IDE
	 * in order to determine what object this method returns for
	 * the specific version of Minecraft you are developing for.
	 */
	Object getWrappedMcObject_UNSAFE();
	
}
