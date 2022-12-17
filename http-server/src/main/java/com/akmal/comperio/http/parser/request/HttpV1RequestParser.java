package com.akmal.comperio.http.parser.request;

import com.akmal.comperio.http.HttpHeaders;
import com.akmal.comperio.http.HttpProtocol;
import com.akmal.comperio.http.HttpStatus;
import com.akmal.comperio.http.QueryParameters;
import com.akmal.comperio.http.exception.http.MalformedRequestException;
import com.akmal.comperio.http.router.HttpMethod;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpV1RequestParser implements HttpRequestParser {
  private static final Pattern HEADER_SEPARATOR = Pattern.compile(":\\s");
  private static final long MAX_CLIENT_SIZE_BYTES = 10240;

  @Override
  public ParsedHttpRequestDetails parse(InputStream in) throws IOException {
    final var reader =  new InputStreamReader(in, StandardCharsets.UTF_8); // not using buffered reader because it will consume the data from the stream too.
    List<String> headerLines = readHeaderLines(reader);

    if (headerLines.isEmpty()) {
      throw new MalformedRequestException(HttpStatus.BAD_REQUEST, "Malformed HTTP request");
    }
    System.out.println("AVVV " + in.available());
    String[] requestLine = headerLines.get(0).split("\s");

    if (requestLine.length != 3) {
      throw new MalformedRequestException(
          HttpStatus.BAD_REQUEST, "Malformed HTTP request. Could not parse method, path and protocol");
    }
    HttpMethod method = HttpMethod.valueOf(requestLine[0].toUpperCase());
    String path = requestLine[1];
    HttpProtocol protocol = HttpProtocol.fromName(requestLine[2]);

    if (protocol == null) {
      throw new MalformedRequestException(HttpStatus.BAD_REQUEST, "Malformed HTTP request. Unknown HTTP protocol");
    }

    final var headers = this.parseHeaders(headerLines.subList(1, headerLines.size()));
    Map<String, String> queryParams = Collections.emptyMap();

    // now we need to remove the query string from the path;
    final var querySeparatorIndex = path.indexOf('?');
    if (querySeparatorIndex != -1 && querySeparatorIndex != path.length() - 1) {
      final var queryString = path.substring(querySeparatorIndex + 1);
      queryParams = parseQueryParams(queryString);
      path = path.substring(0, querySeparatorIndex);
    }


    return new ParsedHttpRequestDetails(method, protocol, path, new HttpHeaders(headers), new QueryParameters(queryParams));
  }


  private Map<String, String> parseQueryParams(String queryString) {
    final var parts = queryString.split("&");
    final var map = new HashMap<String, String>();

    for (String queryParam: parts) {
      final var queryParamParts = queryParam.split("=");
      if (queryParamParts.length == 0 || queryParamParts[0].isBlank()) throw new MalformedRequestException(HttpStatus.BAD_REQUEST, "Malformed HTTP request. Corrupted query param " + queryParamParts);

      map.put(queryParamParts[0], queryParamParts.length > 1 ? queryParamParts[1] : "");
    }

    return map;
  }

  private Map<String, String> parseHeaders(List<String> lines) {
    final var headers = new HashMap<String, String>();

    for (String line : lines) {
      String[] parts = HEADER_SEPARATOR.split(line);
      if (parts.length != 2) {
        throw new MalformedRequestException(HttpStatus.BAD_REQUEST, "Malformed HTTP request. Corrupted header " + line);
      }

      headers.put(normalizeHeaderName(parts[0]), parts[1]);
    }

    return headers;
  }

  private String normalizeHeaderName(String header) {
    if (header.length() <= 1) return header.toUpperCase();

    String[] parts = header.split("-");

    for (int i = 0; i < parts.length; i++) {
      parts[i] = parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1);
    }

    return String.join("-", parts);
  }

  /**
   * Reads HTTP header up until the body (i.e. blank line)
   *
   * @param reader
   * @return
   */
  private List<String> readHeaderLines(Reader reader) throws IOException {
    List<String> lines = new ArrayList<>();
    String line;

    StringBuilder sb = new StringBuilder();

    int ch;

    while ((ch = reader.read()) != -1) {
      sb.appendCodePoint(ch);

      if (sb.length() > 4 && ((sb.charAt(sb.length() - 1) == '\n' && sb.charAt(sb.length() - 3) == '\n')
                                  || sb.charAt(sb.length() - 1) == '\n' && sb.charAt(sb.length() - 2) == '\n')) break; // end of headers
    }

//    while ((line = reader.read()) != null && !line.isBlank()) {
//      lines.add(line);
//    }

//    return lines;

    return Arrays.asList(sb.toString().split("\r\n"));
  }
}
