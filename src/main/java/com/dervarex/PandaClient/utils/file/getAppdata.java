package com.dervarex.PandaClient.utils.file;

import com.dervarex.PandaClient.utils.OS.OS;
import com.dervarex.PandaClient.utils.OS.OSUtil;

import java.io.File;

public class getAppdata {

    public static File getAppDataFolder(OS os) {
        switch (os) {
            case WINDOWS:
                return new File(System.getenv("APPDATA"));
            case MAC:
                return new File(System.getProperty("user.home"), "Library/Application Support");
            case LINUX:
                return new File(System.getProperty("user.home"), ".config");
            default:
                return new File(System.getProperty("user.home"));
        }
    }
}
