//    HelloBridge is a bridge from HTTP to MQTT
//    Copyright (C) 2018 Adrián Romero Corchado.
//
//    This file is part of HelloBridge.
//
//    HelloIot is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    HelloIot is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with HelloIot.  If not, see <http://www.gnu.org/licenses/>.
//
package com.adr.hellobridge;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 *
 * @author adrian
 */
public class ManagerMQTT {

    public final static String SYS_PREFIX = "$SYS/";

    private final static Logger logger = Logger.getLogger(ManagerMQTT.class.getName());

    private final String url;
    private final String username;
    private final String password;
    private final String clientid;
    private final int timeout;
    private final int keepalive;
    private final int defaultqos;
    private final int version;
    private final int maxinflight;
    private final Properties sslproperties;

    private MqttAsyncClient mqttClient;

    public ManagerMQTT(String url, String username, String password, String clientid, int timeout, int keepalive, int defaultqos, int version, int maxinflight, Properties sslproperties) {

        this.url = url;
        this.username = username;
        this.password = password;
        this.clientid = clientid;
        this.timeout = timeout;
        this.keepalive = keepalive;
        this.defaultqos = defaultqos;
        this.version = version;
        this.maxinflight = maxinflight;
        this.sslproperties = sslproperties;

        this.mqttClient = null;
    }

    public void connect() throws MqttException {

        mqttClient = new MqttAsyncClient(url, clientid, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
        options.setConnectionTimeout(timeout);
        options.setKeepAliveInterval(keepalive);
        options.setMqttVersion(version);
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        options.setMaxInflight(maxinflight);
        options.setSSLProperties(sslproperties);
        mqttClient.connect(options).waitForCompletion(1000);

        logger.log(Level.INFO, "Connected to MQTT broker on [{0}]", url);
    }

    public void disconnect() {
        // To be invoked by executor thread
        if (mqttClient != null) {
            if (mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
            mqttClient = null;
        }
    }

    public void publish(String topic, int qos, byte[] message, boolean isRetained) throws MqttException {

        // To be executed in Executor thread
        if (mqttClient == null) {
            return;
        }

        logger.log(Level.INFO, "Publishing message to broker. [{0}]", topic);
        MqttMessage mm = new MqttMessage(message);
        mm.setQos(qos < 0 ? defaultqos : qos);
        mm.setRetained(isRetained);
        mqttClient.publish(topic, mm);

        logger.log(Level.INFO, "Message published to topic [{0}]", topic);
    }
}
