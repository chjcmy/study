# 계층 아키텍처 (Layered Architecture)

---

## 구조

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │  API 엔드포인트 (FastAPI Router)
│         (프레젠테이션 계층)               │  요청/응답 처리, 유효성 검증
├─────────────────────────────────────────┤
│         Service / Business Layer        │  비즈니스 로직
│         (서비스 / 비즈니스 계층)          │  도메인 규칙, 오케스트레이션
├─────────────────────────────────────────┤
│         Repository / Data Layer         │  데이터 접근
│         (데이터 접근 계층)               │  DB CRUD, 외부 API 호출
├─────────────────────────────────────────┤
│         Infrastructure Layer            │  인프라
│         (인프라 계층)                    │  DB 연결, 캐시, 메시징
└─────────────────────────────────────────┘

핵심 규칙: 상위 계층 → 하위 계층만 호출 (역방향 ❌)
```

---

## 각 계층의 역할

### Presentation Layer (Router)

```python
# 역할: HTTP 요청/응답 처리, 입력 검증, 라우팅
# 하지 말 것: 비즈니스 로직, DB 직접 접근

@app.post("/api/v1/tenants", status_code=201)
async def register_tenant(
    request: TenantCreateRequest,  # Pydantic 입력 검증
    service: TenantService = Depends(get_service)
):
    # 비즈니스 로직은 Service에 위임!
    tenant = await service.register(request)
    return TenantResponse.from_domain(tenant)
```

### Service Layer (Business Logic)

```python
# 역할: 비즈니스 규칙, 여러 Repository 조합, 트랜잭션 관리
# 하지 말 것: HTTP 관련 로직, DB 쿼리 직접 작성

class TenantService:
    def __init__(
        self,
        tenant_repo: TenantRepository,
        license_repo: LicenseRepository,
        notification: NotificationService
    ):
        self.tenant_repo = tenant_repo
        self.license_repo = license_repo
        self.notification = notification
    
    async def register(self, request: TenantCreateRequest) -> Tenant:
        # 비즈니스 규칙 1: 중복 확인
        existing = await self.tenant_repo.get_by_id(request.tenant_id)
        if existing:
            raise ConflictError("이미 등록된 테넌트")
        
        # 비즈니스 규칙 2: 라이선스 확인
        license = await self.license_repo.validate(request.license_key)
        if not license.is_valid:
            raise InvalidLicenseError("유효하지 않은 라이선스")
        
        # 생성
        tenant = Tenant(
            id=request.tenant_id,
            name=request.name,
            license=license
        )
        await self.tenant_repo.create(tenant.to_dict())
        
        # 후처리
        await self.notification.send("새 테넌트 등록", tenant)
        return tenant
```

### Repository Layer (Data Access)

```python
# 역할: DB CRUD, 쿼리 로직, 외부 API 호출
# 하지 말 것: 비즈니스 규칙

class CosmosTenantRepository(TenantRepository):
    async def get_by_id(self, tenant_id: str) -> Optional[dict]:
        try:
            return await self.container.read_item(
                item=tenant_id,
                partition_key=tenant_id
            )
        except CosmosResourceNotFoundError:
            return None
```

---

## Log-Doctor 프로젝트 구조

```
app/
├── routers/          ← Presentation Layer
│   ├── tenant.py     │  @app.get("/api/v1/tenants/me")
│   ├── agent.py      │  @app.post("/api/v1/agents/handshake")
│   └── subscription.py
│
├── services/         ← Service Layer
│   ├── tenant_service.py
│   ├── agent_service.py
│   └── obo_service.py
│
├── repositories/     ← Repository Layer
│   ├── base.py       │  BaseRepository (ABC)
│   ├── tenant_repo.py
│   └── agent_repo.py
│
├── models/           ← Domain / DTO
│   ├── tenant.py     │  Pydantic 모델
│   └── agent.py
│
├── core/             ← Infrastructure
│   ├── config.py
│   ├── database.py
│   └── security.py
│
└── main.py           ← 앱 진입점
```

---

## 면접 핵심 포인트

```
Q: 왜 계층을 나누는가?
A: 1. 관심사 분리 → 각 계층이 하나의 역할만
   2. 테스트 용이 → 계층별 독립 테스트
   3. 유지보수성 → 변경 영향 최소화
   4. 팀 협업 → 계층별 병렬 개발

Q: 계층 아키텍처의 단점?
A: 1. Boilerplate 코드 증가 (단순 CRUD도 3계층 통과)
   2. 성능 오버헤드 (계층 간 데이터 변환)
   3. 과도한 추상화 가능성

Q: Clean Architecture와의 차이?
A: Layered: 상위→하위 단방향 의존
   Clean: 의존성이 바깥→안쪽 (Domain이 중심)
   Clean이 더 엄격한 의존성 규칙 적용
```
