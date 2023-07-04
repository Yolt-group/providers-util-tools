package com.yolt.clients.client.ipallowlist;

import com.yolt.clients.jira.Status;

import java.util.Collection;

public class AllowedIPNotInExpectedStateException extends RuntimeException {
    public AllowedIPNotInExpectedStateException(Collection<Status> expected, Status actual) {
        super("expected the status of AllowedIP to be in: %s but was: %s".formatted(expected, actual));
    }
}
