import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ========================================
// Custom Metrics
// ========================================
const errorRate = new Rate('errors');
const postListLatency = new Trend('post_list_latency');

// Case A: Low Load (Stability Check)
const scenario_low = {
    low_load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '1m', target: 13 },
            { duration: '1m', target: 13 },
            { duration: '30s', target: 0 },
        ],
        gracefulRampDown: '10s',
    }
};

// Case B: Normal Load (Optimization Verification)
const scenario_normal = {
    normal_load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '2m', target: 100 },
            { duration: '5m', target: 100 },
            { duration: '1m', target: 0 },
        ],
        gracefulRampDown: '20s',
    }
};

// Case C: Heavy Load (Threshold Identification)
const scenario_heavy = {
    heavy_load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '3m', target: 300 },
            { duration: '2m', target: 300 },
            { duration: '1m', target: 0 },
        ],
        gracefulRampDown: '30s',
    }
};

// Case D: 1000 Users Load Test
const scenario_1000 = {
    load_1000: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '10m', target: 1000 },
            { duration: '2m', target: 1000 },
            { duration: '1m', target: 0 }
        ],
        gracefulRampDown: '1m',
    }
};

// ========================================
// Test Options
// ========================================
export const options = {
    scenarios: scenario_low,

    thresholds: {
        http_req_duration: ['p(95)<500'],
        errors: ['rate<0.1'],
        post_list_latency: ['p(99)<1000'],
    },
};

// ========================================
// Test Environment
// ========================================
const BASE_URL = __ENV.BASE_URL || 'http://34.47.72.92:8081';
const TEST_PATH = '/posts';

// Post Types (Random Selection)
const POST_TYPES = ['DAY', 'TERM', null];
const GENDER_TYPES = ['MALE', 'FEMALE', null];
const HELP_CATEGORIES = [1, 2, 3, 4, 5, 6, 7, 8];
const DISABILITY_CATEGORIES = [1, 2, 3, 4, 5, 6];
const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

const getRandomType = () => POST_TYPES[Math.floor(Math.random() * POST_TYPES.length)];
const getRandomGender = () => GENDER_TYPES[Math.floor(Math.random() * GENDER_TYPES.length)];

// Pick random items from array (0 to maxCount)
const getRandomItems = (arr, maxCount) => {
    const count = Math.floor(Math.random() * (maxCount + 1)); // 0 ~ maxCount
    if (count === 0) return null;
    const shuffled = [...arr].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, count).join(',');
};

// Get random help categories (0~4 items from 1~8)
const getRandomHelpCategories = () => getRandomItems(HELP_CATEGORIES, 4);

// Get random disability categories (0~3 items from 1~6)
const getRandomDisabilityCategories = () => getRandomItems(DISABILITY_CATEGORIES, 3);

// Get random days of week (0~5 items)
const getRandomDays = () => getRandomItems(DAYS_OF_WEEK, 5);

// Get random honey range (min: 5~20, max: 50~200, or both null)
const getRandomHoneyRange = () => {
    const hasRange = Math.random() > 0.5; // 50% chance to have range
    if (!hasRange) return { minHoney: null, maxHoney: null };

    const minHoney = Math.floor(Math.random() * 15) + 5; // 5 ~ 200
    const maxHoney = Math.floor(Math.random() * 150) + 50; // minHoney ~ 200
    return { minHoney, maxHoney };
};


// ========================================
// Main Test Function
// ========================================
export default function () {
    const memberId = 831479662068515054
    const type = getRandomType();
    const count = 20;

    // Random filter params
    const gender = getRandomGender();
    const helpCategories = getRandomHelpCategories();
    const disabilityCategories = getRandomDisabilityCategories();
    const days = getRandomDays();
    const { minHoney, maxHoney } = getRandomHoneyRange();

    // Build query params

    let queryParams = `?count=${count}`;

    if (type) {
        queryParams += `&type=${type}`;
    }
    if(gender) {
        queryParams += `&gender=${gender}`;
    }
    if (helpCategories) {
        queryParams += `&helpCategories=${helpCategories}`;
    }
    if (disabilityCategories) {
        queryParams += `&disabilityCategoryIds=${disabilityCategories}`;
    }
    if (days) {
        queryParams += `&days=${days}`;
    }
    if (minHoney !== null && maxHoney !== null) {
        queryParams += `&minHoney=${minHoney}&maxHoney=${maxHoney}`;
    }

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Member-Id': String(memberId),
        },
        tags: { name: 'get_posts_list' },
    };

    const startTime = new Date().getTime();
    const response = http.get(BASE_URL + TEST_PATH + queryParams, params);
    const endTime = new Date().getTime();

    // Record custom metrics
    postListLatency.add(endTime - startTime);

    // Response validation
    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'response has posts array': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.posts !== undefined || body.content !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(!success);

    // Random sleep (0.5 ~ 1.5s)
    sleep(Math.random() + 0.5);
}

// ========================================
// Pagination Test (Optional)
// ========================================
export function paginationTest() {
    const memberId = getRandomMemberId();
    const count = 20;
    let lastPostId = null;
    let pageCount = 0;
    const maxPages = 5;

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Member-Id': String(memberId),
        },
        tags: { name: 'pagination_test' },
    };

    while (pageCount < maxPages) {
        let queryParams = `?count=${count}`;
        if (lastPostId) {
            queryParams += `&lastPostId=${lastPostId}`;
        }

        const response = http.get(BASE_URL + TEST_PATH + queryParams, params);

        const success = check(response, {
            'pagination status is 200': (r) => r.status === 200,
        });

        if (!success || response.status !== 200) {
            break;
        }

        try {
            const body = JSON.parse(response.body);
            const posts = body.posts || body.content || [];

            if (posts.length === 0) {
                break;
            }

            lastPostId = posts[posts.length - 1].postId || posts[posts.length - 1].id;
            pageCount++;
        } catch (e) {
            break;
        }

        sleep(0.3);
    }

    sleep(Math.random() + 0.5);
}

// ========================================
// Setup
// ========================================
export function setup() {
    console.log('========================================');
    console.log('Post List Load Test Started');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Path Info: ${TEST_PATH}`);
    console.log('========================================');

    // Health check
    const healthCheck = http.get(`${BASE_URL}/actuator/health`);
    if (healthCheck.status !== 200) {
        console.warn('Health check failed. Continuing test.');
    }

    return { startTime: new Date().toISOString() };
}

// ========================================
// Teardown
// ========================================
export function teardown(data) {
    console.log('========================================');
    console.log('Post List Load Test Completed');
    console.log(`Start Time: ${data.startTime}`);
    console.log(`End Time: ${new Date().toISOString()}`);
    console.log('========================================');
}