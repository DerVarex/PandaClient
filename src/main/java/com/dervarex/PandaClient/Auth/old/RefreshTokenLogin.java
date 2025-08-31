package com.dervarex.PandaClient.Auth.old;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;

public class RefreshTokenLogin {

    public static void LoginWithRefreshToken(){

        UserSession.User user = UserSession.getInstance().getUser();
        String refreshToken = user.getRefreshToken();
        final String BASE_DIR = System.getenv("APPDATA") + "\\PandaClient";


        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        MicrosoftAuthResult result = null;

        {
            try {
                result = authenticator.loginWithRefreshToken(refreshToken);
                String username = result.getProfile().getName();
                MinecraftProfile minecraftProfile = result.getProfile();
                String uuid = minecraftProfile.getId();
                result.getAccessToken();
            } catch (MicrosoftAuthenticationException e) {
                System.out.println("Error while login with refreshtoken: " + e.getMessage());
            }
        }

        MinecraftProfile profile = result.getProfile();
    }
}
