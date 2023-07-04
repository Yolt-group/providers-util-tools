package com.yolt.clients.model;

import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "client")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class Client {

    @Id
    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "client_group_id")
    private UUID clientGroupId;

    @Column(name = "name")
    private String name;

    @Column(name = "kyc_country_code")
    private String kycCountryCode;

    @Column(name = "kyc_private_individuals")
    private boolean kycPrivateIndividuals;

    @Column(name = "kyc_entities")
    private boolean kycEntities;

    @Column(name = "sbi_code")
    private String sbiCode;

    @Column(name = "grace_period_in_days")
    private Integer gracePeriodInDays;

    @Column(name = "data_enrichment_merchant_recognition")
    private boolean dataEnrichmentMerchantRecognition;

    @Column(name = "data_enrichment_categorization")
    private boolean dataEnrichmentCategorization;

    @Column(name = "data_enrichment_cycle_detection")
    private boolean dataEnrichmentCycleDetection;

    @Column(name = "data_enrichment_labels")
    private boolean dataEnrichmentLabels;

    @Column(name = "cam")
    private boolean cam;

    @Column(name = "psd2_licensed")
    private boolean psd2Licensed;

    @Column(name = "ais")
    private boolean ais;

    @Column(name = "pis")
    private boolean pis;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "consent_starter")
    private boolean consentStarter;

    @Column(name = "one_off_ais")
    private boolean oneOffAis;

    @Column(name = "risk_insights")
    private boolean riskInsights;

    @Column(name = "jira_id")
    private Long jiraId;

    /**
     * @deprecated see YCL-2517
     */
    @Deprecated(forRemoval = true)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
    private Set<ClientAdminInvitation> clientAdminInvitations;
}
