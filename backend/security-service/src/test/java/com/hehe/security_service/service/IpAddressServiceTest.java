package com.hehe.security_service.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpAddressServiceTest {
    private final IpAddressService service = new IpAddressService();

    @Test
    void canonicalizesIpv4WithoutDnsLookup() {
        assertThat(service.canonicalize("192.168.001.010")).isEqualTo("192.168.1.10");
    }

    @Test
    void rejectsHostname() {
        assertThatThrownBy(() -> service.canonicalize("example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
