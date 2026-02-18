# Azure Cosmos DB 완전 정리

**글로벌 분산 NoSQL 데이터베이스** — 밀리초 응답, 자동 스케일링, 다중 리전 복제를 기본 제공합니다.

---

## Cosmos DB란?

```
RDB (SQL Server)              vs    Cosmos DB (NoSQL)
├── 테이블 + 관계형 스키마            ├── JSON 문서 (스키마 자유)
├── JOIN이 강력                      ├── JOIN 대신 비정규화
├── 수직 확장 (scale-up)             ├── 수평 확장 (scale-out)
└── 단일 리전                        └── 글로벌 분산

Log-Doctor에서: 테넌트/에이전트/라이선스 데이터를 Cosmos DB에 저장
```

---

## API 모델 비교

| API | 설명 | 데이터 모델 | 사용 시나리오 |
|-----|------|-----------|-------------|
| **NoSQL** | 기본, 가장 많이 사용 | JSON 문서 | 범용 |
| MongoDB | MongoDB 호환 | BSON 문서 | MongoDB 마이그레이션 |
| PostgreSQL | PostgreSQL 호환 | 관계형 + JSON | RDB 경험 활용 |
| Cassandra | Cassandra 호환 | 와이드 컬럼 | 대규모 쓰기 |
| Gremlin | 그래프 DB | 노드 + 엣지 | 소셜 네트워크, 추천 |
| Table | Azure Table Storage 호환 | 키-값 | 단순 키-값 |

> Log-Doctor는 **NoSQL API** 사용

---

## 핵심 개념

### 계층 구조

```
Cosmos DB Account (logdoctordb)
├── Database (log-doctor)
│   ├── Container (tenants)        ← 파티션 키: /tenant_id
│   │   ├── Item {tenant_id: "abc", is_active: true, ...}
│   │   └── Item {tenant_id: "xyz", is_active: false, ...}
│   │
│   ├── Container (agents)         ← 파티션 키: /tenant_id
│   │   └── Item {tenant_id: "abc", agent_id: "agent-1", ...}
│   │
│   └── Container (licenses)       ← 파티션 키: /tenant_id
│       └── Item {tenant_id: "abc", plan: "premium", ...}
```

- **Account**: 최상위 리소스 (URL: `logdoctordb.documents.azure.com`)
- **Database**: 컨테이너의 논리적 그룹
- **Container**: 실제 데이터 저장소 (SQL의 테이블에 해당)
- **Item**: JSON 문서 하나 (SQL의 행에 해당)

### 파티션 키 (가장 중요!)

```
Container: tenants
파티션 키: /tenant_id

Partition "tenant-abc"          Partition "tenant-xyz"
├── {id: "1", tenant_id: "abc"}    ├── {id: "2", tenant_id: "xyz"}
└── {id: "3", tenant_id: "abc"}    └── {id: "4", tenant_id: "xyz"}

같은 파티션 = 같은 물리 서버 = 빠른 쿼리
다른 파티션 = 크로스 파티션 쿼리 = 느림 + RU 비쌈
```

### 파티션 키 선택 가이드

| 기준 | Good | Bad |
|------|------|----|
| 카디널리티 | 값이 다양 (`tenant_id`) | 값이 적음 (`status`) |
| 쿼리 패턴 | WHERE 조건에 자주 사용 | 거의 안 쓰는 필드 |
| 분포 | 데이터가 고르게 분산 | 핫 파티션 (한 값에 집중) |
| 크기 | 파티션당 < 20GB | 초과 가능성 있는 키 |

> Log-Doctor: **`/tenant_id`** — 모든 쿼리가 테넌트 단위이므로 최적

---

## RU (Request Unit) — 과금 단위

```
1 RU = 1KB 문서를 id로 읽는 비용

읽기 (point read):     1 RU
쓰기:                  ~5 RU
쿼리 (인덱스 사용):    ~3 RU
쿼리 (전체 스캔):      ~50+ RU   ← 피해야 함
크로스 파티션 쿼리:     ~10+ RU   ← 파티션 키 WHERE 필수
```

### 과금 모델

| 모델 | RU 설정 | 비용 | 사용 시나리오 |
|------|---------|------|-------------|
| **프로비저닝** | 고정 RU (400~무제한) | 예측 가능 | 안정적 트래픽 |
| **자동 스케일링** | 최대 RU 설정, 자동 조절 | 유연 | 변동 트래픽 |
| **서버리스** | 사용한 만큼 | 최저 | 개발/소규모 |

> Log-Doctor 개발: **서버리스** (비용 최소화), 프로덕션: **자동 스케일링**

---

## Python SDK 사용법

### 클라이언트 초기화

```python
from azure.cosmos import CosmosClient, PartitionKey

# 방법 1: 연결 문자열 (개발용)
client = CosmosClient.from_connection_string(COSMOS_CONNECTION_STRING)

# 방법 2: Managed Identity (프로덕션 권장)
from azure.identity import DefaultAzureCredential
credential = DefaultAzureCredential()
client = CosmosClient(url=COSMOS_ENDPOINT, credential=credential)

# 컨테이너 참조
database = client.get_database_client("log-doctor")
container = database.get_container_client("tenants")
```

### CRUD 작업

