# API 엔드포인트 구조

## 🌐 전체 API 구조

### Base URL
```
http://localhost:3001/api/v1
```

### API 문서
```
http://localhost:3001/api/docs (Swagger UI)
```

## 🔐 인증 (Authentication)

### `/api/v1/auth`

| Method | Endpoint | Description | Body |
|--------|----------|-------------|------|
| POST | `/login` | 사용자 로그인 | `{ email, password }` |
| POST | `/register` | 사용자 회원가입 | `{ email, password, username }` |
| POST | `/profile` | 토큰으로 프로필 조회 | `Authorization: Bearer <token>` |

#### 예시 요청
```bash
# 로그인
curl -X POST "http://localhost:3001/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'

# 회원가입
curl -X POST "http://localhost:3001/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123", "username": "testuser"}'
```

## 👤 사용자 관리 (Users)

### `/api/v1/users`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/profile` | 사용자 프로필 조회 | ✅ |
| PATCH | `/profile` | 사용자 프로필 수정 | ✅ |
| POST | `/favorites/:recipeId` | 즐겨찾기 추가 | ✅ |
| DELETE | `/favorites/:recipeId` | 즐겨찾기 제거 | ✅ |

## 🍳 레시피 관리 (Recipes)

### `/api/v1/recipes`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/` | 새 레시피 생성 | ✅ |
| GET | `/` | 레시피 목록 조회 | ❌ |
| GET | `/popular` | 인기 레시피 조회 | ❌ |
| GET | `/recent` | 최근 레시피 조회 | ❌ |
| GET | `/by-ingredients` | 재료별 레시피 검색 | ❌ |
| GET | `/:id` | 특정 레시피 조회 | ❌ |
| PATCH | `/:id` | 레시피 수정 | ✅ |
| DELETE | `/:id` | 레시피 삭제 | ✅ |
| GET | `/:id/similar` | 유사 레시피 조회 | ❌ |
| PATCH | `/:id/rating` | 레시피 평점 수정 | ✅ |
| POST | `/reindex` | 레시피 재인덱싱 | ✅ (Admin) |

#### 쿼리 파라미터 예시
```bash
# 페이지네이션
GET /api/v1/recipes?page=1&limit=20

# 필터링
GET /api/v1/recipes?category=dessert&difficulty=easy&maxMinutes=30

# 재료별 검색
GET /api/v1/recipes/by-ingredients?ingredients=chicken,rice&exclude=nuts
```

## 🔍 검색 (Search)

### `/api/v1/search`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | 통합 검색 |
| GET | `/suggest` | 자동완성 제안 |

#### 검색 쿼리 예시
```bash
# 기본 검색
GET /api/v1/search?q=pasta&type=recipe

# 자동완성
GET /api/v1/search/suggest?q=chic
```

## 🧠 AI 기능 (AI)

### `/api/v1/ai`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/recommend` | AI 레시피 추천 |
| POST | `/generate` | AI 레시피 생성 |
| POST | `/embedding` | 텍스트 임베딩 생성 |
| GET | `/health` | AI 서비스 상태 확인 |
| GET | `/models` | 사용 가능한 모델 조회 |

#### 요청 예시
```bash
# AI 추천
curl -X POST "http://localhost:3001/api/v1/ai/recommend" \
  -H "Content-Type: application/json" \
  -d '{"preferences": ["healthy", "quick"], "allergies": ["nuts"], "ingredients": ["chicken"]}'
```

## 🤖 RAG 챗봇 (RAG)

### `/api/v1/rag`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/ask` | 일반 질문 응답 |
| POST | `/recipe/ask` | 레시피 관련 질문 |
| POST | `/conversation` | 대화형 챗봇 |
| POST | `/hybrid-search` | 하이브리드 검색 |
| POST | `/explain-recipe` | 레시피 설명 |
| POST | `/suggest-variations` | 레시피 변형 제안 |
| POST | `/cooking-tips` | 요리 팁 제공 |

## 💬 채팅 세션 (Chat)

### `/api/v1/chat`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/sessions` | 새 채팅 세션 생성 | ✅ |
| POST | `/sessions/:sessionId/messages` | 메시지 전송 | ✅ |
| GET | `/sessions/:sessionId` | 세션 조회 | ✅ |
| GET | `/sessions/:sessionId/history` | 채팅 히스토리 | ✅ |
| POST | `/sessions/:sessionId/rag-messages` | RAG 메시지 전송 | ✅ |

## 🔢 벡터 검색 (Vectors)

### `/api/v1/vectors`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | 벡터 생성 |
| POST | `/search` | 벡터 유사도 검색 |
| PUT | `/:vectorId` | 벡터 업데이트 |
| DELETE | `/:vectorId` | 벡터 삭제 |
| DELETE | `/source/:sourceType/:sourceId` | 소스별 벡터 삭제 |
| GET | `/:vectorId/metadata` | 벡터 메타데이터 조회 |
| GET | `/source/:sourceType/:sourceId` | 소스별 벡터 조회 |
| POST | `/bulk` | 벌크 벡터 처리 |
| GET | `/stats` | 벡터 통계 |

## 📊 데이터 인덱싱 (Indexing)

### `/api/v1/indexing`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/setup-indices` | Elasticsearch 인덱스 설정 | ✅ (Admin) |
| POST | `/load-ingredients` | 재료 데이터 로드 | ✅ (Admin) |
| POST | `/load-recipes` | 레시피 데이터 로드 | ✅ (Admin) |
| GET | `/status` | 인덱싱 상태 확인 | ❌ |

## 📁 데이터 로딩 (Data)

### `/api/v1/data`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/load-recipes` | CSV에서 레시피 로드 | ✅ (Admin) |
| POST | `/load-allergens` | 알레르기 데이터 로드 | ✅ (Admin) |
| POST | `/load-all` | 모든 데이터 로드 | ✅ (Admin) |

## 🏥 헬스체크 (Health)

### `/api/v1/health`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | 전체 시스템 상태 |

## 📝 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": {...},
  "message": "Success message",
  "timestamp": "2025-06-11T12:00:00.000Z"
}
```

### 오류 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": {...}
  },
  "timestamp": "2025-06-11T12:00:00.000Z"
}
```

## 🔒 인증 헤더

### JWT 토큰 사용
```bash
Authorization: Bearer <your-jwt-token>
```

## 📈 Rate Limiting

### 제한사항
- **일반 API**: 100 requests/minute
- **AI API**: 10 requests/minute
- **Data Loading**: 1 request/minute

---
*총 44개의 API 엔드포인트가 정상적으로 구성되어 있음*