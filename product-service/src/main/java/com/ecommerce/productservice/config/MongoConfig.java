package com.ecommerce.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

  @Bean
  public MongoCustomConversions mongoCustomConversions() {
    return MongoCustomConversions.create(adapter -> adapter.bigDecimal(MongoCustomConversions.BigDecimalRepresentation.DECIMAL128));
  }
}
