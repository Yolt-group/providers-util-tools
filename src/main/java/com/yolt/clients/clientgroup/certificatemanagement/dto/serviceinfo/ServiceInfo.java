package com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo;

import lombok.Value;

/**
 * Contains information about a service (AIS, PIS) such as what authentication means are required.
 */
@Value
public class ServiceInfo {
   KeyRequirementsWrapper signing;
   KeyRequirementsWrapper transport;
}
