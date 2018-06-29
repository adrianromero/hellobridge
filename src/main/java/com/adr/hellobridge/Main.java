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

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import spark.Request;
import spark.Response;
import spark.Service;
import spark.utils.SparkUtils;

/**
 *
 * @author adrian
 */
public class Main {

    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws MqttException {

        File configfile;
        if (args.length > 0) {
            configfile = new File(args[0]);
        } else {
            configfile = new File(System.getProperty("user.home"), "hellobridge.properties");
        }

        Properties config = getConfig(configfile);

        ManagerMQTT manager = createManagerMQTT(config);
        manager.connect();

        int port = Integer.parseInt(config.getProperty("web.port", "8080"));
        String token = config.getProperty("web.token", "HELLOBRIDGE");
        if (token.equals("HELLOBRIDGE")) {
            log.warning("Using default security token, please change it in configuration property [web.token].");
        }

        Service s = Service.ignite();
        s.port(port);

        s.post("/*", (Request request, Response response) -> {
            JsonObject result = new JsonObject();

            String[] route = request.splat();
            if (route.length != 1) {
                response.status(400); // BAD_REQUEST
                result.addProperty("success", false);
                result.addProperty("message", "Empty topic.");
            } else {

                String topic = route[0];

                try {
                    JsonParser gsonparser = new JsonParser();
                    JsonObject body = gsonparser.parse(request.body()).getAsJsonObject();

                    int qos = body.has("qos") ? body.get("qos").getAsInt() : -1;
                    byte[] message = parseMessage(body.get("message").getAsString());
                    boolean retained = body.has("retained") ? body.get("retained").getAsBoolean() : false;

                    try {
                        // Publish message
                        manager.publish(topic, qos, message, retained);

                        result.addProperty("success", true);
                        result.addProperty("message", "Successfully sent message to topic [" + topic + "]");
                    } catch (MqttException ex) {
                        response.status(500); // Internal error
                        result.addProperty("success", false);
                        result.addProperty("message", "Cannot publish message to MQTT broker.");
                        log.log(Level.WARNING, "Cannot publish message to MQTT broker.", ex);
                    }
                } catch (JsonIOException | JsonSyntaxException ex) {
                    response.status(400); // BAD_REQUEST
                    result.addProperty("success", false);
                    result.addProperty("message", "Body must be a valid JSON.");
                    log.log(Level.WARNING, "Body must be a valid JSON.", ex);

                } catch (Exception ex) {
                    response.status(400); // BAD_REQUEST
                    result.addProperty("success", false);
                    result.addProperty("message", "Body must be a valid MQTT message.");
                    log.log(Level.WARNING, "Body must be a valid MQTT message.", ex);
                }
            }

            // Build response
            response.type("application/json");
            return result.toString();
        });

        s.before(SparkUtils.ALL_PATHS, (request, response) -> {

            String auth = request.headers("Authorization");
            if (auth != null) {
                if (auth.startsWith("Basic ")) { // "Bearer " for JWT token
                    auth = auth.substring(6);
                    String remotetoken = new String(Base64.getDecoder().decode(auth));
                    if (remotetoken.equals("mqtt:" + token)) {
                        return;
                    }
                }
            }
            log.log(Level.INFO, "Unauthorized request from {0}", request.ip());
            s.halt(401);
        });
    }

    private static Properties getConfig(File file) {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
            return props;
        } catch (IOException ex) {
            throw new RuntimeException("Configuration file cannot be loaded: " + file, ex);
        }
    }

    private static ManagerMQTT createManagerMQTT(Properties config) {

        String host = config.getProperty("mqtt.host", "localhost");
        int port = Integer.parseInt(config.getProperty("mqtt.port", "1883"));
        boolean websockets = Boolean.parseBoolean(config.getProperty("mqtt.websockets", "false"));
        boolean ssl = Boolean.parseBoolean(config.getProperty("mqtt.ssl", "false"));
        String protocol = websockets
                ? (ssl ? "wss" : "ws")
                : (ssl ? "ssl" : "tcp");
        Properties sslproperties;
        if (ssl) {
            sslproperties = new Properties();
            sslproperties.setProperty("com.ibm.ssl.protocol", config.getProperty("mqtt.protocol", "TLSv1.2"));
            if (!config.getProperty("mqtt.keystore", "").isEmpty()) {
                sslproperties.setProperty("com.ibm.ssl.keyStore", config.getProperty("mqtt.keystore", ""));
                sslproperties.setProperty("com.ibm.ssl.keyStorePassword", config.getProperty("mqtt.keystorepassword", ""));
                sslproperties.setProperty("com.ibm.ssl.keyStoreType", "JKS");
            }
            if (!config.getProperty("mqtt.truststore", "").isEmpty()) {
                sslproperties.setProperty("com.ibm.ssl.trustStore", config.getProperty("mqtt.truststore", ""));
                sslproperties.setProperty("com.ibm.ssl.trustStorePassword", config.getProperty("mqtt.truststorepassword", ""));
                sslproperties.setProperty("com.ibm.ssl.trustStoreType", "JKS");
            }
        } else {
            sslproperties = null;
        }
        String mqtturl = protocol + "://" + host + ":" + port;
        return new ManagerMQTT(
                mqtturl,
                config.getProperty("mqtt.username", ""),
                config.getProperty("mqtt.password", ""),
                config.getProperty("mqtt.clientid", "MQTTBridge"),
                Integer.parseInt(config.getProperty("mqtt.connectiontimeout", Integer.toString(MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT))),
                Integer.parseInt(config.getProperty("mqtt.keepaliveinterval", Integer.toString(MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT))),
                Integer.parseInt(config.getProperty("mqtt.defaultqos", "1")),
                Integer.parseInt(config.getProperty("mqtt.version", Integer.toString(MqttConnectOptions.MQTT_VERSION_DEFAULT))),
                Integer.parseInt(config.getProperty("mqtt.maxinflight", Integer.toString(MqttConnectOptions.MAX_INFLIGHT_DEFAULT))),
                sslproperties);
    }

    private static byte[] parseMessage(String message) {
        if (message.startsWith("plain:")) {
            return message.substring(6).getBytes(StandardCharsets.UTF_8);
        } else if (message.startsWith("base64:")) {
            return Base64.getDecoder().decode(message.substring(7));
        } else {
            return message.getBytes(StandardCharsets.UTF_8);
        }
    }
}
