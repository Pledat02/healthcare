package com.hehe.api_gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Component
public class ClientIpResolver {
    private static final String CF_CONNECTING_IP = "CF-Connecting-IP";

    private final boolean cloudflareEnabled;
    private final boolean trustForwardedFor;
    private final List<IpCidr> trustedCloudflareProxies;

    public ClientIpResolver(
            @Value("${security.cloudflare.enabled:false}") boolean cloudflareEnabled,
            @Value("${security.cloudflare.trusted-proxy-cidrs:127.0.0.1/32,::1/128}")
            String trustedProxyCidrs,
            @Value("${security.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.cloudflareEnabled = cloudflareEnabled;
        this.trustForwardedFor = trustForwardedFor;
        this.trustedCloudflareProxies = Arrays.stream(trustedProxyCidrs.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(IpCidr::parse)
                .toList();
    }

    public String resolve(ServerWebExchange exchange) {
        if (isCloudflareRequest(exchange)) {
            return canonicalize(exchange.getRequest().getHeaders().getFirst(CF_CONNECTING_IP));
        }
        if (!cloudflareEnabled && trustForwardedFor) {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null) {
                String canonical = canonicalize(forwarded.split(",", 2)[0].trim());
                if (canonical != null) return canonical;
            }
        }
        return remoteIp(exchange);
    }

    public boolean isCloudflareRequest(ServerWebExchange exchange) {
        if (!cloudflareEnabled) return false;
        String remoteIp = remoteIp(exchange);
        boolean trustedProxy = trustedCloudflareProxies.stream().anyMatch(cidr -> cidr.contains(remoteIp));
        if (!trustedProxy) return false;
        return canonicalize(exchange.getRequest().getHeaders().getFirst(CF_CONNECTING_IP)) != null;
    }

    public String cloudflareRayId(ServerWebExchange exchange) {
        if (!isCloudflareRequest(exchange)) return null;
        String value = exchange.getRequest().getHeaders().getFirst("CF-Ray");
        if (value == null) return null;
        String candidate = value.trim();
        return candidate.matches("[A-Za-z0-9-]{1,64}") ? candidate : null;
    }

    private String remoteIp(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null || remote.getAddress() == null) return "unknown";
        return remote.getAddress().getHostAddress();
    }

    static String canonicalize(String value) {
        if (value == null) return null;
        String candidate = value.trim();
        if (candidate.indexOf(':') >= 0 && candidate.matches("[0-9A-Fa-f:.%]+")) {
            try {
                return InetAddress.getByName(candidate).getHostAddress();
            } catch (UnknownHostException ignored) {
                return null;
            }
        }
        String[] parts = candidate.split("\\.", -1);
        if (parts.length != 4) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("\\d{1,3}")) return null;
            int octet = Integer.parseInt(parts[i]);
            if (octet > 255) return null;
            if (i > 0) result.append('.');
            result.append(octet);
        }
        return result.toString();
    }

    private record IpCidr(byte[] network, int prefixLength) {
        static IpCidr parse(String value) {
            String[] parts = value.split("/", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Trusted proxy must use CIDR notation: " + value);
            }
            String canonical = canonicalize(parts[0]);
            if (canonical == null) {
                throw new IllegalArgumentException("Invalid trusted proxy address: " + value);
            }
            try {
                byte[] address = InetAddress.getByName(canonical).getAddress();
                int prefix = Integer.parseInt(parts[1]);
                if (prefix < 0 || prefix > address.length * 8) {
                    throw new IllegalArgumentException("Invalid trusted proxy prefix: " + value);
                }
                return new IpCidr(address, prefix);
            } catch (UnknownHostException | NumberFormatException error) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value, error);
            }
        }

        boolean contains(String value) {
            String canonical = canonicalize(value);
            if (canonical == null) return false;
            try {
                byte[] candidate = InetAddress.getByName(canonical).getAddress();
                if (candidate.length != network.length) return false;
                int fullBytes = prefixLength / 8;
                int remainingBits = prefixLength % 8;
                for (int i = 0; i < fullBytes; i++) {
                    if (candidate[i] != network[i]) return false;
                }
                if (remainingBits == 0) return true;
                int mask = 0xFF << (8 - remainingBits);
                return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
            } catch (UnknownHostException ignored) {
                return false;
            }
        }
    }
}
