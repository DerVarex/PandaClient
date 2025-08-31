package com.dervarex.PandaClient.utils.Minecraft.forge;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.function.Consumer;
//Thanks to etkmlm from GitHub
public interface ForgeInstaller {
    ForgeInstaller[] INSTALLERS = { new ForgeV3Installer() }; // Removed         new ForgeV1Installer(), new ForgeV2Installer(),

    void install(URLClassLoader loader, File target, Consumer<String> logState) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException;
}