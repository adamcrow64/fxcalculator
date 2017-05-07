FROM java:openjdk-8-jdk

MAINTAINER Adam Crow <adamcrow63@gmail.com>

USER root

ADD docker-entrypoint.sh /opt
ADD target/fxcalculator.jar /opt/app.jar

ENTRYPOINT [ "/opt/docker-entrypoint.sh" ]

