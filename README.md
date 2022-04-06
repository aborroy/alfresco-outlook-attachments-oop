# Alfresco Outlook Attachments Extractor - OoP

This is an ACS application for Alfresco Java SDK 5.1.4 (ACS 7.2).

When uploading an Outlook Message File (EML, MSG) to Alfresco, this application extracts the attachment files and stores them in a separate folder. This project is using the [Events API](https://docs.alfresco.com/content-services/latest/develop/oop-ext-points/).

Following parameters can be adapted using `application.properties` file or Java Environment variables.

```
spring.activemq.brokerUrl=tcp://localhost:61616
spring.jms.cache.enabled=false
alfresco.events.enableSpringIntegration=false
alfresco.events.enableHandlers=true

content.service.security.basicAuth.username=admin
content.service.security.basicAuth.password=admin

content.service.url=http://localhost:8080
content.service.path=/alfresco/api/-default-/public/alfresco/versions/1
```

## Building

Build the code as a regular Maven project.

```
$ mvn clean package
$ ls target/
alfresco-outlook-attachments-oop-5.1.4.jar
```

## Running

This application requires a running ACS 7.2 deployment with ActiveMQ 61616 port exposed.

Run this project as a standalone Spring Boot app.

```
$ java -jar target/alfresco-outlook-attachments-oop-5.1.4.jar
```

Alternatively, you may build the Docker Image to use this application as Container with

```
$ docker build . -t alfresco-outlook-attachments-oop
```
