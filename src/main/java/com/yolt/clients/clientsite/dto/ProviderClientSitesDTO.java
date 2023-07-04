package com.yolt.clients.clientsite.dto;

import com.yolt.clients.clientsite.AuthenticationMeansScope;
import com.yolt.clients.clientsite.dto.ClientSiteDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProviderClientSitesDTO {
    String provider;
    List<AuthenticationMeansScope> registeredAuthenticationMeans;
    List<ClientSiteDTO> sites;
}
