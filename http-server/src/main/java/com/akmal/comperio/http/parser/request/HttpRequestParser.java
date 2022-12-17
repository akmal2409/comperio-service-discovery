package com.akmal.comperio.http.parser.request;

import java.io.IOException;
import java.io.InputStream;

public interface HttpRequestParser {

  ParsedHttpRequestDetails parse(InputStream in) throws IOException;
}
