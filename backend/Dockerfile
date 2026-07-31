# =============================================================================
#  Dockerfile multi-stage DUNG CHUNG cho 6 service backend.
#  Tham so hoa bang ARG SERVICE -> 1 file build duoc moi service:
#    docker build -f backend/Dockerfile --build-arg SERVICE=doctor-service \
#                 --build-arg APP_PORT=8082 -t healthcare/doctor-service backend
#  (compose.yaml da truyen san SERVICE + APP_PORT cho tung service.)
#
#  Stage build: maven + JDK 21 -> package jar (skip test, test chay o CI rieng).
#  Stage runtime: JRE 21, chay bang non-root user, healthcheck TCP tren APP_PORT.
# =============================================================================

# ---- Build stage --------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy TRUOC cac pom (aggregator + 6 module) de tan dung layer cache:
# doi source thi khong phai tai lai dependency.
COPY pom.xml ./
COPY api-gateway/pom.xml            api-gateway/
COPY patient-service/pom.xml        patient-service/
COPY doctor-service/pom.xml         doctor-service/
COPY appointment-service/pom.xml    appointment-service/
COPY medical-record-service/pom.xml medical-record-service/
COPY notification-service/pom.xml   notification-service/

ARG SERVICE
RUN mvn -B -q -pl "${SERVICE}" dependency:go-offline

# Copy source va build DUY NHAT module can thiet.
COPY . .
RUN mvn -B -q -pl "${SERVICE}" clean package -DskipTests \
 && cp "${SERVICE}"/target/*-SNAPSHOT.jar /workspace/app.jar

# ---- Runtime stage ------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user (bao mat: khong chay container bang root).
RUN groupadd -r app && useradd -r -g app -d /app app

COPY --from=build /workspace/app.jar /app/app.jar
RUN chown -R app:app /app
USER app

# Port service lang nghe (compose truyen vao, mac dinh 8080).
ARG APP_PORT=8080
ENV APP_PORT=${APP_PORT}
EXPOSE ${APP_PORT}

# Healthcheck: kiem tra port da lang nghe (TCP connect qua /dev/tcp cua bash).
# Khong phu thuoc Actuator -> BL-08 tu chua. Khi bat Actuator (BL-07) nen doi
# sang: curl -f http://127.0.0.1:${APP_PORT}/actuator/health/readiness
HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=60s \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${APP_PORT}' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
