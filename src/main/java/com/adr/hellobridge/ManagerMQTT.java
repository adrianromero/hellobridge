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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 *
 * @author adrian
 */
public class ManagerMQTT implements MqttCallback {

    public final static String SYS_PREFIX = "$SYS/";

    private final static Logger logger = Logger.getLogger(ManagerMQTT.class.getName());

    private final String url;
    private final String username;
    private final String password;
    private final String clientid;
    private final int timeout;
    private final int keepalive;
    private final int version;
    private final int maxinflight;
    private final Properties sslproperties;

    // Manager
    private GroupManagers group;
    // MQTT
    private MqttAsyncClient mqttClient;
    private final List<String> worktopics = new ArrayList<>();
    private final List<Integer> workqos = new ArrayList<>();

    public ManagerMQTT(String url, String username, String password, String clientid, int timeout, int keepalive, int version, int maxinflight, Properties sslproperties) {

        this.url = url;
        this.username = username;
        this.password = password;
        this.clientid = clientid;
        this.timeout = timeout;
        this.keepalive = keepalive;
        this.version = version;
        this.maxinflight = maxinflight;
        this.sslproperties = sslproperties;

        this.mqttClient = null;
    }
    
    public void registerTopicsManager(GroupManagers group) {
        this.group = group;
    }
    
    public void registerSubscription(String topic, int qos) {
        worktopics.add(topic);
        workqos.add(qos);
    }
    
    public void connect() throws MqttException {
        
        String[] listtopics = worktopics.toArray(new String[worktopics.size()]);
        int[] listqos = new int[workqos.size()];
        for (int i = 0; i < workqos.size(); i++) {
            listqos[i] = workqos.get(i);
        }
        
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
        mqttClient.setCallback(this);
        if (listtopics.length > 0) {
            mqttClient.subscribe(listtopics, listqos);
        }
            
        logger.log(Level.INFO, "Connected to MQTT broker on [{0}]", url);
    }

    public void disconnect() {
        // To be invoked by executor thread
        if (mqttClient != null) {
            if (mqttClient.isConnected()) {
                try {
                    mqttClient.setCallback(null);
                    String[] listtopics = worktopics.toArray(new String[worktopics.size()]);
                    mqttClient.unsubscribe(listtopics);                    
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
            mqttClient = null;
        }
    }

    public void publish(EventMessage message) throws MqttException {

        // To be executed in Executor thread
        if (mqttClient == null) {
            return;
        }

        MqttMessage mm = new MqttMessage(message.getMessage());
        mm.setQos(message.getQoS());
        mm.setRetained(message.isRetained());
        mqttClient.publish(message.getTopic(), mm);
    }
    
    @Override
    public void connectionLost(Throwable ex) {
        logger.log(Level.WARNING, "Connection to MQTT broker lost.", ex);
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        group.distributeMessage(new EventMessage(topic, mm.getPayload(), mm.getQos(), mm.isRetained()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
    }    
}
