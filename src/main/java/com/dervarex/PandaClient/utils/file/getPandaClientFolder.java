package com.dervarex.PandaClient.utils.file;

import java.io.File;

public class getPandaClientFolder {
    public File getPandaClientFolder() {
        return new File(new getAppdata() + File.separator + "PandaClient");
    }
}
