package com.akmal.http.parser.request;

import com.akmal.http.server.HttpRequest;
import java.io.IOException;
import java.io.InputStream;

public interface HttpRequestParser {

  ParsedHttpRequestDetails parse(InputStream in) throws IOException;
}
