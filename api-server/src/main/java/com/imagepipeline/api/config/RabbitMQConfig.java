package com.imagepipeline.api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Nombres de colas ────────────────────────────────────────────
    public static final String QUEUE_IMAGE_UPLOADED   = "image.uploaded";
    public static final String QUEUE_IMAGE_RESIZED    = "image.resized";
    public static final String QUEUE_RESIZE_JOBS      = "image.resize.jobs";
    public static final String QUEUE_COMPRESS_JOBS    = "image.compress.jobs";
    public static final String QUEUE_WATERMARK_JOBS   = "image.watermark.jobs";

    // ── Exchange principal ──────────────────────────────────────────
    public static final String EXCHANGE_IMAGES = "images.exchange";

    // ── Routing keys ────────────────────────────────────────────────
    public static final String RK_UPLOADED  = "image.uploaded";
    public static final String RK_RESIZED   = "image.resized";
    public static final String RK_RESIZE    = "image.resize";
    public static final String RK_COMPRESS  = "image.compress";
    public static final String RK_WATERMARK = "image.watermark";

    // ── Exchange ────────────────────────────────────────────────────
    @Bean
    public TopicExchange imagesExchange() {
        return new TopicExchange(EXCHANGE_IMAGES, true, false);
    }

    // ── Colas ───────────────────────────────────────────────────────
    @Bean public Queue queueImageUploaded()  { return new Queue(QUEUE_IMAGE_UPLOADED,  true); }
    @Bean public Queue queueImageResized()   { return new Queue(QUEUE_IMAGE_RESIZED,   true); }
    @Bean public Queue queueResizeJobs()     { return new Queue(QUEUE_RESIZE_JOBS,     true); }
    @Bean public Queue queueCompressJobs()   { return new Queue(QUEUE_COMPRESS_JOBS,   true); }
    @Bean public Queue queueWatermarkJobs()  { return new Queue(QUEUE_WATERMARK_JOBS,  true); }

    // ── Bindings ────────────────────────────────────────────────────
    @Bean
    public Binding bindingUploaded(Queue queueImageUploaded, TopicExchange imagesExchange) {
        return BindingBuilder.bind(queueImageUploaded).to(imagesExchange).with(RK_UPLOADED);
    }

    @Bean
    public Binding bindingResized(Queue queueImageResized, TopicExchange imagesExchange) {
        return BindingBuilder.bind(queueImageResized).to(imagesExchange).with(RK_RESIZED);
    }

    @Bean
    public Binding bindingResizeJobs(Queue queueResizeJobs, TopicExchange imagesExchange) {
        return BindingBuilder.bind(queueResizeJobs).to(imagesExchange).with(RK_RESIZE);
    }

    @Bean
    public Binding bindingCompressJobs(Queue queueCompressJobs, TopicExchange imagesExchange) {
        return BindingBuilder.bind(queueCompressJobs).to(imagesExchange).with(RK_COMPRESS);
    }

    @Bean
    public Binding bindingWatermarkJobs(Queue queueWatermarkJobs, TopicExchange imagesExchange) {
        return BindingBuilder.bind(queueWatermarkJobs).to(imagesExchange).with(RK_WATERMARK);
    }

    // ── Serialización JSON ──────────────────────────────────────────
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
}
