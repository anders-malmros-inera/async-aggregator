FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
# Build only the aggregator module and its dependencies
RUN mvn -f pom.xml -pl aggregator -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/aggregator/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
