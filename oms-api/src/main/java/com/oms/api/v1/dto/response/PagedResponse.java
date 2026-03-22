package com.oms.api.v1.dto.response;

import java.util.List;

public record PagedResponse<T>(
    List<T> data,
    String nextCursor,
    int limit
) {}
