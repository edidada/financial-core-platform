# 多阶段构建：JDK 21 编译 -> JRE 21 运行
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common common
COPY infrastructure infrastructure
COPY gateway gateway
COPY engines engines
COPY services services
COPY benchmark benchmark
COPY tests tests
COPY app app
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 fcp || true
COPY --from=build /src/app/financial-core-app/target/financial-core-app-*.jar app.jar
USER 10001
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xms512m -Xmx1g"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
HEALTHCHECK --interval=15s --timeout=3s --retries=5 \
  CMD wget -qO- http://localhost:8080/v1/invariants >/dev/null || exit 1
