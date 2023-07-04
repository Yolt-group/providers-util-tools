package com.yolt.clients.clientgroup.certificatemanagement.yoltbank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YoltbankServiceTest {

    private YoltbankService yoltbankService;

    private String yoltbankurl;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        yoltbankurl = "https://yoltbank/yoltbank";

        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.rootUri(yoltbankurl)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        yoltbankService = new YoltbankService(restTemplateBuilder, yoltbankurl);
        verifyNoMoreInteractions(ignoreStubs(restTemplateBuilder));
    }

    @Test
    void signCSR() {
        String csrPEM = "csr text";

        String chain = "certificate";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(chain, HttpStatus.OK);

        when(restTemplate.postForEntity("/csr", csrPEM, String.class)).thenReturn(responseEntity);

        String result = yoltbankService.signCSR(csrPEM);

        assertThat(result).isEqualTo(chain);
    }

    @Test
    void signCSRNoResponseBody() {
        String csrPEM = "csr text";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity("/csr", csrPEM, String.class)).thenReturn(responseEntity);

        Exception thrown = assertThrows(IllegalStateException.class, () ->
                yoltbankService.signCSR(csrPEM)
        );
        assertThat(thrown.getMessage()).isEqualTo("Creating the PEM chain failed, no body returned");
    }

    @Test
    void signCSRBadResponse() {
        String csrPEM = "csr text";

        when(restTemplate.postForEntity("/csr", csrPEM, String.class)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Exception thrown = assertThrows(IllegalStateException.class, () ->
                yoltbankService.signCSR(csrPEM)
        );
        assertThat(thrown.getMessage()).isEqualTo("Signing the csr failed with http error code: 400 BAD_REQUEST, body: ");
    }
}
