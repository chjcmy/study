# DI (Dependency Injection) & Factory 패턴

---

## DI (의존성 주입)

### 핵심 개념

```
DI = 의존하는 객체를 외부에서 주입받는 것

❌ 내부에서 직접 생성 (강한 결합)
class UserService:
    def __init__(self):
        self.repo = MySQLRepository()  # ← 직접 생성 → 교체 불가!

✅ 외부에서 주입 (느슨한 결합)
class UserService:
    def __init__(self, repo: UserRepository):
        self.repo = repo  # ← 뭐가 들어올지 모름 (인터페이스 의존)

# 사용:
repo = PostgresRepository()  # 교체 가능!
service = UserService(repo)
```

### 주입 방식 3가지

1.  **Constructor Injection (생성자 주입) ⭐**
    *   가장 권장됨. 필수 의존성 명시.
    *   `__init__`에서 받음.
2.  **Setter Injection (수정자 주입)**
    *   선택적 의존성. 객체 생성 후 `set_repo()` 등으로 주입.
    *   `service.set_repo(repo)`
3.  **Field Injection (필드 주입)**
    *   프레임워크(Spring, Java)가 리플렉션으로 주입.
    *   테스트하기 어려워 지양하는 추세.

### FastAPI의 DI (Dependency Injection)

FastAPI는 매우 강력한 DI 시스템을 내장.

```python
from fastapi import Depends

# 1. 의존성 정의 (접속 정보 등)
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# 2. 주입 (요청 시 자동으로 db 연결 생성 및 주입)
@app.get("/users/{id}")
def get_user(id: int, db: Session = Depends(get_db)):
    return db.query(User).filter(User.id == id).first()
```

---

## Factory 패턴

### Factory Method 패턴

객체 생성을 서브클래스나 별도 메소드에 위임.
"어떤 객체를 만들지"를 런타임에 결정하거나 캡슐화.

```python
class RepositoryFactory:
    @staticmethod
    def create(db_type: str):
        if db_type == "mysql":
            return MySQLRepository()
        elif db_type == "mongo":
            return MongoRepository()
        else:
            raise ValueError("Unknown DB type")

# 사용:
repo = RepositoryFactory.create(config["DB_TYPE"])
```

### Abstract Factory 패턴

관련된 객체들의 집합(Family)을 생성하는 인터페이스 제공.

```python
class InfrastructureFactory:
    def create_user_repo(self): pass
    def create_order_repo(self): pass

# MySQL 패밀리
class MySQLFactory(InfrastructureFactory):
    def create_user_repo(self): return MySQLUserRepository(...)
    def create_order_repo(self): return MySQLOrderRepository(...)

# Mongo 패밀리
class MongoFactory(InfrastructureFactory):
    def create_user_repo(self): return MongoUserRepository(...)
    def create_order_repo(self): return MongoOrderRepository(...)

# 환경에 따라 공통 팩토리 교체
factory = MySQLFactory() if ENV == "production" else MongoFactory()
```

---

## 면접 핵심 포인트

```
Q: DI(의존성 주입)를 사용하는 이유는?
A: 객체 간 결합도를 낮추기 위해서입니다.
   결합도가 낮아지면:
   1. 유닛 테스트 시 Mock 객체로 쉽게 대체 가능 (테스트 용이성)
   2. 구현체 변경 시 코드 수정 없이 설정만 변경 (유연성)

Q: IoC (Inversion of Control)란?
A: 제어의 역전. 개발자가 직접 객체를 생성/호출하는 게 아니라,
   프레임워크(Spring, FastAPI)가 객체의 생명주기를 관리하고 필요한 곳에 주입해주는 것.
   DI는 IoC를 구현하는 대표적인 패턴입니다.

Q: Factory 패턴은 언제 쓰나?
A: 객체 생성 로직이 복잡하거나, 조건에 따라 다른 객체를 생성해야 할 때 사용합니다.
   생성 로직을 한 곳에 캡슐화하여 중복을 줄이고 유지보수성을 높입니다.
```
