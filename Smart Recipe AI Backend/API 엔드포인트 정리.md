# API 엔드포인트 정리

## 🔗 기본 정보
- **Base URL**: `http://localhost:3001/api/v1`
- **API 문서**: `http://localhost:3001/api/docs`
- **헬스체크**: `http://localhost:3001/api/v1/health`

---

## 🔐 인증 (Authentication)
**Base Path**: `/api/v1/auth`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/login` | 사용자 로그인 | ❌ |
| POST | `/register` | 사용자 회원가입 | ❌ |
| POST | `/profile` | 프로필 조회 | ✅ |

---

## 👤 사용자 관리 (Users)
**Base Path**: `/api/v1/users`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| GET | `/profile` | 사용자 프로필 조회 | ✅ |
| PATCH | `/profile` | 사용자 프로필 수정 | ✅ |
| POST | `/favorites/:recipeId` | 즐겨찾기 추가 | ✅ |
| PATCH | `/favorites/:recipeId` | 즐겨찾기 제거 | ✅ |

---

## 🍳 레시피 관리 (Recipes)
**Base Path**: `/api/v1/recipes`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/` | 새 레시피 생성 | ✅ |
| GET | `/` | 레시피 목록 조회 | ❌ |
| GET | `/popular` | 인기 레시피 조회 | ❌ |
| GET | `/recent` | 최근 레시피 조회 | ❌ |
| GET | `/by-ingredients` | 재료별 레시피 검색 | ❌ |
| GET | `/:id/similar` | 유사 레시피 조회 | ❌ |
| POST | `/reindex` | 레시피 재인덱싱 | ✅ |
| GET | `/:id` | 특정 레시피 조회 | ❌ |
| PATCH | `/:id` | 레시피 수정 | ✅ |
| DELETE | `/:id` | 레시피 삭제 | ✅ |
| PATCH | `/:id/rating` | 레시피 평점 수정 | ✅ |

---

## 🔍 검색 (Search)
**Base Path**: `/api/v1/search`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| GET | `/` | 통합 검색 | ❌ |
| GET | `/suggest` | 검색 자동완성 | ❌ |

---

## 🔄 벡터 관리 (Vectors)
**Base Path**: `/api/v1/vectors`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/` | 벡터 생성 | ✅ |
| POST | `/search` | 벡터 유사성 검색 | ❌ |
| PUT | `/:vectorId` | 벡터 업데이트 | ✅ |
| DELETE | `/:vectorId` | 벡터 삭제 | ✅ |
| DELETE | `/source/:sourceType/:sourceId` | 소스별 벡터 삭제 | ✅ |
| GET | `/:vectorId/metadata` | 벡터 메타데이터 조회 | ❌ |
| GET | `/source/:sourceType/:sourceId` | 소스별 벡터 조회 | ❌ |
| POST | `/bulk` | 벌크 벡터 생성 | ✅ |
| GET | `/stats` | 벡터 통계 정보 | ❌ |

---

## 🤖 AI 기능 (AI)
**Base Path**: `/api/v1/ai`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/recommend` | AI 레시피 추천 | ❌ |
| POST | `/generate` | AI 레시피 생성 | ✅ |
| POST | `/embedding` | 텍스트 임베딩 생성 | ✅ |
| GET | `/health` | AI 서비스 상태 확인 | ❌ |
| GET | `/models` | 사용 가능한 AI 모델 목록 | ❌ |

---

## 🧠 RAG 시스템 (RAG)
**Base Path**: `/api/v1/rag`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/ask` | 일반 질문 답변 | ❌ |
| POST | `/recipe/ask` | 레시피 관련 질문 답변 | ❌ |
| POST | `/conversation` | 대화형 질문 답변 | ❌ |
| POST | `/hybrid-search` | 하이브리드 검색 | ❌ |
| POST | `/explain-recipe` | 레시피 설명 생성 | ❌ |
| POST | `/suggest-variations` | 레시피 변형 제안 | ❌ |
| POST | `/cooking-tips` | 요리 팁 제공 | ❌ |

---

## 💬 채팅 (Chat)
**Base Path**: `/api/v1/chat`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/sessions` | 새 채팅 세션 생성 | ✅ |
| POST | `/sessions/:sessionId/messages` | 메시지 전송 | ✅ |
| GET | `/sessions/:sessionId` | 채팅 세션 조회 | ✅ |
| GET | `/sessions/:sessionId/history` | 채팅 히스토리 조회 | ✅ |
| POST | `/sessions/:sessionId/rag-messages` | RAG 메시지 전송 | ✅ |

---

## 📊 데이터 인덱싱 (Indexing)
**Base Path**: `/api/v1/indexing`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/setup-indices` | Elasticsearch 인덱스 설정 | ✅ |
| POST | `/load-ingredients` | 재료 데이터 로드 | ✅ |
| POST | `/load-recipes` | 레시피 데이터 로드 | ✅ |
| GET | `/status` | 인덱싱 상태 확인 | ❌ |

---

## 📁 데이터 관리 (Data)
**Base Path**: `/api/v1/data`

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | `/load-recipes` | 레시피 CSV 로드 | ✅ |
| POST | `/load-allergens` | 알레르기 데이터 로드 | ✅ |
| POST | `/load-all` | 모든 데이터 로드 | ✅ |

---

## 🔧 요청/응답 예제

### 레시피 검색 예제
```bash
# 기본 검색
curl -X GET "http://localhost:3001/api/v1/search?q=pasta&limit=10"

# 재료별 검색
curl -X GET "http://localhost:3001/api/v1/recipes/by-ingredients?ingredients=tomato,basil&limit=5"

# 인기 레시피
curl -X GET "http://localhost:3001/api/v1/recipes/popular?limit=10"
```

### AI 추천 예제
```bash
curl -X POST "http://localhost:3001/api/v1/ai/recommend" \
  -H "Content-Type: application/json" \
  -d '{
    "preferences": ["italian", "vegetarian"],
    "allergies": ["nuts", "dairy"],
    "cookingTime": 30
  }'
```

### 데이터 상태 확인 예제
```bash
curl -X GET "http://localhost:3001/api/v1/indexing/status"
```

**응답 예제**:
```json
{
  "mongodb": {
    "recipes": 231637,
    "ingredients": 15244
  },
  "elasticsearch": {
    "recipes": 0,
    "ingredients": 15244
  },
  "synchronized": {
    "recipes": false,
    "ingredients": true
  }
}
```

---

## 🚨 에러 코드

| 상태 코드 | 설명 |
|-----------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 422 | 유효성 검사 실패 |
| 500 | 서버 내부 오류 |

---

## 📝 관련 노트
- [[데이터 로드 과정 정리]]
- [[백엔드 아키텍처 설계]]
- [[MongoDB 스키마 설계]]