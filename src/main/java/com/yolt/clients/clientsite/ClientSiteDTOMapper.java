package com.yolt.clients.clientsite;

import com.yolt.clients.clientsite.dto.*;
import com.yolt.clients.sites.Site;
import com.yolt.clients.sites.ais.LoginRequirement;
import com.yolt.clients.sites.pis.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
class ClientSiteDTOMapper {

    private final String siteIconLink;

    ClientSiteDTOMapper(@Value("${route.content.site-icons}") final String siteIconLink) {
        this.siteIconLink = siteIconLink;
    }

    ClientSiteDTO mapClientSiteDTO(ClientSite clientSite) {
        Site site = clientSite.getSite();

        UUID siteId = site.getId().unwrap();

        List<String> tags = new ArrayList<>(clientSite.getTags());

        ClientSiteDTO.Services.AIS ais = null;
        ClientSiteDTO.Services.PIS pis = null;
        if (site.getServices().contains(ServiceType.AIS)) {
            ais = new ClientSiteDTO.Services.AIS(getOnboarded(clientSite, ServiceType.AIS),
                    site.getUsesStepTypes().getOrDefault(ServiceType.AIS, Collections.emptyList()).contains(LoginRequirement.REDIRECT),
                    site.getUsesStepTypes().getOrDefault(ServiceType.AIS, Collections.emptyList()).contains(LoginRequirement.FORM),
                    new TreeSet<>(site.getConsentBehavior())
            );
        }
        if (site.getServices().contains(ServiceType.PIS)) {
            PaymentDetailsDTO sepaSingleDetails = null;
            SepaSinglePaymentDetails sepaSinglePaymentDetails = site.getSepaSinglePaymentDetails();
            if (sepaSinglePaymentDetails != null) {
                sepaSingleDetails = new PaymentDetailsDTO(sepaSinglePaymentDetails.isSupported(), mapToDynamicFieldsDTO(sepaSinglePaymentDetails.getDynamicFields()));
            }

            PeriodicPaymentDetailsDTO sepaPeriodicDetails = null;
            SepaPeriodicPaymentDetails sepaPeriodicPaymentDetails = site.getSepaPeriodicPaymentDetails();
            if (sepaPeriodicPaymentDetails != null) {
                sepaPeriodicDetails = new PeriodicPaymentDetailsDTO(sepaPeriodicPaymentDetails.isSupported(), mapToDynamicFieldsDTO(sepaPeriodicPaymentDetails.getDynamicFields()), sepaPeriodicPaymentDetails.getSupportedFrequencies());
            }

            PaymentDetailsDTO sepaScheduledDetails = null;
            SepaScheduledPaymentDetails sepaScheduledPaymentDetails = site.getSepaScheduledPaymentDetails();
            if (sepaScheduledPaymentDetails != null) {
                sepaScheduledDetails = new PaymentDetailsDTO(sepaScheduledPaymentDetails.isSupported(), mapToDynamicFieldsDTO(sepaScheduledPaymentDetails.getDynamicFields()));
            }

            PaymentDetailsDTO ukDomesticSingleDetails = null;
            UkDomesticSinglePaymentDetails ukDomesticSinglePaymentDetails = site.getUkDomesticSinglePaymentDetails();
            if (ukDomesticSinglePaymentDetails != null) {
                ukDomesticSingleDetails = new PaymentDetailsDTO(ukDomesticSinglePaymentDetails.isSupported(), mapToDynamicFieldsDTO(ukDomesticSinglePaymentDetails.getDynamicFields()));
            }

            PeriodicPaymentDetailsDTO ukDomesticPeriodicDetails = null;
            UkDomesticPeriodicPaymentDetails ukDomesticPeriodicPaymentDetails = site.getUkDomesticPeriodicPaymentDetails();
            if (ukDomesticPeriodicPaymentDetails != null) {
                ukDomesticPeriodicDetails = new PeriodicPaymentDetailsDTO(ukDomesticPeriodicPaymentDetails.isSupported(), mapToDynamicFieldsDTO(ukDomesticPeriodicPaymentDetails.getDynamicFields()), ukDomesticPeriodicPaymentDetails.getSupportedFrequencies());
            }

            PaymentDetailsDTO ukDomesticScheduledDetails = null;
            UkDomesticScheduledPaymentDetails ukDomesticScheduledPaymentDetails = site.getUkDomesticScheduledPaymentDetails();
            if (ukDomesticScheduledPaymentDetails != null) {
                ukDomesticScheduledDetails = new PaymentDetailsDTO(ukDomesticScheduledPaymentDetails.isSupported(), mapToDynamicFieldsDTO(ukDomesticScheduledPaymentDetails.getDynamicFields()));
            }


            pis = new ClientSiteDTO.Services.PIS(getOnboarded(clientSite, ServiceType.PIS),
                    site.getUsesStepTypes().getOrDefault(ServiceType.PIS, Collections.emptyList()).contains(LoginRequirement.REDIRECT),
                    site.getUsesStepTypes().getOrDefault(ServiceType.PIS, Collections.emptyList()).contains(LoginRequirement.FORM),
                    sepaSingleDetails, ukDomesticSingleDetails, sepaPeriodicDetails, ukDomesticPeriodicDetails, sepaScheduledDetails, ukDomesticScheduledDetails);
        }
        return new ClientSiteDTO(
                siteId,
                site.getName(),
                site.getAccountTypeWhitelist(),
                isScrapingSite(site.getProvider()) ? LoginType.FORM : LoginType.URL,
                isScrapingSite(site.getProvider()) ? ConnectionType.SCRAPER : ConnectionType.DIRECT_CONNECTION,
                new ClientSiteDTO.Services(
                        ais, pis
                ),
                site.getGroupingBy(),
                tags,
                clientSite.isAvailable(),
                clientSite.isEnabled(),
                clientSite.isUseExperimentalVersion(),
                site.getAvailableInCountries(),
                SiteConnectionHealthStatus.SITE_CONNECTION_HEALH_STATUS_NOT_AVAILABLE,
                false,
                getIconPath(siteId));
    }


