package com.yolt.clients.client.outboundallowlist.dto;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameParameters;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class HostnameValidator implements ConstraintValidator<Hostname, String> {

    @Override
    public void initialize(Hostname constraint) {
    }

    @Override
    public boolean isValid(String payload, ConstraintValidatorContext context) {
        HostName host = new HostName(payload, new HostNameParameters.Builder()
                .allowEmpty(false)
                .setEmptyAsLoopback(false)
                .allowIPAddress(false)
                .allowPort(false)
                .allowService(false)
                .toParams());
        return host.isValid();
    }
}
