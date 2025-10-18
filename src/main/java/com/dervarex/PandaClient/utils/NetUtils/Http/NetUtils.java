package com.dervarex.PandaClient.utils.NetUtils.Http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * NetUtils provides many functions to simplify networking tasks.
 */
public class NetUtils {

    /**
     * "Pings" <code>host</code> to check if it's alive
     *
     * @param host The hostname to ping
     * @return true if the host is reachable, false if not
     */
    public boolean pingHost(String host) {
        boolean reach;
        try {
            InetAddress addr = InetAddress.getByName(host);
            reach = addr.isReachable(3000);
        } catch (IOException ioe) {
            System.err.println("Unable to reach " + host + " (" + ioe.getMessage() + ")");
            return false;
        }
        return reach; // Should get here if everything goes well
    }

    /**
     * "Pings" an address to check if it's alive
     *
     * @param addr The ip address to ping
     * @return true if the host is reachable, false if not
     */
    public boolean pingAddr(String addr) {
        boolean reach;
        try {
            InetAddress host = InetAddress.getByName(addr);
            reach = host.isReachable(3000);
        } catch (IOException ioe) {
            System.err.println("Unable to reach " + addr + " (" + ioe.getMessage() + ")");
            return false;
        }
        return reach; // Should get here if everything goes well
    }

    /**
     * Sends an HTTP request of type {@link HttpTypes}.
     *
     * @param host The host to request
     * @param type The type of HTTP request to make (GET, POST, etc.)
     * @return The response from the server
     * @see SimpleHttpRequest
     * @see SimpleHttpRequest#fire()
     */
    public String HttpRequest(String host, HttpTypes type) {
        SimpleHttpRequest simpleHttpRequest = new SimpleHttpRequest(host, type);
        try {
            return simpleHttpRequest.fire();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return "";
    }

    /**
     * Gets the WAN IP address of the current network using an AWS service.
     *
     * @return String - The WAN IP
     */
    public String getWanIp() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            return in.readLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "";
        }
    }

    /**
     * Gets the local IP address of the current machine.
     *
     * @return String - The local IP address
     */
    public String getLocalIp() {
        try {
            return InetAddress.getLocalHost().toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "";
        }
    }

    /**
     * Uses Google's URL shortening API to shorten a given url (goo.gl format)
     *
     * @param url The URL to shorten
     * @return String - The shortened URL
     */
    public String urlShortenGoogle(String url) {
        String jUulkiggf = "QUl6YVN5RFozNnRGNmlWekxSTncySDJKSzZ0S1RuMlJic1RHUjln";
        SimpleHttpRequest req = new SimpleHttpRequest("https://www.googleapis.com/urlshortener/v1/url?key=" + Base64.getDecoder().decode(jUulkiggf), HttpTypes.POST);
        req.postBody = "{\"longUrl\": \"" + url + "\"/";
        final Pattern pattern = Pattern.compile("\"id\": \"(.+?)\"");  // Get something between two markers

        try {
            String res = req.fire();  // postBody should automatically be passed in here
            final Matcher matcher = pattern.matcher(res);
            matcher.find();
            return matcher.group(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "";
        }
    }

}

