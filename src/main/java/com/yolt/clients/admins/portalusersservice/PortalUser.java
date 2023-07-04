package com.yolt.clients.admins.portalusersservice;

import lombok.Value;

import java.util.UUID;

@Value
public class PortalUser {
    UUID id;
    String name;
    String email;
    String organisation;
    UUID inviteId;
}
