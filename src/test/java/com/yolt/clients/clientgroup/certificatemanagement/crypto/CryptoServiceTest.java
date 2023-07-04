package com.yolt.clients.clientgroup.certificatemanagement.crypto;

import com.yolt.clients.clientgroup.certificatemanagement.crypto.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.SimpleDistinguishedNameElement;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.logging.MDCContextCreator.CLIENT_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoServiceTest {
    private static final String SERIALIZED_TOKEN = "serialized client token";
    private CryptoService cryptoService;

    private String cryptoUrl;
    private UUID clientId = UUID.randomUUID();
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ClientGroupToken clientGroupToken;
    @Mock
    private ClientToken clientToken;

    @BeforeEach
    void setup(){
        cryptoUrl = "https://crypto/crypto";

        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.rootUri(cryptoUrl)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        cryptoService = new CryptoService(cryptoUrl, restTemplateBuilder);
        verifyNoMoreInteractions(ignoreStubs(restTemplateBuilder));

        reset(clientGroupToken);
        when(clientGroupToken.getSerialized()).thenReturn(SERIALIZED_TOKEN);

        reset(clientToken);
    }

    @Test
    void testCreatePrivateKey(){
        CertificateUsageType usageType = CertificateUsageType.SIGNING;
        String keyAlgorithm = "keyAlg";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        KeyRequirementsDTO keyRequirementsDto = new KeyRequirementsDTO(usageType, keyAlgorithm);
        HttpEntity<KeyRequirementsDTO> request = new HttpEntity<>(keyRequirementsDto, headers);

        String keyId = "key";
        ResponseEntity<KidDTO> responseEntity = new ResponseEntity<>(new KidDTO(keyId), HttpStatus.OK);

        when(restTemplate.postForEntity("/key", request, KidDTO.class)).thenReturn(responseEntity);

        String result = cryptoService.createPrivateKey(clientGroupToken, keyAlgorithm, usageType);

        assertThat(result).isEqualTo(keyId);
    }

    @Test
    void testCreatePrivateKeyNoResponseBody(){
        CertificateUsageType usageType = CertificateUsageType.SIGNING;
        String keyAlgorithm = "keyAlg";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        KeyRequirementsDTO keyRequirementsDto = new KeyRequirementsDTO(usageType, keyAlgorithm);
        HttpEntity<KeyRequirementsDTO> request = new HttpEntity<>(keyRequirementsDto, headers);

        ResponseEntity<KidDTO> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity("/key", request, KidDTO.class)).thenReturn(responseEntity);

        Exception thrown = assertThrows(IllegalStateException.class, () -> cryptoService.createPrivateKey(clientGroupToken, keyAlgorithm, usageType));
        assertThat(thrown.getMessage()).isEqualTo("Creating the private key failed, null kid returned.");
    }

    @Test
    void testCreatePrivateKeyErrorResponse(){
        CertificateUsageType usageType = CertificateUsageType.SIGNING;
        String keyAlgorithm = "keyAlg";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        KeyRequirementsDTO keyRequirementsDto = new KeyRequirementsDTO(usageType, keyAlgorithm);
        HttpEntity<KeyRequirementsDTO> request = new HttpEntity<>(keyRequirementsDto, headers);

        when(restTemplate.postForEntity("/key", request, KidDTO.class)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Exception thrown = assertThrows(IllegalStateException.class, () -> cryptoService.createPrivateKey(clientGroupToken, keyAlgorithm, usageType));
        assertThat(thrown.getMessage()).isEqualTo("Creating the private key failed with http error code: 400 BAD_REQUEST, body: ");
    }

    @Test
    void testCreateCSR(){
        String kid = "keyId";
        CertificateUsageType usageType = CertificateUsageType.TRANSPORT;
        String signatureAlgorithm = "signingAlg";
        List< SimpleDistinguishedNameElement > distinguishedNames = List.of(new SimpleDistinguishedNameElement("C", "NL"));
        boolean addQcStatements = true;
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS, ServiceType.PIS);
        Set<String> subjectAlternativeNames = Set.of("SAN1", "SAN2");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        CSRRequirementsDTO dto = new CSRRequirementsDTO(usageType, serviceTypes, signatureAlgorithm, distinguishedNames, addQcStatements, subjectAlternativeNames);
        HttpEntity<CSRRequirementsDTO> request = new HttpEntity<>(dto, headers);

        String csr = "certificate Signing Request";
        ResponseEntity<CSRDTO> responseEntity = new ResponseEntity<>(new CSRDTO(csr), HttpStatus.OK);

        when(restTemplate.postForEntity("/key/{kid}/csr", request, CSRDTO.class, kid)).thenReturn(responseEntity);

        String result = cryptoService.createCSR(clientGroupToken, kid, usageType, signatureAlgorithm, distinguishedNames, addQcStatements, serviceTypes, subjectAlternativeNames);

        assertThat(result).isEqualTo(csr);
    }

    @Test
    void testCreateCSRNoBody(){
        String kid = "keyId";
        CertificateUsageType usageType = CertificateUsageType.TRANSPORT;
        String signatureAlgorithm = "signingAlg";
        List< SimpleDistinguishedNameElement > distinguishedNames = List.of(new SimpleDistinguishedNameElement("C", "NL"));
        boolean addQcStatements = true;
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS, ServiceType.PIS);
        Set<String> subjectAlternativeNames = Set.of("SAN1", "SAN2");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        CSRRequirementsDTO dto = new CSRRequirementsDTO(usageType, serviceTypes, signatureAlgorithm, distinguishedNames, addQcStatements, subjectAlternativeNames);
        HttpEntity<CSRRequirementsDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<CSRDTO> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity("/key/{kid}/csr", request, CSRDTO.class, kid)).thenReturn(responseEntity);

        Exception thrown =  assertThrows(IllegalStateException.class, () ->
                cryptoService.createCSR(clientGroupToken, kid, usageType, signatureAlgorithm, distinguishedNames, addQcStatements, serviceTypes, subjectAlternativeNames)
        );
        assertThat(thrown.getMessage()).isEqualTo("Creating the CSR failed, nothing was returned");
    }

    @Test
    void testCreateCSRErrorResponse(){
        String kid = "keyId";
        CertificateUsageType usageType = CertificateUsageType.TRANSPORT;
        String signatureAlgorithm = "signingAlg";
        List< SimpleDistinguishedNameElement > distinguishedNames = List.of(new SimpleDistinguishedNameElement("C", "NL"));
        boolean addQcStatements = true;
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS, ServiceType.PIS);
        Set<String> subjectAlternativeNames = Set.of("SAN1", "SAN2");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        CSRRequirementsDTO dto = new CSRRequirementsDTO(usageType, serviceTypes, signatureAlgorithm, distinguishedNames, addQcStatements, subjectAlternativeNames);
        HttpEntity<CSRRequirementsDTO> request = new HttpEntity<>(dto, headers);

        when(restTemplate.postForEntity("/key/{kid}/csr", request, CSRDTO.class, kid)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Exception thrown =  assertThrows(IllegalStateException.class, () ->
                cryptoService.createCSR(clientGroupToken, kid, usageType, signatureAlgorithm, distinguishedNames, addQcStatements, serviceTypes, subjectAlternativeNames)
        );
        assertThat(thrown.getMessage()).isEqualTo("Creating the CSR failed with http error code: 400 BAD_REQUEST, body: ");
    }

    @Test
    void testSign(){
        CertificateUsageType usageType = CertificateUsageType.SIGNING;
        String privateKid = UUID.randomUUID().toString();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA;
        byte[] bytesToSign = "bytesToSign".getBytes(StandardCharsets.UTF_8);
        String encodedSigningInput = Base64.toBase64String(bytesToSign);

        HttpHeaders headers = new HttpHeaders();
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        SignRequestDTO signRequestDTO = new SignRequestDTO(UUID.fromString(privateKid), signatureAlgorithm, encodedSigningInput, usageType);
        HttpEntity<SignRequestDTO> request = new HttpEntity<>(signRequestDTO, headers);

        String encodedSignature = "certificate Signing Request";
        ResponseEntity<SignatureDTO> responseEntity = new ResponseEntity<>(new SignatureDTO(encodedSignature), HttpStatus.OK);

        when(restTemplate.postForEntity("/sign", request, SignatureDTO.class)).thenReturn(responseEntity);

        String result = cryptoService.sign(clientGroupToken, usageType, privateKid, signatureAlgorithm, bytesToSign);

        assertThat(result).isEqualTo(encodedSignature);
    }

    @Test
    void testSignRestClientException(){
        reset(clientGroupToken);
        when(clientToken.getSerialized()).thenReturn(SERIALIZED_TOKEN);
        when(clientToken.getClientIdClaim()).thenReturn(clientId);

        CertificateUsageType usageType = CertificateUsageType.SIGNING;
        String privateKid = UUID.randomUUID().toString();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA;
        byte[] bytesToSign = "bytesToSign".getBytes(StandardCharsets.UTF_8);
        String encodedSigningInput = Base64.toBase64String(bytesToSign);

        HttpHeaders headers = new HttpHeaders();
        headers.add(CLIENT_ID_HEADER_NAME, clientId.toString());
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        SignRequestDTO signRequestDTO = new SignRequestDTO(UUID.fromString(privateKid), signatureAlgorithm, encodedSigningInput, usageType);
        HttpEntity<SignRequestDTO> request = new HttpEntity<>(signRequestDTO, headers);

        when(restTemplate.postForEntity("/sign", request, SignatureDTO.class)).thenThrow(new RestClientException("oops"));

        Exception thrown =  assertThrows(IllegalArgumentException.class, () ->
                cryptoService.sign(clientToken, usageType, privateKid, signatureAlgorithm, bytesToSign)
        );
        assertThat(thrown.getMessage()).isEqualTo("Signing on the crypto service went wrong");
    }

    @Test
    void testSignErrorResponse(){
        CertificateUsageType usageType = CertificateUsageType.SIGNING;
        String privateKid = UUID.randomUUID().toString();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA;
        byte[] bytesToSign = "bytesToSign".getBytes(StandardCharsets.UTF_8);
        String encodedSigningInput = Base64.toBase64String(bytesToSign);

        HttpHeaders headers = new HttpHeaders();
        headers.add(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_TOKEN);

        SignRequestDTO signRequestDTO = new SignRequestDTO(UUID.fromString(privateKid), signatureAlgorithm, encodedSigningInput, usageType);
        HttpEntity<SignRequestDTO> request = new HttpEntity<>(signRequestDTO, headers);

        when(restTemplate.postForEntity("/sign", request, SignatureDTO.class)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Exception thrown =  assertThrows(IllegalArgumentException.class, () ->
                cryptoService.sign(clientGroupToken, usageType, privateKid, signatureAlgorithm, bytesToSign)
        );
        assertThat(thrown.getMessage()).isEqualTo("Signing on the crypto service went wrong, status code: 400");
    }
}