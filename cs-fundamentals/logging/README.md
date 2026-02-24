# 로깅 (Logging)

로그의 근본 개념부터, 애플리케이션 로그 종류, 보안 관점에서의 로그 보존, Azure 리소스 로그까지 체계적으로 정리합니다.

---

## 파일 목록

| # | 파일 | 핵심 내용 |
|---|------|----------|
| 1 | [로그란 무엇인가](./01-로그란-무엇인가.md) | 정의, 목적, 구성 요소, 로그 vs 메트릭 vs 트레이스 |
| 2 | [로그 레벨](./02-로그-레벨.md) | FATAL~TRACE 6단계, 환경별 설정 가이드 |
| 3 | [애플리케이션 로그 종류](./03-애플리케이션-로그-종류.md) | 운영(에러/성능/가용성), 보안(인증/접근제어/감사), 비즈니스(트랜잭션/행동/변경) |
| 4 | [구조화된 로깅](./04-구조화된-로깅.md) | 비구조화 vs 구조화, 핵심 원칙, 표준 필드 설계 |
| 5 | [보안 로그 보존](./05-보안-로그-보존.md) | NIST SP 800-92/800-53, 필수 기록 이벤트, 보존 기간, 티어드 스토리지 |
| 6 | [로그 관리 아키텍처](./06-로그-관리-아키텍처.md) | 중앙 집중식 로깅, 파이프라인, SIEM |
| 7 | [Azure 리소스 로그](./07-Azure-리소스-로그.md) | Activity/Resource/Entra ID 로그, Diagnostic Settings |

---

## 📚 참고 자료

| 자료 | 설명 |
|------|------|
| [NIST SP 800-92](https://csrc.nist.gov/publications/detail/sp/800-92/final) | 컴퓨터 보안 로그 관리 가이드 |
| [NIST SP 800-53 (AU 제어)](https://csrc.nist.gov/publications/detail/sp/800-53/rev-5/final) | 보안·개인정보 제어 (감사·책임) |
| [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html) | 웹 앱 로깅 모범 사례 |
| [Azure Monitor 개요](https://learn.microsoft.com/azure/azure-monitor/overview) | Azure 모니터링 서비스 문서 |
