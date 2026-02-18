# Repository 패턴

---

## 개념

```
Repository = 데이터 접근 로직을 캡슐화하는 계층

비즈니스 로직 (Service)
      │
      │  "테넌트 데이터 줘"  (어떤 DB인지 모름)
      ▼
┌─────────────────┐
│   Repository    │  ← 인터페이스 (추상화)
│   (추상 계층)    │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
CosmosRepo  PostgresRepo  ← 구현체 (교체 가능)
```

### Why Repository?

```
1. 관심사 분리: Service는 "무엇을" 할지, Repository는 "어떻게" 할지
2. 테스트 용이: 실제 DB 없이 Mock Repository로 테스트
3. DB 교체 용이: Repository 구현체만 교체
4. 중복 제거: 같은 쿼리를 여러 서비스에서 재사용
```

---

## 구현

### 추상 인터페이스

```python
from abc import ABC, abstractmethod
from typing import Optional

class TenantRepository(ABC):
    """테넌트 데이터 접근 인터페이스"""
    
    @abstractmethod
    async def get_by_id(self, tenant_id: str) -> Optional[dict]:
        """테넌트 ID로 조회. 없으면 None"""
        ...
    
    @abstractmethod
    async def create(self, tenant: dict) -> dict:
        """테넌트 생성. 이미 존재하면 ConflictError"""
        ...
    
    @abstractmethod
    async def update(self, tenant_id: str, data: dict) -> dict:
        """테넌트 부분 업데이트"""
        ...
    
    @abstractmethod
    async def delete(self, tenant_id: str) -> None:
        """테넌트 삭제"""
        ...
    
    @abstractmethod
    async def list_all(self, filters: dict = None) -> list[dict]:
        """조건부 목록 조회"""
        ...
```

### Cosmos DB 구현체

```python
class CosmosTenantRepository(TenantRepository):
    def __init__(self, container):
        self.container = container
    
    async def get_by_id(self, tenant_id: str) -> Optional[dict]:
        try:
            return await self.container.read_item(
                item=tenant_id,
                partition_key=tenant_id
            )
        except CosmosResourceNotFoundError:
            return None
    
    async def create(self, tenant: dict) -> dict:
        try:
            return await self.container.create_item(body=tenant)
        except CosmosResourceExistsError:
            raise ConflictError(f"Tenant {tenant['id']} already exists")
    
    async def update(self, tenant_id: str, data: dict) -> dict:
        existing = await self.get_by_id(tenant_id)
        if not existing:
            raise NotFoundError(f"Tenant {tenant_id} not found")
        existing.update(data)
        return await self.container.upsert_item(body=existing)
    
    async def delete(self, tenant_id: str) -> None:
        await self.container.delete_item(
            item=tenant_id,
            partition_key=tenant_id
        )
    
    async def list_all(self, filters: dict = None) -> list[dict]:
        query = "SELECT * FROM c"
        params = []
        if filters:
            conditions = []
            for key, value in filters.items():
                conditions.append(f"c.{key} = @{key}")
                params.append({"name": f"@{key}", "value": value})
            query += " WHERE " + " AND ".join(conditions)
        
        return [item async for item in 
                self.container.query_items(query, parameters=params)]
```

### 테스트용 Mock 구현체

```python
class InMemoryTenantRepository(TenantRepository):
    """테스트용 인메모리 구현"""
    
    def __init__(self):
        self._store: dict[str, dict] = {}
    
    async def get_by_id(self, tenant_id: str) -> Optional[dict]:
        return self._store.get(tenant_id)
    
    async def create(self, tenant: dict) -> dict:
        if tenant["id"] in self._store:
            raise ConflictError(f"Already exists")
        self._store[tenant["id"]] = tenant
        return tenant
    
    async def list_all(self, filters=None) -> list[dict]:
        items = list(self._store.values())
        if filters:
            for key, value in filters.items():
                items = [i for i in items if i.get(key) == value]
        return items
    # ...
```

---

## Service에서 사용

```python
class TenantService:
    def __init__(self, repository: TenantRepository):  # DIP!
        self.repository = repository
    
    async def register(self, data: TenantCreateRequest) -> dict:
        # 비즈니스 로직만! DB접근 방법 모름
        existing = await self.repository.get_by_id(data.tenant_id)
        if existing:
            raise ConflictError("이미 등록된 테넌트")
        
        tenant = {
            "id": data.tenant_id,
            "name": data.name,
            "is_active": True,
            "created_at": datetime.utcnow().isoformat()
        }
        return await self.repository.create(tenant)

# FastAPI 의존성 주입
def get_tenant_service():
    container = get_cosmos_container("tenants")
    repo = CosmosTenantRepository(container)
    return TenantService(repository=repo)

@app.post("/api/v1/tenants")
async def register(
    data: TenantCreateRequest,
    service: TenantService = Depends(get_tenant_service)
):
    return await service.register(data)
```

---

## 면접 핵심 포인트

```
Q: Repository 패턴의 장점?
A: 1. 비즈니스 로직과 데이터 접근 분리 (SRP)
   2. DB 교체 용이 (구현체만 교체, DIP)
   3. 테스트 용이 (Mock Repository로 단위 테스트)
   4. 쿼리 로직 재사용

Q: Repository와 DAO의 차이?
A: DAO: DB 테이블 1:1 매핑, CRUD 중심 (데이터 관점)
   Repository: 도메인 객체 중심, 비즈니스 의미 부여 (도메인 관점)
   실무에서는 혼용되는 경우가 많음.

Q: 항상 Repository를 써야 하나?
A: 작은 프로젝트나 단순 CRUD에서는 과도할 수 있음.
   DB 교체 가능성, 테스트 필요성, 팀 규모를 고려하여 결정.
```
