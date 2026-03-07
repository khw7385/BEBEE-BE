import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ========================================
// 커스텀 메트릭 정의
// ========================================
const errorRate = new Rate('errors');
const pushLatency = new Trend('push_latency');

// Case A: 일상적인 저부하 상황 (안정성 확인)
const scenario_low = {
    low_load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '1m', target: 30 },  // 1분 동안 30명까지 서서히 증가
            { duration: '1m', target: 30 },  // 3분 동안 30명 유지 (평상시 부하)
            { duration: '30s', target: 0 },  // 정리
        ],
        gracefulRampDown: '10s',
    }
};

// Case B: 예상 평균 부하 (최적화 효율 검증)
const scenario_normal = {
    normal_load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '2m', target: 100 }, // 2분 동안 100명까지 증가
            { duration: '5m', target: 100 }, // 5분 동안 유지 (실제 목표 부하)
            { duration: '1m', target: 0 },   // 정리
        ],
        gracefulRampDown: '20s',
    }
};

// Case C: 한계 지점 측정 (임계점 식별)
const scenario_heavy = {
    heavy_load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '3m', target: 300 }, // 3분 동안 300명까지 꾸준히 증가
            { duration: '2m', target: 300 }, // 2분 동안 최대 부하 유지
            { duration: '1m', target: 0 },   // 정리
        ],
        gracefulRampDown: '30s',
    }
};

const scenario_1000 = {
    load_1000: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages:[
            { duration: '10m', target: 1000 },
            { duration: '2m', target: 1000},
            { duration: '1m', target: 0}
        ],
        gracefulRampDown: '1m',
    }
}

// ========================================
// 테스트 설정
// ========================================
export const options = {
    // 시나리오 설정
    scenarios: scenario_1000,

    // 임계값 설정
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 95% 요청이 500ms 이내
        errors: ['rate<0.1'],               // 에러율 10% 미만
        push_latency: ['p(99)<1000'],       // 99% 푸시 응답이 1초 이내
    },
};

// ========================================
// 테스트 환경 설정
// ========================================
const BASE_URL = 'http://34.47.77.230:8081';
const TEST_PATH = '/notifications/fcm/test/batch/lock-free';

const FCM_TOKEN = __ENV.FCM_TOKEN || 'test-fcm-token';



// ========================================
// 메인 테스트 함수
// ========================================
export default function () {
    // 푸시 알림 전송 테스트
    const payload = JSON.stringify({
        token: FCM_TOKEN,
        title: `테스트 알림 ${__VU}-${__ITER}`,
        body: `k6 부하 테스트 메시지입니다. VU: ${__VU}, Iteration: ${__ITER}`,
        data: {
            testId: `${__VU}-${__ITER}`,
            timestamp: new Date().toISOString(),
        },
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Member-Id' : '1'
        },
        tags: { name: 'push_notification' },
    };

    const startTime = new Date().getTime();
    const response = http.post(BASE_URL + TEST_PATH, payload, params);
    const endTime = new Date().getTime();

    // 커스텀 메트릭 기록
    pushLatency.add(endTime - startTime);

    // 응답 검증
    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);

    // 요청 간 간격 (0.5 ~ 1.5초 랜덤)
    sleep(Math.random() + 0.5);
}

// ========================================
// 테스트 시작 전 설정
// ========================================
export function setup() {
    console.log('========================================');
    console.log('FCM 푸시 알림 부하 테스트 시작');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Path Info: ${TEST_PATH}`)
    console.log(`FCM Token: ${FCM_TOKEN.substring(0, 20)}...`);
    console.log('========================================');

    // 서버 상태 확인
    const healthCheck = http.get(`${BASE_URL}/actuator/health`);
    if (healthCheck.status !== 200) {
        console.warn('서버 상태 확인 실패. 테스트를 계속합니다.');
    }

    return { startTime: new Date().toISOString() };
}

// ========================================
// 테스트 종료 후 정리
// ========================================
export function teardown(data) {
    console.log('========================================');
    console.log('FCM 푸시 알림 부하 테스트 완료');
    console.log(`시작 시간: ${data.startTime}`);
    console.log(`종료 시간: ${new Date().toISOString()}`);
    console.log('========================================');
}