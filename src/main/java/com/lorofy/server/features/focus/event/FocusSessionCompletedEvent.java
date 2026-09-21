package com.lorofy.server.features.focus.event;

import java.util.UUID;

public record FocusSessionCompletedEvent(
    UUID profileId,
    String username,
    String displayName,
    String avatarUrl,
    String countryCode,
    int earnedPoints
) {}
