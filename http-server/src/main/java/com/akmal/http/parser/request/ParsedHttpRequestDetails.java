package com.akmal.http.parser.request;

import com.akmal.http.HttpHeaders;
import com.akmal.http.HttpProtocol;
import com.akmal.http.QueryParameters;
import com.akmal.http.router.HttpMethod;

public record ParsedHttpRequestDetails(
    HttpMethod method,
    HttpProtocol protocol,
    String path,
    HttpHeaders headers,
    QueryParameters queryParams
) {

}
