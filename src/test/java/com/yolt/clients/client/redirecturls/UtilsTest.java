package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.exceptions.RedirectURLMalformedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UtilsTest {

    @Test
    void testLowercaseURL() {
        assertThat(Utils.lowercaseURL("https://Some-URL.com")).isEqualTo("https://some-url.com");

        assertThat(Utils.lowercaseURL("https://Some-URL.com:2334/page")).isEqualTo("https://some-url.com:2334/page");

        assertThat(Utils.lowercaseURL("https://Some-URL.com/Custom-Part/Should-Remain-Unchainged"))
                .isEqualTo("https://some-url.com/Custom-Part/Should-Remain-Unchainged");

        assertThat(Utils.lowercaseURL("https://Some-URL.com/CustomPage?param1=100&ParamTwo=init"))
                .isEqualTo("https://some-url.com/CustomPage?param1=100&ParamTwo=init");

        assertThat(Utils.lowercaseURL("HTTPS://NewUrl.ORG:8443/some-path/CAPS?query=value&queryWithoutValue#Anchor"))
                .isEqualTo("https://newurl.org:8443/some-path/CAPS?query=value&queryWithoutValue#Anchor");

        var ex = assertThrows(
                RedirectURLMalformedException.class,
                () -> Utils.lowercaseURL("://malformed"));
        assertThat(ex).hasMessage("Redirect URL ://malformed has wrong format.");
    }
}