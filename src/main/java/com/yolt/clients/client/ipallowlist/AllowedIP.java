package com.yolt.clients.client.ipallowlist;

import com.yolt.clients.jira.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ip_allow_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllowedIP {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "cidr")
    private String cidr;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "jira_ticket")
    private String jiraTicket;

    public AllowedIP(UUID allowedIPId, UUID clientId, String cidr, Status status, LocalDateTime lastUpdated) {
        this(allowedIPId, clientId, cidr, status, lastUpdated, null);
    }
}
