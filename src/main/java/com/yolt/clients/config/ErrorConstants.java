package com.yolt.clients.config;

import nl.ing.lovebird.errorhandling.ErrorInfo;

public enum ErrorConstants implements ErrorInfo {
    CLIENT_GROUP_NOT_FOUND("001", "client group not found"),
    TOKEN_MISMATCH("002", "The client id and/or client group id validation failed."),
    CLIENT_NOT_FOUND("003", "client not found"),
    CLIENT_EXISTS("004", "The client id is linked with an existing client."),
    CLIENT_GROUP_EXISTS("005", "The client group id is linked with an existing client group."),
    INVALID_DOMAIN("006", "not allowed to send invitation to requested email domain"),
    DOMAIN_NOT_FOUND("007", "domain not found in the allowed domain list"),
    CLIENT_GROUP_ADMIN_NOT_FOUND("008", "client group admin not found"),
    INVITATION_CODE_NOT_FOUND("009", "invitation code not found"),
    REQUEST_TOKEN_PUBLIC_KEY_ALREADY_STORED("010", "request token public key is already stored"),
    REQUEST_TOKEN_PUBLIC_KEY_NOT_FOUND("011", "request token public key not found"),
    CERTIFICATE_NOT_FOUND("012", "certificate not found"),
    CERTIFICATE_NAME_IN_USE("013", "The name is already in use for another certificate."),
    CERTIFICATE_CHAIN_INVALID("014", "The certificate chain is not valid."),
    CERTIFICATE_ALREADY_SIGNED("015", "The certificate is already signed."),
    PROVIDER_INFO_NOT_FOUND("016", "Could not find the key requirements for the selected filters."),
    WEBHOOK_URL_NOT_FOUND("017", "The webhook url does not exist."),
    WEBHOOK_URL_TAKEN("018", "The webhook url already exists."),
    TOO_MANY_JIRA_TICKETS("019", "There are already too many pending tickets created."),
    ALLOWED_IP_IN_WRONG_STATE("020", "The allowed IP is not in the expected state."),
    INVITE_WRONG_STATE("021", "The invite is not allowed to be used anymore."),
    INVITE_MAIL_IN_USE("022", "The supplied address is already invited or an admin."),
    INVITE_NOT_FOUND("023", "The invitation could not be found."),
    INVITE_IN_USE("024", "The invite is connected to an active portal user."),
    USER_IS_ADMIN("025", "The portal user is already a client admin."),
    USER_IS_GROUP_ADMIN("026", "The portal user is already a client group admin."),
    TOO_MANY_CLIENT_ADMIN_INVITES("027", "There are too many invited admins."),
    TOO_MANY_CERTIFICATES("028", "There are too many certificates already created."),
    ALLOWED_OUTBOUND_HOST_IN_WRONG_STATE("029", "The allowed outbound host is not in the expected state."),
    WEBHOOK_HOST_NOT_ALLOWED("030", "The webhook URLs host is not allowed."),
    WEBHOOK_MALFORMED("031", "The webhook url is not correctly formatted."),
    CERTIFICATE_EXISTS("032", "The provided client certificate is already stored in the system."),
    REDIRECT_URL_NOT_FOUND("033", "The redirect URL not found."),
    REDIRECT_URL_EXISTS("034", "The redirect URL already exists."),
    REDIRECT_URL_MALFORMED("035", "The redirect URL has wrong format."),
    CLIENT_HAS_USERS("036", "The client still has users."),
    DN_IS_DENIED("037", "The DN in the supplied certificate has already been denied in the past."),
    REDIRECT_URL_EXCEEDED("038", "The number of redirect URLs exceeded the allowed threshold."),
    REDIRECT_URL_HAS_LINKED_ONBOARDINGS("039", "The redirect URL cannot be deleted, there are still onboardings using it."),
    SITE_NOT_AVAILABLE("040", "The client site is not available."),
    PROVIDER_NOT_ENABLED("041", "The site provider is not enabled. Enable the provider by providing authentication means before continuing."),
    CLIENT_CONFIGURATION_ERROR("042", "Client configuration error."),
    SITE_ID_NOT_FOUND("043", "Site not found");

    private final String code;
    private final String message;

    ErrorConstants(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
