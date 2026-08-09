#!/usr/bin/env bash
#
# Create the Kafka topics janus depends on. docker-compose sets
# auto.create.topics.enable=false, so topics must be created explicitly.
#
# Usage:
#   ./scripts/kafka-topics.sh
#
# Prerequisite: docker compose up -d kafka
#
set -euo pipefail

KAFKA_CONTAINER="janus-kafka"
BOOTSTRAP="localhost:9092"

if ! docker ps --format '{{.Names}}' | grep -qx "$KAFKA_CONTAINER"; then
    echo "ERROR: container '$KAFKA_CONTAINER' is not running." >&2
    echo "       Run: docker compose up -d kafka" >&2
    exit 1
fi

echo "Creating Kafka topics..."

# janus-audit: 审计事件流 — AuditProducer 写入，AuditConsumer 批量落 usage_log
docker exec "$KAFKA_CONTAINER" kafka-topics --create \
      --if-not-exists \
      --bootstrap-server "$BOOTSTRAP" \
      --topic janus-audit \
      --partitions 1 \
      --replication-factor 1

echo "Topics created:"
docker exec "$KAFKA_CONTAINER" kafka-topics --list --bootstrap-server "$BOOTSTRAP"
