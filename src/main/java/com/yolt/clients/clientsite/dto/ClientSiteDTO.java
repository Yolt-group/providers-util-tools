package com.yolt.clients.clientsite.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yolt.clients.sites.CountryCode;
import com.yolt.clients.sites.ais.ConsentBehavior;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import nl.ing.lovebird.providerdomain.AccountType;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ClientSiteEntity", description = "A site can be a bank or a creditcard company, etc.")
public class ClientSiteDTO {

    private UUID id;
    private String name;
    private List<AccountType> supportedAccountTypes;

    /**
     * @deprecated To see which 'login steps' are required. See {@link #services}.
     * To see whether it's scraper or not, see {@link #connectionType}
     * As of YCO-1236 this field is also hidden in the documentation on our developer portal. The field is still mapped
     * for backwards compatability.
     */
    @Deprecated
    @Schema(hidden = true)
    private LoginType loginType;
    @Schema(required = true, description = "Indicates whether this is a direct connection or through a scraper.")
    private ConnectionType connectionType;
    private Services services;
    private String groupingBy;
    @ArraySchema(arraySchema = @Schema(description = "The list of tags added to the site. This can be used to retrieve a specific site list and the tags are controlled by the client."))
    private List<String> tags;
    @Schema(description = "A flag which marks the site available for control by the client.")
    private boolean available;
    @Schema(description = "A flag which enables or disables the site, and it is controlled by the client.")
    private boolean enabled;
    @Schema(description = "A flag which enables or disables experimental version of site and it is controlled by the client.")
    private boolean useExperimentalVersion;
    private List<CountryCode> availableInCountries;
    /**
     * As of YCO-1236 this field is deprecated and should be hidden from the documentation on our developer portal.
     * New clients should not rely on the contents of this field anymore because the contents are unreliable at best.
     */
    @Deprecated
    @Schema(hidden = true)
    private SiteConnectionHealthStatus health;
    @Schema(required = true, description = "If set to true, this site is no longer supported.")
    private Boolean noLongerSupported;
    @Schema(required = true, example = "/content/images/sites/icons/96da1236-1bf7-45e0-b87d-1fd8e264255b.png")
    private final String iconLink;

    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "The services of the site. All are nullable, however, at least 1 of them is not-null")
    public static class Services {
        // Conditional: either ais or pis is not-null
        @Nullable
        @Schema(required = false)
        AIS ais;
        @Schema(required = false)
        @Nullable
        PIS pis;

        @Value
        @Schema(description = "AIS, Account Information Service.")
        public static class AIS {
            @NotNull
            @Schema(required = true)
            Onboarded onboarded;
            @NotNull
            @Schema(description = "If the authorization process involves a redirect this is true.", required = true)
            boolean hasRedirectSteps;
            @NotNull
            @Schema(description = "If the authorization process involves a form step this is true. (usually false, but true for" +
                    " scrapers, or more complicated banks.", required = true)
            boolean hasFormSteps;
            @ArraySchema(arraySchema = @Schema(description = "Informational field describing how a site handles consents.  " +
                    "Possible values: " +
                    "CONSENT_PER_ACCOUNT = at most one account can be linked to a user-site"))
            SortedSet<ConsentBehavior> consentBehavior;
        }

        @Value
        @Schema(description = "PIS, Payment Initiation Service.")
        public static class PIS {
            @NotNull
            @Schema(required = true)
            Onboarded onboarded;
            @NotNull
            @Schema(description = "If the authorization process involves a redirect this is true.", required = true)
            boolean hasRedirectSteps;
            @NotNull
            @Schema(description = "If the authorization process involves a form step this is true. (usually false, but true for" +
                    " scrapers, or more complicated banks.", required = true)
            boolean hasFormSteps;

            @JsonInclude(JsonInclude.Include.NON_NULL)
            PaymentDetailsDTO sepaSingle;
            @JsonInclude(JsonInclude.Include.NON_NULL)
            PaymentDetailsDTO ukDomesticSingle;
            @Hidden
            PeriodicPaymentDetailsDTO sepaPeriodic;
            @Hidden
            PeriodicPaymentDetailsDTO ukDomesticPeriodic;
            @Hidden
            PaymentDetailsDTO sepaScheduled;
            @Hidden
            PaymentDetailsDTO ukDomesticScheduled;
        }

        @Value
        @Schema(description = "Describes whether the client is onboarded for this particular service on this site.")
        public static class Onboarded {
            Set<UUID> redirectUrlIds;
            boolean client;
        }
    }
}
