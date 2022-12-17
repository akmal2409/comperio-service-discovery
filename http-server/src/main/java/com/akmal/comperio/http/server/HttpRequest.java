package com.akmal.comperio.http.server;

import com.akmal.comperio.http.HttpHeaders;
import com.akmal.comperio.http.HttpProtocol;
import com.akmal.comperio.http.QueryParameters;
import com.akmal.comperio.http.router.HttpMethod;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Map;

public record HttpRequest(
    HttpMethod method,
    HttpProtocol protocol,
    String path,
    InetAddress remoteAddress,
    HttpHeaders headers,
    QueryParameters queryParams,
    InputStream inputStream,
    Map<String, String> variables
) {

}
