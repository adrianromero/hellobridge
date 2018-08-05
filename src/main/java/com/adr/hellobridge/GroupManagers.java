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

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * @author adrian
 */
public class GroupManagers {
    
    private static final Logger logger = Logger.getLogger(GroupManagers.class.getName());          
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final String url;

    private final OkHttpClient client = new OkHttpClient();
    private final Map<String, SubscriptionDefinition> subscriptions = new HashMap<>();
    
    public GroupManagers(Properties config, SubscriptionDefinition[] subs) {
        url = config.getProperty("webhook.url");
        
        for (SubscriptionDefinition sub : subs) {
            subscriptions.put(sub.getTopic(), sub);
        }     
    }
    
    public void distributeMessage(EventMessage message) {
        distributeWilcardMessage(message.getTopic(), message);
        distributeRecursiveMessage(message.getTopic().length() - 1, message);
    }    
    
    private void distributeRecursiveMessage(int starting, EventMessage message) {
        String topic = message.getTopic();
        int i = topic.lastIndexOf('/', starting);
        if (i < 0) {
            distributeWilcardMessage("#", message);
        } else {
            distributeWilcardMessage(topic.substring(0, i) + "/#", message);
            distributeRecursiveMessage(i - 1, message);
        }
    }

    private void distributeWilcardMessage(String subscriptiontopic, EventMessage message) {
        
        SubscriptionDefinition sub = subscriptions.get(subscriptiontopic);
        if (sub == null) {
            logger.log(Level.CONFIG, () -> "Discarded subscription topic: " + subscriptiontopic);
            return;
        }

        try {
            String weburl = url
                    .replace("{{subscription}}", URLEncoder.encode(subscriptiontopic, "UTF-8"))
                    .replace("{{decodedsubscription}}", subscriptiontopic);

            
            JsonObject result = new JsonObject();
            result.addProperty("topic", message.getTopic());
            result.addProperty("message", SubscriptionDefinition.formatMessage(message.getMessage(), sub.getFormat()));
            if (message.getQoS() > 0) {
                result.addProperty("qos", message.getQoS());
            }
            if (message.isRetained()) {
                result.addProperty("retained", true);
            }
            
            logger.log(Level.CONFIG, () -> String.format("Subscription topic command: %s -> %s ", weburl, result.toString()));

            RequestBody body = RequestBody.create(JSON, result.toString());
            Request request = new Request.Builder()
                    .url(weburl)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
               logger.log(Level.INFO, () -> "Subscription notified for: " + subscriptiontopic);
            } else {
               logger.log(Level.WARNING, () -> String.format("Subscription cannot be notified for: %s. Server returned: %s.", subscriptiontopic, response.code()));
            }
            
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            logger.log(Level.WARNING, String.format("Subscription failed for: %s.", subscriptiontopic), ex);
        }
    }
}

