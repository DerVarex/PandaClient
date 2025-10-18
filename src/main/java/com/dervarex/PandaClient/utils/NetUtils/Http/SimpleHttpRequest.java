package com.dervarex.PandaClient.utils.NetUtils.Http;

import javafx.util.Pair;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A wrapper for simple http requests.
 * <p>
 * The default request type is <code>{@link HttpTypes#GET}</code>
 */
public class SimpleHttpRequest {

    private String hostname;
    private HttpTypes type = HttpTypes.GET;
    private String putFilePath = "";
    private String prevResponse = "";
    private StringBuilder completeParams = new StringBuilder();

    /**
     * The body of the POST request used with {@link SimpleHttpRequest#fireJsonPost(String)}
     */
    public String postBody = "";


    /**
     * Load up a HTTP request of type <code>type</code> to <code>hostname</code>
     *
     * @param hostname The host to request
     * @param type     The type of HTTP request
     * @see HttpTypes
     */
    public SimpleHttpRequest(String hostname, HttpTypes type) {
        this.hostname = hostname;
        this.type = type;
    }

    /**
     * Load up a HTTP GET request to <code>hostname</code>
     *
     * @param hostname The host to request
     */
    public SimpleHttpRequest(String hostname) {
        this.hostname = hostname;
    }

    /**
     * <b><i>USE THIS ONLY FOR PUT REQUESTS!!!!</i></b>
     * <p>
     * Loads up a PUT request to <code>hostname</code>. The data that will be "put"
     * on the server will be data read in from <code>putFilePath</code>
     * </p>
     *
     * @param hostname    The host to request
     * @param putFilePath The data to put
     */
    public SimpleHttpRequest(String hostname, String putFilePath) {
        this.hostname = hostname;
        this.putFilePath = putFilePath;
    }

    /**
     * <b><i>USE THIS ONLY FOR POST REQUESTS WITH PARAMETERS!!!!</i></b>
     * <p>
     * Loads up a post request with the given parameters.
     * </p>
     *
     * @param hostname   The host to request
     * @param postParams The url parameters (<code>var=val</code> associates to <code>key=value</code> (in Java)
     */
    public SimpleHttpRequest(String hostname, Pair<String, String>[] postParams) {
        this.hostname = hostname;
        for (int i = 0; i < postParams.length; i++) {
            if (i != 0) {
                completeParams.append("&");
            }
            completeParams.append(postParams[i].getKey()).append("=").append(postParams[i].getValue());

        }

        // TODO check that this works
        // Delete last '&' if it's the last character
        if (completeParams.charAt(completeParams.length() - 1) == '&') {
            completeParams.deleteCharAt(completeParams.length() - 1);
        }
    }

    /**
     * Decides which type of request to fire off based on the type stated.
     *
     * @return The response from the server
     * @throws IOException if the server wasn't reachable for some reason
     */
    public String fire() throws IOException {

        String res = "";
        switch (type) {
            case GET:
                res = fireGet();
            case PUT:
                res = firePut();
            case HEAD:
                res = fireHead();
            case POST:
                res = firePost();
            case POST_PARAMS:
                res = fireJsonPost(postBody);
            case TRACE:
                res = fireTrace();
            case CONNECT:
                res = fireConnect();
            case OPTIONS:
                res = fireOptions();
        }
        prevResponse = "";  // Clear previous response
        prevResponse = res;
        return res;
    }

    private String fireOptions() throws IOException {
        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request properties
        conn.setRequestMethod(type.toString());

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) {
            res.append(inLine);
        }
        in.close();
        return res.toString();
    }

    private String firePut() throws IOException {
        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String baseUrl = url.getProtocol() + "://" + url.getHost();

        FileReader fr = new FileReader(putFilePath);
        BufferedReader reader = new BufferedReader(fr);

        String line;
        StringBuilder contents = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            contents.append(line);
        }

        String data = contents.toString();


        // Set request properties
        conn.setRequestMethod(type.toString());
        conn.setRequestProperty("Host", baseUrl);
        conn.setRequestProperty("Content-type", "text/html");  // TODO MENTION THIS IN THE DOCS
        conn.setRequestProperty("Content-length", String.valueOf(data.getBytes().length));  // TODO test this
        conn.getOutputStream().write(data.getBytes("UTF-8"));

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) {
            res.append(inLine);
        }
        in.close();
        return res.toString();
    }

    private String fireHead() throws IOException {
        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request properties
        conn.setRequestMethod(type.toString());

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) res.append(inLine);
        in.close();
        return res.toString();
    }

    private String firePost() throws IOException {

        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request properties
        conn.setRequestMethod(type.toString());
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String urlParams = completeParams.toString();

        // Write params
        conn.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(urlParams);
        wr.flush();
        wr.close();

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) res.append(inLine);
        in.close();
        return res.toString();

    }

    private String fireJsonPost(String content) throws IOException {
        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod(type.toString());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.getOutputStream().write(content.getBytes("UTF-8"));

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) res.append(inLine);
        in.close();
        return res.toString();

    }

    private String fireTrace() throws IOException {
        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request properties
        conn.setRequestMethod(type.toString());

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) res.append(inLine);
        in.close();
        return res.toString();
    }


    private String fireConnect() throws IOException {

        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request properties
        conn.setRequestMethod(type.toString());
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) res.append(inLine);
        in.close();
        return res.toString();

    }

    private String fireGet() throws IOException {
        URL url = new URL(hostname);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request properties
        conn.setRequestMethod(type.toString());
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Send and read request
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine = null;
        StringBuffer res = new StringBuffer();

        while ((inLine = in.readLine()) != null) {
            res.append(inLine);
        }
        in.close();
        return res.toString();
    }

    /**
     * @return The most recent response received
     */
    public String getResponse() {
        return prevResponse;
    }

}
