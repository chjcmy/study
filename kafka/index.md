# Kafka 완전 정복 — 학습 로드맵

> **총 예상 학습 시간:** 25~30시간 (4파트)
> **목표:** log-friends 시스템의 핵심 메시지 버스인 Kafka를 내부 동작 원리까지 이해하고, SDK·Pipeline 코드와 직접 연결하여 체화한다

---

## 학습 순서

```
Part 1 (아키텍처) ← 브로커, 토픽, 파티션, 복제 메커니즘
  ↓
Part 2 (프로듀서) ← BatchTransporter 설계 근거 이해
  ↓
Part 3 (컨슈머) ← Spark StreamingJob 소비 패턴 이해
  ↓
Part 4 (KRaft & 운영) ← 단일 노드 운영, 튜닝
```

---

## 파트별 개요

| 파트 | 범위 | 시간 | 핵심 | 프로젝트 연결 |
|---|---|---|---|---|
| **Part 1** | 아키텍처 전체 | 6~7시간 | 브로커, 토픽, 파티션, 오프셋, ISR, Commit Log | `log-friends.batch` 토픽 설계 근거 |
| **Part 2** | 프로듀서 심화 | 6~7시간 | RecordAccumulator, acks, 멱등성, 압축 | `BatchTransporter` — acks=1, linger_ms=5, retries=3 |
| **Part 3** | 컨슈머 심화 | 6~7시간 | 컨슈머 그룹, 오프셋 커밋, 리밸런싱 | Spark Structured Streaming 소비 패턴 |
| **Part 4** | KRaft & 운영 | 6~8시간 | KRaft 합의, 모니터링, 튜닝, 트러블슈팅 | KRaft 단일 노드, docker-compose 설정 |

---

## log-friends와의 연결 지도

```
SDK / Pipeline 컴포넌트                →  Kafka 파트

BatchTransporter (kafka-clients 3.9.0)
  ├── acks=1, retries=3                →  Part 2 (프로듀서 acks 심화)
  ├── linger_ms=5, batch.size=16384   →  Part 2 (RecordAccumulator 배치 메커니즘)
  ├── ProducerRecord("log-friends.batch") → Part 1 (토픽, 파티션, 오프셋)
  └── KafkaProducer lazy init         →  Part 2 (프로듀서 생명주기)

Pipeline StreamingJob
  ├── Spark Structured Streaming       →  Part 3 (컨슈머 그룹, 오프셋 커밋)
  └── log-friends.batch 소비           →  Part 3 (At-Least-Once 시맨틱)

KRaft 단일 노드 (docker-compose)
  ├── ZooKeeper 없는 메타데이터 관리    →  Part 4 (KRaft 합의 알고리즘)
  └── __cluster_metadata 토픽          →  Part 4 (KRaft 내부 구조)
```

---

## 추천 학습 방법

1. **Part 1을 완전히 이해한 후** Part 2로 이동한다. 토픽/파티션 개념 없이 프로듀서를 배우면 설정 값의 의미를 체화할 수 없다.
2. **핵심 질문**을 먼저 읽고, 답변을 가린 채 자기 언어로 설명해보기. 설명이 막히면 해당 섹션을 다시 읽는다.
3. **실습 명령어**는 반드시 직접 실행한다. `kafka-topics.sh --describe` 결과를 실제로 눈으로 확인해야 체화된다.
4. **프로젝트 연결** 섹션에서 실제 SDK 코드 파일을 열어 대조하며 읽는다.
   - `log-friends-sdk/src/main/kotlin/com/logfriends/agent/BatchTransporter.kt`
5. **체크리스트** 항목을 모두 체크할 수 있을 때 다음 파트로 이동한다.

---

## 필수 사전 준비

```bash
# Docker Compose로 Kafka KRaft 단일 노드 실행
docker compose up -d kafka

# Kafka CLI 도구 확인 (컨테이너 내부)
docker exec -it kafka kafka-topics.sh --version

# 또는 로컬 kafka 설치 후
brew install kafka  # macOS
```

---

## 참고 자료

- [Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka: The Definitive Guide (2판)](https://www.confluent.io/resources/kafka-the-definitive-guide-v2/)
- [KRaft 공식 KIP-500](https://cwiki.apache.org/confluence/display/KAFKA/KIP-500)
- log-friends SDK 소스: `log-friends-sdk/src/main/kotlin/com/logfriends/agent/BatchTransporter.kt`
