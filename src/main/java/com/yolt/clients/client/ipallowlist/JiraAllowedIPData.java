package com.yolt.clients.client.ipallowlist;

import com.yolt.clients.client.redirecturls.Action;
import com.yolt.clients.jira.AbstractJiraData;

import java.util.function.Function;

public class JiraAllowedIPData extends AbstractJiraData<AllowedIP, JiraAllowedIPData> {

    private static final String IP_SUMMARY = "Request for %s IPs from/to the allowlist";

    private final Action action;

    public JiraAllowedIPData(Action action) {
        super(JiraAllowedIPData.class);
        this.action = action;
    }

    @Override
    public Function<AllowedIP, String> getMapperToDescription() {
        return AllowedIP::getCidr;
    }

    @Override
    public String getSummary() {
        return switch (action) {
            case CREATE -> IP_SUMMARY.formatted("adding");
            case UPDATE -> IP_SUMMARY.formatted("updating");
            case DELETE -> IP_SUMMARY.formatted("removing");
        };
    }

    @Override
    public String getDescription(
            String toBeAddedList,
            String toBeEditedList,
            String toBeRemovedList
    ) {
        return """
                Add the following IPs to the allowlist:
                %s
                                
                Remove the following IPs from the allowlist:
                %s
                """.formatted(toBeAddedList, toBeRemovedList);
    }

    @Override
    public String getComment() {
        return """
                The following IPs have been handled:
                %s
                """;
    }
}
