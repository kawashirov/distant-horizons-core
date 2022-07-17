package com.seibel.lod.core.api.implementation.interfaces.events;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * A combination of all interfaces required by
 * DH Api events.
 *
 * @param <InputType> This is the datatype that should be passed into the
 * 					  event handler's method.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public interface IDhApiEvent<InputType> extends IDhApiEventExternal<InputType>, IDhApiEventInternal<InputType>, IBindable
{
	// Don't add any methods here.
	// Add them to: IDhApiEventExternal or IDhApiEventInternal
	// (depending on if they should be available to
	// implementing developers or only DH devs)
}
