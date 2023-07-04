package com.yolt.clients.clientgroup.certificatemanagement;

import com.yolt.clients.clientgroup.certificatemanagement.crypto.CryptoService;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyMaterialRequirements;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyRequirementsWrapper;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.ServiceInfo;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateAlreadySignedException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateNotFoundException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.NameIsAlreadyUsedException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.TooManyCertificatesException;
import com.yolt.clients.clientgroup.certificatemanagement.providers.ProvidersService;
import com.yolt.clients.clientgroup.certificatemanagement.providers.dto.ProviderInfo;
import com.yolt.clients.clientgroup.certificatemanagement.repository.Certificate;
import com.yolt.clients.clientgroup.certificatemanagement.repository.CertificateRepository;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.EXTRA_CLAIM_CLIENT_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {
    private UUID clientGroupId;
    private ClientGroupToken clientGroupToken;
    private CertificateType certificateType;
    private String keyId;
    private String certName;
    private Set<ServiceType> serviceTypes;
    private CertificateUsageType certificateUsageType;
    private String keyAlgorithm;
    private String signatureAlgorithm;
    private String signatureAlgorithmBouncyCastle;
    private List<SimpleDistinguishedNameElement> subjectDNs;
    private Set<String> subjectAlternativeNames;
    private String certificateSigningRequest;
    private String signedCertificateChain;
    private Certificate certificate;
    private CertificateDTO certificateDTO;

    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CryptoService cryptoService;
    @Mock
    private CertificateValidationService certificateValidationService;
    @Mock
    private ProvidersService providersService;

    private CertificateService certificateService;

    @BeforeEach
    void setup() {
        clientGroupId = UUID.randomUUID();
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setStringClaim(EXTRA_CLAIM_CLIENT_GROUP_ID, clientGroupId.toString());
        clientGroupToken = new ClientGroupToken("serialized", jwtClaims);
        certificateType = CertificateType.OB_ETSI;
        keyId = UUID.randomUUID().toString();
        certName = "certName";
        serviceTypes = Set.of(ServiceType.AS);
        certificateUsageType = CertificateUsageType.TRANSPORT;
        keyAlgorithm = "RSA2048";
        signatureAlgorithm = "SHA256_WITH_RSA";
        signatureAlgorithmBouncyCastle = "SHA256withRSA";
        subjectDNs = List.of(new SimpleDistinguishedNameElement("type", "value"));
        subjectAlternativeNames = Set.of("SAN1", "SAN2");
        certificateSigningRequest = "CSR string";
        signedCertificateChain = "signed Certificate Chain";
        certificate = new Certificate(
                certificateType,
                keyId,
                clientGroupId,
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectAlternativeNames,
                certificateSigningRequest,
                null
        );
        certificateDTO = new CertificateDTO(
                certName,
                certificateType,
                keyId,
                certificateUsageType,
                serviceTypes,
                keyAlgorithm,
                signatureAlgorithm,
                certificateSigningRequest,
                null
        );
        certificateService = new CertificateService(certificateRepository, cryptoService, certificateValidationService, providersService, 10);
    }

    @ParameterizedTest
    @CsvSource({"EIDAS,true", "OB_LEGACY,false", "OB_ETSI,true"})
    void createCertificateSigningRequest(CertificateType certificateType, boolean addQCStatements) {
        CertificateSigningRequestDTO inputDTO = new CertificateSigningRequestDTO(
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectDNs,
                subjectAlternativeNames
        );
        certificate = new Certificate(
                certificateType,
                keyId,
                clientGroupId,
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectAlternativeNames,
                certificateSigningRequest,
                null
        );
        certificateDTO = new CertificateDTO(
                certName,
                certificateType,
                keyId,
                certificateUsageType,
                serviceTypes,
                keyAlgorithm,
                signatureAlgorithm,
                certificateSigningRequest,
                null
        );

        when(certificateRepository.existsByClientGroupIdAndName(clientGroupId, certName)).thenReturn(false);
        when(cryptoService.createPrivateKey(clientGroupToken, inputDTO.getKeyAlgorithm(), certificateUsageType)).thenReturn(keyId);
        when(cryptoService.createCSR(
                clientGroupToken,
                keyId,
                certificateUsageType,
                signatureAlgorithm,
                subjectDNs,
                addQCStatements,
                serviceTypes,
                subjectAlternativeNames
        )).thenReturn(certificateSigningRequest);
        when(certificateRepository.save(certificate)).thenReturn(certificate);

        CertificateDTO result = certificateService.createCertificateSigningRequest(clientGroupToken, inputDTO, certificateType);
        assertThat(result).isEqualTo(certificateDTO);
    }

    @Test
    void createCertificateSigningRequest_nameTaken() {
        CertificateSigningRequestDTO inputDTO = new CertificateSigningRequestDTO(
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectDNs,
                subjectAlternativeNames
        );
        when(certificateRepository.existsByClientGroupIdAndName(clientGroupId, certName)).thenReturn(true);

        assertThrows(NameIsAlreadyUsedException.class, () -> certificateService.createCertificateSigningRequest(clientGroupToken, inputDTO, certificateType));
    }

    @Test
    void getCertificatesByCertificateType() {
        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateType(clientGroupId, certificateType))
                .thenReturn(List.of(certificate));
        List<CertificateDTO> result = certificateService.getCertificatesByCertificateType(clientGroupToken.getClientGroupIdClaim(), certificateType);
        assertThat(result).containsExactlyInAnyOrder(certificateDTO);
    }

    @Test
    void getCertificateByIdAndType() {
        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId))
                .thenReturn(Optional.of(certificate));
        CertificateDTO result = certificateService.getCertificateByIdAndType(clientGroupToken, certificateType, keyId);
        assertThat(result).isEqualTo(certificateDTO);
    }

    @Test
    void getCertificateByIdAndType_not_found() {
        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId))
                .thenReturn(Optional.empty());

        CertificateNotFoundException thrown = assertThrows(CertificateNotFoundException.class, () -> certificateService.getCertificateByIdAndType(clientGroupToken, certificateType, keyId));
        assertThat(thrown.getMessage()).isEqualTo(String.format("Certificate not found for client-group: %s, certificateType: %s and kid: %s", clientGroupId, certificateType, keyId));
    }

    @Test
    void updateCertificateNameForId() {
        String newName = "new cert name";
        NewCertificateNameDTO newCertificateNameDTO = new NewCertificateNameDTO(newName);

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));
        when(certificateRepository.existsByClientGroupIdAndName(clientGroupId, newName)).thenReturn(false);
        when(certificateRepository.save(certificate)).thenReturn(certificate);

        certificateDTO = new CertificateDTO(
                newName,
                certificateType,
                keyId,
                certificateUsageType,
                serviceTypes,
                keyAlgorithm,
                signatureAlgorithm,
                certificateSigningRequest,
                null
        );

        CertificateDTO result = certificateService.updateCertificateNameForId(clientGroupToken, certificateType, keyId, newCertificateNameDTO);

        assertThat(result).isEqualTo(certificateDTO);
        assertThat(certificate.getName()).isEqualTo(newName);
    }

    @Test
    void updateCertificateNameForId_name_not_changed_should_not_update() {
        NewCertificateNameDTO newCertificateNameDTO = new NewCertificateNameDTO(certName);

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));

        CertificateDTO result = certificateService.updateCertificateNameForId(clientGroupToken, certificateType, keyId, newCertificateNameDTO);

        assertThat(result).isEqualTo(certificateDTO);
        assertThat(certificate.getName()).isEqualTo(certName);
    }

    @Test
    void updateCertificateNameForId_name_taken_should_throw_NameIsAlreadyUsedException() {
        String newName = "new cert name";
        NewCertificateNameDTO newCertificateNameDTO = new NewCertificateNameDTO(newName);

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));
        when(certificateRepository.existsByClientGroupIdAndName(clientGroupId, newName)).thenReturn(true);

        assertThrows(NameIsAlreadyUsedException.class, () -> certificateService.updateCertificateNameForId(clientGroupToken, certificateType, keyId, newCertificateNameDTO));
    }

    @Test
    void updateCertificateNameForId_certificate_not_found_should_throw_exception() {
        String newName = "new cert name";
        NewCertificateNameDTO newCertificateNameDTO = new NewCertificateNameDTO(newName);

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.empty());

        CertificateNotFoundException thrown = assertThrows(CertificateNotFoundException.class, () -> certificateService.updateCertificateNameForId(clientGroupToken, certificateType, keyId, newCertificateNameDTO));
        assertThat(thrown.getMessage()).isEqualTo(String.format("Certificate not found for client-group: %s, certificateType: %s and kid: %s", clientGroupId, certificateType, keyId));
    }

    @Test
    void validateCertificate_RSAPublicKey() throws Exception {
        X509Certificate x509Certificate = mock(X509Certificate.class);
        BigInteger serialNumber = BigInteger.TEN;
        Principal subjectDN = mock(Principal.class);
        String subjectDNName = "subject DN name";
        Principal issuerDN = mock(Principal.class);
        String issuerDNName = "issuer DN name";
        Date startDate = Date.from(Instant.now().minusSeconds(10000L));
        Date expDate = Date.from(Instant.now().plusSeconds(10000L));
        RSAPublicKey publicKey = mock(RSAPublicKey.class);

        when(x509Certificate.getSerialNumber()).thenReturn(serialNumber);
        when(x509Certificate.getSubjectDN()).thenReturn(subjectDN);
        when(x509Certificate.getIssuerDN()).thenReturn(issuerDN);
        when(x509Certificate.getNotBefore()).thenReturn(startDate);
        when(x509Certificate.getNotAfter()).thenReturn(expDate);
        when(x509Certificate.getPublicKey()).thenReturn(publicKey);
        when(x509Certificate.getSigAlgName()).thenReturn(signatureAlgorithmBouncyCastle);

        when(subjectDN.getName()).thenReturn(subjectDNName);
        when(issuerDN.getName()).thenReturn(issuerDNName);

        when(publicKey.getAlgorithm()).thenReturn("RSAPubKeyAlg");
        when(publicKey.getModulus()).thenReturn(BigInteger.TWO);

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));

        CertificateInfoDTO expected = new CertificateInfoDTO(
                keyId,
                serialNumber,
                subjectDNName,
                issuerDNName,
                startDate,
                expDate,
                "RSAPubKeyAlg2",
                signatureAlgorithmBouncyCastle
        );

        CertificateInfoDTO result = certificateService.validateCertificate(clientGroupToken, certificateType, keyId, x509Certificate);

        assertThat(result).isEqualTo(expected);

        verify(certificateValidationService).validateValidity(x509Certificate);
        verify(certificateValidationService).validateCertificateWithPrivateKey(clientGroupToken, certificateUsageType, keyId, x509Certificate);
    }

    @Test
    void validateCertificate_DSAPublicKey() throws Exception {
        X509Certificate x509Certificate = mock(X509Certificate.class);
        BigInteger serialNumber = BigInteger.TEN;
        Principal subjectDN = mock(Principal.class);
        String subjectDNName = "subject DN name";
        Principal issuerDN = mock(Principal.class);
        String issuerDNName = "issuer DN name";
        Date startDate = Date.from(Instant.now().minusSeconds(10000L));
        Date expDate = Date.from(Instant.now().plusSeconds(10000L));
        DSAPublicKey publicKey = mock(DSAPublicKey.class);

        when(x509Certificate.getSerialNumber()).thenReturn(serialNumber);
        when(x509Certificate.getSubjectDN()).thenReturn(subjectDN);
        when(x509Certificate.getIssuerDN()).thenReturn(issuerDN);
        when(x509Certificate.getNotBefore()).thenReturn(startDate);
        when(x509Certificate.getNotAfter()).thenReturn(expDate);
        when(x509Certificate.getPublicKey()).thenReturn(publicKey);
        when(x509Certificate.getSigAlgName()).thenReturn(signatureAlgorithmBouncyCastle);

        when(subjectDN.getName()).thenReturn(subjectDNName);
        when(issuerDN.getName()).thenReturn(issuerDNName);

        when(publicKey.getAlgorithm()).thenReturn("DSAPubKeyAlg");
        when(publicKey.getY()).thenReturn(BigInteger.TWO);

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));

        CertificateInfoDTO expected = new CertificateInfoDTO(
                keyId,
                serialNumber,
                subjectDNName,
                issuerDNName,
                startDate,
                expDate,
                "DSAPubKeyAlg2",
                signatureAlgorithmBouncyCastle
        );

        CertificateInfoDTO result = certificateService.validateCertificate(clientGroupToken, certificateType, keyId, x509Certificate);

        assertThat(result).isEqualTo(expected);

        verify(certificateValidationService).validateValidity(x509Certificate);
        verify(certificateValidationService).validateCertificateWithPrivateKey(clientGroupToken, certificateUsageType, keyId, x509Certificate);
    }

    @Test
    void validateCertificate_ECPublicKey() throws Exception {
        X509Certificate x509Certificate = mock(X509Certificate.class);
        BigInteger serialNumber = BigInteger.TEN;
        Principal subjectDN = mock(Principal.class);
        String subjectDNName = "subject DN name";
        Principal issuerDN = mock(Principal.class);
        String issuerDNName = "issuer DN name";
        Date startDate = Date.from(Instant.now().minusSeconds(10000L));
        Date expDate = Date.from(Instant.now().plusSeconds(10000L));
        ECPublicKey publicKey = mock(ECPublicKey.class);

        when(x509Certificate.getSerialNumber()).thenReturn(serialNumber);
        when(x509Certificate.getSubjectDN()).thenReturn(subjectDN);
        when(x509Certificate.getIssuerDN()).thenReturn(issuerDN);
        when(x509Certificate.getNotBefore()).thenReturn(startDate);
        when(x509Certificate.getNotAfter()).thenReturn(expDate);
        when(x509Certificate.getPublicKey()).thenReturn(publicKey);
        when(x509Certificate.getSigAlgName()).thenReturn(signatureAlgorithmBouncyCastle);

        when(subjectDN.getName()).thenReturn(subjectDNName);
        when(issuerDN.getName()).thenReturn(issuerDNName);

        when(publicKey.getAlgorithm()).thenReturn("ECPubKeyAlg");

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));

        CertificateInfoDTO expected = new CertificateInfoDTO(
                keyId,
                serialNumber,
                subjectDNName,
                issuerDNName,
                startDate,
                expDate,
                "ECPubKeyAlg",
                signatureAlgorithmBouncyCastle
        );

        CertificateInfoDTO result = certificateService.validateCertificate(clientGroupToken, certificateType, keyId, x509Certificate);

        assertThat(result).isEqualTo(expected);

        verify(certificateValidationService).validateValidity(x509Certificate);
        verify(certificateValidationService).validateCertificateWithPrivateKey(clientGroupToken, certificateUsageType, keyId, x509Certificate);
    }

    @Test
    void validateCertificate_should_throw_exception_when_certificate_not_found() {
        X509Certificate x509Certificate = mock(X509Certificate.class);
        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.empty());
        CertificateNotFoundException thrown = assertThrows(CertificateNotFoundException.class, () -> certificateService.validateCertificate(clientGroupToken, certificateType, keyId, x509Certificate));
        assertThat(thrown.getMessage()).isEqualTo(String.format("Certificate not found for client-group: %s, certificateType: %s and kid: %s", clientGroupId, certificateType, keyId));
    }

    @Test
    void updateCertificateChainForId() throws Exception {
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(
                new String(this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes())
        );
        X509Certificate leafCertificate = certificateChain.get(0);

        Date startDate = new GregorianCalendar(2020, Calendar.FEBRUARY, 19, 17, 19, 4).getTime();
        Date expDate = new GregorianCalendar(2030, Calendar.FEBRUARY, 19, 17, 19, 4).getTime();
        byte[] encodedCert = new byte[256];
        new Random().nextBytes(encodedCert);

        CertificateInfoDTO expected = new CertificateInfoDTO(
                keyId,
                new BigInteger("16115018079775059010"),
                "CN=TppSandboxCertificate, O=ABC, ST=Some-State, C=NL",
                "CN=TppSandboxCertificate, O=ABC, ST=Some-State, C=NL",
                startDate,
                expDate,
                keyAlgorithm,
                signatureAlgorithmBouncyCastle
        );

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(certificate)).thenReturn(certificate);

        List<CertificateInfoDTO> result = certificateService.updateCertificateChainForId(clientGroupToken, certificateType, keyId, certificateChain);

        assertThat(result).usingElementComparatorIgnoringFields("validFrom", "validTo").containsExactly(expected);
        assertThat(certificate.getSignedCertificateChain()).isEqualToIgnoringNewLines(
                """
                        -----BEGIN CERTIFICATE-----
                        MIIDdjCCAl6gAwIBAgIJAN+kC5AB/WxCMA0GCSqGSIb3DQEBCwUAMFAxCzAJBgNV
                        BAYTAk5MMRMwEQYDVQQIDApTb21lLVN0YXRlMQwwCgYDVQQKDANBQkMxHjAcBgNV
                        BAMMFVRwcFNhbmRib3hDZXJ0aWZpY2F0ZTAeFw0yMDAyMTkxNjE5MDRaFw0zMDAy
                        MTYxNjE5MDRaMFAxCzAJBgNVBAYTAk5MMRMwEQYDVQQIDApTb21lLVN0YXRlMQww
                        CgYDVQQKDANBQkMxHjAcBgNVBAMMFVRwcFNhbmRib3hDZXJ0aWZpY2F0ZTCCASIw
                        DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJkUcwLLs9dr3ngvR5ULGHoCvCpy
                        aeVA9s/VBZ4q4ysbLAjdGux3mR18GKtGMkIeB1Dmw65vBdJWCBXxbPdeWA3ZRoC6
                        yUCU6w5HMg0IMMq4Z5dbUs5cgvrUF1ZD12uUW/4zSQ6dw4DpyVzE2rDQ88dSGBGS
                        C2U/Ql3aR8W2RaDL0Ii5MobKM1VtCrL2bjGKyPf4rViJZDrvFQBBH2WzlGJnDQVY
                        xgQnINVQa1lIY+B/gNvm1iw/znAqeAN38FrNNXy6LpHXmi7viDh1/pBMbG2L6SRn
                        uSOu79QrXaMPsaupklEHlyrY5s/SDvsjgEGC3IQNVduAL87zhjTLt+ElGPcCAwEA
                        AaNTMFEwHQYDVR0OBBYEFC845pGDdNNAtsOkJLHxJoQc8LZdMB8GA1UdIwQYMBaA
                        FC845pGDdNNAtsOkJLHxJoQc8LZdMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcN
                        AQELBQADggEBAHyKoI4s7Lf0lnMOE7/FmLZf1dIOmvda2n08bxmwLGBna/o6XaND
                        tt9OjXm6t1I1Wc/gVaR8mhRwXYVSgiQ6AYlgCnL+JjY5SSrGrpu0RkE4uputx+Nf
                        QAC21GOSfMX22MLqkopaF4hwB0nKCEAToiM3RboUOeFBdK5AFawSYbeYXLJtHGsl
                        OyZ4sFHgMo9w1ivAZNv3XpDBEA5t8qJ4hDJmERXiPx3I1hwsrO2MUtNKvSbh/L+3
                        fGaVny7kTSJ04wFiOgUP6yd+N5e7nXpCXuxfaL66r78lF2wxzTGcgq89QeXBlXNy
                        bJGQnWkkpwM6ANf5F4WcsAPLke7G731rmsg=
                        -----END CERTIFICATE-----
                        """
        );

        verify(certificateValidationService).validateValidity(leafCertificate);
        verify(certificateValidationService).validateCertificateWithPrivateKey(clientGroupToken, certificateUsageType, keyId, leafCertificate);
    }

    @Test
    void updateCertificateChainForId_certificate_already_signed() throws Exception {
        String signedCertificateChain = new String(this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes());
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(signedCertificateChain);
        X509Certificate leafCertificate = certificateChain.get(0);

        certificate = new Certificate(
                certificateType,
                keyId,
                clientGroupId,
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectAlternativeNames,
                certificateSigningRequest,
                signedCertificateChain
        );

        when(certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(clientGroupId, certificateType, keyId)).thenReturn(Optional.of(certificate));

        assertThrows(CertificateAlreadySignedException.class, () -> certificateService.updateCertificateChainForId(clientGroupToken, certificateType, keyId, certificateChain));

        verify(certificateValidationService).validateValidity(leafCertificate);
        verify(certificateValidationService).validateCertificateWithPrivateKey(clientGroupToken, certificateUsageType, keyId, leafCertificate);
    }

    @ParameterizedTest
    @EnumSource(CertificateUsageType.class)
    void findCompatibleCertificates(CertificateUsageType usageType) {
        String providerKey = "providerKey";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AS, ServiceType.AIS, ServiceType.IC);

        String key1 = UUID.randomUUID().toString();

        List<Certificate> certificates = List.of(
                new Certificate(
                        CertificateType.OTHER,
                        key1,
                        clientGroupId,
                        "cert1",
                        Set.of(ServiceType.AS, ServiceType.AIS, ServiceType.PIS, ServiceType.IC),
                        usageType,
                        "keyAlg2",
                        "sigAlg2",
                        subjectAlternativeNames,
                        certificateSigningRequest,
                        signedCertificateChain,
                        providerKey
                ),
                new Certificate(
                        CertificateType.OTHER,
                        key1,
                        clientGroupId,
                        "cert2",
                        Set.of(ServiceType.AS, ServiceType.AIS, ServiceType.IC),
                        usageType,
                        "keyAlg2",
                        "sigAlg2",
                        subjectAlternativeNames,
                        certificateSigningRequest,
                        signedCertificateChain,
                        providerKey
                ),
                new Certificate(
                        CertificateType.OTHER,
                        key1,
                        clientGroupId,
                        "less service types",
                        Set.of(ServiceType.AS),
                        usageType,
                        "keyAlg2",
                        "sigAlg2",
                        subjectAlternativeNames,
                        certificateSigningRequest,
                        signedCertificateChain,
                        providerKey
                ),
                new Certificate(
                        CertificateType.OTHER,
                        key1,
                        clientGroupId,
                        "wrong key alg",
                        Set.of(ServiceType.AS, ServiceType.AIS, ServiceType.IC),
                        usageType,
                        "keyAlg1",
                        "sigAlg2",
                        subjectAlternativeNames,
                        certificateSigningRequest,
                        signedCertificateChain,
                        providerKey
                ),
                new Certificate(
                        CertificateType.OTHER,
                        key1,
                        clientGroupId,
                        "wrong sign alg",
                        Set.of(ServiceType.AS, ServiceType.AIS, ServiceType.IC),
                        usageType,
                        "keyAlg2",
                        "sigAlg1",
                        subjectAlternativeNames,
                        certificateSigningRequest,
                        signedCertificateChain,
                        providerKey
                )
        );
        List<CertificateDTO> expectedResult = List.of(
                new CertificateDTO(
                        "cert1",
                        CertificateType.OTHER,
                        key1,
                        usageType,
                        Set.of(ServiceType.AS, ServiceType.AIS, ServiceType.PIS, ServiceType.IC),
                        "keyAlg2",
                        "sigAlg2",
                        certificateSigningRequest,
                        signedCertificateChain
                ),
                new CertificateDTO(
                        "cert2",
                        CertificateType.OTHER,
                        key1,
                        usageType,
                        Set.of(ServiceType.AS, ServiceType.AIS, ServiceType.IC),
                        "keyAlg2",
                        "sigAlg2",
                        certificateSigningRequest,
                        signedCertificateChain
                ),
                new CertificateDTO(
                        "less service types",
                        CertificateType.OTHER,
                        key1,
                        usageType,
                        Set.of(ServiceType.AS),
                        "keyAlg2",
                        "sigAlg2",
                        certificateSigningRequest,
                        signedCertificateChain
                )
        );

        ServiceInfo serviceInfo = new ServiceInfo(
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg1", "keyAlg2"),
                        Set.of("sigAlg1", "sigAlg2"),
                        List.of()
                )),
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg3", "keyAlg2"),
                        Set.of("sigAlg3", "sigAlg2"),
                        List.of()
                ))
        );

        ServiceInfo icServiceInfo = new ServiceInfo(
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg4", "keyAlg2"),
                        Set.of("sigAlg4", "sigAlg2"),
                        List.of()
                )),
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg5", "keyAlg2"),
                        Set.of("sigAlg5", "sigAlg2"),
                        List.of()
                ))
        );

        ProviderInfo providerInfo = new ProviderInfo(
                providerKey,
                Map.of(
                        ServiceType.AS, serviceInfo,
                        ServiceType.AIS, serviceInfo,
                        ServiceType.IC, icServiceInfo,
                        ServiceType.PIS, serviceInfo
                )
        );

        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateUsageType(clientGroupId, usageType)).thenReturn(certificates);
        when(providersService.getProviderInfo(providerKey)).thenReturn(providerInfo);

        List<CertificateDTO> result = certificateService.findCompatibleCertificates(clientGroupToken, providerKey, serviceTypes, usageType);

        assertThat(result).containsExactlyInAnyOrderElementsOf(expectedResult);
    }


    @Test
    void deleteCertificate() {
        when(certificateRepository.findCertificateByClientGroupIdAndKid(clientGroupId, keyId)).thenReturn(Optional.of(certificate));

        certificateService.deleteCertificate(clientGroupToken, keyId);

        verify(certificateRepository).findCertificateByClientGroupIdAndKid(clientGroupId, keyId);
        verify(cryptoService).deletePrivateKey(clientGroupToken, keyId);
        verify(certificateRepository).delete(certificate);
    }

    @Test
    void deleteCertificate_while_certificate_not_found() {
        when(certificateRepository.findCertificateByClientGroupIdAndKid(clientGroupId, keyId)).thenReturn(Optional.empty());

        CertificateNotFoundException ex = assertThrows(CertificateNotFoundException.class, () -> certificateService.deleteCertificate(clientGroupToken, keyId));
        assertThat(ex).hasMessage("Certificate not found for client-group: %s and certificateId: %s".formatted(clientGroupToken.getClientGroupIdClaim(), keyId));

        verify(certificateRepository).findCertificateByClientGroupIdAndKid(clientGroupId, keyId);
    }

    @Test
    void create_certificate_limit_reached() {
        CertificateSigningRequestDTO inputDTO = new CertificateSigningRequestDTO(
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectDNs,
                subjectAlternativeNames
        );

        when(certificateRepository.countByClientGroupId(clientGroupId)).thenReturn(10L);

        TooManyCertificatesException ex = assertThrows(TooManyCertificatesException.class, () -> certificateService.createCertificateSigningRequest(clientGroupToken, inputDTO, certificateType));

        assertThat(ex).hasMessage(String.format("The limit of the number of certificates for client group with id %s is already reached.", clientGroupId));
    }
}
