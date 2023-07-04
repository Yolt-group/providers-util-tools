package com.yolt.clients.sites.pis;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
public class DynamicFields {

    CreditorAgentBic creditorAgentBic;
    CreditorAgentName creditorAgentName;
    RemittanceInformationStructured remittanceInformationStructured;
    CreditorPostalAddressLine creditorPostalAddressLine;
    CreditorPostalCountry creditorPostalCountry;
    DebtorName debtorName;


    public DynamicFields(CreditorAgentBic creditorAgentBic,
                         CreditorAgentName creditorAgentName,
                         RemittanceInformationStructured remittanceInformationStructured,
                         CreditorPostalAddressLine creditorPostalAddressLine,
                         CreditorPostalCountry creditorPostalCountry,
                         DebtorName debtorName) {
        this.creditorAgentBic = creditorAgentBic;
        this.creditorAgentName = creditorAgentName;
        this.remittanceInformationStructured = remittanceInformationStructured;
        this.creditorPostalAddressLine = creditorPostalAddressLine;
        this.creditorPostalCountry = creditorPostalCountry;
        this.debtorName = debtorName;
    }

    @Value
    @RequiredArgsConstructor
    public static class CreditorAgentBic {
        boolean required;
    }


    @Value
    @RequiredArgsConstructor
    public static class CreditorAgentName {
        boolean required;
    }

    @Value
    @RequiredArgsConstructor
    public static class RemittanceInformationStructured {
        boolean required;
    }

    @Value
    @RequiredArgsConstructor
    public static class CreditorPostalAddressLine {
        boolean required;
    }

    @Value
    @RequiredArgsConstructor
    public static class CreditorPostalCountry {
        boolean required;
    }

    @Value
    @RequiredArgsConstructor
    public static class DebtorName {
        boolean required;
    }
}
