package com.yolt.clients.sites;

import com.google.common.collect.ImmutableMap;
import com.yolt.clients.sites.ais.ConsentBehavior;
import com.yolt.clients.sites.ais.LoginRequirement;
import com.yolt.clients.sites.pis.*;
import lombok.NonNull;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.*;

public class SiteCreatorUtil {

    public static final List<AccountType> CURRENT_CREDIT_SAVINGS = List.of(AccountType.CURRENT_ACCOUNT, AccountType.CREDIT_CARD, AccountType.SAVINGS_ACCOUNT);
    public static final Map<ServiceType, List<LoginRequirement>> AIS_WITH_REDIRECT_STEPS = ImmutableMap.of(ServiceType.AIS, Collections.singletonList(LoginRequirement.REDIRECT));


    /**
     * Method to create a site with required args.
     */
    public static Site createTestSite(UUID id, String name, String provider, List<AccountType> accountTypeWhitelist, List<CountryCode> availableCountries, Map<ServiceType, List<LoginRequirement>> usesStepTypes) {
        return Site.builder().id(new Site.SiteId(id)).name(name).provider(provider).accountTypeWhitelist(accountTypeWhitelist).availableInCountries(availableCountries)
                .usesStepTypes(usesStepTypes)
                .build();
    }

    /**
     * Method to create a site with required args.
     */
    public static Site create(@NonNull UUID id, @NonNull String name, @NonNull String provider, @NonNull List<AccountType> accountTypeWhitelist, @NonNull List<CountryCode> availableInCountries,
                                      @NonNull Map<ServiceType, List<LoginRequirement>> usesStepTypes, String groupingBy, String externalId, Integer consentExpiryInDays, Set<ConsentBehavior> consentBehavior,
                                      SepaSinglePaymentDetails sepaSinglePaymentDetails, SepaScheduledPaymentDetails sepaScheduledPaymentDetails, SepaPeriodicPaymentDetails sepaPeriodicPaymentDetails,
                                      UkDomesticSinglePaymentDetails ukDomesticSinglePaymentDetails, UkDomesticPeriodicPaymentDetails ukDomesticPeriodicPaymentDetails, UkDomesticScheduledPaymentDetails ukDomesticScheduledPaymentDetails) {
        return  Site.builder()
                .id(new Site.SiteId(id))
                .name(name)
                .provider(provider)
                .groupingBy(groupingBy)
                .accountTypeWhitelist(accountTypeWhitelist)
                .consentBehavior(consentBehavior)
                .consentExpiryInDays(consentExpiryInDays)
                .externalId(externalId)
                .usesStepTypes(usesStepTypes)
                .availableInCountries(availableInCountries)
                .sepaSinglePaymentDetails(sepaSinglePaymentDetails)
                .sepaPeriodicPaymentDetails(sepaPeriodicPaymentDetails)
                .sepaScheduledPaymentDetails(sepaScheduledPaymentDetails)
                .ukDomesticSinglePaymentDetails(ukDomesticSinglePaymentDetails)
                .ukDomesticPeriodicPaymentDetails(ukDomesticPeriodicPaymentDetails)
                .ukDomesticScheduledPaymentDetails(ukDomesticScheduledPaymentDetails)
                .build();
    }
}
