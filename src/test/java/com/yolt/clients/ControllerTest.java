package com.yolt.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yolt.clients.client.redirecturls.RedirectURLController;
import com.yolt.clients.client.redirecturls.RedirectURLService;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import nl.ing.lovebird.clienttokens.verification.ClientTokenParser;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ContextConfiguration(classes = ClientsApplication.class)
@WebMvcTest(
        controllers = {
                RedirectURLController.class
        }
)
public abstract class ControllerTest {

    private static final String DEV_PORTAL = "dev-portal";
    protected static final String SERIALIZED_CLIENT_TOKEN = "client token";
    protected static final UUID clientId = randomUUID();

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected RedirectURLService redirectURLService;
    @MockBean
    protected ClientIdVerificationService clientIdVerificationService;
    @MockBean
    protected ClientTokenParser clientTokenParser;
    @MockBean
    protected ClientToken clientToken;

    @BeforeEach
    public void setUp() {
        when(clientTokenParser.parseClientToken(SERIALIZED_CLIENT_TOKEN)).thenReturn(clientToken);

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(randomUUID());
        when(clientToken.getSerialized()).thenReturn(SERIALIZED_CLIENT_TOKEN);
        when(clientToken.getIssuedForClaim()).thenReturn(DEV_PORTAL);
        when(clientToken.isDeleted()).thenReturn(false);
    }

}
