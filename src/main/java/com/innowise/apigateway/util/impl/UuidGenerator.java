package com.innowise.apigateway.util.impl;

import com.innowise.apigateway.util.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidGenerator implements IdGenerator {
    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
