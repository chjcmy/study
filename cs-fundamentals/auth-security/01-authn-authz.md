# 인증 (Authentication) vs 인가 (Authorization)

---

## 핵심 구분

```
인증 (Authentication) = "너 누구야?" → 신원 확인
인가 (Authorization)  = "너 이거 해도 돼?" → 권한 확인

순서: 인증 → 인가 (누구인지 알아야 권한 확인 가능)

쇼핑몰 예시:
1. 인증: 이메일/비밀번호로 "이 사람은 user123"임을 확인
2. 인가: "user123은 자기 주문내역만 볼 수 있고, 관리자 페이지는 불가"
```

### 비유

```
호텔 예시:
  인증: 프론트에서 여권으로 본인 확인 (Check-in)
  인가: 방 카드키로 자기 방만 열 수 있음 (Access Control)

회사 예시:
  인증: 사원증 찍고 출입 (너 우리 직원 맞아?)
  인가: 서버실은 인프라팀만 들어갈 수 있음 (권한 체크)
```

---

## 인증 방식 비교

### 1. 세션 기반 인증

```
클라이언트              서버                   세션 저장소 (Redis 등)
    │──로그인──────→ │                         │
    │               │ 검증→세션 생성→저장 ──→ │ {sid: user_data}
    │← Set-Cookie ──│ (Session ID)            │
    │               │                         │
    │──요청+쿠키───→ │                         │
    │               │ 세션 조회 ──────────→   │ → user_data 반환
    │← 응답────────│                         │

장점: 서버에서 세션 관리 (강제 만료 가능, 즉시 로그아웃)
단점: 서버 상태 관리 필요, 수평 확장 시 세션 공유 문제
      → Redis 같은 외부 저장소로 세션 공유하면 해결
```

### 2. 토큰 기반 인증 (JWT)

```
클라이언트              서버
    │──로그인──────→ │
    │               │ 검증 → JWT 생성 (서명)
    │← JWT Token ──│ (서버에 저장하지 않음!)
    │               │
    │──요청 + JWT──→│
    │  Authorization:│ JWT 검증 (서명 확인만)
    │  Bearer eyJ... │ → 유효하면 사용자 정보 추출
    │← 응답────────│

장점: 무상태 (Stateless) → 수평 스케일링 쉬움
      서버에 저장 불필요 → 세션 공유 문제 없음
단점: 토큰 무효화 어려움 (만료까지 기다려야)
      토큰 크기가 쿠키보다 큼
```

### 3. OAuth 2.0 (제3자 인증)

```
사용자               클라이언트 (앱)        인증 서버 (Google 등)
  │──"구글 로그인"──→│                     │
  │                 │──인증 요청──────→│
  │←──구글 로그인 페이지──────────────│
  │──ID/PW 입력──────────────────→│
  │                 │←──Auth Code────│
  │                 │──Code+Secret──→│
  │                 │←──Access Token──│
  │←──로그인 완료──│                  │

장점: 비밀번호를 앱에 노출하지 않음, SSO 가능
단점: 복잡한 플로우, 외부 서비스 의존
```

### 비교 표

| 방식 | 스케일링 | 보안 | 복잡도 | 무효화 | 적합 |
|------|---------|------|--------|--------|------|
| 세션 | 어려움 (공유 필요) | 강제 만료 가능 | 낮음 | ✅ 즉시 | 전통 웹앱 |
| JWT | **쉬움** (무상태) | 만료 후 무효 | 보통 | ❌ 어려움 | **API 서버** |
| OAuth | **쉬움** | 제3자 관리 | 높음 | 토큰에 따라 | **SSO, 소셜 로그인** |

---

## RBAC (Role-Based Access Control)

```
역할 기반 접근 제어 — 사용자에게 역할을 부여하고, 역할에 권한을 부여

사용자 A ── 역할: Admin ── 권한: 읽기, 쓰기, 삭제
사용자 B ── 역할: Editor ── 권한: 읽기, 쓰기
사용자 C ── 역할: Viewer ── 권한: 읽기

예: JWT의 roles 클레임에서 역할 확인:
{
  "sub": "user123",
  "roles": ["admin"]
}
→ admin이면 삭제 가능, viewer면 읽기만
```

```python
# Python 예시: RBAC 구현
def require_role(allowed_roles: list[str]):
    def decorator(func):
        def wrapper(user, *args, **kwargs):
            if not any(role in user.roles for role in allowed_roles):
                raise PermissionError("권한 없음")
            return func(user, *args, **kwargs)
        return wrapper
    return decorator

@require_role(["admin"])
def delete_user(user, target_id):
    # admin만 실행 가능
    ...
```

### RBAC vs ABAC

```
RBAC (역할 기반): 
  "admin은 삭제 가능" → 단순, 널리 사용
  
ABAC (속성 기반):
  "근무 시간 + 본인 부서 데이터만 접근 가능" → 세밀, 복잡
  속성: 시간, 위치, IP, 리소스 소유자 등

대부분의 시스템: RBAC으로 충분
대규모 기업: ABAC 또는 RBAC + 추가 조건
```

---

## 면접 핵심 포인트

```
Q: 인증과 인가의 차이?
A: 인증은 "신원 확인"(who?), 인가는 "권한 확인"(what?).
   인증이 선행되어야 인가 가능.

Q: 세션 vs JWT 선택 기준?
A: 세션: 즉시 무효화 필요, 서버 상태 관리 가능할 때 (전통 웹앱)
   JWT: 무상태 확장 필요, MSA/API 서버 환경
   JWT 즉시 무효화 필요 시 → 블랙리스트(Redis) 병행

Q: RBAC vs ABAC?
A: RBAC: 역할 기반 (Admin, Editor) — 단순, 대부분 충분
   ABAC: 속성 기반 (시간, 위치, 리소스) — 세밀, 복잡
```
