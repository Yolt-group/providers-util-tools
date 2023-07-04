package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.dto.RedirectURLLicensedDTO;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedirectURLLicensedService {

    private final RedirectURLRepository redirectURLRepository;

    public List<RedirectURLLicensedDTO> findAll(final UUID clientId) {
        return redirectURLRepository
                .findAllByClientId(clientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private RedirectURLLicensedDTO mapToDTO(final RedirectURL redirectURL) {
        return new RedirectURLLicensedDTO(
                redirectURL.getRedirectURLId(),
                redirectURL.getRedirectURL()
        );
    }
}
