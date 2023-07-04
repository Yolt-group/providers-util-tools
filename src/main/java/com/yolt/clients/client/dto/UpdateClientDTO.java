package com.yolt.clients.client.dto;

import lombok.Value;
import nl.ing.lovebird.validation.sbi.SBICode;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Value
public class UpdateClientDTO {

    @NotBlank
    @Size(min = 1, max = 80)
    String name;

    String kycCountryCode;
    boolean kycPrivateIndividuals;
    boolean kycEntities;

    @SBICode
    String sbiCode;

    @Range(min = 1, max = 5000, message = "The grace period should be between 1 and 5000 days")
    Integer gracePeriodInDays;

    boolean dataEnrichmentMerchantRecognition;
    boolean dataEnrichmentCategorization;
    boolean dataEnrichmentCycleDetection;
    boolean dataEnrichmentLabels;
    boolean cam;
    boolean ais;
    boolean pis;
    boolean consentStarter;
    boolean oneOffAis;
    boolean riskInsights;

    Long jiraId;
}
