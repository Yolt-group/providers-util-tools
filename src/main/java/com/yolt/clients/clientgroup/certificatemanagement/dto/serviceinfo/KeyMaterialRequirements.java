package com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo;

import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
public class KeyMaterialRequirements {
    Set<String> keyAlgorithms;
    Set<String> signatureAlgorithms;
    List<DistinguishedNameElement> distinguishedNames;
}
