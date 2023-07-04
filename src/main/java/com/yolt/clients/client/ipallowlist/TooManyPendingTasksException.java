package com.yolt.clients.client.ipallowlist;

import java.util.UUID;

public class TooManyPendingTasksException extends RuntimeException {

    public TooManyPendingTasksException(UUID clientId) {
        super("client with id [" + clientId + "] exceeded the amount of allowed tasks to be created per day");
    }
}
