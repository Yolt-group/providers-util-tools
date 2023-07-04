package com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo;

import lombok.Value;

@Value
public class DistinguishedNameElement {
   String type;
   String value;
   String placeholder;
   boolean editable;
}
