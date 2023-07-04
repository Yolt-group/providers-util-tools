package com.yolt.clients.sites;


import com.yolt.clients.sites.pis.*;

import java.util.*;

import static com.yolt.clients.sites.ProvidersSites.RegisteredPisSite;
import static com.yolt.clients.sites.ProvidersSites.RegisteredPisSite.DynamicFieldName.*;
import static com.yolt.clients.sites.ProvidersSites.RegisteredSite;
import static com.yolt.clients.sites.pis.PaymentType.*;

class SitesMapper {

    public Site mapToSite(RegisteredSite registeredSite, List<RegisteredPisSite> pisDetails) {
        Site.SiteBuilder siteBuilder = Site.builder()
                .name(registeredSite.getName())
                .provider(registeredSite.getProviderKey())
                .groupingBy(registeredSite.getGroupingBy())
                .id(new Site.SiteId(registeredSite.getId()))
                .accountTypeWhitelist(registeredSite.getAccountTypeWhiteList())
                .availableInCountries(registeredSite.getAvailableCountries())
                .consentExpiryInDays(defaultValue(registeredSite.getConsentExpiryInDays(), 90))
                .consentBehavior(defaultValue(registeredSite.getConsentBehavior(), Collections.emptySet()))
                .externalId(registeredSite.getExternalId())
                .usesStepTypes(registeredSite.getUsesStepTypes());

        // TODO YCO-2055 Map actually supported frequencies once C4PO supports it
        var allFrequencies = Arrays.asList(Frequency.values());

        if (pisDetails == null) {
            return siteBuilder.build();
        }

        for (RegisteredPisSite pisSiteDetail : pisDetails) {
            var dynamicFields = fromMap(pisSiteDetail.getDynamicFields());

            switch (pisSiteDetail.getPaymentMethod()) {
                case SEPA -> {
                    switch (pisSiteDetail.getPaymentType()) {
                        case SINGLE -> siteBuilder.sepaSinglePaymentDetails(
                                new SepaSinglePaymentDetails(pisSiteDetail.isSupported(), SEPA_SINGLE, pisSiteDetail.isRequiresSubmitStep(), dynamicFields));
                        case PERIODIC -> siteBuilder.sepaPeriodicPaymentDetails(
                                new SepaPeriodicPaymentDetails(allFrequencies, pisSiteDetail.isSupported(), SEPA_PERIODIC, pisSiteDetail.isRequiresSubmitStep(), dynamicFields));
                        case SCHEDULED -> siteBuilder.sepaScheduledPaymentDetails(
                                new SepaScheduledPaymentDetails(pisSiteDetail.isSupported(), SEPA_SCHEDULED, pisSiteDetail.isRequiresSubmitStep(), dynamicFields));
                    }
                }
                case UKDOMESTIC -> {
                    switch (pisSiteDetail.getPaymentType()) {
                        case SINGLE -> siteBuilder.ukDomesticSinglePaymentDetails(
                                new UkDomesticSinglePaymentDetails(pisSiteDetail.isSupported(), SEPA_SINGLE, pisSiteDetail.isRequiresSubmitStep(), dynamicFields));
                        case PERIODIC -> siteBuilder.ukDomesticPeriodicPaymentDetails(
                                new UkDomesticPeriodicPaymentDetails(allFrequencies, pisSiteDetail.isSupported(), SEPA_PERIODIC, pisSiteDetail.isRequiresSubmitStep(), dynamicFields));
                        case SCHEDULED -> siteBuilder.ukDomesticScheduledPaymentDetails(
                                new UkDomesticScheduledPaymentDetails(pisSiteDetail.isSupported(), SEPA_SCHEDULED, pisSiteDetail.isRequiresSubmitStep(), dynamicFields));
                    }
                }
            }

        }
        return siteBuilder.build();

    }

    private <T> T defaultValue(T nullable, T defaultvalue) {
        return Objects.nonNull(nullable) ? nullable : defaultvalue;
    }

    private static DynamicFields fromMap(Map<RegisteredPisSite.DynamicFieldName, RegisteredPisSite.DynamicFieldOptions> dynamicFields) {
        if (dynamicFields == null || dynamicFields.isEmpty()) {
            return null;
        }

        return DynamicFields.builder()
                .remittanceInformationStructured(maybeDynamicField(dynamicFields, REMITTANCE_INFORMATION_STRUCTURED)
                        .map(RegisteredPisSite.DynamicFieldOptions::isRequired)
                        .map(DynamicFields.RemittanceInformationStructured::new)
                        .orElse(null))
                .debtorName(maybeDynamicField(dynamicFields, DEBTOR_NAME)
                        .map(RegisteredPisSite.DynamicFieldOptions::isRequired)
                        .map(DynamicFields.DebtorName::new)
                        .orElse(null))
                .creditorPostalCountry(maybeDynamicField(dynamicFields, CREDITOR_POSTAL_COUNTRY)
                        .map(RegisteredPisSite.DynamicFieldOptions::isRequired)
                        .map(DynamicFields.CreditorPostalCountry::new)
                        .orElse(null))
                .creditorPostalAddressLine(maybeDynamicField(dynamicFields, CREDITOR_POSTAL_ADDRESS_LINE)
                        .map(RegisteredPisSite.DynamicFieldOptions::isRequired)
                        .map(DynamicFields.CreditorPostalAddressLine::new)
                        .orElse(null))
                .creditorAgentName(maybeDynamicField(dynamicFields, CREDITOR_AGENT_NAME)
                        .map(RegisteredPisSite.DynamicFieldOptions::isRequired)
                        .map(DynamicFields.CreditorAgentName::new)
                        .orElse(null))
                .creditorAgentBic(maybeDynamicField(dynamicFields, CREDITOR_AGENT_BIC)
                        .map(RegisteredPisSite.DynamicFieldOptions::isRequired)
                        .map(DynamicFields.CreditorAgentBic::new)
                        .orElse(null))
                .build();
    }
    private static Optional<RegisteredPisSite.DynamicFieldOptions> maybeDynamicField(Map<RegisteredPisSite.DynamicFieldName, RegisteredPisSite.DynamicFieldOptions> dynamicFields,
                                                                                     RegisteredPisSite.DynamicFieldName field) {
        return Optional.ofNullable(dynamicFields.get(field));
    }

}
