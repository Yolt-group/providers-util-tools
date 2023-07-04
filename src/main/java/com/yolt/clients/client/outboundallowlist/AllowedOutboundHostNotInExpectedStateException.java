package com.yolt.clients.client.outboundallowlist;

import com.yolt.clients.jira.Status;

import java.util.Collection;

public class AllowedOutboundHostNotInExpectedStateException extends RuntimeException {
    public AllowedOutboundHostNotInExpectedStateException(Collection<Status> expected, Status actual) {
        super("expected the status of AllowedOutboundHost to be in: %s but was: %s".formatted(expected, actual));
    }
}
