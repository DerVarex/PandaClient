package com.dervarex.PandaClient.utils.NetUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetUtils {
    /*
    * NetUtils
    * isOnline, isServerOnline,
    * TODO: add some utils here
     */

    public static boolean isOnline() {
        try {
            InetAddress inetAddress = InetAddress.getByName("8.8.8.8");
            return inetAddress.isReachable(2000);

        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isServerOnline(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
