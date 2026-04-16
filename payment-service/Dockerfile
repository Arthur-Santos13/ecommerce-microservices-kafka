# --- Build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY payment-service/pom.xml payment-service/
COPY payment-service/src payment-service/src

COPY product-service/pom.xml product-service/
COPY order-service/pom.xml order-service/
COPY notification-service/pom.xml notification-service/
COPY api-gateway/pom.xml api-gateway/
COPY config-server/pom.xml config-server/

RUN apk add --no-cache maven \
    && mvn -pl payment-service -am -B -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/payment-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
