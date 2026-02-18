# CORS (Cross-Origin Resource Sharing)

```
SPA (https://www.shop.com)
    │
    │ API 호출
    ▼
API 서버 (https://api.shop.com)
    │
    └─ Origin이 다르므로 CORS 필요!

브라우저가 자동으로:
1. Preflight 요청 (OPTIONS) 전송
2. 서버가 허용된 Origin 응답
3. 실제 요청 진행
```

```python
# FastAPI CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://www.shop.com"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```
