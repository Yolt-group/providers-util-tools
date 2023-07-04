package com.yolt.clients.client.redirecturls.jira;

import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogEntry;
import com.yolt.clients.jira.AbstractJiraData;

import java.util.function.Function;

public class RedirectURLChangelogJiraData extends AbstractJiraData<RedirectURLChangelogEntry, RedirectURLChangelogJiraData> {

    private static final String SUMMARY = "Request for updating the redirect URLs for licensed client.";

    public RedirectURLChangelogJiraData() {
        super(RedirectURLChangelogJiraData.class);
    }

    @Override
    public Function<RedirectURLChangelogEntry, String> getMapperToDescription() {
        return changelogEntry -> {
            var url = changelogEntry.getRedirectURL() != null ? changelogEntry.getRedirectURL() : changelogEntry.getNewRedirectURL();
            return switch (changelogEntry.getAction()) {
                case CREATE -> "create url with id: %s, and url: %s (req id: %s)".formatted(changelogEntry.getRedirectURLId(), url, changelogEntry.getId());
                case UPDATE -> "update url with id: %s, from: %s, to: %s (req id: %s)".formatted(changelogEntry.getRedirectURLId(), changelogEntry.getRedirectURL(), changelogEntry.getNewRedirectURL(), changelogEntry.getId());
                case DELETE -> "delete url with id: %s, and url: %s (req id: %s)".formatted(changelogEntry.getRedirectURLId(), url, changelogEntry.getId());
            };
        };
    }

    @Override
    public String getSummary() {
        return SUMMARY;
    }

    @Override
    public String getDescription(
            String toBeAddedList,
            String toBeEditedList,
            String toBeRemovedList
    ) {
        return """
                Add the following redirect URLs:
                %s
                            
                Update the following redirect URLs:
                %s
                            
                Delete the following redirect URLs:
                %s
                """.formatted(toBeAddedList, toBeEditedList, toBeRemovedList);
    }

    @Override
    public String getComment() {
        return """
                The following redirect URL change requests have been handled:
                %s
                """;
    }
}
