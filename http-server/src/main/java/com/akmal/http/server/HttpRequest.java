package com.akmal.http.server;

import com.akmal.http.HttpHeaders;
import com.akmal.http.HttpProtocol;
import com.akmal.http.QueryParameters;
import com.akmal.http.router.HttpMethod;
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
