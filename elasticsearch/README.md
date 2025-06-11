# Elasticsearch 정리

Elasticsearch는 Lucene 기반의 오픈소스 분산형 RESTful 검색 및 분석 엔진입니다. 대량의 데이터를 빠르게 저장, 검색, 분석할 수 있도록 설계되었습니다.

## 목차

1. [소개](#소개)
2. [주요 개념](#주요-개념)
3. [설치 및 설정](#설치-및-설정)
4. [기본 사용법](#기본-사용법)
5. [쿼리 예제](#쿼리-예제)
6. [성능 최적화](#성능-최적화)
7. [관련 도구](#관련-도구)

## 소개

Elasticsearch는 다음과 같은 용도로 널리 사용됩니다:

- 전문 검색(Full-text search)
- 로그 분석
- 보안 분석
- 비즈니스 분석
- 메트릭 분석
- 애플리케이션 모니터링

현재 Elastic Stack(이전의 ELK Stack)은 Elasticsearch, Logstash, Kibana, Beats로 구성됩니다.

## 주요 개념

### 클러스터와 노드

- **클러스터(Cluster)**: 하나 이상의 노드(서버)가 모인 집합. 모든 데이터는 클러스터에 분산 저장됨
- **노드(Node)**: 클러스터에 속한 단일 서버. 데이터를 저장하고 클러스터의 색인화/검색 기능에 참여

### 인덱스와 도큐먼트

- **인덱스(Index)**: 비슷한 특성을 가진 문서들의 집합
- **도큐먼트(Document)**: 인덱싱할 수 있는 기본 정보 단위. JSON 형태로 저장됨
- **타입(Type)**: 인덱스의 논리적 카테고리/파티션 (Elasticsearch 7.0 이후로 사용 중단됨)

### 샤드와 레플리카

- **샤드(Shard)**: 인덱스를 여러 조각으로 나눈 것. 수평적 확장 및 분산 병렬 처리 가능
- **레플리카(Replica)**: 샤드의 복제본. 고가용성 및 병렬 처리 성능 향상

### 매핑(Mapping)

- 문서와 문서에 포함된 필드가 인덱스에 저장되고 색인되는 방식을 정의
- 필드의 데이터 타입, 분석기 등을 지정할 수 있음

### 분석기(Analyzer)

- 전문 검색을 위해 텍스트를 처리하는 구성 요소
- 문자 필터, 토크나이저, 토큰 필터로 구성됨

## 설치 및 설정

### Docker를 이용한 설치

```bash
# 단일 노드 Elasticsearch 실행
docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:8.9.0
```

### 직접 설치 (Linux/macOS)

```bash
# 다운로드
curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.9.0-darwin-x86_64.tar.gz

# 압축 해제
tar -xzvf elasticsearch-8.9.0-darwin-x86_64.tar.gz

# 실행
cd elasticsearch-8.9.0/
./bin/elasticsearch
```

### 기본 설정 파일

- `elasticsearch.yml`: 클러스터, 노드, 네트워크, 디스크 등의 설정
- `jvm.options`: JVM 설정
- `log4j2.properties`: 로깅 설정

## 기본 사용법

### 인덱스 생성

```json
PUT /my_index
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2
  },
  "mappings": {
    "properties": {
      "title": { "type": "text" },
      "content": { "type": "text" },
      "created_at": { "type": "date" },
      "user_id": { "type": "keyword" }
    }
  }
}
```

### 도큐먼트 추가

```json
POST /my_index/_doc
{
  "title": "Elasticsearch 가이드",
  "content": "Elasticsearch는 확장성이 뛰어난 검색 엔진입니다.",
  "created_at": "2023-07-21T10:30:15",
  "user_id": "user123"
}
```

### 도큐먼트 검색

```json
GET /my_index/_search
{
  "query": {
    "match": {
      "content": "elasticsearch"
    }
  }
}
```

## 쿼리 예제

### 기본 쿼리 타입

#### Match 쿼리

```json
GET /my_index/_search
{
  "query": {
    "match": {
      "content": "elasticsearch 검색"
    }
  }
}
```

#### Term 쿼리

```json
GET /my_index/_search
{
  "query": {
    "term": {
      "user_id": "user123"
    }
  }
}
```

#### Boolean 쿼리

```json
GET /my_index/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "elasticsearch" }}
      ],
      "filter": [
        { "range": { "created_at": { "gte": "2023-01-01" }}}
      ],
      "should": [
        { "match": { "content": "가이드" }}
      ],
      "must_not": [
        { "term": { "user_id": "blocked_user" }}
      ]
    }
  }
}
```

### 집계(Aggregation)

```json
GET /my_index/_search
{
  "size": 0,
  "aggs": {
    "user_count": {
      "cardinality": {
        "field": "user_id"
      }
    },
    "monthly_docs": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "month"
      }
    }
  }
}
```

## 성능 최적화

### 인덱스 최적화

- 적절한 샤드 개수 설정 (기본값 1, 과도한 샤드는 성능 저하)
- 레플리카 수 조정 (고가용성과 성능 사이의 균형)
- 주기적인 인덱스 병합 (force merge)

### 매핑 최적화

- 필요한 필드만 인덱싱 (`enabled: false` 활용)
- 적절한 데이터 타입 선택
- 불필요한 동적 매핑 제한

### 쿼리 최적화

- 필터 컨텍스트 활용 (점수 계산 없이 빠른 필터링)
- 페이지네이션 최적화 (search_after, scroll API)
- 불필요한 필드 로딩 제한 (`_source` 필터링)

## 관련 도구

### Kibana

데이터 시각화 및 Elasticsearch 관리 웹 인터페이스

### Logstash

다양한 소스에서 데이터를 수집, 변환하여 Elasticsearch로 전송하는 데이터 처리 파이프라인

### Beats

서버에 설치하여 다양한 유형의 데이터를 Elasticsearch나 Logstash로 전송하는 경량 데이터 수집기

- Filebeat: 로그 파일
- Metricbeat: 시스템 및 서비스 메트릭
- Packetbeat: 네트워크 데이터
- Heartbeat: 가용성 모니터링

### Elastic APM

애플리케이션 성능 모니터링 도구
