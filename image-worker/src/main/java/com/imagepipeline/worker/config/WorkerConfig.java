package com.imagepipeline.worker.config;

import io.minio.MinioClient;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfig {

    // ---------------------------------------------------------------
    // Queue names (read from application.yml)
    // ---------------------------------------------------------------

    @Value("${rabbitmq.queues.resize}")
    private String resizeQueue;

    @Value("${rabbitmq.queues.compress}")
    private String compressQueue;

    @Value("${rabbitmq.queues.watermark}")
    private String watermarkQueue;

    @Value("${rabbitmq.queues.dead-letter}")
    private String deadLetterQueue;

    // ---------------------------------------------------------------
    // MinIO config (read from application.yml / env vars)
    // ---------------------------------------------------------------

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    // ---------------------------------------------------------------
    // RabbitMQ: declare queues
    // If the queue doesn't exist yet, Spring creates it on startup.
    // durable=true means the queue survives broker restarts.
    // ---------------------------------------------------------------

    @Bean
    public Queue resizeQueue() {
        return QueueBuilder.durable(resizeQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", deadLetterQueue)
                .build();
    }

    @Bean
    public Queue compressQueue() {
        return QueueBuilder.durable(compressQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", deadLetterQueue)
                .build();
    }

    @Bean
    public Queue watermarkQueue() {
        return QueueBuilder.durable(watermarkQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", deadLetterQueue)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    // ---------------------------------------------------------------
    // JSON message converter
    // Tells Spring to serialize/deserialize messages as JSON
    // instead of Java binary serialization.
    // ---------------------------------------------------------------

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // ---------------------------------------------------------------
    // MinIO client
    // ---------------------------------------------------------------

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }
}
