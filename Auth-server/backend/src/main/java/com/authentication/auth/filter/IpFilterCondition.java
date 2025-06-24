package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
// Placeholder for potential CIDR library, e.g., Apache Commons Net
// import org.apache.commons.net.util.SubnetUtils;

@Slf4j
@Getter
public class IpFilterCondition implements FilterCondition {

    private final String id;
    private final String description;
    private final String ipAddressOrRange; // Stores the IP address or CIDR string
    private boolean enabled = true;

    // Example for CIDR:
    // private final SubnetUtils subnetInfo; // If using Apache Commons Net SubnetUtils

    /**
     * Constructor for IpFilterCondition.
     * @param description A description of the condition.
     * @param ipAddressOrRange The IP address (e.g., "192.168.1.100") or CIDR block (e.g., "192.168.1.0/24").
     *                         Currently, only exact IP matching is implemented. CIDR requires additional logic/library.
     */
    public IpFilterCondition(String description, String ipAddressOrRange) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.ipAddressOrRange = ipAddressOrRange;

        // TODO: Initialize SubnetUtils or similar if CIDR support is added.
        // For example:
        // if (ipAddressOrRange != null && ipAddressOrRange.contains("/")) {
        //     try {
        //         this.subnetInfo = new SubnetUtils(ipAddressOrRange);
        //         this.subnetInfo.setInclusiveHostCount(true); // Include network and broadcast addresses
        //     } catch (IllegalArgumentException e) {
        //         log.warn("Invalid CIDR notation for IP condition '{}': {}. Will only perform exact match.", description, ipAddressOrRange, e);
        //         this.subnetInfo = null;
        //     }
        // } else {
        //     this.subnetInfo = null;
        // }
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
        return "ip";
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        if (!isEnabled()) {
            log.trace("Condition '{}' ({}) is disabled. Will not cause skipping.", description, id);
            return false; // Do not skip if disabled
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            log.warn("Remote IP address is null or empty. Condition '{}' ({}) cannot match for skipping.", description, id);
            return false; // Cannot determine remote IP, so cannot match
        }

        boolean matches;
        // TODO: Implement CIDR matching logic if subnetInfo is initialized
        // if (this.subnetInfo != null) {
        //     matches = this.subnetInfo.getInfo().isInRange(remoteAddr);
        // } else {
            // Basic exact string comparison for IP address
            matches = this.ipAddressOrRange.equals(remoteAddr);
        // }

        if (matches) {
            log.debug("Request IP '{}' matched configured IP/range '{}'. Condition '{}' ({}) matched for skipping.", 
                      remoteAddr, this.ipAddressOrRange, description, id);
        } else {
            log.debug("Request IP '{}' did not match configured IP/range '{}'. Condition '{}' ({}) does not match for skipping.", 
                      remoteAddr, this.ipAddressOrRange, description, id);
        }
        return matches; // True if it matches (and thus filter should be skipped)
    }
}
