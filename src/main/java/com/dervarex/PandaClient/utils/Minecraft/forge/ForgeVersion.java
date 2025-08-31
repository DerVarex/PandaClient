package com.dervarex.PandaClient.utils.Minecraft.forge;

import java.util.List;

public class ForgeVersion extends WrapperVersion {

    private ForgeHelper helper;

    public ForgeVersion(ForgeHelper helper) {
        this.helper = helper;
    }
    String id = helper.getVersionID();

    public enum ForgeVersionType{
        LATEST, RECOMMENDED, NORMAL
    }


    public ForgeVersion(String id){
        this.id = id;
    }

    public ForgeVersion(String id, String wrId){
        this.id = id;
        this.wrapperVersion = wrId;
    }
    public ForgeVersionType forgeVersionType;
    public List<FArtifact> fArtifacts;
    //@Override
    public String getJsonName(){
        return id + "-forge-" + wrapperVersion;
    }
    public String getBaseIdentity(){
        return id + "-" + wrapperVersion;
    }

    //@Override
    public String getClientName(){
        return "forge-" + getBaseIdentity();
    }
    public Arguments arguments;
}