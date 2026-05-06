# 🍯 비비 (Be Bee)
백엔드 서버 레포지토리입니다.
<br/>

<img width="1600" alt="비비" src="https://github.com/user-attachments/assets/ff195871-0a1a-44f6-8f5e-3e6e35f24546" />

### Team Members
|                           <a href="https://github.com/MinJaeSon"><img src="https://github.com/MinJaeSon.png" width=120/></a>                           |                          <a href="https://github.com/khw7385"><img src="https://github.com/khw7385.png" width=120/></a>                           |                       <a href="https://github.com/Hannaoo1"><img src="https://github.com/Hannaoo1.png" width=120 /></a>                        |                         <a href="https://github.com/YongjaeKwon0629"><img src="https://github.com/YongjaeKwon0629.png" width=120/></a>                          |                         <a href="https://github.com/doteeth83"><img src="https://github.com/doteeth83.png" width=120/></a>                          |                         <a href="https://github.com/minsunmanju"><img src="https://github.com/minsunmanju.png" width=120/></a>                          |
|:-----------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------:|
|                                     <a href="https://youthing.tistory.com/">손민재</a>                                     |                                 <a href="https://pingzeming.tistory.com/">김현원</a>                                  |                                  <a href="https://hochi-dev.tistory.com/">정한나</a>                                  |                                   <a href="https://velog.io/@maxgun98/posts">권용재</a>                                    |                                   <a href="https://velog.io/@seoyeon5117/posts/ ">고도희</a>                                    |                                   <a href="https://study-csa.tistory.com/">김민선</a>                                    |
| Leader, BE | Infra, BE | BE  | BE, FE | Design, FE | FE |

<br/><br/>

## 1. 프로젝트 개요
<img width="1600" alt="프로젝트 개요" src="https://github.com/user-attachments/assets/ee5a9b98-faa3-4278-bd2c-a99c6b7d76e5" />

### 1-1. 서비스 시연
[🎥 시연 영상 보러가기](https://youtu.be/mCwxWMNSfpc)

<img width="1600" alt="시연 영상" src="https://github.com/user-attachments/assets/5570ded0-201d-4c92-810c-1047514deed4" />

### 1-2. 스크린리더 적용 시연 영상
[🎥 시연 영상 보러가기](https://youtu.be/h2tctPD_5qY)

<img width="1600" alt="스크린 리더 시연 영상" src="https://github.com/user-attachments/assets/34c0db8e-6bc4-44ab-910e-a4e428f14cef" />

<br/><br/>

## 2. 기술스택
<img width="5760" height="3240" alt="기술스택" src="https://github.com/user-attachments/assets/925f6bd5-da82-4cd4-aa66-a05eb5d8b0a6" />

<br/><br/>

## 3. 시스템 아키텍처
<img width="5283" height="5163" alt="image" src="https://github.com/user-attachments/assets/33b0f51c-688b-4e78-bc10-2ed6499b6f18" />


<br/><br/>

[📌 발표 자료 전체 보기](https://github.com/user-attachments/files/25568291/1._._.-compressed.pdf)

---

<br/>

# 시작하기

## 📁 프로젝트 구조

```shell
├── common // 공통 모듈
│   ├── build.gradle
│   └── src/
├── common-data // DB와 연관된 공통 모듈
│   ├── build.gradle
│   └── src/
├── chat-service
│   ├── build.gradle
│   └── src/
├── match-service
│   ├── build.gradle
│   └── src/
├── member-service
│   ├── build.gradle
│   └── src/
├── notification-service
│   ├── build.gradle
│   └── src/
├── payment-service
│   ├── build.gradle
│   └── src
├── docker
│   └── mysql
├── build.gradle
├── settings.gradle
├── docker-compose.yaml
└── env.example
```
- **`member-service`** : 회원 도메인 관리 및 인증/인가 처리
- **`match-service`** : 매칭 도메인 로직 및 비즈니스 규칙 처리
- **`chat-service`** : WebSocket 기반 실시간 채팅 서비스
- **`payment-service`** : 결제 처리 및 트랜잭션 관리 모듈
- **`notification-service`** : FCM 기반 실시간 알림 서비스

<br/><br/>

## 🚀 로컬 실행 방법

### 1) 환경변수 설정

```shell
# env.example 파일을 .env로 복사
cp env.example .env

# .env 파일에서 실제 값으로 수정
# 특히 JWT 시크릿 키는 반드시 변경해야 합니다!
```

### 2) 인프라 서비스 실행
```shell
# Docker Compose로 MySQL, Redis 실행
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 특정 서비스 중지 및 삭제
docker-compose rm -sf ${서비스명}  # ex) 서비스 명: localstack
```

### flyway 관련 명령
- flway 초기화
```shell
./gradlew :member-service:flywayClean # member-service 자리에 다른 서비스 이름이 와도 된다.
```
