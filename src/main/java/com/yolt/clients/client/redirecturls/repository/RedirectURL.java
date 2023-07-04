package com.yolt.clients.client.redirecturls.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "redirect_url")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RedirectURL.RedirectURLId.class)
public class RedirectURL {

    @Id
    @Column(name = "client_id")
    private UUID clientId;

    @Id
    @Column(name = "redirect_url_id")
    private UUID redirectURLId;

    @Column(name = "redirect_url")
    private String redirectURL;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RedirectURLId implements Serializable {
        UUID clientId;
        UUID redirectURLId;
    }
}
