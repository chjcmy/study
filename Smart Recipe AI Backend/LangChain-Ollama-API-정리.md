# LangChain + Ollama API 연동 완료 정리

## 🚀 프로젝트 개요
- **프로젝트명**: AI Recipe Assistant
- **목적**: 개인 알레르기 프로필 기반 안전한 레시피 추천
- **기술 스택**: NestJS + LangChain + Ollama + MongoDB + Elasticsearch

## 📋 LangChain을 사용하는 API 목록

### 1. **실제 Ollama 연동 API** (프로덕션용)
```
Base URL: http://localhost:3001/api/v1/ai/langchain
```

#### 🤖 요리 상담 채팅
- **Endpoint**: `POST /ai/langchain/chat`
- **기능**: 자연어로 요리 관련 질문답변
- **LangChain 체인**: PromptTemplate → ChatOllama → StringOutputParser

**요청 예시:**
```json
{
  "message": "파스타 만드는 법 알려주세요",
  "context": "이전 대화 내용 (선택사항)"
}
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "response": "안녕하세요! 무엇을 도와드릴까요? 레시피 찾기, 요리 팁, 또는 특정한 질문이 있으신가요?",
    "timestamp": "2025-06-11T13:34:58.707Z"
  }
}
```

#### 🍳 알레르기 안전 레시피 추천
- **Endpoint**: `POST /ai/langchain/recommend`
- **기능**: 개인 알레르기 프로필 기반 맞춤 레시피 추천
- **LangChain 체인**: 
  1. 레시피 추천 체인: PromptTemplate → ChatOllama → StringOutputParser
  2. 알레르기 체크 체인: AllergyCheckTemplate → ChatOllama → StringOutputParser

**요청 예시:**
```json
{
  "userQuery": "간단한 파스타",
  "userAllergies": ["견과류", "글루텐"],
  "preferences": ["건강한", "간단한"],
  "availableIngredients": ["토마토", "올리브오일", "마늘"]
}
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "recommendation": "### 레시피 이름: 토마토 소스 파스타\n\n#### 재료 목록:\n- 파스타 200g\n- 토마토 캐니드 통조림 400g...",
    "safetyCheck": true,
    "allergiesDetected": []
  },
  "timestamp": "2025-06-11T13:35:45.592Z"
}
```

#### ❤️ 연결 상태 체크
- **Endpoint**: `GET /ai/langchain/health`
- **기능**: LangChain + Ollama 연결 상태 확인
- **LangChain 체인**: 단순 테스트 체인

### 2. **모크 서비스 API** (테스트용)
```
Base URL: http://localhost:3001/api/v1/ai/mock-langchain
```

#### 테스트용 엔드포인트들
- `POST /ai/mock-langchain/chat` - 모크 요리 상담
- `POST /ai/mock-langchain/recommend` - 모크 레시피 추천
- `GET /ai/mock-langchain/health` - 모크 상태 체크

## 🏗️ LangChain 아키텍처 구조

### 서비스 계층
```
LangChainOllamaService
├── ChatOllama 설정
├── 프롬프트 템플릿들
├── RunnableSequence 체인들
└── 알레르기 안전성 검증 로직
```

### 주요 컴포넌트

#### 1. **ChatOllama 설정**
```typescript
this.chatModel = new ChatOllama({
  baseUrl: 'http://192.168.0.111:11434',
  model: 'gemma3:1b-it-qat',
  temperature: 0.7,
  topK: 40,
  topP: 0.9,
});
```

#### 2. **프롬프트 템플릿**
```typescript
// 레시피 추천용
this.recipePromptTemplate = PromptTemplate.fromTemplate(`
당신은 전문 요리사이자 영양사입니다. 사용자의 요청에 따라 안전하고 맛있는 레시피를 추천해주세요.

사용자 요청: {userQuery}
사용자 알레르기: {allergies}
선호사항: {preferences}
보유 재료: {ingredients}
...
`);

// 알레르기 체크용
this.allergyCheckTemplate = PromptTemplate.fromTemplate(`
다음 레시피 재료를 분석하여 알레르기 위험성을 체크해주세요.

레시피 재료: {ingredients}
사용자 알레르기: {userAllergies}
...
`);
```

#### 3. **RunnableSequence 체인**
```typescript
// 레시피 추천 체인
const recipeChain = RunnableSequence.from([
  this.recipePromptTemplate,
  this.chatModel,
  new StringOutputParser(),
]);

// 알레르기 체크 체인
const allergyChain = RunnableSequence.from([
  this.allergyCheckTemplate,
  this.chatModel,
  new StringOutputParser(),
]);
```

