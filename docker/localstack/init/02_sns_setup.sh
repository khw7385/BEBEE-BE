#!/bin/bash

# ==========================================
# SNS 토픽 생성 스크립트
# ==========================================
#
# [목적]
# 프로젝트에 필요한 SNS 토픽들을 생성합니다.
#
# [SNS란?]
# Simple Notification Service - Pub/Sub 메시징
# Publisher → Topic → Subscribers

# ------------------------------------------
# 환경 변수 불러오기
# ------------------------------------------
source /etc/localstack/init/ready.d/00_env_setup.sh

echo "=========================================="
echo "SNS 토픽 생성 시작"
echo "=========================================="

# ------------------------------------------
# 서비스 목록 (각 서비스의 도메인 이벤트 토픽)
# ------------------------------------------
SERVICES=("member" "match" "chat" "notification" "payment")

# ------------------------------------------
# 각 서비스별 도메인 이벤트 토픽 생성
# ------------------------------------------
declare -A TOPIC_ARNS

# 정책 JSON 정의
RETRY_POLICY=$(cat << 'EOF' | tr -d '\n '
{
  "DeliveryPolicy": "{
    \"http\": {
      \"defaultHealthyRetryPolicy\": {
        \"minDelayTarget\": 1,
        \"maxDelayTarget\": 20,
        \"numTries\": 3,
        \"backOffFunction\": \"exponential\"
      }
    }
  }"
}
EOF
)

for SERVICE in "${SERVICES[@]}"; do
  TOPIC_NAME="${PROJECT_NAME}-${ENVIRONMENT}-${SERVICE}-topic"

  echo "Creating topic: ${TOPIC_NAME}"

  TOPIC_ARN=$(awslocal sns create-topic \
    --name "${TOPIC_NAME}" \
    --attributes "${RETRY_POLICY}" \
    --output text \
    --query 'TopicArn')

  if [ $? -eq 0 ]; then
    echo "✓ ${TOPIC_NAME} 생성 완료"
    echo "  ARN: ${TOPIC_ARN}"
    TOPIC_ARNS[$SERVICE]=$TOPIC_ARN
  else
    echo "✗ ${TOPIC_NAME} 생성 실패"
  fi
done

echo ""
echo "=========================================="
echo "SNS 토픽 생성 완료"
echo "=========================================="

# ------------------------------------------
# 확인
# ------------------------------------------
echo "생성된 토픽 목록:"
awslocal sns list-topics --query 'Topics[].TopicArn' --output text | tr '\t' '\n' | grep "${PROJECT_NAME}-${ENVIRONMENT}"

# ------------------------------------------
# 토픽 ARN을 파일에 저장 (다음 스크립트에서 사용)
# ------------------------------------------
echo "# SNS Topic ARNs" > /tmp/sns_topics.env
for SERVICE in "${!TOPIC_ARNS[@]}"; do
  echo "export ${SERVICE^^}_TOPIC_ARN=${TOPIC_ARNS[$SERVICE]}" >> /tmp/sns_topics.env
done

echo ""
echo "토픽 ARN이 /tmp/sns_topics.env에 저장되었습니다."