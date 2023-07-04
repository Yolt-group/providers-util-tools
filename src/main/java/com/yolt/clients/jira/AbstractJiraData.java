package com.yolt.clients.jira;

import lombok.Getter;

import java.util.*;
import java.util.function.Function;

@Getter
public abstract class AbstractJiraData<T, U extends AbstractJiraData<T, ?>> {
    private final Set<T> itemsToBeAdded;
    private final Set<T> itemsToBeRemoved;
    private final Set<T> itemsToBeEdited;
    private final Set<T> itemsToBeProcessed; // all items together
    private final Map<String, Set<T>> jiraTicketsToBeEdited;
    private final Class<U> type;

    protected AbstractJiraData(Class<U> type) {
        itemsToBeAdded = new LinkedHashSet<>();
        itemsToBeRemoved = new LinkedHashSet<>();
        itemsToBeEdited = new LinkedHashSet<>();
        itemsToBeProcessed = new LinkedHashSet<>();
        jiraTicketsToBeEdited = new HashMap<>();
        this.type = type;
    }

    public U withItemToBeAdded(T item) {
        itemsToBeAdded.add(item);
        itemsToBeProcessed.add(item);
        return type.cast(this);
    }

    public U withItemToBeRemoved(T item) {
        itemsToBeRemoved.add(item);
        itemsToBeProcessed.add(item);
        return type.cast(this);
    }

    public U withItemToBeEdited(T item) {
        itemsToBeEdited.add(item);
        itemsToBeProcessed.add(item);
        return type.cast(this);
    }

    public U withJiraTicketToBeEdited(String ticket, T item) {
        Set<T> items = jiraTicketsToBeEdited.computeIfAbsent(ticket, $ -> new HashSet<>());
        items.add(item);
        return type.cast(this);
    }

    public boolean contains(T item) {
        return itemsToBeProcessed.stream().anyMatch(item::equals);
    }

    public abstract Function<T, String> getMapperToDescription();

    public abstract String getSummary();

    public abstract String getDescription(
            String toBeAddedList,
            String toBeEditedList,
            String toBeRemovedList
    );

    public abstract String getComment();
}
