---
tags:
  - architecture
  - sequence_diagram
  - system_design
date: 2026-02-20
---

# 🚀 Azure Projects (Log-Doctor)

> [!info] 문서 개요
> 이 문서는 [[Log-Doctor]] SaaS 솔루션이 [[Entra ID]], [[Azure ARM API|ARM API]], 그리고 고객의 [[Azure Functions|로컬 에이전트]]와 어떻게 상호작용하는지 정리한 **아키텍처 스펙 문서**입니다.

## 🔄 1. 시스템 동작 시퀀스 (Architecture Sequence)

> [!abstract] SaaS 백엔드, 프론트엔드(Teams), 그리고 고객사 인프라(Agent) 간의 상호작용 및 인증 흐름을 구체적인 기술 스택과 함께 정의합니다.

```mermaid
sequenceDiagram
    autonumber

    participant P_Entra as ☁️ [공급사] Entra ID
    participant SaaS as 🛠️ [구현-Back] API
    participant Teams as 🛠️ [구현-Front] React
    participant Agent as 🛠️ [구현-Infra] Agent
    participant Admin as 🏢 [고객사] 관리자
    participant C_Entra as 🏢 [고객사] Entra ID
    participant ARM as 🏢 [고객사] ARM API

    Note over P_Entra, ARM: [0단계: 공급사 인프라 사전 설정 (코드 X)]
    P_Entra->>P_Entra: 앱 등록 (Multi-tenant, API 권한 설정)
    P_Entra->>SaaS: Client ID / Secret 발급 후 .env에 주입

    Note over P_Entra, ARM: [1단계: 접속 및 고객사 연동 (Admin Consent)]
    Admin->>Teams: 앱 실행 (Tab 접속)
    
    Note right of Teams: 🛠️ [프론트 구현]: getAuthToken() 호출
    Teams->>C_Entra: Silent SSO 토큰 요청 
    
    alt 최초 접속 (권한 미승인 상태)
        C_Entra-->>Teams: Error: Consent Required
        Note right of Teams: 🛠️ [프론트 구현]: authenticate() 팝업 폴백 로직
        Teams->>Admin: 팝업창(Interactive Login) 표시
        Admin->>C_Entra: 조직 전체를 대신하여 '동의(Consent)' 클릭
        C_Entra-->>Teams: SSO 토큰 (JWT) 반환
    else 기존 사용자 (승인 완료 상태)
        C_Entra-->>Teams: SSO 토큰 (JWT) 즉시 반환 (Silent)
    end

    Note right of Teams: 🛠️ [프론트 구현]: API 호출 (헤더에 토큰 첨부)
    Teams->>SaaS: SSO 토큰 전달 (GET /subscriptions)

    Note left of SaaS: 🛠️ [백엔드 구현]: 토큰 서명(Signature) 검증 및 DB 연동
    SaaS->>C_Entra: MS 공개키(JWKS) 요청 및 서명 검증
    C_Entra-->>SaaS: 성공 (위조 없음 확인)
    
    SaaS->>SaaS: 사용자(oid, tid) DB 조회 및 신규 연동 처리

    Note over P_Entra, ARM: [2단계: 구독 목록 조회 (OBO Flow)]
    Note left of SaaS: 🛠️ [백엔드 구현]: MSAL 라이브러리로 OBO 토큰 교환
    SaaS->>P_Entra: OBO 토큰 교환 요청 (Client Secret + SSO 토큰)
    P_Entra-->>SaaS: 고객사 ARM 접근용 Access Token 발급
    
    Note left of SaaS: 🛠️ [백엔드 구현]: ARM REST API 호출
    SaaS->>ARM: GET /subscriptions 호출 (Bearer {ARM_Token})
    ARM-->>SaaS: 구독 리스트 반환
    SaaS-->>Teams: 도메인 모델로 변환하여 응답

    Note over P_Entra, ARM: [3단계: 자동 배포 실행 (Portal Handoff)]
    Admin->>Teams: 특정 구독 선택 후 [설치] 클릭
    
    Note right of Teams: 🛠️ [프론트 구현]: Bicep URL + Webhook 파라미터 창 띄우기
    Teams->>Admin: Azure Portal 커스텀 배포 화면 리다이렉트
    Admin->>ARM: Portal에서 템플릿 검토 후 [만들기] 클릭
    ARM-->>Agent: Function App 리소스 생성

    Note over P_Entra, ARM: [4단계: 배포 완료 알림 (Handshake & Webhook)]
    Note right of Agent: 🛠️ [에이전트 구현]: Bicep 템플릿 및 기동 시 Webhook 발송
    Agent->>SaaS: POST /agents/webhook (설치 완료 알림)
    
    Note left of SaaS: 🛠️ [백엔드 구현]: 웹훅 수신 API 및 DB 상태 업데이트
    SaaS->>SaaS: DB에 "Active" 상태 업데이트
    
    loop 상태 확인
        Note right of Teams: 🛠️ [프론트 구현]: 상태 폴링(Polling) 로직
        Teams->>SaaS: 에이전트 상태 폴링
        SaaS-->>Teams: "Active" 반환
    end
    
     Teams->>Admin: 🎉 대시보드 화면 렌더링 전환
```

