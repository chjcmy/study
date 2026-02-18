# SOLID 원칙

---

## 개요

```
SOLID = 객체지향 설계의 5가지 핵심 원칙 (Robert C. Martin)

S — Single Responsibility Principle (단일 책임)
O — Open/Closed Principle (개방/폐쇄)
L — Liskov Substitution Principle (리스코프 치환)
I — Interface Segregation Principle (인터페이스 분리)
D — Dependency Inversion Principle (의존성 역전)

→ 유지보수성, 확장성, 테스트 용이성 향상
```

---

## S — 단일 책임 원칙 (SRP)

```
"클래스는 변경의 이유가 하나만 있어야 한다"
```

```python
# ❌ SRP 위반: TenantService가 너무 많은 책임
class TenantService:
    def register_tenant(self, data): ...      # 비즈니스 로직
    def send_email(self, tenant): ...         # 알림 로직
    def validate_license(self, license): ...  # 검증 로직
    def save_to_db(self, tenant): ...         # 데이터 접근
    def generate_report(self, tenant): ...    # 리포트 생성

# ✅ SRP 준수: 책임별 분리
class TenantService:           # 비즈니스 로직만
    def register_tenant(self, data): ...

class NotificationService:     # 알림만
    def send_email(self, tenant): ...

class LicenseValidator:        # 검증만
    def validate(self, license): ...

class TenantRepository:        # DB 접근만
    def save(self, tenant): ...
```

### 언제 분리할까?

```
질문: "이 클래스를 변경해야 하는 이유가 몇 개인가?"

이유 1: 비즈니스 로직 변경 → TenantService
이유 2: DB 스키마 변경 → TenantRepository
이유 3: 알림 채널 추가 → NotificationService

→ 변경 이유마다 클래스를 분리
```

---

## O — 개방/폐쇄 원칙 (OCP)

```
"확장에는 열려있고, 수정에는 닫혀있어야 한다"
= 기존 코드를 수정하지 않고 새 기능을 추가할 수 있어야 함
```

```python
# ❌ OCP 위반: 새 리소스 타입 추가 시 기존 코드 수정 필요
class ResourceMonitor:
    def check(self, resource_type):
        if resource_type == "vm":
            return self._check_vm()
        elif resource_type == "storage":
            return self._check_storage()
        elif resource_type == "network":   # ← 추가할 때마다 수정!
            return self._check_network()

# ✅ OCP 준수: 추상화로 확장
from abc import ABC, abstractmethod

class ResourceChecker(ABC):    # 인터페이스
    @abstractmethod
    def check(self) -> dict: ...

class VMChecker(ResourceChecker):
    def check(self): return {"type": "vm", "status": "ok"}

class StorageChecker(ResourceChecker):
    def check(self): return {"type": "storage", "status": "ok"}

# 새 리소스? → 기존 코드 수정 없이 클래스만 추가!
class NetworkChecker(ResourceChecker):
    def check(self): return {"type": "network", "status": "ok"}

class ResourceMonitor:
    def __init__(self, checkers: list[ResourceChecker]):
        self.checkers = checkers
    
    def check_all(self):  # 이 코드는 변경 불필요!
        return [c.check() for c in self.checkers]
```

---

## L — 리스코프 치환 원칙 (LSP)

```
"자식 클래스는 부모 클래스를 대체할 수 있어야 한다"
= 부모 타입을 사용하는 곳에 자식을 넣어도 동작이 올바라야 함
```

```python
# ❌ LSP 위반
class Bird:
    def fly(self): return "날기"

class Penguin(Bird):
    def fly(self): raise Exception("펭귄은 못 날아!")  # 부모 계약 위반!

# ✅ LSP 준수: 추상화 재설계
class Bird:
    def move(self): ...  # 모든 새가 할 수 있는 것

class FlyingBird(Bird):
    def fly(self): return "날기"

class Penguin(Bird):
    def move(self): return "수영"  # 부모 계약 준수
```

