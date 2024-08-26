FROM eclipse-temurin:17.0.3_7-jdk

LABEL author="Bruno Ziswiler"
LABEL description="Backend Image for PV Logger"
LABEL version="1.0.2"

RUN mkdir -p "/opt/pvlogger/masterdata/"
COPY "pvlogger-masterdata-server-all.jar" "/opt/pvlogger/masterdata-server.jar"
COPY "Sun2000*.csv" "/opt/pvlogger/masterdata/"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/masterdata-server.jar", \
    "-config=application-prod.conf" \
]
