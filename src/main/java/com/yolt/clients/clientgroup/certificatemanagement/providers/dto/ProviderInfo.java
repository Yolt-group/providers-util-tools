package com.yolt.clients.clientgroup.certificatemanagement.providers.dto;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyMaterialRequirements;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyRequirementsWrapper;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.ServiceInfo;
import com.yolt.clients.clientgroup.certificatemanagement.providers.exceptions.NoProviderInfoFoundException;
import lombok.Value;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
public class ProviderInfo {
    String displayName;
    Map<ServiceType, ServiceInfo> services;

    public List<KeyMaterialRequirements> getKeyRequirements(Set<ServiceType> serviceTypes, CertificateUsageType usageType) {
        return switch (usageType) {
            case SIGNING -> getKeyRequirements(serviceTypes, ServiceInfo::getSigning, usageType);
            case TRANSPORT -> getKeyRequirements(serviceTypes, ServiceInfo::getTransport, usageType);
        };
    }

    private List<KeyMaterialRequirements> getKeyRequirements(Set<ServiceType> serviceTypes, Function<ServiceInfo, KeyRequirementsWrapper> keyRequirementsFunction, CertificateUsageType usageType) {
        return serviceTypes.stream()
                .map(serviceType ->
                        Optional.ofNullable(services.get(serviceType))
                                .orElseThrow(() -> new NoProviderInfoFoundException("No provider info for serviceType " + serviceType + " and usageType " + usageType))
                )
                .map(serviceInfo ->
                        Optional.ofNullable(keyRequirementsFunction.apply(serviceInfo))
                                .orElseThrow(() -> new NoProviderInfoFoundException("No provider info for usageType " + usageType))
                )
                .map(keyRequirementsWrapper ->
                        Optional.ofNullable(keyRequirementsWrapper.getKeyRequirements())
                                .orElseThrow(() -> new NoProviderInfoFoundException("No provider info for usageType " + usageType + ", wrapper is empty"))
                )
                .collect(Collectors.toList());
    }
}
