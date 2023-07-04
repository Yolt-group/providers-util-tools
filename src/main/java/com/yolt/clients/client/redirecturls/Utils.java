package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.exceptions.RedirectURLMalformedException;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;

@UtilityClass
public class Utils {

    public static String lowercaseURL(final String redirectURL) {
        try {
            var uri = new URI(redirectURL);
            var toReplace = uri.getScheme() + "://" + uri.getHost();
            var replacement = toReplace.toLowerCase();
            return redirectURL.replaceFirst(toReplace, replacement);
        } catch (URISyntaxException e) {
            throw new RedirectURLMalformedException(redirectURL, e);
        }
    }
}