package com.yolt.clients.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageCreator {

    private static final String CHARSET = "UTF-8";
    private static final String TEMPLATE = "html/AdminInvitation.html";

    private final ITemplateEngine templateEngine;

    public Message createEmailMessage(String subject, Map<String, Object> templateVariables) {
        return Message.builder()
                .subject(createEmailSubject(subject))
                .body(createEmailBody(templateVariables))
                .build();
    }

    private Content createEmailSubject(String subject) {
        return Content.builder()
                .data(subject)
                .charset(CHARSET)
                .build();
    }

    private Body createEmailBody(Map<String, Object> templateVariables) {
        return Body.builder()
                .html(createEmailBodyHtml(templateVariables))
                .build();
    }

    private Content createEmailBodyHtml(Map<String, Object> templateVariables) {
        String output = templateEngine.process(TEMPLATE, new Context(Locale.getDefault(), templateVariables));

        return Content.builder()
                .data(output)
                .charset(CHARSET)
                .build();
    }

}
