# Spark 완전 정복 — 학습 로드맵

> **목표:** Spark 4.0.2 + Structured Streaming 원리를 이해하고, log-friends Pipeline을 직접 개선할 수 있는 수준으로 체화
> **총 예상 학습 시간:** 20~25시간 (4파트)
> **기준 코드:** `log-friends-pipeline/src/main/kotlin/com/logfriends/spark/`

---

## 학습 순서

```
Part 1 (Spark 기초 — 5~6시간)
  ↓  RDD/DataFrame/DAG 개념 없이 Streaming을 이해하기 어렵다
Part 2 (Structured Streaming — 6~7시간)
  ↓  StreamingJob.kt의 readStream → writeStream 패턴 완전 이해
Part 3 (Kafka Source/Sink — 5~6시간)
  ↓  Kafka 오프셋 관리, 역직렬화, 파티션 연동
Part 4 (Window & 집계 — 4~5시간)
     Aggregator.kt의 1분 윈도우 집계, 이벤트 시간 처리
```

---

## 파트별 개요

| 파트 | 제목 | 학습 시간 | 핵심 내용 |
|---|---|---|---|
| [Part 1](part1/README.md) | Spark 기초 | 5~6시간 | 아키텍처, RDD/DataFrame/Dataset, Lazy Evaluation, Shuffle |
| [Part 2](part2/README.md) | Structured Streaming | 6~7시간 | 무한 테이블 추상화, 트리거, 출력 모드, 체크포인트 |
| Part 3 | Kafka Source/Sink | 5~6시간 | Kafka 커넥터, 오프셋 관리, 역직렬화 전략 |
| Part 4 | Window & 집계 | 4~5시간 | 이벤트 시간, Watermark, 슬라이딩/텀블링 윈도우 |

---

## log-friends Pipeline 연결 지도

```
log-friends-pipeline 코드                →  학습 파트

StreamingJob.kt
  SparkSession.builder().getOrCreate()   →  Part 1 (아키텍처, local 모드)
  readStream().format("kafka")           →  Part 3 (Kafka Source)
  writeStream().foreachBatch(...)        →  Part 2 (Structured Streaming)
  trigger(ProcessingTime(10s))           →  Part 2 (트리거 종류)
  checkpointLocation                     →  Part 2 (체크포인트)

ProtoDeserializer.kt
  AgentMessage.parseFrom(bytes)          →  Part 3 (바이너리 역직렬화)
  EventRow 변환                           →  Part 1 (DataFrame 스키마 설계)

Aggregator.kt
  groupBy { workerId, truncateToMinute } →  Part 4 (윈도우 집계)
  filter { type == "HTTP" }              →  Part 1 (Narrow Transformation)
  sumOf { durationMs }                   →  Part 4 (집계 함수)

ClickHouseWriter.kt
  HTTP API POST → metrics 테이블         →  Part 3 (foreachBatch Sink 패턴)
  SummingMergeTree 멱등성                →  Part 3 (장애 복구, 재처리)

TimescaleDBWriter.kt
  JDBC 배치 INSERT → 4개 테이블          →  Part 3 (JDBC Sink, 연결 풀링)
  DCL + @Volatile 연결 관리              →  Part 2 (Sink 장애 처리)
```

---

## 추천 학습 방법

1. **순서 준수**: Part 1 개념이 Part 2~4의 기반이다. 건너뛰지 말 것
2. **코드 대조**: 각 파트의 "프로젝트 연결" 섹션에서 반드시 실제 파일을 열어 비교
3. **질문 선행**: Q&A 섹션의 질문을 먼저 읽고, 답을 가린 채 자기 언어로 설명해보기
4. **체크리스트 활용**: 모든 항목을 체크할 수 있을 때 다음 파트로 이동
5. **실습 필수**: 코드 블록은 반드시 직접 실행하여 Spark UI(port 4040)로 확인

---

## 핵심 용어 사전

| 용어 | 설명 |
|---|---|
| DAG (Directed Acyclic Graph) | Spark가 생성하는 작업 실행 계획 그래프 |
| Stage | Shuffle 경계로 나뉜 작업 단계 |
| Task | 파티션 하나를 처리하는 최소 실행 단위 |
| Executor | Worker 노드에서 실행되는 JVM 프로세스 |
| Micro-Batch | 주기적으로 데이터를 소규모 배치로 처리하는 방식 |
| Checkpoint | 오프셋과 상태를 디스크에 저장하는 내결함성 메커니즘 |
| Watermark | 늦게 도착하는 이벤트를 얼마나 기다릴지 지정하는 임계값 |
| foreachBatch | 마이크로배치마다 커스텀 로직을 실행하는 Sink API |
