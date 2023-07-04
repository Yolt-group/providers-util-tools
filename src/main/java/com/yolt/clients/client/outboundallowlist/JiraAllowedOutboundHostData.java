package com.yolt.clients.client.outboundallowlist;

import com.yolt.clients.jira.AbstractJiraData;

import java.util.function.Function;

public class JiraAllowedOutboundHostData extends AbstractJiraData<AllowedOutboundHost, JiraAllowedOutboundHostData> {
    private static final String HOST_SUMMARY = "Request for adding/removing Outbound Hosts to allowlist";

    public JiraAllowedOutboundHostData() {
        super(JiraAllowedOutboundHostData.class);
    }

    @Override
    public Function<AllowedOutboundHost, String> getMapperToDescription() {
        return AllowedOutboundHost::getHost;
    }

    @Override
    public String getSummary() {
        return HOST_SUMMARY;
    }

    @Override
    public String getDescription(
            String toBeAddedList,
            String toBeEditedList,
            String toBeRemovedList
    ) {
        return """
                Add the following outbound hosts to the allowlist:
                %s
                remove the following outbound hosts from the allowlist:
                %s
                """.formatted(toBeAddedList, toBeRemovedList);
    }

    @Override
    public String getComment() {
        return """
                The following hosts have been handled:
                %s
                """;
    }

}
