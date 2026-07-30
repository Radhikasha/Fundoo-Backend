package com.fundoonotes.fundoo_notes.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REMINDER_QUEUE       = "reminder-queue";
    public static final String REMINDER_EXCHANGE    = "reminder-exchange";
    public static final String REMINDER_ROUTING_KEY = "reminder-key";

    @Value("${spring.rabbitmq.host:warthog.lmq.cloudamqp.com}")
    private String host;

    @Value("${spring.rabbitmq.port:5671}")
    private int port;

    @Value("${spring.rabbitmq.username:hmgrgaan}")
    private String username;

    @Value("${spring.rabbitmq.password:ZysPvVKs_aZm4lsp3fm5WifgOMP8FEow}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host:hmgrgaan}")
    private String virtualHost;

    @Value("${spring.rabbitmq.ssl.enabled:true}")
    private boolean sslEnabled;

    /**
     * Custom ConnectionFactory so we can enforce:
     * - SSL (port 5671) for CloudAMQP
     * - Correct virtual host (hmgrgaan, not '/')
     * - CHANNEL caching mode (one TCP connection, many channels — required on CloudAMQP free tier)
     *
     * Heartbeat (30s) and connection-timeout are configured via application.yml
     * spring.rabbitmq.requested-heartbeat and spring.rabbitmq.connection-timeout
     * and are applied automatically by Spring Boot's RabbitAutoConfiguration to the
     * underlying com.rabbitmq.client.ConnectionFactory before we wrap it.
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();

        int targetPort = port;
        boolean useSsl = sslEnabled;

        // Always use SSL port 5671 for CloudAMQP
        if (host != null && host.contains("cloudamqp.com")) {
            targetPort = 5671;
            useSsl = true;
        }

        factory.setHost(host);
        factory.setPort(targetPort);
        factory.setUsername(username);
        factory.setPassword(password);

        // CloudAMQP free tier: virtual host must equal the username (e.g. hmgrgaan)
        String vhost = (virtualHost == null || virtualHost.isBlank() || "/".equals(virtualHost))
                ? username : virtualHost;
        factory.setVirtualHost(vhost);

        if (useSsl) {
            try {
                factory.getRabbitConnectionFactory().useSslProtocol();
            } catch (Exception e) {
                System.err.println("RabbitMQ SSL setup failed: " + e.getMessage());
            }
        }

        // Set heartbeat directly on the underlying AMQP ConnectionFactory
        // Keeps CloudAMQP from closing idle connections (free tier disconnects after ~60s)
        factory.getRabbitConnectionFactory().setRequestedHeartbeat(30);

        // CHANNEL mode: single TCP connection shared across all threads
        // Critical for CloudAMQP free tier which only allows 1 concurrent connection
        factory.setCacheMode(CachingConnectionFactory.CachingMode.CHANNEL);

        return factory;
    }

    /**
     * Listener container factory — uses the shared connection above
     * and retries every 10 seconds if the connection temporarily drops.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setRecoveryInterval(10000L);
        return factory;
    }

    @Bean
    public Queue reminderQueue() {
        return new Queue(REMINDER_QUEUE, true); // durable, not auto-delete
    }

    @Bean
    public DirectExchange reminderExchange() {
        return new DirectExchange(REMINDER_EXCHANGE);
    }

    @Bean
    public Binding reminderBinding(Queue reminderQueue, DirectExchange reminderExchange) {
        return BindingBuilder
                .bind(reminderQueue)
                .to(reminderExchange)
                .with(REMINDER_ROUTING_KEY);
    }

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