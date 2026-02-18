# FastAPI 비동기 적용

---

## FastAPI의 async/def 규칙

```python
# ⭐ 핵심 규칙: I/O가 있는가? → async def + await

# ✅ 비동기 I/O가 있을 때 → async def
@app.get("/api/v1/subscriptions")
async def get_subscriptions():
    token = await obo_service.get_token()         # 비동기 I/O
    subs = await arm_client.list_subscriptions()   # 비동기 I/O
    return subs

# ✅ I/O 없이 CPU 작업만 → def (스레드 풀에서 자동 실행)
@app.get("/api/v1/health")
def health_check():
    return {"status": "healthy"}  # CPU만, I/O 없음

# ❌ 위험! async def 안에서 동기 I/O
@app.get("/api/v1/bad")
async def bad_endpoint():
    data = requests.get("https://...")   # 이벤트 루프 블로킹!
    return data.json()

# ✅ 해결: sync 작업을 스레드 풀에 위임
@app.get("/api/v1/good")
async def good_endpoint():
    data = await asyncio.to_thread(
        requests.get, "https://..."
    )
    return data.json()
```

### FastAPI의 동작 방식

```
요청 수신 시:

async def 핸들러:
  → 이벤트 루프에서 직접 실행
  → await에서 양보, 다른 요청 처리

def 핸들러 (일반 함수):
  → 스레드 풀(threadpool)에서 실행
  → 이벤트 루프 블로킹 없음
  → 동기 라이브러리 안전하게 사용 가능
```

---

## 의존성 주입과 async

```python
# 비동기 의존성 (DB 연결 등)
async def get_db():
    db = await create_connection()
    try:
        yield db
    finally:
        await db.close()

# 동기 의존성 (설정 로드 등)
def get_settings():
    return Settings()  # 스레드 풀에서 실행

# 엔드포인트에서 사용
@app.get("/api/v1/tenants/me")
async def get_my_tenant(
    db = Depends(get_db),            # async 의존성 → await
    settings = Depends(get_settings)  # sync 의존성 → 스레드 풀
):
    return await db.query("SELECT ...")
```

---

## 미들웨어와 async

```python
# 비동기 미들웨어 (요청/응답 전후 처리)
@app.middleware("http")
async def add_request_id(request: Request, call_next):
    request_id = str(uuid.uuid4())
    
    # 요청 처리 타이밍 측정
    start = time.time()
    response = await call_next(request)
    duration = time.time() - start
    
    response.headers["X-Request-ID"] = request_id
    response.headers["X-Process-Time"] = str(duration)
    logger.info(
        f"{request.method} {request.url.path} "
        f"→ {response.status_code} ({duration:.3f}s)"
    )
    return response
```

---

## 백그라운드 작업

```python
from fastapi import BackgroundTasks

# 응답 후 백그라운드에서 실행
@app.post("/api/v1/tenants")
async def register_tenant(
    data: TenantRequest,
    background_tasks: BackgroundTasks
):
    tenant = await tenant_service.create(data)
    
    # 응답은 즉시 반환, 알림은 백그라운드에서
    background_tasks.add_task(
        send_notification,     # 함수 (sync 또는 async)
        tenant_id=tenant.id,   # 인자들
        event="registered"
    )
    
    return {"status": "created", "id": tenant.id}  # 즉시 응답!
```

---

## Lifespan (앱 시작/종료)

```python
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 🟢 앱 시작 시 실행 (startup)
    db_client = await init_cosmos_db()
    http_client = httpx.AsyncClient()
    app.state.db = db_client
    app.state.http = http_client
    print("✅ 서비스 시작")
    
    yield  # ← 앱 실행 중
    
    # 🔴 앱 종료 시 실행 (shutdown)
    await http_client.aclose()
    print("🛑 서비스 종료")

app = FastAPI(lifespan=lifespan)
```

---

## 성능 최적화 패턴

```python
# 1. 연결 재사용 (Connection Pooling)
# ❌ 매 요청마다 새 클라이언트
@app.get("/bad")
async def bad():
    async with httpx.AsyncClient() as client:  # 매번 생성/파괴
        return await client.get(url)

# ✅ 앱 전체에서 1개 클라이언트 재사용
http_client = httpx.AsyncClient()  # Lifespan에서 관리
@app.get("/good")
async def good():
    return await http_client.get(url)  # 커넥션 풀 재사용

# 2. 동시 요청 (gather)
@app.get("/api/v1/dashboard")
async def dashboard():
    tenant, subs, agents = await asyncio.gather(
        get_tenant(), get_subscriptions(), get_agents()
    )
    return {"tenant": tenant, "subs": subs, "agents": agents}

# 3. 캐싱
from functools import lru_cache

@lru_cache(maxsize=100)
def get_settings():
    return Settings()  # 한 번만 로드, 이후 캐시
```

---

## 면접 핵심 포인트

```
Q: FastAPI에서 async def vs def 선택 기준?
A: async def: 비동기 I/O(httpx, async DB)가 있을 때
   def: 동기 작업 또는 CPU 바운드 (스레드 풀 자동 실행)
   핵심: async def에서 동기 I/O 쓰면 이벤트 루프 블로킹!

Q: 왜 requests 대신 httpx?
A: requests는 동기 전용 → async 함수에서 이벤트 루프 블로킹
   httpx는 async 지원 → await로 논블로킹 I/O
   또한 HTTP/2, 연결 풀 자동 관리 지원

Q: FastAPI의 동시 처리 능력?
A: uvicorn + asyncio로 싱글 프로세스에서 수천 동시 연결.
   gunicorn --workers 4로 멀티 프로세스 활용 시 4배.
   I/O 바운드 워크로드에서 Flask 대비 5~10배 처리량.
```