```python
# Log-Doctor 예시
class BaseRepository(ABC):
    @abstractmethod
    async def get(self, id: str) -> dict | None: ...
    
    @abstractmethod
    async def save(self, data: dict) -> None: ...

class CosmosRepository(BaseRepository):
    async def get(self, id: str) -> dict | None:
        return await self.container.read_item(id)  # 계약 준수
    
    async def save(self, data: dict) -> None:
        await self.container.upsert_item(data)     # 계약 준수

# 나중에 PostgresRepository로 교체해도 동작 보장!
```

---

## I — 인터페이스 분리 원칙 (ISP)

```
"사용하지 않는 메서드에 의존하지 않아야 한다"
= 큰 인터페이스보다 작고 구체적인 인터페이스가 좋다
```

```python
# ❌ ISP 위반: 하나의 거대한 인터페이스
class IRepository(ABC):
    @abstractmethod
    def create(self, data): ...
    @abstractmethod
    def read(self, id): ...
    @abstractmethod
    def update(self, id, data): ...
    @abstractmethod
    def delete(self, id): ...
    @abstractmethod
    def export_csv(self, query): ...     # 불필요한 메서드
    @abstractmethod
    def send_notification(self, msg): ... # 관계없는 책임

# ✅ ISP 준수: 역할별 분리
class Readable(ABC):
    @abstractmethod
    def read(self, id): ...

class Writable(ABC):
    @abstractmethod
    def create(self, data): ...
    @abstractmethod
    def update(self, id, data): ...

class Deletable(ABC):
    @abstractmethod
    def delete(self, id): ...

# 필요한 인터페이스만 구현
class TenantRepository(Readable, Writable):  # CRUD 중 일부만
    def read(self, id): ...
    def create(self, data): ...
    def update(self, id, data): ...
```

---

## D — 의존성 역전 원칙 (DIP)

```
"상위 모듈이 하위 모듈에 의존하면 안 된다. 
 둘 다 추상화에 의존해야 한다."
```

```python
# ❌ DIP 위반: 상위 모듈이 구체적 구현에 의존
class TenantService:
    def __init__(self):
        self.db = CosmosDB()  # ← 구체 클래스에 직접 의존!
        
    def get_tenant(self, id):
        return self.db.query(id)

# 문제: DB를 PostgreSQL로 바꾸려면 TenantService 코드 수정 필요!

# ✅ DIP 준수: 추상화에 의존
class TenantService:
    def __init__(self, repository: BaseRepository):  # ← 추상화에 의존!
        self.repository = repository
    
    def get_tenant(self, id):
        return self.repository.get(id)

# 주입 시점에 구현체 선택 (DI)
service = TenantService(repository=CosmosRepository())  # 또는
service = TenantService(repository=PostgresRepository()) # 교체 쉬움!
```

### 의존성 방향

```
❌ 변경 전 (구체에 의존):
  TenantService → CosmosDB (상위 → 하위)

✅ 변경 후 (추상에 의존):
  TenantService → BaseRepository ← CosmosDB
                    (추상화)         (하위가 추상에 의존)
  → 의존성의 방향이 역전됨!
```

---

## 면접 핵심 포인트

```
Q: SOLID 중 가장 중요한 원칙?
A: SRP와 DIP. SRP로 책임을 분리하고, DIP로 추상화에 의존하면
   대부분의 다른 원칙도 자연스럽게 충족됨.

Q: SOLID을 과도하게 적용하면?
A: Over-engineering. 단순한 코드가 불필요하게 복잡해짐.
   클래스 폭발 (class explosion).
   → 실무에서는 "변경이 예상되는 부분"에만 적용.

Q: DIP와 DI의 차이?
A: DIP = 원칙 (추상화에 의존하라)
   DI  = 구현 기법 (외부에서 의존성을 주입)
   DIP를 실현하는 방법 중 하나가 DI
```
