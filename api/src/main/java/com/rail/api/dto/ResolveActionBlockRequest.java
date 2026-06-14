package com.rail.api.dto;

import java.util.List;
import java.util.Map;

public record ResolveActionBlockRequest(
    String tappedItemId,
    List<Map<String, Object>> resolvedItems
) {}
