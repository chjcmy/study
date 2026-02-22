# Log Doctor - Azure Projects

이곳은 **Log Doctor** 프로젝트의 Azure 인프라 및 인증 아키텍처를 정리하는 공간입니다. 
복잡한 MFA와 시크릿 관리에서 벗어나, Azure의 권장 표준인 **Managed Identity**를 중심으로 시스템을 재편했습니다.

## 📂 문서 가이드

### 1단계: 인프라 및 권한 설정
- [1-managed-identity-setup.md](file:///Users/choeseonghyeon/study/azure/projects/1-managed-identity-setup.md)
  - 관리 ID(User-Assigned Managed Identity) 생성 및 구독 레벨의 '기여자' 권한 부여 방법

### 2단계: 백엔드 구현 및 인증 연동
- [2-backend-config.md](file:///Users/choeseonghyeon/study/azure/projects/2-backend-config.md)
  - Python `azure-identity` 라이브러리를 활용한 `DefaultAzureCredential` 구현 및 `.env` 설정

### 3단계: 보안 아키텍처 및 트러블슈팅
- [3-security-architecture.md](file:///Users/choeseonghyeon/study/azure/projects/3-security-architecture.md)
  - MFA 프리 인증 원리, 최소 권한 원칙(PoLP) 및 주요 에러 해결 가이드

---
*최종 업데이트: 2026-02-23*
