package com.dervarex.PandaClient.Minecraft.Profile;

import com.dervarex.PandaClient.Minecraft.loader.LoaderType;

public class Profile {

    // Felder, die die Factory setzen kann
    private String profileName;
    private String versionId;
    private String profileImagePath;
    private LoaderType loader;

    // Getter
    public String getProfileName() { return profileName; }
    public String getVersionId() { return versionId; }
    public LoaderType getLoader() { return loader; }
    public String getProfileImagePath() { return profileImagePath; }

    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    // ------------------ ProfileFactory ------------------
    public static final class ProfileFactory {
        private String profileName;
        private String versionId;
        private String profileImagePath;
        private LoaderType loader;

        public ProfileFactory(String profileName, String versionId, LoaderType loader){
            this.profileName = profileName;
            this.versionId = versionId;
            this.loader = loader;
        }

        public Profile build(){
            Profile p = new Profile();
            p.profileName = this.profileName;
            p.versionId = this.versionId;
            p.loader = this.loader;
            p.profileImagePath = this.profileImagePath;
            return p;
        }

        // Setter und Getter
        public void setProfileName(String profileName){ this.profileName = profileName; }
        public void setVersionId(String versionId){ this.versionId = versionId; }
        public void setLoader(LoaderType loader){ this.loader = loader; }
        public void setProfileImagePath(String profileImagePath){ this.profileImagePath = profileImagePath; }

        public String getProfileName(){ return profileName; }
        public String getVersionId(){ return versionId; }
        public LoaderType getLoader(){ return loader; }
        public String getProfileImagePath(){ return profileImagePath; }

        public boolean isEmpty(){ return profileName == null || versionId == null || loader == null; }
    }
}
