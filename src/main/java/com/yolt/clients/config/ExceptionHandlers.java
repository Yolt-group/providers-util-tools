package com.yolt.clients.config;

import com.yolt.clients.client.ClientHasUsersException;
import com.yolt.clients.client.admins.*;
import com.yolt.clients.client.ipallowlist.AllowedIPNotInExpectedStateException;
import com.yolt.clients.client.ipallowlist.TooManyPendingTasksException;
import com.yolt.clients.client.mtlscertificates.MTLSCertificateExistsException;
import com.yolt.clients.client.mtlsdn.exceptions.DistinguishedNameDeniedException;
import com.yolt.clients.client.outboundallowlist.AllowedOutboundHostNotInExpectedStateException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLAlreadyExistsException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLCardinalityException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLMalformedException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLNotFoundException;
import com.yolt.clients.client.requesttokenpublickeys.exception.RequestTokenPublicKeyAlreadyStoredException;
import com.yolt.clients.client.requesttokenpublickeys.exception.RequestTokenPublicKeyNotFoundException;
import com.yolt.clients.client.webhooks.exceptions.OutboundHostNotAllowedException;
import com.yolt.clients.client.webhooks.exceptions.WebhookAlreadyExistsException;
import com.yolt.clients.client.webhooks.exceptions.WebhookMalformedException;
import com.yolt.clients.client.webhooks.exceptions.WebhookNotFoundException;
import com.yolt.clients.clientgroup.admins.*;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.*;
import com.yolt.clients.clientgroup.certificatemanagement.providers.exceptions.NoProviderInfoFoundException;
import com.yolt.clients.clientsite.ClientSiteConfigurationException;
import com.yolt.clients.clientsite.ClientSiteNotAvailableException;
import com.yolt.clients.clientsite.ProviderNotEnabledException;
import com.yolt.clients.exceptions.*;
import com.yolt.clients.sites.SiteNotFoundException;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.verification.exception.MismatchedClientGroupIdAndClientTokenException;
import nl.ing.lovebird.clienttokens.verification.exception.MismatchedClientIdAndClientTokenException;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.errorhandling.ExceptionHandlingService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class ExceptionHandlers {

    private final ExceptionHandlingService service;

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.CLIENT_GROUP_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(ClientNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.CLIENT_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    protected ErrorDTO handle(MismatchedClientGroupIdAndClientTokenException ex) {
        return service.logAndConstruct(ErrorConstants.TOKEN_MISMATCH, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    protected ErrorDTO handle(MismatchedClientIdAndClientTokenException ex) {
        return service.logAndConstruct(ErrorConstants.TOKEN_MISMATCH, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    protected ErrorDTO handle(ClientTokenMisMatchException ex) {
        return service.logAndConstruct(ErrorConstants.TOKEN_MISMATCH, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    protected ErrorDTO handle(ClientAlreadyExistsException ex) {
        return service.logAndConstruct(ErrorConstants.CLIENT_EXISTS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupAlreadyExistsException ex) {
        return service.logAndConstruct(ErrorConstants.CLIENT_GROUP_EXISTS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    protected ErrorDTO handle(InvalidDomainException ex) {
        return service.logAndConstruct(ErrorConstants.INVALID_DOMAIN, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(DomainNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.DOMAIN_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupAdminNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.CLIENT_GROUP_ADMIN_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(InvitationCodeNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.INVITATION_CODE_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    protected ErrorDTO handle(RequestTokenPublicKeyAlreadyStoredException ex) {
        return service.logAndConstruct(ErrorConstants.REQUEST_TOKEN_PUBLIC_KEY_ALREADY_STORED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(RequestTokenPublicKeyNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.REQUEST_TOKEN_PUBLIC_KEY_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(CertificateNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.CERTIFICATE_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(NameIsAlreadyUsedException ex) {
        return service.logAndConstruct(ErrorConstants.CERTIFICATE_NAME_IN_USE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(CertificateValidationException ex) {
        return service.logAndConstruct(ErrorConstants.CERTIFICATE_CHAIN_INVALID, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(CertificateAlreadySignedException ex) {
        return service.logAndConstruct(ErrorConstants.CERTIFICATE_ALREADY_SIGNED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(NoProviderInfoFoundException ex) {
        return service.logAndConstruct(ErrorConstants.PROVIDER_INFO_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(WebhookAlreadyExistsException ex) {
        return service.logAndConstruct(ErrorConstants.WEBHOOK_URL_TAKEN, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(WebhookNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.WEBHOOK_URL_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ResponseBody
    protected ErrorDTO handle(TooManyPendingTasksException ex) {
        return service.logAndConstruct(ErrorConstants.TOO_MANY_JIRA_TICKETS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(AllowedIPNotInExpectedStateException ex) {
        return service.logAndConstruct(ErrorConstants.ALLOWED_IP_IN_WRONG_STATE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(AllowedOutboundHostNotInExpectedStateException ex) {
        return service.logAndConstruct(ErrorConstants.ALLOWED_OUTBOUND_HOST_IN_WRONG_STATE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(OutboundHostNotAllowedException ex) {
        return service.logAndConstruct(ErrorConstants.WEBHOOK_HOST_NOT_ALLOWED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(WebhookMalformedException ex) {
        return service.logAndConstruct(ErrorConstants.WEBHOOK_HOST_NOT_ALLOWED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupAdminEmailInUseException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_MAIL_IN_USE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupAdminInvitationNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupAdminInviteInInvalidStateException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_WRONG_STATE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupAdminInviteUsedException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_IN_USE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientAdminEmailInUseException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_MAIL_IN_USE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(ClientAdminInvitationNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientAdminInviteInInvalidStateException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_WRONG_STATE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientAdminInviteUsedException ex) {
        return service.logAndConstruct(ErrorConstants.INVITE_IN_USE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(PortalUserIsClientAdminException ex) {
        return service.logAndConstruct(ErrorConstants.USER_IS_ADMIN, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(PortalUserIsClientGroupAdminException ex) {
        return service.logAndConstruct(ErrorConstants.USER_IS_GROUP_ADMIN, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(TooManyClientAdminInvitesException ex) {
        return service.logAndConstruct(ErrorConstants.TOO_MANY_CLIENT_ADMIN_INVITES, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ResponseBody
    protected ErrorDTO handle(TooManyCertificatesException ex) {
        return service.logAndConstruct(ErrorConstants.TOO_MANY_CERTIFICATES, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    protected ErrorDTO handle(MTLSCertificateExistsException ex) {
        return service.logAndConstruct(ErrorConstants.CERTIFICATE_EXISTS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(RedirectURLAlreadyExistsException ex) {
        return service.logAndConstruct(ErrorConstants.REDIRECT_URL_EXISTS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(RedirectURLCardinalityException ex) {
        return service.logAndConstruct(ErrorConstants.REDIRECT_URL_EXCEEDED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(RedirectURLNotFoundException ex) {
        return service.logAndConstruct(ErrorConstants.REDIRECT_URL_NOT_FOUND, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(RedirectURLMalformedException ex) {
        return service.logAndConstruct(ErrorConstants.REDIRECT_URL_MALFORMED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientHasUsersException ex) {
        return service.logAndConstruct(ErrorConstants.CLIENT_HAS_USERS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(DistinguishedNameDeniedException ex) {
        return service.logAndConstruct(ErrorConstants.DN_IS_DENIED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(CannotDeleteRedirectUrlException ex) {
        return service.logAndConstruct(ErrorConstants.REDIRECT_URL_HAS_LINKED_ONBOARDINGS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handle(final ClientSiteNotAvailableException e) {
        return service.logAndConstruct(ErrorConstants.SITE_NOT_AVAILABLE, e);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handle(final ProviderNotEnabledException e) {
        return service.logAndConstruct(ErrorConstants.PROVIDER_NOT_ENABLED, e);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handle(final ClientSiteConfigurationException e) {
        return service.logAndConstruct(ErrorConstants.CLIENT_CONFIGURATION_ERROR, e);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDTO handle(final SiteNotFoundException e) {
        return service.logAndConstruct(ErrorConstants.SITE_ID_NOT_FOUND, e);
    }
}
