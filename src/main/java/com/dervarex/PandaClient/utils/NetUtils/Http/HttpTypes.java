package com.dervarex.PandaClient.utils.NetUtils.Http;

/**
 * Enum for using various http types (GET, POST, etc.)
 * <p>
 * <b>NOTE: PATCH &amp; DELETE requests are <i>not</i> supported</b>
 * </p>
 */
public enum HttpTypes {
    GET("GET"),
    POST("POST"),
    POST_PARAMS("POST"),
    HEAD("HEAD"),
    PUT("PUT"),
    TRACE("TRACE"),
    OPTIONS("OPTIONS"),
    CONNECT("CONNECT");

    private final String val;

    private HttpTypes(String s) {
        val = s;
    }

    public boolean equalsName(String otherName) {
        return val.equals(otherName);
    }

    public String toString() {
        return this.val;
    }
}
