# AOP (Aspect-Oriented Programming)

---

## 개념

```
AOP = 횡단 관심사(Cross-cutting Concern)를 분리하는 프로그래밍 패러다임

횡단 관심사 = 여러 모듈에 공통으로 필요하지만 핵심 로직은 아닌 것

비즈니스 로직:         횡단 관심사:
├── 테넌트 관리    ←── 로깅
├── 에이전트 관리  ←── 인증/인가
├── 구독 관리      ←── 에러 처리
└── 리포트 생성    ←── 성능 측정

→ 모든 곳에 로깅/인증 코드를 넣으면 중복!
→ AOP로 분리하면 비즈니스 로직이 깔끔해짐
```

---

## AOP 핵심 용어

```
Aspect:      횡단 관심사를 모듈화한 것 (로깅 Aspect, 인증 Aspect)
Advice:      실제 실행되는 코드 (언제 + 무엇을)
Join Point:  Advice가 적용될 수 있는 지점 (메서드 호출, 예외 발생)
Pointcut:    실제 Advice가 적용될 Join Point 선택
Weaving:     Aspect를 대상 코드에 결합하는 과정

Advice 유형:
├── Before:   메서드 실행 전 (인증 확인)
├── After:    메서드 실행 후 (리소스 정리)
├── Around:   메서드 실행 전후 감싸기 (성능 측정) ⭐
└── AfterThrowing: 예외 발생 시 (에러 로깅)
```

---

## Python에서의 AOP — 데코레이터

```python
# 데코레이터 = Python의 AOP 구현 방식

import functools
import time
import logging

# 1. 로깅 데코레이터 (Around Advice)
def log_execution(func):
    @functools.wraps(func)
    async def wrapper(*args, **kwargs):
        logger = logging.getLogger(func.__module__)
        logger.info(f"▶ {func.__name__} 시작 | args={args[1:]}, kwargs={kwargs}")
        try:
            result = await func(*args, **kwargs)
            logger.info(f"✅ {func.__name__} 성공")
            return result
        except Exception as e:
            logger.error(f"❌ {func.__name__} 실패: {e}")
            raise
    return wrapper

# 2. 성능 측정 데코레이터
def measure_time(func):
    @functools.wraps(func)
    async def wrapper(*args, **kwargs):
        start = time.time()
        result = await func(*args, **kwargs)
        duration = time.time() - start
        logger.info(f"⏱ {func.__name__}: {duration:.3f}초")
        if duration > 5.0:
            logger.warning(f"⚠️ 느린 호출: {func.__name__} ({duration:.1f}초)")
        return result
    return wrapper

# 3. 재시도 데코레이터
def retry(max_retries=3, delay=1.0):
    def decorator(func):
        @functools.wraps(func)
        async def wrapper(*args, **kwargs):
            for attempt in range(max_retries):
                try:
                    return await func(*args, **kwargs)
                except Exception as e:
                    if attempt == max_retries - 1:
                        raise
                    logger.warning(
                        f"🔄 {func.__name__} 재시도 "
                        f"({attempt+1}/{max_retries}): {e}"
                    )
                    await asyncio.sleep(delay * (2 ** attempt))
        return wrapper
    return decorator

# 적용
class TenantService:
    @log_execution        # AOP: 로깅
    @measure_time         # AOP: 성능 측정
    @retry(max_retries=3) # AOP: 재시도
    async def register(self, data):
        # 순수 비즈니스 로직만!
        tenant = await self.repo.create(data)
        return tenant
```

---

## FastAPI에서의 AOP

### 미들웨어 (전체 요청에 적용)

```python
# 모든 요청에 자동 적용 — 로깅, 인증, 에러 처리

@app.middleware("http")
async def logging_middleware(request: Request, call_next):
    request_id = str(uuid.uuid4())
    start = time.time()
    
    response = await call_next(request)
    
    duration = time.time() - start
    logger.info(
        f"[{request_id}] {request.method} {request.url.path} "
        f"→ {response.status_code} ({duration:.3f}s)"
    )
    response.headers["X-Request-ID"] = request_id
    return response
```

### 의존성으로 AOP (특정 엔드포인트에 적용)

```python
# 인증 — 특정 엔드포인트에만
async def require_auth(
    authorization: str = Header(...)
) -> TokenPayload:
    token = authorization.replace("Bearer ", "")
    try:
        payload = jwt.decode(token, key=PUBLIC_KEY)
        return TokenPayload(**payload)
    except JWTError:
        raise HTTPException(401, "Invalid token")

@app.get("/api/v1/tenants/me")
async def get_tenant(
    user: TokenPayload = Depends(require_auth)  # AOP: 인증
):
    return await service.get_tenant(user.tenant_id)
```

### Exception Handler (에러 처리 AOP)

```python
@app.exception_handler(NotFoundError)
async def not_found_handler(request: Request, exc: NotFoundError):
    return JSONResponse(status_code=404, content={"detail": str(exc)})

@app.exception_handler(ConflictError)
async def conflict_handler(request: Request, exc: ConflictError):
    return JSONResponse(status_code=409, content={"detail": str(exc)})

# 모든 예외를 잡는 핸들러
@app.exception_handler(Exception)
async def global_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled: {exc}", exc_info=True)
    return JSONResponse(status_code=500, content={"detail": "Internal Error"})
```

---

## AOP vs 일반 코드

```python
# ❌ AOP 없이 (횡단 관심사가 비즈니스 로직에 산재)
async def register_tenant(data):
    logger.info("register 시작")                  # 로깅
    start = time.time()                            # 성능 측정
    try:
        token = verify_token(request.headers)      # 인증
        # ... 비즈니스 로직 ...
        logger.info(f"완료: {time.time()-start}s") # 성능 측정
    except Exception as e:
        logger.error(f"에러: {e}")                 # 에러 로깅
        raise

# ✅ AOP 적용 (깔끔한 비즈니스 로직)
@log_execution    # 로깅 분리
@measure_time     # 성능 분리
async def register_tenant(data):
    # 순수 비즈니스 로직만!
    tenant = await repo.create(data)
    return tenant
```

---

## 면접 핵심 포인트

```
Q: AOP란 무엇이고 왜 사용하나?
A: 로깅/인증/에러처리 같은 횡단 관심사를 분리하는 패러다임.
   비즈니스 로직의 가독성 향상, 코드 중복 제거, 유지보수 용이.

Q: Python에서 AOP를 어떻게 구현?
A: 1. 데코레이터 (함수/메서드 단위)
   2. 미들웨어 (요청 단위, FastAPI)
   3. 의존성 주입 (Depends, 선택적 적용)
   4. Exception Handler (에러 처리)

Q: AOP의 단점?
A: 1. 디버깅 어려움 (호출 흐름이 보이지 않음)
   2. 과도한 사용 시 코드 추적 어려움
   3. 성능 오버헤드 (데코레이터 스택)
```
