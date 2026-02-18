# LangGraph (랭그래프)

LangGraph는 **복잡한 멀티 에이전트 시스템** 개발에 특화된 프레임워크로, LangChain의 기능을 확장하여 자연어 처리 및 AI 애플리케이션의 복잡한 워크플로우를 그래프 기반으로 구조화합니다.

---

## 핵심 개념

- **State (상태):**
  - 모든 노드가 공유하는 전역 변수 공간
  - `TypedDict` 등으로 타입 명시
  - 예시:
    ```python
    class ExampleState(TypedDict):
        x: int
        y: float
        sentence: str
        queries: list
    ```
- **Node (노드):**
  - 각 노드 = LLM 에이전트(예: 검색, 웹드라이버, 생성모델)
  - 기능 수행 후 State를 갱신하여 반환
- **Edge (엣지):**
  - 에이전트 간 상호작용(데이터 전달, 라우팅)

---

## 주요 특징

- **그래프 구조:** 복잡한 워크플로우를 시각적으로 표현/관리
- **상태 관리:** LLM 기반 상태 유지, 에이전트 간 정보 공유
- **유연성:** 에이전트 로직/통신 프로토콜 자유롭게 정의
- **다이나믹 플로우:** 사이클 구조로 반복적 정보 보강 및 품질 향상

---

## 활용 예시

- **멀티 에이전트 협업**: 여러 에이전트가 협력해 복잡한 문제 해결 (예: 마케팅 캠페인 최적화)
- **대화형 AI**: 지능형 챗봇, 사용자 컨텍스트 기반 답변
- **자동화 워크플로우**: 문서 작성, 검토 등 반복적 프로세스 자동화

---

## 장점

- **빠르고 효율적인 개발**
- **모듈화/재사용성 및 유지보수 용이**
- **확장성**: 새로운 기능/에이전트 손쉽게 추가

---

## 대표 기능

- StateGraph 구현
- 노드/엣지 정의 및 조건부 라우팅
- 체크포인터, 서브그래프 등 고급 워크플로우 설계

---

## LangChain과의 비교

| 구분       | LangChain                | LangGraph                   |
| ---------- | ------------------------ | --------------------------- |
| 특화 영역  | 기본 LLM 애플리케이션    | 복잡한 멀티 에이전트 시스템 |
| 구조       | 선형 파이프라인          | 그래프 기반 워크플로우      |
| 상태 관리  | 제한적                   | 강력한 상태 관리            |

---

**태그:** #LangGraph #AI #Framework #MultiAgent #LLM #StateManagement #WorkflowAutomation

**관련 개념:** LangChain, AI Agent, RAG, 자연어처리, 멀티에이전트시스템

**참고/출처**
- [1] 위키독스: https://wikidocs.net/261577
- [2] AI 알리미: https://ai-inform.tistory.com/entry/%EB%9E%AD%EA%B7%B8%EB%9E%98%ED%94%84%EB%9E%80-LangGraph-%EC%89%AC%EC%9A%B4%EC%84%A4%EB%AA%85
- [3] 위키독스 기초: https://wikidocs.net/261576
- [4] YouTube: https://www.youtube.com/watch?v=W_uwR_yx4-c
- [5] x2bee 블로그: https://x2bee.tistory.com/430
- [6] 패스트캠퍼스: https://fastcampus.co.kr/data_online_langgraph
- [7] velog: https://velog.io/@kwon0koang/%EB%9E%AD%EA%B7%B8%EB%9E%98%ED%94%84-LangGraph-%ED%9A%A8%EC%9C%A8%EC%A0%81%EC%9D%B8-AI-%EC%9B%8C%ED%81%AC%ED%94%8C%EB%A1%9C%EC%9A%B0-%EA%B5%AC%EC%B6%95
- [8] modulabs: https://modulabs.co.kr/blog/langgraph_multiagent
- [9] brunch: https://brunch.co.kr/@@aPda/339
- [10] 예스24: https://www.yes24.com/product/goods/143646468
