package com.yolt.clients.events.sema;

import com.yolt.clients.client.creditoraccounts.AccountIdentifierSchemeEnum;
import lombok.Builder;
import net.logstash.logback.marker.Markers;
import nl.ing.lovebird.logging.SemaEvent;
import org.slf4j.Marker;

import java.util.UUID;

/**
 * Path to this class is configured in ElastAlert in k8s-manifest-privileged repository.
 * Changing path requires changes in k8s-manifest-privileged.
 * @see <a href="https://git.yolt.io/deployment/k8s-manifests-privileged/-/blob/master/base/elastalert/base/rules/app-ciso-30-cashflow-analyser-invalid-signature.yml">k8s-manifest-privileged configuration</a>
 */
@Builder
public class CreditorAccountSemaEvent implements SemaEvent {

    private final String message;
    private final UUID clientId;
    private final String accountHolderName;
    private final String accountNumber;
    private final AccountIdentifierSchemeEnum accountIdentifierScheme;
    private final String secondaryIdentification;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Marker getMarkers() {
        return Markers.append("clientId", clientId)
                .and(Markers.append("accountHolderName", accountHolderName))
                .and(Markers.append("accountNumber", accountNumber))
                .and(Markers.append("accountIdentifierScheme", accountIdentifierScheme.name()))
                .and(Markers.append("secondaryIdentification", secondaryIdentification));
    }
}
