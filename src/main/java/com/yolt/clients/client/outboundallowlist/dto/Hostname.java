package com.yolt.clients.client.outboundallowlist.dto;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HostnameValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Hostname {
    String message() default "Invalid hostname";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
