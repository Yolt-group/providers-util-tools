package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.DistinguishedNameElement;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyMaterialRequirements;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyRequirementsWrapper;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.ServiceInfo;
import lombok.experimental.UtilityClass;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class EIDASHelper {

    private static final String RSA_2048 = "RSA2048";
    private static final String RSA_4096 = "RSA4096";
    private static final String SHA_256_WITH_RSA = "SHA256_WITH_RSA";

    public static Map<ServiceType, ServiceInfo> getServiceInfoMap() {
        Map<ServiceType, ServiceInfo> services = new EnumMap<>(ServiceType.class);

        KeyRequirementsWrapper signingKeyRequirements = new KeyRequirementsWrapper(new KeyMaterialRequirements(
                Set.of(RSA_2048, RSA_4096),
                Set.of(SHA_256_WITH_RSA),
                Arrays.asList(
                        new DistinguishedNameElement("C", "", "Country Code", true),
                        new DistinguishedNameElement("ST", "", "State / Province", true),
                        new DistinguishedNameElement("L", "", "City (Locality Name)", true),
                        new DistinguishedNameElement("STREET", "", "Street / First line of address", true),
                        new DistinguishedNameElement("O", "", "Organization name", true),
                        new DistinguishedNameElement("OU", "", "Organizational unit", true),
                        new DistinguishedNameElement("2.5.4.97", "", "National PSP Identifier (XXXXX-XXX-123456)", true),

                        // Entrust mentioned that CN should be equal to the O in mailing contact on the 27th of June.
                        new DistinguishedNameElement("CN", "", "Common name (should be equal to 'O')", true)
                )));
        KeyRequirementsWrapper transportKeyRequirements = new KeyRequirementsWrapper(new KeyMaterialRequirements(
                Set.of(RSA_2048, RSA_4096),
                Set.of(SHA_256_WITH_RSA),
                Arrays.asList(
                        new DistinguishedNameElement("C", "", "Country Code", true),
                        new DistinguishedNameElement("ST", "", "State / Province", true),
                        new DistinguishedNameElement("L", "", "City (Locality Name)", true),
                        new DistinguishedNameElement("STREET", "", "Street / First line of address", true),
                        new DistinguishedNameElement("O", "", "Organization name", true),
                        new DistinguishedNameElement("OU", "", "Organizational unit", true),
                        new DistinguishedNameElement("2.5.4.97", "", "National PSP Identifier (XXXXX-XXX-123456)", true),
                        new DistinguishedNameElement("CN", "yolt.io", "Common name (SAN)", true)
                )));

        ServiceInfo serviceInfo = new ServiceInfo(signingKeyRequirements, transportKeyRequirements);

        services.put(ServiceType.AIS, serviceInfo);
        services.put(ServiceType.PIS, serviceInfo);
        services.put(ServiceType.AS, serviceInfo);
        services.put(ServiceType.IC, serviceInfo);

        return services;
    }
}
