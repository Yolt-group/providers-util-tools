package com.yolt.clients.client.mtlsdn;

import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDN;
import com.yolt.clients.jira.AbstractJiraData;

import java.util.function.Function;

public class JiraDistinguishedNameData extends AbstractJiraData<ClientMTLSCertificateDN, JiraDistinguishedNameData> {
    protected JiraDistinguishedNameData() {
        super(JiraDistinguishedNameData.class);
    }

    @Override
    public Function<ClientMTLSCertificateDN, String> getMapperToDescription() {
        return clientMTLSCertificateDN -> """
                %s
                  issued by:
                  %s
                """.formatted(clientMTLSCertificateDN.getSubjectDN(), clientMTLSCertificateDN.getIssuerDN());
    }

    @Override
    public String getSummary() {
        return "Request for updating the distinguished names allowlist";
    }

    @Override
    public String getDescription(
            String toBeAddedList,
            String toBeEditedList,
            String toBeRemovedList
    ) {
        return """
                Add the following distinguished names to the allowlist:
                %s
                                
                Remove the following distinguished names from the allowlist:
                %s
                """.formatted(toBeAddedList, toBeRemovedList);
    }

    @Override
    public String getComment() {
        return """
                The following distinguished names have been handled:
                %s
                """;
    }
}
