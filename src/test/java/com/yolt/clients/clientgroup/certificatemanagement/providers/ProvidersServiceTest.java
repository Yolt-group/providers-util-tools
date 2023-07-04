package com.yolt.clients.clientgroup.certificatemanagement.providers;

import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyMaterialRequirements;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyRequirementsWrapper;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.ServiceInfo;
import com.yolt.clients.clientgroup.certificatemanagement.providers.dto.ProviderInfo;
import com.yolt.clients.clientgroup.certificatemanagement.providers.exceptions.ProviderInfoFetchException;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvidersServiceTest {
    private ProvidersService providersService;

    private String providersURL;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        providersURL = "https://providers/providers";

        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.rootUri(providersURL)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        providersService = new ProvidersService(providersURL, restTemplateBuilder);
        verifyNoMoreInteractions(ignoreStubs(restTemplateBuilder));
    }

    @Test
    void getProviderInfo() {
        String providerKey = "key1";
        ServiceInfo serviceInfo = new ServiceInfo(
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg5", "keyAlg2"),
                        Set.of("sigAlg5", "sigAlg2"),
                        List.of()
                )),
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg6", "keyAlg4"),
                        Set.of("sigAlg6", "sigAlg4"),
                        List.of()
                ))
        );
        ProviderInfo providerInfo = new ProviderInfo(
                providerKey,
                Map.of(
                        ServiceType.AS, serviceInfo,
                        ServiceType.AIS, serviceInfo,
                        ServiceType.IC, serviceInfo,
                        ServiceType.PIS, serviceInfo
                )
        );

        when(restTemplate.getForEntity("/provider-info/{providerKey}", ProviderInfo.class, providerKey))
                .thenReturn(new ResponseEntity<>(providerInfo, HttpStatus.OK));

        ProviderInfo result = providersService.getProviderInfo(providerKey);

        assertThat(result).isEqualTo(providerInfo);
    }

    @Test
    void getProviderInfo_null_body() {
        String providerKey = "key1";

        when(restTemplate.getForEntity("/provider-info/{providerKey}", ProviderInfo.class, providerKey))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        Exception thrown = assertThrows(ProviderInfoFetchException.class, () -> providersService.getProviderInfo(providerKey));
        assertThat(thrown).hasMessage("No response body");
    }

    @Test
    void getProviderInfo_error_response() {
        String providerKey = "key1";

        HttpStatusCodeException cause = new HttpStatusCodeException(HttpStatus.BAD_REQUEST) {
            @Override
            public String getResponseBodyAsString() {
                return "error response";
            }
        };
        when(restTemplate.getForEntity("/provider-info/{providerKey}", ProviderInfo.class, providerKey))
                .thenThrow(cause);

        Exception thrown = assertThrows(ProviderInfoFetchException.class, () -> providersService.getProviderInfo(providerKey));
        assertThat(thrown).hasMessage(String.format("Error getting provider info for providerKey: %s, status code: %d, body: %s", providerKey, 400, "error response"));
    }
}
