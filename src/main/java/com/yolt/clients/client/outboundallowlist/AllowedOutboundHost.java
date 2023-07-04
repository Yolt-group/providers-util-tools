package com.yolt.clients.client.outboundallowlist;

import com.yolt.clients.jira.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allowed_outbound_hosts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllowedOutboundHost {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "host")
    private String host;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "jira_ticket")
    private String jiraTicket;

    public AllowedOutboundHost(UUID allowedOutboundHostId, UUID clientId, String host, Status status, LocalDateTime lastUpdated) {
        this(allowedOutboundHostId, clientId, host, status, lastUpdated, null);
    }
}
