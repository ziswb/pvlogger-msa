FROM postgres:16.2

LABEL author="Bruno Ziswiler"
LABEL description="Postgres Image for PV Logger"
LABEL version="1.0.0"

ENV POSTGRES_USER="postgres"
ENV POSTGRES_DB="postgres"
ENV TZ="Europe/Zurich"

RUN useradd -u 1000 bziswiler
USER bziswiler
