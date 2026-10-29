package com.bank.yanki.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.BigDecimalRepresentation;

/** Stores BigDecimal as Decimal128 so atomic numeric updates work. */
@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(a -> a.bigDecimal(BigDecimalRepresentation.DECIMAL128));
    }
}