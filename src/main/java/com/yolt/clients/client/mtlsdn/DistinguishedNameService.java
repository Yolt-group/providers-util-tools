package com.yolt.clients.client.mtlsdn;

import com.yolt.clients.client.ipallowlist.TooManyPendingTasksException;
import com.yolt.clients.client.mtlsdn.dto.DistinguishedNameDTO;
import com.yolt.clients.client.mtlsdn.dto.DistinguishedNameIdListDTO;
import com.yolt.clients.client.mtlsdn.dto.NewDistinguishedNameDTO;
import com.yolt.clients.client.mtlsdn.exceptions.DistinguishedNameDeniedException;
import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDN;
import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDNRepository;
import com.yolt.clients.jira.JiraService;
import com.yolt.clients.jira.Status;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.yolt.clients.jira.Status.*;

@Service
@Slf4j
public class DistinguishedNameService {
    private static final Set<Status> ACTIVE_STATUSES = Set.of(ADDED, PENDING_REMOVAL);
    private static final Set<Status> PENDING_STATUSES = Set.of(PENDING_ADDITION);

    private final ClientMTLSCertificateDNRepository clientMTLSCertificateDNRepository;
    private final JiraService jiraService;
    private final Clock clock;
    private final int maxOpenTicketsInLast24Hours;

    public DistinguishedNameService(ClientMTLSCertificateDNRepository clientMTLSCertificateDNRepository,
                                    JiraService jiraService,
                                    Clock clock,
                                    @Value("${yolt.jira.max_open_tickets}") int maxOpenTicketsInLast24Hours) {
        this.clientMTLSCertificateDNRepository = clientMTLSCertificateDNRepository;
        this.jiraService = jiraService;
        this.clock = clock;
        this.maxOpenTicketsInLast24Hours = maxOpenTicketsInLast24Hours;
    }

    public boolean hasActiveCertificates(UUID clientId) {
        return clientMTLSCertificateDNRepository.existsByClientIdAndStatusIn(clientId, ACTIVE_STATUSES);
    }

    public boolean hasPendingCertificates(UUID clientId) {
        return clientMTLSCertificateDNRepository.existsByClientIdAndStatusIn(clientId, PENDING_STATUSES);
    }

