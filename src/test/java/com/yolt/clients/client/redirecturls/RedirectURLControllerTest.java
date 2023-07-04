package com.yolt.clients.client.redirecturls;

import com.yolt.clients.ControllerTest;
import com.yolt.clients.client.redirecturls.dto.RedirectURLDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RedirectURLControllerTest extends ControllerTest {

    private static final String BASE_URI = "/internal/clients/{clientId}/redirect-urls";

    private static final  UUID redirectURLId = randomUUID();

    @Test
    void shouldCreateRedirectURL_WithoutSandboxFlag() throws Exception {
        RedirectURLDTO input = new RedirectURLDTO(redirectURLId, "https://junit.test");
        when(redirectURLService.create(clientToken, redirectURLId, "https://junit.test")).thenReturn(input);

        mockMvc.perform(post(BASE_URI, clientId)
                .secure(true)
                .content(objectMapper.writeValueAsString(input))
                .header(CLIENT_TOKEN_HEADER_NAME, SERIALIZED_CLIENT_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectURLId").value(redirectURLId.toString()))
                .andExpect(jsonPath("$.redirectURL").value("https://junit.test"));

        verify(redirectURLService, times(1)).create(clientToken, redirectURLId, "https://junit.test");
    }
}
