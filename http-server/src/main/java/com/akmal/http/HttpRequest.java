package com.akmal.http;

public record HttpRequest(
    String url,
    HttpHeaders headers
) {

}
