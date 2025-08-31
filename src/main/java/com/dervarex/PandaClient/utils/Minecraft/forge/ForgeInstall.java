package com.dervarex.PandaClient.utils.Minecraft.forge;

import com.dervarex.PandaClient.Minecraft.MinecraftLauncher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

public class ForgeInstall {
    public void install(String version, File target, File TempFolder, File ForgeInstallerJar) throws MalformedURLException {
        URLClassLoader loader = URLClassLoader.newInstance(new URL[]{ForgeInstallerJar.toURI().toURL()});
        for(ForgeInstaller installer : ForgeInstaller.INSTALLERS) {
            try {
                //
                //MinecraftLauncher.LaunchMinecraft(version, );
                //

                installer.install(loader, target, System.out::println);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
