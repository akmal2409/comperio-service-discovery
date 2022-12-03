package com.akmal.http.server;

import com.akmal.http.HttpHeaders;
import com.akmal.http.HttpProtocol;
import com.akmal.http.HttpStatus;
import com.akmal.http.MediaType;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(access = AccessLevel.PACKAGE)
@Getter
public class HttpResponse {
  private static final String SEPARATOR = "\r\n";

  private HttpProtocol httpProtocol = HttpProtocol.HTTP_V1;
  @Setter private HttpStatus status = HttpStatus.OK;
  private String server;
  @Setter private String contentType = MediaType.TEXT_PLAIN;
  private Instant timestamp = Instant.now();
  private final OutputStream outputStream;
  private final HttpHeaders headers = new HttpHeaders(new HashMap<>());

  /**
   * Prior to returning the output stream it tries to write the header.
   * Therefore, make sure that you set the header properties prior to invoking this method!.
   *
   * @return outputStream
   */
  public OutputStream getOutputStream() {
    final var writer = new PrintWriter(this.outputStream);
    writer.write(this.httpProtocol.getName() + " " + this.status.value() + " " + this.status.getReasonPhrase());
    writer.write(SEPARATOR);

    if (this.server != null) {
      writer.write("Server: " + this.server);
      writer.write(SEPARATOR);
    }

    if (this.contentType != null) {
      writer.write("Content-Type: " + this.contentType);
      writer.write(SEPARATOR);
    }

    if (this.timestamp== null) this.timestamp = Instant.now();
    writer.write("Date: " + this.timestamp.toString());
    writer.write(SEPARATOR);

    for (Map.Entry<String, String> header: this.headers.headers().entrySet()) {
      writer.write(header.getKey() + ": " + header.getValue());
      writer.write(SEPARATOR);
    }

    writer.write(SEPARATOR);
    writer.flush();
    return outputStream;
  }
}
