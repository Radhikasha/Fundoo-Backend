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

    @Bean
    public ConnectionFactory connectionFactory() {
        com.rabbitmq.client.ConnectionFactory rabbitFactory =
                new com.rabbitmq.client.ConnectionFactory();

        int targetPort = port;
        boolean useSsl = sslEnabled;

        // Force SSL port for CloudAMQP regardless of environment variable
        if (host != null && host.contains("cloudamqp.com")) {
            targetPort = 5671;
            useSsl = true;
        }

        rabbitFactory.setHost(host);
        rabbitFactory.setPort(targetPort);
        rabbitFactory.setUsername(username);
        rabbitFactory.setPassword(password);

        // CloudAMQP virtual host = username (e.g. hmgrgaan), not '/'
        String vhost = (virtualHost == null || virtualHost.isBlank() || "/".equals(virtualHost))
                ? username : virtualHost;
        rabbitFactory.setVirtualHost(vhost);

        // KEY FIX: Heartbeat keeps CloudAMQP connection alive.
        // CloudAMQP free tier closes idle connections after ~60s.
        // Heartbeat of 30s sends a keep-alive ping every 30 seconds.
        rabbitFactory.setRequestedHeartbeat(30);

        // Connection timeout and auto-recovery
        rabbitFactory.setConnectionTimeout(60000);
        rabbitFactory.setAutomaticRecoveryEnabled(true);
        rabbitFactory.setNetworkRecoveryInterval(10000); // retry every 10s on drop

        if (useSsl) {
            try {
                rabbitFactory.useSslProtocol();
            } catch (Exception e) {
                System.err.println("RabbitMQ SSL protocol setup failed: " + e.getMessage());
            }
        }

        CachingConnectionFactory cachingFactory = new CachingConnectionFactory(rabbitFactory);
        // CHANNEL mode: single TCP connection, multiple channels — correct for CloudAMQP free tier
        cachingFactory.setCacheMode(CachingConnectionFactory.CachingMode.CHANNEL);

        return cachingFactory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        // Recover gracefully if connection drops — retry after 10 seconds
        factory.setRecoveryInterval(10000L);
        return factory;
    }

    @Bean
    public Queue reminderQueue() {
        return new Queue(REMINDER_QUEUE, true); // durable=true, auto-delete=false
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