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

/**
 *
 * @author adrian
 */
public class EventMessage {

    private final String topic;
    private final byte[] message;
    private final int qos;
    private final boolean retained;

    public EventMessage(String topic, byte[] message) {
        this(topic, message, 0, false);
    }

    public EventMessage(String topic, byte[] message, int qos, boolean retained) {
        this.topic = topic;
        this.message = message;
        this.qos = qos;
        this.retained = retained;
    }
    

    public String getTopic() {
        return topic;
    }

    public byte[] getMessage() {
        return message;
    }
    
    public int getQoS() {
        return qos;
    }
    
    public boolean isRetained() {
        return retained;
    }
}
