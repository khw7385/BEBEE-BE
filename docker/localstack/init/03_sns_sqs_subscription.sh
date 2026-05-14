#!/bin/bash

# ==========================================
# SNS → SQS 구독 설정 스크립트
# ==========================================
#
# [목적]
# SNS 토픽을 SQS 큐로 연결합니다.
#
# [동작]
# SNS 토픽에 메시지 발행 → 자동으로 SQS 큐에 전달
#
# [구독 관계]
# - match-service: member 토픽 구독
# - chat-service: member, match 토픽 구독
# - notification-service: chat, match 토픽 구독
# - payment-service: match 토픽 구독

# ------------------------------------------
# 환경 변수 불러오기
# ------------------------------------------
source /etc/localstack/init/ready.d/00_env_setup.sh
source /tmp/sns_topics.env

echo "=========================================="
echo "SNS → SQS 구독 설정 시작"
echo "=========================================="

# ------------------------------------------
# 구독 관계 정의
# ------------------------------------------
# 형식: "QUEUE_SERVICE:TOPIC_SERVICE"
SUBSCRIPTIONS=(
  "match:member"
  "match:match"
  "chat:member"
  "chat:match"
  "notification:chat"
  "notification:match"
  "payment:match"
  "member:payment"
  "member:match"
)

# ------------------------------------------
# 구독 관계 및 필터 정책 정의
# ------------------------------------------
# 각 구독에 대한 필터 정책을 정의합니다.
declare -A FILTER_POLICIES

# Chat 서비스: Agreement 관련 이벤트만 수신
FILTER_POLICIES["chat:match"]='{
  "eventType": [
    "AgreementCreatedEvent",
    "AgreementConfirmedEvent",
    "AgreementRefusedEvent"
  ]
}'

# Payment 서비스: Agreement Confirmed 이벤트만 수신
FILTER_POLICIES["payment:match"]='{
  "eventType": [
    "AgreementConfirmedEvent"
  ]
}'

# Match 서비스
FILTER_POLICIES["match:match"]='{
  "eventType": [
    "AgreementConfirmedEvent"
  ]
}'

# Member 서비스
FILTER_POLICIES["member:payment"]='{
  "eventType": [
    "HoneyWalletChangedEvent"
  ]
}'

# Member 서비스: EngagementCompleted, ReviewCreated 이벤트 수신
FILTER_POLICIES["member:match"]='{
  "eventType": [
    "EngagementCompletedEvent",
    "ReviewCreatedEvent"
  ]
}'

  # Match 서비스: BadgeCreated 이벤트 수신
FILTER_POLICIES["match:member"]='{
    "eventType": [
      "BadgeCreatedEvent"
  ]
}'

# ------------------------------------------
# 구독 생성
# ------------------------------------------
for SUB in "${SUBSCRIPTIONS[@]}"; do
  IFS=':' read -r QUEUE_SERVICE TOPIC_SERVICE <<< "$SUB"

  QUEUE_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${QUEUE_SERVICE}-queue"
  TOPIC_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${TOPIC_SERVICE}-topic"
  QUEUE_URL="http://localhost:4566/${AWS_ACCOUNT_ID}/${QUEUE_NAME}"

  echo ""
  echo "구독 설정: ${QUEUE_SERVICE}-queue <- ${TOPIC_SERVICE}-topic"

  # SQS 큐 ARN 가져오기
  QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url "${QUEUE_URL}" \
    --attribute-names QueueArn \
    --output text \
    --query 'Attributes.QueueArn')

  if [ -z "$QUEUE_ARN" ]; then
    echo "✗ ${QUEUE_NAME} ARN을 찾을 수 없습니다."
    continue
  fi

  # SNS 토픽 ARN 가져오기
  TOPIC_ARN=$(awslocal sns list-topics \
    --output text \
    --query "Topics[?contains(TopicArn, '${TOPIC_NAME}')].TopicArn | [0]")

  if [ -z "$TOPIC_ARN" ]; then
    echo "✗ ${TOPIC_NAME} ARN을 찾을 수 없습니다."
    continue
  fi

  echo "  Queue ARN: ${QUEUE_ARN}"
  echo "  Topic ARN: ${TOPIC_ARN}"

  # SNS → SQS 구독 생성
  SUBSCRIPTION_ARN=$(awslocal sns subscribe \
    --topic-arn "${TOPIC_ARN}" \
    --protocol sqs \
    --notification-endpoint "${QUEUE_ARN}" \
    --attributes '{"RawMessageDelivery": "true"}' \
    --output text \
    --query 'SubscriptionArn')

  if [ $? -eq 0 ]; then
    echo "✓ 구독 생성 완료"
    echo "  Subscription ARN: ${SUBSCRIPTION_ARN}"

    # 필터 정책 설정
    FILTER_KEY="${QUEUE_SERVICE}:${TOPIC_SERVICE}"
    if [[ -v "FILTER_POLICIES[$FILTER_KEY]" ]]; then
      FILTER_POLICY="${FILTER_POLICIES[$FILTER_KEY]}"

      if [ "$FILTER_POLICY" != "{}" ]; then
        echo "  필터 정책 적용 중..."

        awslocal sns set-subscription-attributes \
          --subscription-arn "${SUBSCRIPTION_ARN}" \
          --attribute-name FilterPolicy \
          --attribute-value "$FILTER_POLICY"

        if [ $? -eq 0 ]; then
          echo "  ✓ 필터 정책 적용 완료"
          echo "  Filter: $(echo $FILTER_POLICY)"
        else
          echo "  ✗ 필터 정책 적용 실패"
        fi
      fi
    fi
  else
    echo "✗ 구독 생성 실패"
    continue
  fi

  # SQS 큐 정책 설정 (SNS가 SQS에 메시지를 보낼 수 있도록 허용)
  POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "sns.amazonaws.com"
      },
      "Action": "sqs:SendMessage",
      "Resource": "${QUEUE_ARN}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${TOPIC_ARN}"
        }
      }
    }
  ]
}
EOF
)

  awslocal sqs set-queue-attributes \
    --queue-url "${QUEUE_URL}" \
    --attributes "{\"Policy\":\"$(echo $POLICY | sed 's/"/\\"/g')\"}"

  if [ $? -eq 0 ]; then
    echo "✓ 큐 정책 설정 완료"
  else
    echo "✗ 큐 정책 설정 실패"
  fi
done

echo ""
echo "=========================================="
echo "SNS → SQS 구독 설정 완료"
echo "=========================================="

# ------------------------------------------
# 확인
# ------------------------------------------
echo ""
echo "구독 목록:"
for SERVICE in "member" "match" "chat" "notification" "payment"; do
  TOPIC_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${SERVICE}-topic"
  TOPIC_ARN=$(awslocal sns list-topics \
    --output text \
    --query "Topics[?contains(TopicArn, '${TOPIC_NAME}')].TopicArn | [0]")

  if [ -n "$TOPIC_ARN" ]; then
    echo ""
    echo "[${SERVICE}-topic]"
    awslocal sns list-subscriptions-by-topic \
      --topic-arn "${TOPIC_ARN}" \
      --output table \
      --query 'Subscriptions[].{Protocol:Protocol,Endpoint:Endpoint}'
  fi
done