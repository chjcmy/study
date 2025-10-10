# 11. 벡터와 pgvector 확장
#PostgreSQL/Vector #PostgreSQL/AI #pgvector

최근 인공지능(AI) 기술, 특히 자연어 처리와 이미지 검색 분야가 발전하면서 **벡터 데이터**의 중요성이 크게 대두되었습니다. PostgreSQL은 `pgvector`라는 강력한 확장을 통해 이러한 벡터 데이터를 데이터베이스 내에서 직접 다루는 기능을 제공합니다.

## 11.1. 벡터 데이터와 pgvector란?

### 벡터 데이터 (Vector Embeddings)

> [!INFO] 벡터 임베딩이란?
> 텍스트, 이미지, 오디오 등 비정형 데이터를 OpenAI의 `text-embedding-ada-002`와 같은 임베딩 모델을 사용하여 고차원의 숫자 배열(벡터)로 변환한 것입니다. 이 벡터는 데이터의 의미적, 문맥적 특징을 수학적으로 표현합니다.
> - **예시**: "강아지"와 "개"라는 단어는 서로 다른 텍스트이지만, 임베딩 모델을 거치면 벡터 공간에서 매우 가까운 위치에 표현됩니다.

이러한 특징 덕분에 벡터 간의 거리를 계산하여 "의미적으로 유사한" 데이터를 찾는 **유사도 검색(Similarity Search)** 이 가능해집니다. 이는 추천 시스템, 의미 기반 검색, 이미지 검색 등 다양한 AI 애플리케이션의 핵심 기술입니다.

### pgvector 확장

`pgvector`는 PostgreSQL에 벡터 데이터 타입을 추가하고, 벡터 간의 유사도 검색 및 고속 검색을 위한 인덱싱 기능을 제공하는 오픈소스 확장 프로그램입니다. 이를 통해 개발자들은 별도의 벡터 전문 데이터베이스 없이, 기존에 익숙한 PostgreSQL 안에서 AI 기반 애플리케이션을 구축할 수 있습니다.

## 11.2. 설치 및 기본 사용법

### 1. 확장 활성화

`pgvector`는 대부분의 주요 PostgreSQL 클라우드 서비스(예: Amazon RDS, Google Cloud SQL)에서 지원됩니다. 데이터베이스에 접속하여 다음 명령어로 간단히 활성화할 수 있습니다.

```sql
CREATE EXTENSION vector;
```

### 2. `vector` 타입 사용

`vector` 타입을 사용하여 테이블에 벡터 데이터를 저장할 열을 생성합니다. 벡터의 차원(dimension)을 함께 지정해야 합니다.

```sql
-- OpenAI 임베딩 모델(1536차원)을 저장하기 위한 테이블 생성
CREATE TABLE items (
    id SERIAL PRIMARY KEY,
    content TEXT,
    embedding vector(1536)
);
```

### 3. 유사도 검색

`pgvector`는 다양한 거리 계산 연산자를 제공하여 유사도 검색을 수행합니다.

-   `<->`: **L2 거리 (Euclidean distance)** - 두 벡터 간의 직선 거리.
-   `<=>`: **코사인 유사도 (Cosine Similarity)** - 두 벡터가 이루는 각도를 기반으로 방향성의 유사도를 계산. **텍스트 임베딩 검색에 가장 널리 사용됩니다.**
-   `<#>`: **내적 (Inner Product)**

```sql
-- 특정 임베딩과 가장 유사한(코사인 거리가 가까운) 5개의 아이템을 검색
SELECT content, 1 - (embedding <=> '[0.1, 0.2, ...]') AS similarity
FROM items
ORDER BY embedding <=> '[0.1, 0.2, ...]'
LIMIT 5;
```
> [!NOTE]
> 코사인 유사도는 1에 가까울수록 유사하며, 코사인 거리는 0에 가까울수록 유사합니다. 따라서 `1 - (코사인 거리)`를 통해 유사도 점수로 변환하여 사용하기도 합니다.

## 11.3. 고속 검색을 위한 인덱스

수십만 개 이상의 벡터 데이터에서 유사도 검색을 수행하려면 인덱스는 필수적입니다. `pgvector`는 **ANN(Approximate Nearest Neighbor, 근사 근접 이웃)** 검색을 위한 인덱스를 지원하여, 100% 정확도를 약간 희생하는 대신 검색 속도를 비약적으로 향상시킵니다.

### 1. IVFFlat 인덱스

-   **원리**: 전체 벡터를 여러 개의 '목록(list)'으로 나눈 뒤, 검색 시 가장 관련 있는 몇 개의 목록만 탐색하는 방식입니다.
-   **특징**: 인덱스 생성 속도가 빠르고 메모리를 적게 사용합니다.
-   **생성**:
    ```sql
    CREATE INDEX ON items USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100); -- 데이터 크기에 맞춰 lists 개수 조정
    ```

### 2. HNSW (Hierarchical Navigable Small World) 인덱스

-   **원리**: 벡터들을 여러 계층의 그래프로 연결하여, 멀리 있는 노드를 건너뛰며 효율적으로 탐색하는 방식입니다.
-   **특징**: IVFFlat보다 검색 성능(속도-재현율)이 뛰어나며, 최근 가장 널리 사용되는 벡터 인덱스입니다. 단, 인덱스 생성 시간이 더 길고 메모리를 더 많이 사용합니다.
-   **생성**:
    ```sql
    CREATE INDEX ON items USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
    ```

> [!TIP] 어떤 인덱스를 선택해야 할까?
> - **HNSW**: 대부분의 실시간 검색 시나리오에서 가장 좋은 성능을 제공합니다. **일반적으로 HNSW 사용을 우선적으로 고려**하는 것이 좋습니다.
> - **IVFFlat**: 인덱스를 매우 빠르게 생성해야 하거나, 메모리 사용량에 제약이 있는 경우 고려할 수 있습니다.

---
> [[00. 포스트그레스 목차|⬆️ 목차로 돌아가기]]
> [[10. 사용자 및 권한 관리/README|⬅️ 이전: 10. 사용자 및 권한 관리]]