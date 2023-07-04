package com.yolt.clients.client.redirecturls;

import com.yolt.clients.authmeans.ClientOnboardedProviderRepository;
import com.yolt.clients.client.redirecturls.dto.RedirectURLDTO;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLAlreadyExistsException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLCardinalityException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLNotFoundException;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.certificatemanagement.CertificateService;
import com.yolt.clients.clientgroup.certificatemanagement.KeyUtil;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateDTO;
import com.yolt.clients.clientgroup.certificatemanagement.providers.ProvidersService;
import com.yolt.clients.clientgroup.certificatemanagement.yoltbank.YoltbankService;
import com.yolt.clients.clientsite.ClientSiteService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yolt.clients.client.redirecturls.Utils.lowercaseURL;
import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType.*;
import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType.SIGNING;
import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType.TRANSPORT;
import static com.yolt.clients.clientgroup.certificatemanagement.providers.YoltProvider.YOLT_TEST_BANK_SITE_ID;
import static nl.ing.lovebird.providerdomain.ServiceType.AIS;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Slf4j
@Service
public class RedirectURLService {
    private static final String CERT_NAME_TRANSPORT = "auto-created YOLT_PROVIDER-%s-%s".formatted(TRANSPORT, AIS);
    private static final String CERT_NAME_SIGNING = "auto-created YOLT_PROVIDER-%s-%s".formatted(SIGNING, AIS);
    private static final int REDIRECT_URL_THRESHOLD = 5;

    private final RedirectURLRepository redirectURLRepository;
    private final ClientOnboardedProviderRepository clientOnboardedProviderRepository;
    private final RedirectURLProducer redirectURLProducer;
    private final CertificateService certificateService;
    private final ProvidersService providersService;
    private final YoltbankService yoltbankService;
    private final ClientSiteService clientSiteService;
    private final boolean autoRegistrationEnabled;

    public RedirectURLService(
            RedirectURLRepository redirectURLRepository,
            ClientOnboardedProviderRepository clientOnboardedProviderRepository,
            RedirectURLProducer redirectURLProducer,
            CertificateService certificateService,
            ProvidersService providersService,
            YoltbankService yoltbankService,
            ClientSiteService clientSiteService,
            @Value("${yolt.redirectUrl.autoRegistration.enabled}") boolean autoRegistrationEnabled
    ) {
        this.redirectURLRepository = redirectURLRepository;
        this.clientOnboardedProviderRepository = clientOnboardedProviderRepository;
        this.redirectURLProducer = redirectURLProducer;
        this.certificateService = certificateService;
        this.providersService = providersService;
        this.yoltbankService = yoltbankService;
        this.clientSiteService = clientSiteService;
        this.autoRegistrationEnabled = autoRegistrationEnabled;
    }