    public Set<DistinguishedNameDTO> findAll(UUID clientId) {
        return clientMTLSCertificateDNRepository
                .findAllByClientId(clientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    public DistinguishedNameDTO create(ClientToken clientToken, NewDistinguishedNameDTO newDistinguishedNameDTO) {
        UUID clientId = clientToken.getClientIdClaim();
        rateLimit(clientId);

        var certificate = getClientCertificate(newDistinguishedNameDTO.getCertificateChain());

        final var subjectDN = StringUtils.substring(certificate.getSubject().toString(), 0, 1024);
        final var issuerDN = StringUtils.substring(certificate.getIssuer().toString(), 0, 1024);

        return clientMTLSCertificateDNRepository.findByClientIdAndSubjectDNAndIssuerDN(clientId, subjectDN, issuerDN)
                .map(clientMTLSCertificateDN -> switch (clientMTLSCertificateDN.getStatus()) {
                    case PENDING_ADDITION, ADDED -> clientMTLSCertificateDN;
                    case DENIED -> throw new DistinguishedNameDeniedException(clientMTLSCertificateDN);
                    case PENDING_REMOVAL -> cancelRemoval(clientMTLSCertificateDN);
                    case REMOVED -> reAddDN(clientToken, clientMTLSCertificateDN);
                })
                .map(this::mapToDTO)
                .orElseGet(() -> {
                    ClientMTLSCertificateDN clientMTLSCertificateDN = new ClientMTLSCertificateDN(
                            UUID.randomUUID(),
                            clientId,
                            subjectDN,
                            issuerDN,
                            newDistinguishedNameDTO.getCertificateChain(),
                            Status.PENDING_ADDITION,
                            LocalDateTime.now(clock),
                            LocalDateTime.now(clock),
                            null
                    );
                    var jiraData = new JiraDistinguishedNameData();
                    jiraData.withItemToBeAdded(clientMTLSCertificateDN);
                    var jiraTicket = jiraService.createIssue(clientToken, jiraData);
                    clientMTLSCertificateDN.setJiraTicket(jiraTicket);

                    return mapToDTO(clientMTLSCertificateDNRepository.save(clientMTLSCertificateDN));
                });
    }

    public Set<DistinguishedNameDTO> deleteLimited(ClientToken clientToken, DistinguishedNameIdListDTO itemsToRemove) {
        UUID clientId = clientToken.getClientIdClaim();
        rateLimit(clientId);
        var jiraData = new JiraDistinguishedNameData();
        return applyFunction(
                clientToken,
                itemsToRemove,
                jiraData,
                clientMTLSCertificateDN -> {
                    switch (clientMTLSCertificateDN.getStatus()) {
                        case PENDING_REMOVAL, REMOVED -> log.info("Already in deleted state (%s), did not request deleting DN: %s".formatted(clientMTLSCertificateDN.getStatus(), clientMTLSCertificateDN.getSubjectDN()));
                        case PENDING_ADDITION -> {
                            jiraData.withJiraTicketToBeEdited(clientMTLSCertificateDN.getJiraTicket(), clientMTLSCertificateDN);
                            clientMTLSCertificateDN.setStatus(Status.REMOVED);
                            clientMTLSCertificateDN.setJiraTicket(null);
                            clientMTLSCertificateDN.setUpdatedAt(LocalDateTime.now(clock));
                        }
                        case ADDED -> {
                            jiraData.withItemToBeRemoved(clientMTLSCertificateDN);
                            clientMTLSCertificateDN.setStatus(Status.PENDING_REMOVAL);
                            clientMTLSCertificateDN.setJiraTicket(null);
                            clientMTLSCertificateDN.setUpdatedAt(LocalDateTime.now(clock));
                        }
                        default -> log.warn("Wrong state (%s), did not request deleting DN: %s".formatted(clientMTLSCertificateDN.getStatus(), clientMTLSCertificateDN.getSubjectDN()));
                    }
                    return clientMTLSCertificateDN;
                }
        );
    }

    public Set<DistinguishedNameDTO> markApplied(ClientToken clientToken, DistinguishedNameIdListDTO itemsToMarkApplied) {
        var jiraData = new JiraDistinguishedNameData();
        return applyFunction(
                clientToken,
                itemsToMarkApplied,
                jiraData,
                clientMTLSCertificateDN -> {
                    switch (clientMTLSCertificateDN.getStatus()) {
                        case ADDED, REMOVED -> log.info("Already in applied state (%s), did not mark DN: %s as applied".formatted(clientMTLSCertificateDN.getStatus(), clientMTLSCertificateDN.getSubjectDN()));
                        case PENDING_ADDITION -> {
                            clientMTLSCertificateDN.setStatus(Status.ADDED);
                            clientMTLSCertificateDN.setJiraTicket(null);
                            clientMTLSCertificateDN.setUpdatedAt(LocalDateTime.now(clock));
                        }
                        case PENDING_REMOVAL -> {
                            clientMTLSCertificateDN.setStatus(Status.REMOVED);
                            clientMTLSCertificateDN.setJiraTicket(null);
                            clientMTLSCertificateDN.setUpdatedAt(LocalDateTime.now(clock));
                        }
                        default -> log.warn("Wrong state (%s), did not mark DN: %s as applied".formatted(clientMTLSCertificateDN.getStatus(), clientMTLSCertificateDN.getSubjectDN()));
                    }
                    return clientMTLSCertificateDN;
                }
        );
    }

    public Set<DistinguishedNameDTO> markDenied(ClientToken clientToken, DistinguishedNameIdListDTO itemsToMarkDenied) {
        var jiraData = new JiraDistinguishedNameData();
        return applyFunction(
                clientToken,
                itemsToMarkDenied,
                jiraData,
                clientMTLSCertificateDN -> {
                    switch (clientMTLSCertificateDN.getStatus()) {
                        case DENIED -> log.info("Already in denied state, did not mark DN: %s as denied".formatted(clientMTLSCertificateDN.getSubjectDN()));
                        case PENDING_ADDITION -> {
                            clientMTLSCertificateDN.setStatus(Status.DENIED);
                            clientMTLSCertificateDN.setJiraTicket(null);
                            clientMTLSCertificateDN.setUpdatedAt(LocalDateTime.now(clock));
                        }
                        default -> log.warn("Wrong state (%s), did not mark DN: %s as denied".formatted(clientMTLSCertificateDN.getStatus(), clientMTLSCertificateDN.getSubjectDN()));
                    }
                    return clientMTLSCertificateDN;
                }
        );
    }

    private Set<DistinguishedNameDTO> applyFunction(ClientToken clientToken, DistinguishedNameIdListDTO items, JiraDistinguishedNameData jiraData, UnaryOperator<ClientMTLSCertificateDN> function) {
        var clientId = clientToken.getClientIdClaim();

        var intermediate = items.getIds().stream()
                .map(id -> clientMTLSCertificateDNRepository.findByClientIdAndId(clientId, id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(function)
                .collect(Collectors.toSet());

        var jiraTicket = jiraService.createIssue(clientToken, jiraData);
        jiraService.updateIssue(jiraData);

        return intermediate.stream()
                .map(clientMTLSCertificateDN -> {
                    if (jiraData.contains(clientMTLSCertificateDN)) {
                        clientMTLSCertificateDN.setJiraTicket(jiraTicket);
                    }
                    return clientMTLSCertificateDN;
                })
                .map(clientMTLSCertificateDNRepository::save)
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    private DistinguishedNameDTO mapToDTO(ClientMTLSCertificateDN clientMTLSCertificateDN) {
        return new DistinguishedNameDTO(
                clientMTLSCertificateDN.getId(),
                clientMTLSCertificateDN.getClientId(),
                clientMTLSCertificateDN.getSubjectDN(),
                clientMTLSCertificateDN.getIssuerDN(),
                clientMTLSCertificateDN.getCertificateChain(),
                clientMTLSCertificateDN.getStatus(),
                clientMTLSCertificateDN.getCreatedAt(),
                clientMTLSCertificateDN.getUpdatedAt(),
                clientMTLSCertificateDN.getJiraTicket()
        );
    }

    private void rateLimit(UUID clientId) {
        LocalDateTime countAfter = LocalDateTime.now(clock).minusDays(1);
        long numberOfJiraTickets = clientMTLSCertificateDNRepository.findAllByClientIdAndUpdatedAtAfterAndJiraTicketNotNull(clientId, countAfter)
                .stream()
                .map(ClientMTLSCertificateDN::getJiraTicket)
                .distinct()
                .count();
        if (numberOfJiraTickets >= maxOpenTicketsInLast24Hours) {
            throw new TooManyPendingTasksException(clientId);
        }
    }

    @SneakyThrows
    private X509CertificateHolder getClientCertificate(String certificateChainString) {
        PEMParser pemParser = new PEMParser(new StringReader(certificateChainString));
        return (X509CertificateHolder) pemParser.readObject();
    }

    private ClientMTLSCertificateDN reAddDN(ClientToken clientToken, ClientMTLSCertificateDN clientMTLSCertificateDN) {
        clientMTLSCertificateDN.setStatus(Status.PENDING_ADDITION);
        clientMTLSCertificateDN.setUpdatedAt(LocalDateTime.now(clock));
        var jiraData = new JiraDistinguishedNameData();
        jiraData.withItemToBeAdded(clientMTLSCertificateDN);
        var jiraTicket = jiraService.createIssue(clientToken, jiraData);
        clientMTLSCertificateDN.setJiraTicket(jiraTicket);
        return clientMTLSCertificateDNRepository.save(clientMTLSCertificateDN);
    }

    private ClientMTLSCertificateDN cancelRemoval(ClientMTLSCertificateDN clientMTLSCertificateDN) {
        var jiraData = new JiraDistinguishedNameData();
        jiraData.withJiraTicketToBeEdited(clientMTLSCertificateDN.getJiraTicket(), clientMTLSCertificateDN);
        clientMTLSCertificateDN.setStatus(Status.ADDED);
        clientMTLSCertificateDN.setJiraTicket(null);
        clientMTLSCertificateDN.setUpdatedAt(LocalDateTime.now(clock));
        jiraService.updateIssue(jiraData);
        return clientMTLSCertificateDNRepository.save(clientMTLSCertificateDN);
    }
}
