package com.hehe.security_service.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class IpAddressService {
    public String canonicalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("IP không hợp lệ");
        }
        String value = input.trim();
        if (value.indexOf(':') >= 0 && value.matches("[0-9A-Fa-f:.%]+")) {
            try {
                return InetAddress.getByName(value).getHostAddress();
            } catch (UnknownHostException ignored) {
                throw new IllegalArgumentException("IPv6 không hợp lệ");
            }
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("IPv4 không hợp lệ");
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("\\d{1,3}")) {
                throw new IllegalArgumentException("IPv4 không hợp lệ");
            }
            int octet = Integer.parseInt(parts[i]);
            if (octet > 255) {
                throw new IllegalArgumentException("IPv4 không hợp lệ");
            }
            if (i > 0) canonical.append('.');
            canonical.append(octet);
        }
        return canonical.toString();
    }
}