    public List<RedirectURLDTO> findAll(UUID clientId) {
        return redirectURLRepository
                .findAllByClientId(clientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public RedirectURLDTO create(ClientToken clientToken, UUID redirectURLId, String redirectURL) {
        String lowercaseRedirectURL = lowercaseURL(redirectURL);
        requiredValidURL(clientToken, redirectURLId, lowercaseRedirectURL, autoRegistrationEnabled);

        var redirectURLDTO = mapToDTO(
                redirectURLRepository.save(new RedirectURL(clientToken.getClientIdClaim(), redirectURLId, lowercaseRedirectURL))
        );
        redirectURLProducer.sendMessage(clientToken, redirectURLDTO, RedirectURLMessageType.CLIENT_REDIRECT_URL_CREATED);

        if (autoRegistrationEnabled) {
            registerRedirectUrl(clientToken, redirectURLId);
        }

        return redirectURLDTO;
    }

    private void registerRedirectUrl(ClientToken clientToken, UUID redirectURLId) {
        TransportAndSigningKeyPairs keyPair = Stream.of(EIDAS, OB_ETSI, OB_LEGACY, OTHER)
                .flatMap(type -> certificateService.getCertificatesByCertificateType(clientToken.getClientGroupIdClaim(), type).stream())
                .reduce(
                        new TransportAndSigningKeyPairs(),
                        (pair, cert) -> {
                            if (CERT_NAME_TRANSPORT.equals(cert.getName())) return pair.setTransportKey(cert);
                            if (CERT_NAME_SIGNING.equals(cert.getName())) return pair.setSigningKey(cert);
                            return pair;
                        },
                        TransportAndSigningKeyPairs::combine
                );
        if (anyNull(keyPair.transportKey, keyPair.signingKey))
            throw new RuntimeException("Missing transport or signing key for client %s".formatted(clientToken.getClientIdClaim()));

        providersService.uploadAISAuthenticationMeansForTestBank(
                clientToken,
                keyPair.transportKey,
                createYoltbankCertificate(keyPair.transportKey.getCertificateSigningRequest()),
                keyPair.signingKey,
                createYoltbankCertificate(keyPair.signingKey.getCertificateSigningRequest()),
                redirectURLId
        );

        providersService.uploadPISAuthenticationMeansForTestBank(
                clientToken,
                keyPair.signingKey,
                yoltbankService.signSepaPIS(keyPair.signingKey.getCertificateSigningRequest()),
                redirectURLId
        );

        clientSiteService.markSiteAvailable(clientToken, YOLT_TEST_BANK_SITE_ID);
        clientSiteService.enableSite(clientToken, YOLT_TEST_BANK_SITE_ID);
    }

    public RedirectURLDTO get(ClientToken clientToken, UUID redirectURLId) {
        var redirectURL = redirectURLRepository
                .findByClientIdAndRedirectURLId(clientToken.getClientIdClaim(), redirectURLId)
                .orElseThrow(() -> new RedirectURLNotFoundException(clientToken.getClientIdClaim(), redirectURLId));

        return mapToDTO(redirectURL);
    }

    public RedirectURLDTO update(ClientToken clientToken, UUID redirectURLId, @URL @NotNull String url) {
        var redirectURL = redirectURLRepository
                .findByClientIdAndRedirectURLId(clientToken.getClientIdClaim(), redirectURLId)
                .orElseThrow(() -> new RedirectURLNotFoundException(clientToken.getClientIdClaim(), redirectURLId));

        url = lowercaseURL(url);
        if (redirectURL.getRedirectURL().equals(url)) {
            throw new RedirectURLAlreadyExistsException(clientToken.getClientIdClaim(), url);
        }

        redirectURL.setRedirectURL(url);

        var redirectURLDTO = mapToDTO(redirectURLRepository.save(redirectURL));
        redirectURLProducer.sendMessage(clientToken, redirectURLDTO, RedirectURLMessageType.CLIENT_REDIRECT_URL_UPDATED);
        return redirectURLDTO;
    }

    @Transactional
    public RedirectURLDTO delete(ClientToken clientToken, UUID redirectURLId) {
        var redirectURL = redirectURLRepository
                .findByClientIdAndRedirectURLId(clientToken.getClientIdClaim(), redirectURLId)
                .orElseThrow(() -> new RedirectURLNotFoundException(clientToken.getClientIdClaim(), redirectURLId));

        if (autoRegistrationEnabled) {
            clientOnboardedProviderRepository.deleteByClientOnboardedProviderId_ClientIdAndClientOnboardedProviderId_RedirectUrlId(clientToken.getClientIdClaim(), redirectURLId);
        }

        redirectURLRepository.delete(redirectURL);

        var redirectURLDTO = mapToDTO(redirectURL);
        redirectURLProducer.sendMessage(clientToken, redirectURLDTO, RedirectURLMessageType.CLIENT_REDIRECT_URL_DELETED);
        return redirectURLDTO;
    }

    public boolean hasRedirectUrlsConfigured(ClientToken clientToken) {
        return redirectURLRepository.existsByClientId(clientToken.getClientIdClaim());
    }

    private RedirectURLDTO mapToDTO(RedirectURL redirectURL) {
        return new RedirectURLDTO(redirectURL.getRedirectURLId(), redirectURL.getRedirectURL());
    }

    private void requiredValidURL(ClientToken clientToken, UUID redirectURLId, String redirectURL, boolean limitNrOfRedirectUrls) {
        List<RedirectURL> urls = redirectURLRepository.findAllByClientId(clientToken.getClientIdClaim());
        if (limitNrOfRedirectUrls && urls.size() >= REDIRECT_URL_THRESHOLD) throw new RedirectURLCardinalityException();
        if (urls.stream().anyMatch(url -> url.getRedirectURLId().equals(redirectURLId))) throw new RedirectURLAlreadyExistsException(clientToken.getClientIdClaim(), redirectURLId);
        if (urls.stream().anyMatch(url -> url.getRedirectURL().equalsIgnoreCase(redirectURL))) throw new RedirectURLAlreadyExistsException(clientToken.getClientIdClaim(), redirectURL);
    }

    @SneakyThrows
    private String createYoltbankCertificate(String csr) {
        String signedCertificateChain = yoltbankService.signCSR(csr);
        X509Certificate leafCertificate = KeyUtil.parseCertificateChain(signedCertificateChain).get(0);

        return KeyUtil.writeToPem("CERTIFICATE", leafCertificate.getEncoded());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    private static class TransportAndSigningKeyPairs {
        private CertificateDTO transportKey;
        private CertificateDTO signingKey;

        private TransportAndSigningKeyPairs combine(TransportAndSigningKeyPairs other) {
            return new TransportAndSigningKeyPairs(
                    defaultIfNull(this.transportKey, other.transportKey),
                    defaultIfNull(this.signingKey, other.signingKey)
            );
        }
    }

}
