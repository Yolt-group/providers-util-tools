package com.yolt.clients.sites;

import com.yolt.clients.sites.ProvidersSites.RegisteredPisSite;
import com.yolt.clients.sites.ProvidersSites.RegisteredPisSite.ProvidersPaymentType;
import com.yolt.clients.sites.ProvidersSites.RegisteredSite;
import com.yolt.clients.sites.ais.LoginRequirement;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.yolt.clients.sites.CountryCode.*;
import static com.yolt.clients.sites.ais.ConsentBehavior.CONSENT_PER_ACCOUNT;
import static java.util.List.of;
import static nl.ing.lovebird.providerdomain.AccountType.*;
import static nl.ing.lovebird.providerdomain.ServiceType.AIS;
import static nl.ing.lovebird.providerdomain.ServiceType.PIS;

public class TestProviderSites {
    private TestProviderSites() {
    }

    public static final UUID PIS_ENABLED_SITE_ID = UUID.randomUUID();
    public static final UUID PIS_DIABLED_SITE_ID = UUID.fromString("7670247e-323e-4275-82f6-87f31119dbd3");

    public static final RegisteredSite MONZO_SITE = new RegisteredSite(
            "Monzo",
            "MONZO",
            "MONZO",
            UUID.randomUUID(),
            List.of(CURRENT_ACCOUNT, SAVINGS_ACCOUNT),
            List.of(NL, GB),
            null,
            null,
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT), PIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite YOLT_TEST_BANK = new RegisteredSite("Yolt test bank",
            "YOLT_PROVIDER",
            null,
            PIS_ENABLED_SITE_ID,
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT, PREPAID_ACCOUNT, PENSION, INVESTMENT),
            of(GB, FR, IT, ES, BE, DE, NL, PL, CZ),
            null,
            null,
            null,
            Map.of(AIS, List.of(LoginRequirement.REDIRECT), PIS, List.of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredPisSite YOLT_TEST_BANK_UK_PIS_DETAILS = new RegisteredPisSite(
            PIS_ENABLED_SITE_ID,
            "YOLT_PROVIDER",
            true,
            ProvidersPaymentType.SINGLE,
            null,
            false,
            RegisteredPisSite.PaymentMethod.UKDOMESTIC,
            null);

    public static final RegisteredPisSite YOLT_TEST_BANK_SEPA_PIS_DETAILS = new RegisteredPisSite(
            PIS_ENABLED_SITE_ID,
            "YOLT_PROVIDER",
            true,
            ProvidersPaymentType.SINGLE,
            null,
            false,
            RegisteredPisSite.PaymentMethod.SEPA,
            null);

    public static final ProvidersSites.RegisteredSite PIS_DISABLED_SITE = new ProvidersSites.RegisteredSite("Abn-Amro",
            "ABN_AMRO",
            null,
            PIS_DIABLED_SITE_ID,
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT, PREPAID_ACCOUNT, PENSION, INVESTMENT),
            of(GB, FR, IT, ES, BE, DE, NL, PL, CZ),
            null,
            null,
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT))
    );

    public static final ProvidersSites.RegisteredSite CREDIT_AGRICOLE_SITE = new ProvidersSites.RegisteredSite("Crédit Agricole",
            "CREDITAGRICOLE",
            "Crédit Agricole",
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            of(CURRENT_ACCOUNT),
            of(FR),
            null,
            null,
            null,
            Map.of(AIS, List.of(LoginRequirement.REDIRECT, LoginRequirement.FORM))
    );

    public static final RegisteredSite ABN_AMRO = new RegisteredSite("ABN AMRO",
            "ABN_AMRO",
            "ABN AMRO",
            UUID.fromString("7670247e-323e-4275-82f6-87f31119dbd3"), //TODO c4po-7504 potentially change to random uuid. Now must be real one since popular countries are still in SM db
            of(CURRENT_ACCOUNT),
            of(NL),
            null,
            Set.of(CONSENT_PER_ACCOUNT),
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT), PIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite AIB = new RegisteredSite("AIB",
            "AIB",
            "Allied Irish Bank (GB)",
            UUID.fromString("5806ae85-7ee6-48a5-98ea-0f464b9b71cb"), //TODO c4po-7504 potentially change to random uuid. Now must be real one since popular countries are still in SM db
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT),
            of(GB),
            null,
            null,
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite LLOYDS = new RegisteredSite("Lloyds",
            "LLOYDS_BANK",
            "Lloyds",
            UUID.fromString("36130c5f-9024-4a89-91fc-be31fac2f9ec"),
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT),
            of(GB),
            null,
            null,
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT), PIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite BARCLAYS_SCRAPING = new RegisteredSite("Barclays",
            "YODLEE",
            "Barclays",
            UUID.fromString("41de5024-d22e-4d24-b295-fd90a4fed926"),
            of(CURRENT_ACCOUNT, SAVINGS_ACCOUNT),
            of(GB),
            null,
            null,
            "4118",
            Map.of(AIS, of(LoginRequirement.FORM))
    );

    public static final RegisteredSite BARCLAYS_DIRECT = new RegisteredSite(
            "Barclays",
            "BARCLAYS",
            null,
            UUID.fromString("d28b4598-efcf-41c8-8522-08b2744e551a"),
            of(CURRENT_ACCOUNT),
            of(GB),
            null,
            null,
            null,
            Map.of(AIS, List.of(LoginRequirement.REDIRECT), PIS, List.of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite ING_SCRAPING = new RegisteredSite("ING",
            "SALTEDGE",
            "ING Italia",
            UUID.fromString("5300500b-6b83-4fdc-a8bc-7c2f9be014d4"),
            of(CURRENT_ACCOUNT, SAVINGS_ACCOUNT, CREDIT_CARD),
            of(AT, AU, CZ, DE, ES, IT, LU, NL, PL, RO, TH),
            null,
            null,
            "ingdirect_it",
            Map.of(AIS, of(LoginRequirement.FORM))
    );

    public static final UUID RABOBANK_SITE_ID = UUID.fromString("eedd41a8-51f8-4426-9348-314a18dbdec7");
    public static final RegisteredSite RABOBANK = new RegisteredSite("Rabobank",
            "RABOBANK",
            null,
            RABOBANK_SITE_ID,
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT, PREPAID_ACCOUNT, PENSION, INVESTMENT),
            of(GB, FR, IT, ES, BE, DE, NL, PL, CZ),
            null,
            null,
            null,
            Map.of(PIS, List.of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredPisSite RABOBANK_SEPA_PIS_DETAILS = new RegisteredPisSite(
            RABOBANK_SITE_ID,
            "YOLT_PROVIDER",
            true,
            ProvidersPaymentType.SINGLE,
            Map.of(RegisteredPisSite.DynamicFieldName.CREDITOR_POSTAL_COUNTRY, new RegisteredPisSite.DynamicFieldOptions(true)),
            false,
            RegisteredPisSite.PaymentMethod.SEPA,
            null);
}