## 🏛️ 2. 전체 시스템 아키텍처 (System Architecture & Components)

> [!note]
> SaaS 제공자(새싹 테넌트)와 고객사(Customer 테넌트), 그리고 Microsoft 클라우드 인프라 간의 물리적/논리적 컴포넌트 구성도입니다.

```mermaid
flowchart TB
    subgraph Customer ["🏢 고객 테넌트 (Customer Environment)"]
        direction TB
        Admin(["👨‍💼 고객사 관리자"])
        
        subgraph Customer_Sub ["고객사 Azure 구독 (Subscription)"]
            Agent_Func["⚡ Azure Functions\n(로컬 플랫폼 에이전트)"]
            Agent_MI["🔑 Managed Identity\n(시스템 할당)"]
            Agent_RBAC["🛡️ RBAC Role\n(Reader 권한)"]
            Agent_Diag["📊 진단 설정\n(로그/메트릭 수집)"]
            
            Agent_Func --"인증 위임"--> Agent_MI
            Agent_MI --"권한 인가"--> Agent_RBAC
            Agent_Func --"수집 스크립트 실행"--> Agent_Diag
        end
    end

    subgraph Provider ["☁️ 공급사 테넌트 (SaaS Provider - 새싹)"]
        direction TB
        
        subgraph Frontend ["🛠️ 구현-Front"]
            Teams_App["📱 Teams App (Personal Tab)\n(React SPA)"]
        end
        
        subgraph Backend ["🛠️ 구현-Back"]
            FastAPI["🔌 FastAPI 서버\n(Python 3.12+)"]
            MSAL["🔐 MSAL 라이브러리\n(OBO Flow)"]
        end
        
        subgraph Database ["💾 구현-DB"]
            CosmosDB[("🌌 Azure Cosmos DB\nNoSQL Serverless")]
        end
        
        Teams_App --"REST API 호출\n(+ SSO 토큰)"--> FastAPI
        FastAPI --"토큰 교환"--> MSAL
        FastAPI --"CRUD 통신"--> CosmosDB
    end

    subgraph Microsoft ["🌐 Microsoft 인프라"]
        direction TB
        Entra_ID{"Entra ID\n(인증 & 권한)"}
        ARM_API{"ARM REST API\n(리소스 제어)"}
        Portal["🖥️ Azure Portal\n(사용자 UI)"]
    end

    %% 연결선 (Interactions)
    Admin --"(1) 앱 접속"--> Teams_App
    Teams_App -. "(2) Silent SSO" .-> Entra_ID
    MSAL --"(3) OBO 교환 요청"--> Entra_ID
    FastAPI --"(4) 리소스/구독 조회"--> ARM_API
    Teams_App --"(5) Handoff 배포"--> Portal
    Portal --"(6) Bicep 템플릿 배포"--> Customer_Sub
    
    Agent_Func --"(7) Webhook (상태 알림)"--> FastAPI
    
    %% 스타일링
    classDef saas fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    classDef customer fill:#f1f8e9,stroke:#689f38,stroke-width:2px;
    classDef azure fill:#f3e5f5,stroke:#8e24aa,stroke-width:2px;
    
    class Teams_App,FastAPI,MSAL,CosmosDB saas;
    class Agent_Func,Agent_MI,Agent_RBAC,Agent_Diag customer;
    class Entra_ID,ARM_API,Portal azure;
```
