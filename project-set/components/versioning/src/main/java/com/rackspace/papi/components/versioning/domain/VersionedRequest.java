package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.UniformResourceInfo;
import com.rackspace.papi.commons.util.string.JCharSequenceFactory;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;

import static com.rackspace.papi.commons.util.StringUriUtilities.*;

public class VersionedRequest {

    private final HttpRequestInfo requestInfo;
    private final ServiceVersionMapping mapping;
    private final String serviceRootUrl;

    public VersionedRequest(HttpRequestInfo requestInfo, ServiceVersionMapping mapping, String serviceRootUrl) {
        this.requestInfo = requestInfo;
        this.mapping = mapping;
        this.serviceRootUrl = serviceRootUrl.endsWith("/") ? serviceRootUrl.substring(0, serviceRootUrl.length() - 1) : serviceRootUrl;
    }

    public ServiceVersionMapping getMapping() {
        return mapping;
    }

    public HttpRequestInfo getRequestInfo() {
        return requestInfo;
    }

    public String getServiceRootUrl() {
        return serviceRootUrl;
    }

    public boolean isRequestForRoot() {
        return StringUriUtilities.formatUri(requestInfo.getUri()).isEmpty();
    }

    public boolean requestBelongsToVersionMapping() {
        final String requestedUri = StringUriUtilities.formatUri(requestInfo.getUri());
        final String versionUri = StringUriUtilities.formatUri(mapping.getId());
        
        return indexOfUriFragment(JCharSequenceFactory.jchars(requestedUri), versionUri) == 0;
    }

    public boolean requestMatchesVersionMapping() {
        final String requestedUri = StringUriUtilities.formatUri(requestInfo.getUri());

        return requestedUri.equals(StringUriUtilities.formatUri(mapping.getId()));
    }

    public String asExternalURL() {
        return serviceRootUrl + asExternalURI();
    }

    public String asExternalURI() {
        return updateURI(requestInfo, mapping.getContextPath(), mapping.getId());
    }

    public String asInternalURL() {
        return serviceRootUrl + asInternalURI();
    }

    public String asInternalURI() {
        return updateURI(requestInfo, mapping.getId(), mapping.getContextPath());
    }

    public boolean uriRequiresRewrite() {
        final String formattedUri = StringUriUtilities.formatUri(requestInfo.getUri());
        final String formattedReplacement = StringUriUtilities.formatUri(mapping.getContextPath());

        final int replacementIndex = indexOfUriFragment(JCharSequenceFactory.jchars(formattedUri), formattedReplacement);

        return replacementIndex < 0;
    }
    
    private String updateURI(UniformResourceInfo requestInfo, String original, String replacement) {
        if (requestInfo.getUri().charAt(0) != '/') {
            throw new IllegalArgumentException("Request URI must be a URI with a root reference - i.e. the URI must start with '/'");
        }

        final StringBuilder uriBuilder = new StringBuilder(StringUriUtilities.formatUri(requestInfo.getUri()));
        final String formattedReplacement = StringUriUtilities.formatUri(replacement);
        final String formattedOriginal = StringUriUtilities.formatUri(original);

        final int originalIndex = indexOfUriFragment(JCharSequenceFactory.jchars(uriBuilder), formattedOriginal);
        final int replacementIndex = indexOfUriFragment(JCharSequenceFactory.jchars(uriBuilder), formattedReplacement);

        if (replacementIndex < 0) {
            if (originalIndex < 0) {
                uriBuilder.insert(0, formattedReplacement);
            } else {
                uriBuilder.delete(originalIndex, originalIndex + formattedOriginal.length());
                uriBuilder.insert(originalIndex, formattedReplacement);
            }
        }

        return uriBuilder.toString();
    }
}
