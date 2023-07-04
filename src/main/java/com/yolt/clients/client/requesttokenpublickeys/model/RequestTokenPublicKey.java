package com.yolt.clients.client.requesttokenpublickeys.model;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "request_token_public_key")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(RequestTokenPublicKey.RequestTokenPublicKeyId.class)
public class RequestTokenPublicKey {

    @Column(name = "client_id")
    @Id
    UUID clientId;

    @Column(name = "key_id")
    @Id
    String keyId;

    @Column(name = "request_token_public_key")
    String requestTokenPublicKey;

    @Column(name = "created")
    LocalDateTime created;

    @Column(name = "updated")
    LocalDateTime updated;

    public RequestTokenPublicKey(UUID clientId, String keyId, String decodedKey, LocalDateTime now) {
        this(clientId, keyId, decodedKey, now, null);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RequestTokenPublicKeyId implements Serializable {
        UUID clientId;
        String keyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        RequestTokenPublicKey that = (RequestTokenPublicKey) o;

        if (!Objects.equals(clientId, that.clientId)) return false;
        return Objects.equals(keyId, that.keyId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(clientId);
        result = 31 * result + (Objects.hashCode(keyId));
        return result;
    }
}
