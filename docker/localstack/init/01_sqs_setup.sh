#!/bin/bash

# ==========================================
# SQS 큐 생성 스크립트
# ==========================================
#
# [목적]
# 프로젝트에 필요한 모든 SQS 큐를 생성합니다.
#
# [의존성]
# 00-env-setup.sh에 정의된 환경 변수가 필요합니다.

# ------------------------------------------
# 환경 변수 불러오기
# ------------------------------------------
source /etc/localstack/init/ready.d/00_env_setup.sh

echo "=========================================="
echo "SQS 큐 생성 시작"
echo "=========================================="

# ------------------------------------------
# 서비스 목록
# ------------------------------------------
SERVICES=("member" "match" "chat" "notification" "payment")

# ------------------------------------------
# 각 서비스별 메인 큐 생성
# ------------------------------------------
for SERVICE in "${SERVICES[@]}"; do
  QUEUE_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${SERVICE}-queue"

  echo "Creating queue: ${QUEUE_NAME}"

  awslocal sqs create-queue \
    --queue-name "${QUEUE_NAME}" \
    --attributes '{
      "VisibilityTimeout": "30",
      "MessageRetentionPeriod": "345600",
      "MaximumMessageSize": "262144",
      "DelaySeconds": "0",
      "ReceiveMessageWaitTimeSeconds": "20"
    }'

  if [ $? -eq 0 ]; then
    echo "✓ ${QUEUE_NAME} 생성 완료"
  else
    echo "✗ ${QUEUE_NAME} 생성 실패"
  fi
done

echo ""

# ------------------------------------------
# 각 서비스별 DLQ (Dead Letter Queue) 생성
# ------------------------------------------
for SERVICE in "${SERVICES[@]}"; do
  DLQ_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${SERVICE}-queue-dlq"

  echo "Creating DLQ: ${DLQ_NAME}"

  awslocal sqs create-queue \
    --queue-name "${DLQ_NAME}" \
    --attributes '{
      "MessageRetentionPeriod": "1209600"
    }'

  if [ $? -eq 0 ]; then
    echo "✓ ${DLQ_NAME} 생성 완료"
  else
    echo "✗ ${DLQ_NAME} 생성 실패"
  fi
done

echo ""

# ------------------------------------------
# DLQ를 메인 큐에 연결 (Redrive Policy)
# ------------------------------------------
for SERVICE in "${SERVICES[@]}"; do
  QUEUE_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${SERVICE}-queue"
  DLQ_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${SERVICE}-queue-dlq"
  QUEUE_URL="http://localhost:4566/${AWS_ACCOUNT_ID}/${QUEUE_NAME}"
  DLQ_ARN="arn:aws:sqs:${AWS_DEFAULT_REGION}:${AWS_ACCOUNT_ID}:${DLQ_NAME}"

  echo "Attaching DLQ to ${QUEUE_NAME}"

  awslocal sqs set-queue-attributes \
    --queue-url "${QUEUE_URL}" \
    --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"

  if [ $? -eq 0 ]; then
    echo "✓ ${QUEUE_NAME}에 DLQ 연결 완료"
  else
    echo "✗ ${QUEUE_NAME}에 DLQ 연결 실패"
  fi
done

echo ""
echo "=========================================="
echo "SQS 큐 생성 완료"
echo "=========================================="

# ------------------------------------------
# 확인
# ------------------------------------------
echo "생성된 큐 목록:"
awslocal sqs list-queues --query 'QueueUrls' --output text | tr '\t' '\n'| grep "${PROJECT_NAME}-${ENVIRONMENT}"