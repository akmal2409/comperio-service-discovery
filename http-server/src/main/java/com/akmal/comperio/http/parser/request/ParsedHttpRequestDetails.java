package com.akmal.comperio.http.parser.request;

import com.akmal.comperio.http.HttpHeaders;
import com.akmal.comperio.http.HttpProtocol;
import com.akmal.comperio.http.QueryParameters;
import com.akmal.comperio.http.router.HttpMethod;

public record ParsedHttpRequestDetails(
    HttpMethod method,
    HttpProtocol protocol,
    String path,
    HttpHeaders headers,
    QueryParameters queryParams
) {

}
