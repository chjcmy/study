---
tags:
  - architecture
  - sequence_diagram
  - portal_handoff
  - bicep
date: 2026-02-20
completed: false
---

# 🚀 단계 3: 자동 배포 실행 (Portal Handoff)

> [!info] 문서 개요
> 고객사 관리자가 구독 목록 중에서 특정 구독을 픽(Pick)한 후, 로컬 에이전트 인프라스트럭처([[Azure Functions]])를 자동 배포하는 프로세스입니다.

## ✅ 구현 체크리스트

- [ ] 프론트엔드: 통신 결과로 받은 구독 목록 선택 UI 및 [설치] 버튼 구현
- [ ] 프론트엔드: Bicep 템플릿 URL 및 Webhook 파라미터를 포함하여, Azure Portal 커스텀 배포 팝업창을 여는 리다이렉트 URL 생성 로직 구현
- [ ] 인프라 (IAC): 고객사 구독에 배포될 로컬 에이전트(Function App, Managed Identity, RBAC 등)용 Bicep 템플릿 작성 완료

## 🔄 시퀀스 다이어그램 (부분)

> [!abstract] 이 다이어그램은 **Portal Handoff** 모델을 이용하여 Azure Portal로 리다이렉트시키고 템플릿 검토 후 실제 Bicep을 가동하는 로직을 다룹니다.

```mermaid
sequenceDiagram
    participant Teams as 🛠️ [구현-Front] React
    participant Agent as 🛠️ [구현-Infra] Agent
    participant Admin as 🏢 [고객사] 관리자
    participant ARM as 🏢 [고객사] ARM API

    Note over Teams, ARM: [3단계: 자동 배포 실행 (Portal Handoff)]
    Admin->>Teams: 특정 구독 선택 후 [설치] 클릭
    
    Note right of Teams: 🛠️ [프론트 구현]: Bicep URL + Webhook 파라미터 창 띄우기
    Teams->>Admin: Azure Portal 커스텀 배포 화면 리다이렉트
    Admin->>ARM: Portal에서 템플릿 검토 후 [만들기] 클릭
    ARM-->>Agent: Function App 리소스 생성
```

## 🌐 Bicep 템플릿 호스팅 전략 (추천)

> [!tip] Azure Portal 배포 뷰를 열기 위해서는 인터넷상에서 접근 가능한(Public URL) 형태의 `main.json` (ARM 템플릿)이 반드시 필요합니다.

### 옵션 1: GitHub Repository 활용 (권장 🌟)

- **방식**: 로컬에서 작성한 `main.bicep`을 `main.json`으로 빌드(`az bicep build --file main.bicep`) 후 Public GitHub 레포지토리에 커밋.
- **배포 URL**: GitHub의 Raw URL(`https://raw.githubusercontent.com/.../main.json`) 사용.
- **장점**: 무료이며 구성이 가장 쉽고, 인프라 코드 버전 관리와 배포 URL 관리가 일치됨. **현재 프로젝트(초기 B2B 고객사 배포용) 구조에 가장 적합함.**

### 옵션 2: Azure Storage Account (Blob) 브릿지 사용 (엔터프라이즈용 🏢)

- **방식**: SaaS 백엔드 측 Azure 계정에 Storage Account를 생성하고 Blob Container에 `main.json`을 업로드.
- **배포 URL**: Blob URL과 제한된 접근 권한을 가진 **SAS Token**을 결합하여 사용.
- **장점**: 템플릿 코드를 Public 영역(GitHub)에 노출시키지 않고도 배포가 가능하여 기업 내부 보안 규정을 준수할 수 있음.


---

## 📝 상세 설명

1. **옵션 선택 및 커스텀 배포 뷰 생성**:
   - 🏢 **고객사 관리자**가 🛠️ **Teams 탭**에서 `설치` 버튼을 누르면, 프론트엔드가 Bicep 템플릿과 고객사 구독 정보, 그리고 추후 콜백 받을 Webhook 파라미터를 인코딩합니다.
   - 이 데이터들을 Query Parameter 등에 담아 공식 Azure Portal의 "커스텀 배포 팝업창(템플릿 배포 페이지)"으로 사용자를 리다이렉트 시킵니다.

2. **Bicep 템플릿 자동화 (IaC)**:
   - 🏢 **ARM API**는 Bicep 기반 인프라스트럭처 템플릿에 따라 실제 🛠️ **로컬 플랫폼 에이전트 인프라([[Azure Functions]], [[Managed Identity]])**를 자동 구축하게 됩니다.
   - এই 과정 중, 보안 강화를 위한 RBAC Role 역시 시스템 자동 할당 및 부여됩니다.
