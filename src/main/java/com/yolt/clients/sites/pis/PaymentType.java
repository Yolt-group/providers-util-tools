package com.yolt.clients.sites.pis;

import java.util.List;

public enum PaymentType {
    SEPA_SINGLE(false, false),
    SEPA_SCHEDULED(false, true),
    SEPA_PERIODIC(true, false),
    UK_DOMESTIC_SINGLE(false, false),
    UK_DOMESTIC_SCHEDULED(false, true),
    UK_DOMESTIC_PERIODIC(true, false);

    private final boolean periodic;
    private final boolean scheduled;

    PaymentType(boolean periodic, boolean scheduled) {
        if (periodic && scheduled) {
            throw new IllegalArgumentException("A payment type cannot be periodic and scheduled.");
        }

        this.periodic = periodic;
        this.scheduled = scheduled;
    }

    public boolean isSingle() {
        return !periodic;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public boolean isPeriodic() {
        return periodic;
    }

    public boolean isSepa() {
        return List.of(SEPA_SINGLE, SEPA_PERIODIC, SEPA_SCHEDULED).contains(this);
    }

    public boolean isUkDomestic() {
        return List.of(UK_DOMESTIC_SINGLE, UK_DOMESTIC_PERIODIC, UK_DOMESTIC_SCHEDULED).contains(this);
    }
}
