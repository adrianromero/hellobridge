HelloBridge  [![Release](https://jitpack.io/v/adrianromero/hellobridge.svg)](https://jitpack.io/#adrianromero/hellobridge)
===========

HelloBridge is a Simple HTTP to MQTT bridge.

Compile
=======

Clone the repository, and execute:

```
  ./gradlew installDist
```

Then look into `build/install` and you will find a folder named `hellobridge` containing the compiled artifacts.

Configuration
=============

The configuration file by default is named in `hellobridge.properties` and located in the user home directory.

The configuration properties are:

* ''web.port'': The port HelloBridge listen for http requests to publish messages to MQTT topics. Default 8080.
* ''web.token'': Application token used to publish messates.
* ''mqtt.host'': Host of the MQTT broker. Default localhost.
* ''mqtt.port'': Port of the MQTT broker. Default 1883.
* ''mqtt.websockets'': Boolean value that indicates to connect using the websockets or the tcp protocol. Default false.
* ''mqtt.username'':
* ''mqtt.password'':
* ''mqtt.clientid'': If empty a random client id is generated.
* ''mqtt.ssl'': Boolean value that indicates to connect to the MQTT broker using an SSL connection. Default false.
* ''mqtt.connectiontimeout'': Sets the connection timeout value. This value, measured in seconds, defines the maximum time interval the client will wait for the network connection to the MQTT server to be established. A value of 0 disables timeout. Default 30 seconds.
* ''mqtt.keealiveinterval'': Sets the "keep alive" interval. This value, measured in seconds, defines the maximum time interval between messages sent or received. Default 60 seconds.
* ''mqtt.version'': Sets the MQTT version. A value of 3 stands for 3.1, a value of 4 stands for 3.1.1. Default  3.1.1.
* ''mqtt.maxinflight'': Sets the "max inflight". Increase this value in a high traffic environment. Default 10.

I case of setting ''mqtt.ssl'' property to ''true'' configure SSL using the following properties:
* ''mqtt.protocol''. Supported SSL prococols: Default TLSv1.2.
* ''mqtt.keystore''.  JKS key store file path. 
* ''mqtt.keystorepassword''. Key store file password.
* ''mqtt.truststore''.  JKS trust store file path. 
* ''mqtt.truststorepassword''. Trust store file password.


Example configuration file
==========================

```
mqtt.host=localhost
mqtt.username=MYUSER
mqtt.password=MYPASSWORD

mqtt.topic.espurna04=espurna04/relay/0
mqtt.topic.espurna04.qos=1
mqtt.topic.espurna05=espurna05/relay/0
mqtt.topic.espurna05.qos=1

web.port=8080
web.token=R5mERHYvEDfghJGzFZRwzaeT

webhook.url=http://localhost/{{subscription}}/with/key
```


Publication and Subscription
============================

To publish messages to the configued MQTT broker use the following HTTP request

```
curl -H "Content-Type: application/json" -H "Authorization: Basic token" -X POST  -d '{"message": ""}' http://host:port
```

The properties in the JSON are
message
qos
retained

Examples

To subscribe messages 





License
=======

HelloBridge is licensed under the GNU General Public License, Version 3, 29 June 2007
