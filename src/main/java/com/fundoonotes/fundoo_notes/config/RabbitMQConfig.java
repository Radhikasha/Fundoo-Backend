package com.fundoonotes.fundoo_notes.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REMINDER_QUEUE = "reminder-queue";
    public static final String REMINDER_EXCHANGE = "reminder-exchange";
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
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        // For CloudAMQP, virtual host must be username (e.g. hmgrgaan) instead of '/'
        String vhost = (virtualHost == null || virtualHost.isBlank() || "/".equals(virtualHost))
                ? username : virtualHost;
        connectionFactory.setVirtualHost(vhost);

        if (sslEnabled) {
            try {
                connectionFactory.getRabbitConnectionFactory().useSslProtocol();
            } catch (Exception e) {
                System.err.println("Failed to configure SSL protocol for RabbitMQ: " + e.getMessage());
            }
        }
        return connectionFactory;
    }

    @Bean
    public Queue reminderQueue() {
        return new Queue(REMINDER_QUEUE, true);
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