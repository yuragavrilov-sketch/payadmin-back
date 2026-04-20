ARG JRE_IMAGE=harbor.online.tkbbank.ru/library/eclipse-temurin:21-jre-alpine

FROM ${JRE_IMAGE}

LABEL org.opencontainers.image.title="pay-admin-back"

# Jar кладётся в target/ на стадии mvn_package, потом CI копирует в deploy/
# (см. шаблон tkbpay/microservices/template/.gitlab-ci-java.yml).
COPY deploy/*.jar /usr/local/lib/app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Duser.timezone=GMT+3 -Dspring.profiles.active=$APP_PROF -jar /usr/local/lib/app.jar"]