## ⚙️ 설정 파일

### 환경변수 (.env)
```bash
# Ollama Configuration
OLLAMA_BASE_URL=http://192.168.0.111:11434
OLLAMA_MODEL=gemma3:1b-it-qat
OLLAMA_EMBEDDING_MODEL=granite-embedding:278m
```

### 모듈 구성 (ai.module.ts)
```typescript
@Module({
  imports: [ConfigModule, VectorModule],
  controllers: [LangChainController, MockLangChainController],
  providers: [LangChainOllamaService, MockLangChainService],
  exports: [LangChainOllamaService, MockLangChainService],
})
export class AiModule {}
```

## 🔄 API 호출 플로우

### 레시피 추천 플로우
```
1. 사용자 요청 → Controller
2. DTO 검증 → Service
3. 레시피 추천 체인 실행
   ├── PromptTemplate (사용자 요청 + 알레르기 정보 조합)
   ├── ChatOllama (Gemma3 모델로 레시피 생성)
   └── StringOutputParser (응답 파싱)
4. 알레르기 안전성 체크 체인 실행
   ├── AllergyCheckTemplate (생성된 레시피 + 사용자 알레르기 분석)
   ├── ChatOllama (안전성 평가)
   └── StringOutputParser (안전성 결과 파싱)
5. 최종 응답 반환
```

### 요리 상담 플로우
```
1. 사용자 메시지 → Controller
2. DTO 검증 → Service
3. 요리 상담 체인 실행
   ├── ChatPromptTemplate (친근한 요리 어시스턴트 역할)
   ├── ChatOllama (자연스러운 대화 생성)
   └── StringOutputParser (응답 파싱)
4. 응답 반환
```

## 📊 성능 지표

### 응답 시간
- **간단한 요리 상담**: 3초 이내
- **복잡한 레시피 추천**: 30-40초
- **연결 상태 체크**: 15-20초 (첫 로딩)

### 사용 모델
- **채팅 모델**: `gemma3:1b-it-qat` (1B 파라미터, 양자화)
- **임베딩 모델**: `granite-embedding:278m` (278M 파라미터)

## 🛠️ 개발 팁

### 테스트 방법
```bash
# 요리 상담 테스트
curl -X POST http://localhost:3001/api/v1/ai/langchain/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "파스타 만드는 법 알려주세요"}'

# 레시피 추천 테스트
curl -X POST http://localhost:3001/api/v1/ai/langchain/recommend \
  -H "Content-Type: application/json" \
  -d '{"userQuery": "간단한 파스타", "userAllergies": ["견과류"]}'

# 연결 상태 체크
curl -X GET http://localhost:3001/api/v1/ai/langchain/health
```

### 디버깅
- 로그 레벨: `debug` 설정으로 상세 로그 확인
- 타임아웃: 복잡한 요청은 30초 이상 소요될 수 있음
- 에러 처리: 모든 체인에서 try-catch로 에러 핸들링

## 🚀 확장 가능성

### 추가 가능한 체인들
1. **재료 기반 레시피 검색**: 보유 재료로 만들 수 있는 레시피 찾기
2. **영양 분석**: 레시피의 영양 성분 분석
3. **대체 재료 제안**: 알레르기 재료의 안전한 대체재 추천
4. **요리 난이도 평가**: 레시피의 복잡성 평가

### RAG 통합
- **벡터 검색**: 기존 레시피 데이터베이스와 연동
- **하이브리드 검색**: 키워드 + 의미적 유사성 검색
- **컨텍스트 증강**: 검색된 레시피를 컨텍스트로 활용

## 📚 학습 참고 자료

### LangChain 핵심 개념
- **RunnableSequence**: 여러 단계를 순차적으로 연결
- **PromptTemplate**: 동적 프롬프트 생성
- **OutputParser**: 모델 응답 파싱 및 구조화
- **ChatModel**: 대화형 언어 모델 인터페이스

### 모범 사례
- 프롬프트 템플릿 분리로 유지보수성 향상
- 에러 핸들링 및 폴백 메커니즘 구현
- 모크 서비스로 개발/테스트 효율성 증대
- 타입 안전성을 위한 TypeScript 인터페이스 정의

---
*마지막 업데이트: 2025-06-11*
*프로젝트: AI Recipe Assistant*
*기술: NestJS + LangChain + Ollama*