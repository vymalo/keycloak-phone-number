ARG TAG=21.0.1

FROM quay.io/keycloak/keycloak:${TAG}

ENV PHONENUMBER_LOGIN_PLUGIN_VERSION 1.0.0
ENV KEYCLOAK_DIR /opt/keycloak
ENV KC_PROXY edge

LABEL maintainer="Stephane, Segning Lambou <selastlambou@gmail.com>"

USER 0

RUN mkdir $JBOSS_HOME/providers

RUN curl -H "Accept: application/zip" https://github.com/vymalo/keycloak-phonenumber-login/releases/download/v${PHONENUMBER_LOGIN_PLUGIN_VERSION}/keycloak-phonenumber-login-${PHONENUMBER_LOGIN_PLUGIN_VERSION}.jar -o $JBOSS_HOME/providers/keycloak-phonenumber-login-${PHONENUMBER_LOGIN_PLUGIN_VERSION}.jar -Li

RUN $KEYCLOAK_DIR/bin/kc.sh build

USER 1000
