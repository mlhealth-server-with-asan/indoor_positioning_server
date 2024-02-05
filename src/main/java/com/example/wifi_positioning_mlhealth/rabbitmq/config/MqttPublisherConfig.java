package com.example.wifi_positioning_mlhealth.rabbitmq.config;

import org.springframework.beans.factory.annotation.Value;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
@Configuration
public class MqttPublisherConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${spring.rabbitmq.username}") // 사용자 이름 설정
    private String username;

    @Value("${spring.rabbitmq.password}") // 비밀번호 설정
    private String password;


    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { brokerUrl });
        options.setUserName(username); // 사용자 이름 설정
        options.setPassword(password.toCharArray()); // 비밀번호 설정
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MqttPahoMessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler("publisherClient", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic("yourTopic");
        return messageHandler;
    }
}
