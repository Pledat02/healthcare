package com.hehe.api_gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    @Test
    void usesCloudflareClientIpOnlyFromTrustedConnector() {
        ClientIpResolver resolver = new ClientIpResolver(true, "172.29.250.2/32", false);
        MockServerWebExchange exchange = exchange("172.29.250.2", "203.0.113.41");

        assertThat(resolver.isCloudflareRequest(exchange)).isTrue();
        assertThat(resolver.resolve(exchange)).isEqualTo("203.0.113.41");
        assertThat(resolver.cloudflareRayId(exchange)).isEqualTo("abc123-SIN");
    }

    @Test
    void ignoresSpoofedCloudflareHeaderFromUntrustedSource() {
        ClientIpResolver resolver = new ClientIpResolver(true, "172.29.250.2/32", false);
        MockServerWebExchange exchange = exchange("198.51.100.8", "203.0.113.41");

        assertThat(resolver.isCloudflareRequest(exchange)).isFalse();
        assertThat(resolver.resolve(exchange)).isEqualTo("198.51.100.8");
    }

    @Test
    void supportsIpv6TrustedProxyCidrs() {
        ClientIpResolver resolver = new ClientIpResolver(true, "2001:db8:abcd::/48", false);
        MockServerWebExchange exchange = exchange("2001:db8:abcd::2", "2001:db8:1::25");

        assertThat(resolver.isCloudflareRequest(exchange)).isTrue();
        assertThat(resolver.resolve(exchange)).isEqualTo("2001:db8:1:0:0:0:0:25");
    }

    private MockServerWebExchange exchange(String remoteIp, String cloudflareIp) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/patients")
                .remoteAddress(new InetSocketAddress(remoteIp, 43120))
                .header("CF-Connecting-IP", cloudflareIp)
                .header("CF-Ray", "abc123-SIN")
                .build();
        return MockServerWebExchange.from(request);
    }
}
