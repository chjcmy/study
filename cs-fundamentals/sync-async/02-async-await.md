# Python async/await

---

## 동기 코드 (블로킹)

```python
import requests
import time

def get_subscriptions_sync():
    start = time.time()
    
    # 하나씩 순서대로 (각 1초씩 걸린다면)
    token = get_obo_token()           # 1초 대기 ← 이 동안 아무것도 못 함
    subs = requests.get("/subs")      # 1초 대기 ← CPU가 놀고 있음
    tenant = requests.get("/tenant")  # 1초 대기
    
    print(f"총 {time.time() - start}초")  # → 3초
```

```
시간 →
│ OBO 토큰 │────대기────│ 구독 조회 │────대기────│ 테넌트 │────대기────│
0s                      1s                      2s                      3s
→ 총 3초 (순차 실행)
```

---

## 비동기 코드 (논블로킹)

```python
import httpx
import asyncio
import time

async def get_subscriptions_async():
    start = time.time()
    
    async with httpx.AsyncClient() as client:
        # 동시에 요청 (각 1초씩 걸려도)
        token_task = get_obo_token()
        subs_task = client.get("/subs")
        tenant_task = client.get("/tenant")
        
        # 3개를 동시에 실행하고, 모두 완료될 때까지 대기
        token, subs, tenant = await asyncio.gather(
            token_task, subs_task, tenant_task
        )
    
    print(f"총 {time.time() - start}초")  # → 1초!
```

```
시간 →
│ OBO 토큰 │────────────│ 완료 │
│ 구독 조회 │────────────│ 완료 │  ← 동시에!
│ 테넌트   │────────────│ 완료 │
0s                           1s
→ 총 1초 (동시 실행)  = 3배 빠름!
```

---

## 핵심 키워드 정리

### async def

```python
# 코루틴 함수 정의
async def fetch_data():
    ...

# 호출하면 코루틴 객체 반환 (실행은 await 해야!)
coro = fetch_data()       # ← 아직 실행 안 됨!
result = await fetch_data()  # ← 이제 실행!
```

### await

```python
# I/O 작업이 완료될 때까지 "양보"
# 이 동안 이벤트 루프가 다른 코루틴 실행!

async def get_tenant(tenant_id: str):
    tenant = await db.query(tenant_id)  # ← 여기서 양보
    #         ↑ DB 응답 올 때까지 다른 코루틴 실행
    return tenant
```

### asyncio.gather

```python
# 여러 코루틴을 동시에 실행하고 모든 결과를 모음
results = await asyncio.gather(
    fetch_user(),
    fetch_orders(),
    fetch_settings(),
)
# results = [user, orders, settings]

# 하나라도 실패하면? return_exceptions=True
results = await asyncio.gather(
    fetch_user(),
    fetch_orders(),
    return_exceptions=True  # 예외도 결과로 반환
)
```

### asyncio.create_task

```python
# 백그라운드에서 코루틴 실행 (fire-and-forget)
async def process_request():
    # 즉시 시작하고 제어권 반환
    task = asyncio.create_task(send_notification())
    
    # 다른 작업 계속 진행
    result = await main_logic()
    
    # 나중에 필요하면 결과 수집
    await task
```

---

## httpx vs requests

| 항목 | requests | httpx |
|------|----------|-------|
| 동기/비동기 | 동기만 | **둘 다** |
| 연결 풀 | 세션 수동 관리 | 자동 관리 |
| HTTP/2 | ❌ | ✅ |
| async 지원 | ❌ | ✅ (`AsyncClient`) |
| **FastAPI** | ❌ 사용 금지 | ✅ **권장** |

```python
# ❌ requests (동기 → async 함수에서 이벤트 루프 블로킹!)
import requests
response = requests.get("https://api.example.com")

# ✅ httpx (비동기)
import httpx
async with httpx.AsyncClient() as client:
    response = await client.get("https://api.example.com")
```

---

## 면접 핵심 포인트

```
Q: await가 하는 일?
A: 코루틴 실행을 "일시 중단"하고 이벤트 루프에 제어권 반환.
   I/O 완료 시 중단된 위치에서 재개.
   → 대기 시간에 다른 코루틴 실행 가능.

Q: asyncio.gather vs create_task?
A: gather: 여러 코루틴을 동시 실행하고 모든 결과를 모음.
   create_task: 백그라운드 실행, 즉시 반환 (결과는 나중에).
   gather는 "모두 필요", create_task는 "fire-and-forget".

Q: async 함수에서 requests를 쓰면?
A: 이벤트 루프가 블로킹됨! 모든 동시 요청이 멈춤.
   → httpx.AsyncClient를 사용해야 함.
```
