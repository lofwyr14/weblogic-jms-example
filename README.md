# weblogic-jms-example

Demo application with spring boot to send and receive messages
into and from JMS queues of a WebLogic server.

## Configure 

e. g. in file application.yml

## Run with Maven

```
mvn clean spring-boot:run
```

## Run stand alone

```
mvn clean package
java -jar target/weblogic-jms-example-*.jar
```

It runs with Java 8 and Java 11.

For Java 17 add some options to the command line:

```
--add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED
```
