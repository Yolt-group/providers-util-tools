package com.yolt.clients.sites;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yolt.clients.sites.ais.ConsentBehavior;
import com.yolt.clients.sites.ais.LoginRequirement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
class ProvidersSites {

    List<RegisteredSite> aisSiteDetails;

    List<RegisteredPisSite> pisSiteDetails;

    @Value
    static class RegisteredPisSite {
        UUID id;
        String providerKey;
        boolean supported;
        ProvidersPaymentType paymentType;
        Map<DynamicFieldName, DynamicFieldOptions> dynamicFields;
        boolean requiresSubmitStep;
        PaymentMethod paymentMethod;
        List<LoginRequirement> loginRequirements;

        public enum PaymentMethod {
            SEPA,
            UKDOMESTIC
        }

        public enum ProvidersPaymentType {
            SINGLE,
            PERIODIC,
            SCHEDULED
        }

        public enum DynamicFieldName {

            CREDITOR_AGENT_BIC("creditorAgentBic"),
            CREDITOR_AGENT_NAME("creditorAgentName"),
            REMITTANCE_INFORMATION_STRUCTURED("remittanceInformationStructured"),
            CREDITOR_POSTAL_ADDRESS_LINE("creditorPostalAddressLine"),
            CREDITOR_POSTAL_COUNTRY("creditorPostalCountry"),
            DEBTOR_NAME("debtorName");

            @Getter
            private final String value;

            DynamicFieldName(String value) {
                this.value = value;
            }
        }

        @Value
        public static class DynamicFieldOptions {
            boolean required;
        }
    }

    @Value
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RegisteredSite {

        /**
         * Name of the site, this is shown to an end-user and is thus language dependent.
         */
        String name;
        String providerKey;
        /**
         * Used to indicate that several sites form a "group".
         */
        String groupingBy;
        /**
         * Unique identifier of a site.  Do **NOT** ever change this.  Generate a new one using for instance `uuidgen`.
         */
        UUID id;
        /**
         * What account types are supported for this site
         */
        List<AccountType> accountTypeWhiteList;
        /**
         * In what countries is the site available?  Believe this is only cosmetic, no logic based on this.
         */
        List<CountryCode> availableCountries;
        /**
         * Once a user gives us consent to view their data, for how long does that consent remain valid?
         * <p>
         * Note: values for this are all over the place, don't trust it.
         */
        Integer consentExpiryInDays; // default to 90
        /**
         * Does the bank only permit a user to give consent to a single account per user site?
         */
        Set<ConsentBehavior> consentBehavior; // default empty list
        /**
         * Only relevant for scraping sites.  Identifies the bank with which the scraping party must connect.
         */
        String externalId;
        /**
         * Indicates what type of steps are necessary *per* {@link ServiceType}.
         * Deprecated and will be removed as part of C4PO-8806.
         * We should add and use the List<LoginRequirement> loginRequirements field instead.
         * See YCO-1679 for more details
         */
        @Deprecated
        Map<ServiceType, List<LoginRequirement>> usesStepTypes;
    }

}