```python
# CREATE
new_tenant = {
    "id": "tenant-abc",          # id는 필수 (파티션 내 고유)
    "tenant_id": "tenant-abc",   # 파티션 키
    "is_active": False,
    "created_at": "2024-01-15T00:00:00Z"
}
container.create_item(body=new_tenant)

# READ (Point Read — 가장 빠르고 저렴, 1 RU)
item = container.read_item(
    item="tenant-abc",              # id
    partition_key="tenant-abc"      # 파티션 키 필수!
)

# UPDATE (Upsert — 없으면 생성, 있으면 덮어쓰기)
item["is_active"] = True
container.upsert_item(body=item)

# DELETE
container.delete_item(
    item="tenant-abc",
    partition_key="tenant-abc"
)
```

### 쿼리

```python
# 파티션 내 쿼리 (빠름, 저렴)
query = "SELECT * FROM c WHERE c.tenant_id = @tenant_id AND c.is_active = true"
items = container.query_items(
    query=query,
    parameters=[{"name": "@tenant_id", "value": "tenant-abc"}],
    partition_key="tenant-abc"   # 파티션 키 지정!
)
for item in items:
    print(item)

# ⚠️ 크로스 파티션 쿼리 (느림, 비쌈 — 피해야 함)
query = "SELECT * FROM c WHERE c.is_active = true"
items = container.query_items(
    query=query,
    enable_cross_partition_query=True  # 모든 파티션 스캔
)
```

---

## Log-Doctor 실제 코드 분석

### CosmosDB 싱글톤 클라이언트

```python
# app/infra/db/cosmos.py
class CosmosDB:
    _client = None
    _database = None

    @classmethod
    def get_client(cls):
        if cls._client is None:
            credential = DefaultAzureCredential()
            cls._client = CosmosClient(
                url=settings.COSMOS_ENDPOINT,
                credential=credential
            )
            cls._database = cls._client.get_database_client(
                settings.COSMOS_DATABASE
            )
        return cls._database

# 사용법
container = CosmosDB.get_client().get_container_client("tenants")
```

### Repository 패턴

```python
# app/domains/tenant/repository.py
class CosmosTenantRepository(TenantRepository):
    def __init__(self):
        self.container = get_container("tenants")

    async def get_by_id(self, tenant_id: str) -> dict | None:
        try:
            # Point Read = 1 RU, 가장 빠름
            return self.container.read_item(
                item=tenant_id,
                partition_key=tenant_id
            )
        except CosmosResourceNotFoundError:
            return None
```

---

## Bicep 배포

```bicep
param location string = resourceGroup().location
param accountName string = 'logdoctordb'

resource cosmosAccount 'Microsoft.DocumentDB/databaseAccounts@2023-04-15' = {
  name: accountName
  location: location
  kind: 'GlobalDocumentDB'
  properties: {
    databaseAccountOfferType: 'Standard'
    consistencyPolicy: {
      defaultConsistencyLevel: 'Session'  // 가장 많이 사용
    }
    locations: [
      {
        locationName: location
        failoverPriority: 0
      }
    ]
    capabilities: [
      { name: 'EnableServerless' }  // 서버리스 모드
    ]
  }
}

resource database 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases@2023-04-15' = {
  parent: cosmosAccount
  name: 'log-doctor'
  properties: {
    resource: { id: 'log-doctor' }
  }
}

resource tenantsContainer 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers@2023-04-15' = {
  parent: database
  name: 'tenants'
  properties: {
    resource: {
      id: 'tenants'
      partitionKey: {
        paths: [ '/tenant_id' ]
        kind: 'Hash'
      }
      indexingPolicy: {
        indexingMode: 'consistent'
        automatic: true
      }
    }
  }
}
```

---

## 일관성 수준 (Consistency Level)

```
Strong ←──────────────────────────── Eventual
강한 일관성                           최종 일관성
느림, 비쌈                           빠름, 저렴

Strong → Bounded → Session → Consistent Prefix → Eventual
         Staleness    ⭐ 기본
```

| 수준 | 동작 | RU 비용 | 사용 시나리오 |
|------|------|---------|-------------|
| **Strong** | 항상 최신 읽기 | 2x | 금융 |
| **Session** | 같은 세션 내 일관성 | 1x | **일반 앱 (Log-Doctor)** |
| **Eventual** | 언젠가 일관성 | 낮음 | 로그, 분석 |

> Log-Doctor: **Session** — 같은 사용자의 요청은 항상 최신 데이터 보장

---

## 트러블슈팅

### 1. 429 Too Many Requests

```
RU 초과! 요청이 프로비저닝된 RU보다 많음

해결:
1. 자동 스케일링으로 전환
2. 인덱싱 최적화 (불필요한 필드 제외)
3. 크로스 파티션 쿼리 줄이기
4. SDK의 자동 retry에 의존 (기본 9회 retry)
```

### 2. 파티션 키 변경 불가

```
# ⚠️ 컨테이너 생성 후 파티션 키 변경 불가!
# 잘못 설정하면: 새 컨테이너 생성 → 데이터 마이그레이션

# 처음부터 올바른 파티션 키를 선택하는 것이 매우 중요
```

### 3. CosmosResourceNotFoundError

```python
# id와 partition_key가 모두 맞아야 Point Read 성공
try:
    item = container.read_item(
        item="wrong-id",
        partition_key="tenant-abc"  # 파티션 키도 필수!
    )
except CosmosResourceNotFoundError:
    # 아이템이 없으면 여기로
    print("Not found")
```
