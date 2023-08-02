ARG TAG=22.0.1

FROM quay.io/keycloak/keycloak:${TAG}

ENV PHONENUMBER_LOGIN_PLUGIN_VERSION 1.1.0

ENV KEYCLOAK_DIR /opt/keycloak
ENV KC_PROXY edge

LABEL maintainer="Stephane, Segning Lambou <selastlambou@gmail.com>"

USER 0

COPY target/keycloak-phonenumber-login-${PHONENUMBER_LOGIN_PLUGIN_VERSION}.jar $KEYCLOAK_DIR/providers/keycloak-phonenumber-login.jar

RUN $KEYCLOAK_DIR/bin/kc.sh build

USER 1000
