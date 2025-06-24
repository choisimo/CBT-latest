package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.regex.Pattern; // Ensure this import is added

@Slf4j
@Getter // Lombok Getter for fields
public class HeaderFilterCondition implements FilterCondition {

    private final String id;
    private final String description;
    private final String headerName;
    private final String headerValuePattern; // Regex pattern for the header value
    private final Pattern compiledPattern;   // To store the compiled regex pattern
    private boolean enabled = true;

    /**
     * Constructor for HeaderFilterCondition.
     * @param description A description of the condition.
     * @param headerName The name of the HTTP header to check.
     * @param headerValuePattern A regex pattern to match the header's value. If null or empty,
     *                           the condition will match if the header is present (any value).
     */
    public HeaderFilterCondition(String description, String headerName, String headerValuePattern) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.headerName = headerName;
        this.headerValuePattern = headerValuePattern;
        
        if (headerValuePattern != null && !headerValuePattern.isEmpty()) {
            this.compiledPattern = Pattern.compile(headerValuePattern);
        } else {
            // If no pattern, compiledPattern is null. `shouldNotFilter` will handle this as match-any-value if header present.
            this.compiledPattern = null; 
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getType() {
        return "header";
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        if (!isEnabled()) {
            log.trace("Condition '{}' ({}) is disabled. Will not cause skipping.", description, id);
            return false; // Do not skip if disabled
        }

        String actualHeaderValue = request.getHeader(this.headerName);

        if (actualHeaderValue == null) {
            // Header not present, so condition to skip (based on its presence/value) is not met.
            log.trace("Header '{}' not found in request. Condition '{}' ({}) does not match for skipping.", 
                      this.headerName, description, id);
            return false; 
        }

        // If compiledPattern is null, it means headerValuePattern was null or empty.
        // In this case, we interpret it as "skip if header is present, regardless of value".
        if (this.compiledPattern == null) {
            log.debug("Header '{}' is present (value: '{}'). Condition '{}' ({}) (match if present) matched for skipping.", 
                      this.headerName, actualHeaderValue, description, id);
            return true; // Skip because header is present and no specific value pattern was required.
        }

        // If a pattern is specified, try to match it against the actual header value.
        boolean matches = this.compiledPattern.matcher(actualHeaderValue).matches();
        if (matches) {
            log.debug("Header '{}' value '{}' matched pattern '{}'. Condition '{}' ({}) matched for skipping.", 
                      this.headerName, actualHeaderValue, this.headerValuePattern, description, id);
        } else {
            log.debug("Header '{}' value '{}' did not match pattern '{}'. Condition '{}' ({}) does not match for skipping.", 
                      this.headerName, actualHeaderValue, this.headerValuePattern, description, id);
        }
        return matches; // Skip if the header value matches the specified pattern.
    }
}
