package com.yolt.clients.clientsite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import nl.ing.lovebird.providerdomain.ServiceType;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "AuthenticationMeansScope", description = "Authentication means can be registered in two ways; for the client, or for a specific redirectUrl.")
public class AuthenticationMeansScope {
    @Schema(description = "The way in which authentication means have been registered.", required = true)
    private final @NotNull Type type;
    @ArraySchema(arraySchema = @Schema(description = "The redirect url id in case of Type.REDIRECT_URL, null otherwise."))
    private final List<UUID> redirectUrlId;
    @Schema(description = "The serviceType where these authentication means are scoped to.", example = "AIS")
    private final ServiceType serviceType;

    public enum Type {
        CLIENT, REDIRECT_URL
    }

    /**
     * Return a {@see Predicate} which test the {@see ServiceType}
     *
     * @param serviceType the service-type to test
     * @return the predicate
     */
    public static Predicate<AuthenticationMeansScope> hasServiceType(final ServiceType serviceType) {
        return scope -> scope.getServiceType() == serviceType;
    }

    /**
     * Return a {@see Predicate} which test the {@see AuthenticationMeansScope.Type}
     *
     * @param type the type to test
     * @return the predicate
     */
    public static Predicate<AuthenticationMeansScope> hasType(final Type type) {
        return scope -> scope.getType() == type;
    }
}
