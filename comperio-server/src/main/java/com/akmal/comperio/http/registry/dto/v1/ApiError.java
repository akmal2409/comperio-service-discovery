package com.akmal.comperio.http.registry.dto.v1;

import java.time.Instant;

public record ApiError(
    String message,
    Instant timestamp,
    String errorCode
) {

}
