package com.dervarex.PandaClient.utils.file;

import com.dervarex.PandaClient.utils.OS.OSUtil;
import com.dervarex.PandaClient.utils.file.getAppdata;

import java.io.File;

public class getPandaClientFolder {
    public static File getPandaClientFolder() {
        return new File(
                getAppdata.getAppDataFolder(OSUtil.getOS()),
                "PandaClient"
        );
    }
}
