# Elasticsearch 아키텍처

## 클러스터 구성

- 마스터 노드 선출 프로세스 (Zen Discovery)
- 샤드 할당 피보팅 설정
- Hot-Warm 아키텍처 구성

## 데이터 분산

- 프라이머리/레플리카 샤드 배치 전략
- 라우팅 공식: `shard = hash(routing) % num_primary_shards`
- 샤드 재배치 임계값 설정

## 검색 실행 흐름

- Query Phase: 샤드별 결과 수집
- Fetch Phase: 문서 내용 조회
- DFS 모드에서의 통계 정확도 향상

## 역색인 구조

- FST(Finite State Transducer) 기반 용어 사전
- Skip List를 이용한 빠른 조회
- Doc Values 활용 칼럼 기반 저장
