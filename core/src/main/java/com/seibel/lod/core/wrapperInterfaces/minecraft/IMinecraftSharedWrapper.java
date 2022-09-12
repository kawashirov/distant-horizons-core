package com.seibel.lod.core.wrapperInterfaces.minecraft;

import com.seibel.lod.core.dependencyInjection.IBindable;

import java.io.File;

//TODO: Maybe have IMCClientWrapper & IMCDedicatedWrapper extend this interface???
public interface IMinecraftSharedWrapper extends IBindable {
    boolean isDedicatedServer();

    File getInstallationDirectory();

}
