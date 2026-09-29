package com.ecommerce.orderservice.config;

import com.ecommerce.orderservice.event.OrderEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic orderPlacedTopic() {
    return TopicBuilder.name(OrderEventProducer.ORDER_PLACED_TOPIC)
        .partitions(3)
        .replicas(1)
        .build();
  }
}
