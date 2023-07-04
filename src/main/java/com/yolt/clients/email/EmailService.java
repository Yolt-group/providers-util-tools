package com.yolt.clients.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Validated
@Slf4j
public class EmailService {

    private static final String SOURCE_EMAIL = "no-reply-clients@yolt.com";
    private static final String CLIENT_GROUP_ADMIN_SUBJECT = "Invitation for Client Group Admin";
    private static final String CLIENT_ADMIN_SUBJECT = "Invitation for Client Admin";

    private final SesClient sesClient;
    private final MessageCreator messageCreator;
    private final String activationUrl;

    public EmailService(SesClient sesClient,
                        MessageCreator messageCreator,
                        @Value("${clients.activation.url}") String activationUrl) {
        this.sesClient = sesClient;
        this.messageCreator = messageCreator;
        this.activationUrl = activationUrl;
    }

    public void sendInvitationForUser(String email, String name, String invitationCode, String clientGroupName, LocalDateTime validUntil, boolean isClientGroup) {
        ZonedDateTime validUntilWithTimeZone = ZonedDateTime.of(validUntil, ZoneId.systemDefault());
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", name);
        variables.put("invitationCode", invitationCode);
        variables.put("clientGroupName", clientGroupName);
        variables.put("devPortalUrl", activationUrl);
        variables.put("isClientGroup", isClientGroup);
        variables.put("validUntil", DateTimeFormatter.RFC_1123_DATE_TIME.format(validUntilWithTimeZone));

        Message emailMessage = messageCreator.createEmailMessage(
                isClientGroup ? CLIENT_GROUP_ADMIN_SUBJECT: CLIENT_ADMIN_SUBJECT, variables);

        sendEmail(email, emailMessage);
    }

    private void sendEmail(String emailDestination, Message emailMessage) {
        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .message(emailMessage)
                .source(SOURCE_EMAIL)
                .destination(Destination.builder().toAddresses(emailDestination).build())
                .build();

        sesClient.sendEmail(emailRequest);
    }
}
