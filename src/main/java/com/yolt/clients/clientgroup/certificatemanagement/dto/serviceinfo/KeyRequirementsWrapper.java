package com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo;

import lombok.Value;

/**
 * This is just a wrapper, because we are not interested in the specifics differences between signing and transport.
 */
@Value
public class KeyRequirementsWrapper {
    KeyMaterialRequirements keyRequirements;
}
