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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 *
 * @author adrian
 */
public class SubscriptionDefinition {
    
    public final static int PLAIN = 0;
    public final static int FORMAT_BASE64 = 1;
    
    private final String name;
    private final String topic;
    private final int format;
    private final int qos;
    
    public SubscriptionDefinition(String name, String topic, int format, int qos) {
        this.name = name;
        this.topic = topic;
        this.format = format;
        this.qos = qos;
    }
    
    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public int getFormat() {
        return format;
    }

    public int getQos() {
        return qos;
    }
    
    public static byte[] parseMessage(String message) {
        if (message.startsWith("plain:")) {
            return message.substring(6).getBytes(StandardCharsets.UTF_8);
        } else if (message.startsWith("base64:")) {
            return Base64.getDecoder().decode(message.substring(7));
        } else {
            return message.getBytes(StandardCharsets.UTF_8);
        }
    }    
    
    public static String formatMessage(byte[] message, int format) {
        if (FORMAT_BASE64 == format) {
            return "base64:" + Base64.getEncoder().encodeToString(message);
        } else {
            return new String(message, StandardCharsets.UTF_8);
        }     
    } 
}