    private ClientSiteDTO.Services.Onboarded getOnboarded(ClientSite clientSite, ServiceType serviceType) {
        boolean isGloballyEnabled = clientSite.getRegisteredAuthenticationMeans()
                .stream()
                .filter(it -> it.getServiceType().equals(serviceType))
                .anyMatch(it -> it.getType().equals(AuthenticationMeansScope.Type.CLIENT));

        return new ClientSiteDTO.Services.Onboarded(clientSite.getRedirectUrlIds(serviceType), isGloballyEnabled);
    }

    List<ClientSiteDTO> mapClientSiteDTO(List<ClientSite> clientSites) {
        return clientSites
                .stream()
                .sorted(Comparator.comparing(it -> it.getSite().getName()))
                .map(this::mapClientSiteDTO)
                .collect(Collectors.toList());
    }

    private static boolean isScrapingSite(@NonNull String provider) {
        return provider.equals("BUDGET_INSIGHT") || provider.equals("YODLEE") || provider.equals("SALTEDGE");
    }

    private DynamicFieldsDTO mapToDynamicFieldsDTO(@Nullable final DynamicFields dynamicFields) {
        if (dynamicFields == null) {
            return null;
        }
        return DynamicFieldsDTO.builder()
                .creditorAgentBic(mapIfPresent(dynamicFields.getCreditorAgentBic(),
                        it -> new DynamicFieldsDTO.BicDynamicFieldDTO(it.isRequired())))
                .creditorAgentName(mapIfPresent(dynamicFields.getCreditorAgentName(),
                        it -> new DynamicFieldsDTO.CreditorAgentNameDynamicFieldDTO(it.isRequired())))
                .remittanceInformationStructured(mapIfPresent(dynamicFields.getRemittanceInformationStructured(),
                        it -> new DynamicFieldsDTO.RemittanceInformationStructuredDynamicFieldDTO(it.isRequired())))
                .creditorPostalAddressLine(mapIfPresent(dynamicFields.getCreditorPostalAddressLine(),
                        it -> new DynamicFieldsDTO.CreditorPostalAddressLineDynamicFieldDTO(it.isRequired())))
                .creditorPostalCountry(mapIfPresent(dynamicFields.getCreditorPostalCountry(),
                        it -> new DynamicFieldsDTO.CreditorPostalCountryDynamicFieldDTO(it.isRequired())))
                .debtorName(mapIfPresent(dynamicFields.getDebtorName(),
                        it -> new DynamicFieldsDTO.DebtorNameDynamicFieldDTO(it.isRequired())))
                .build();
    }

    private static <T, U> U mapIfPresent(final T maybeValue, final Function<T, U> mapper) {
        return Optional.ofNullable(maybeValue)
                .map(mapper)
                .orElse(null);
    }

    private String getIconPath(final UUID siteId) {
        return siteIconLink.replace("{siteId}", siteId.toString());
    }

}
