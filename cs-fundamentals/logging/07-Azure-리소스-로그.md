# Azure 리소스 로그 (Azure Resource Logs)

> 📌 이 문서는 Azure 클라우드 환경에서의 로그를 다룬다.
> 앞의 1~6장에서 배운 근본 개념이 Azure에서 어떻게 구현되는지를 참고 자료로 정리한다.

---

## 1. Azure 로그의 전체 그림

### 1.1 Azure Monitor란?

**Azure Monitor**는 Azure의 중앙 모니터링 플랫폼이다. 모든 Azure 리소스의 로그·메트릭·트레이스를 수집·분석·시각화한다.

```
                        Azure Monitor
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
      로그 (Logs)       메트릭 (Metrics)    트레이스 (Traces)
         │                  │                  │
  Log Analytics       Metrics Explorer    Application
  Workspace           (실시간 차트)       Insights
  (KQL 쿼리)                              (APM)
```

> 📌 1장에서 배운 Observability 3대 기둥이 Azure에서는 이렇게 구현된다.

### 1.2 Azure 플랫폼 로그의 3가지 계층

```
┌─────────────────────────────────────────────────────────┐
│                                                           │
│  ③ 테넌트 계층 (Tenant Level)                             │
│     └─ Entra ID 로그 (로그인, 감사, 프로비저닝)             │
│     └─ 범위: Azure AD 테넌트 전체                          │
│                                                           │
│  ② 구독 계층 (Subscription Level)                         │
│     └─ Activity Log (활동 로그)                            │
│     └─ 범위: 구독 단위의 관리 작업 (ARM 작업)               │
│                                                           │
│  ① 리소스 계층 (Resource Level)                            │
│     └─ Resource Logs (리소스 로그)                          │
│     └─ 범위: 개별 Azure 리소스 (VM, App Service, DB 등)     │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Activity Log (활동 로그)

### 2.1 정의와 역할

Activity Log는 **구독 수준에서 리소스에 대한 관리 작업(Control Plane)**을 기록한다.

```
Control Plane (관리 평면) vs Data Plane (데이터 평면):

  Control Plane (Activity Log에 기록):
  ├── VM을 생성/삭제/시작/중지
  ├── 네트워크 보안 규칙 변경
  ├── 역할 할당 (RBAC) 변경
  ├── 리소스 그룹 생성/삭제
  └── 배포 작업

  Data Plane (Resource Log에 기록):
  ├── VM 안에서 실행된 프로세스
  ├── App Service에 들어온 HTTP 요청
  ├── SQL Database에 실행된 쿼리
  └── Storage에 업로드된 파일
```

> 📌 **핵심 구분**: "Azure 포털/CLI/API에서 리소스를 **조작**하는 것" = Control Plane → Activity Log.
> "리소스가 실제로 **작동**하면서 생기는 것" = Data Plane → Resource Log.

### 2.2 Activity Log 카테고리

| 카테고리 | 설명 | 예시 |
|---------|------|------|
| **Administrative** | 리소스에 대한 모든 생성/수정/삭제 작업 | VM 생성, NSG 규칙 변경 |
| **Service Health** | Azure 서비스 장애·유지보수 이벤트 | "Korea Central 리전에서 VM 장애" |
| **Resource Health** | 개별 리소스의 건강 상태 변화 | "VM이 Available → Unavailable" |
| **Alert** | Azure Monitor 알림 활성화 기록 | "CPU 90% 초과 알림 발생" |
| **Autoscale** | 자동 확장/축소 이벤트 | "인스턴스 3개 → 5개로 스케일아웃" |
| **Recommendation** | Azure Advisor 권장 사항 | "미사용 리소스 삭제 권장" |
| **Security** | Microsoft Defender 보안 알림 | "비정상 위치에서의 로그인 시도" |
| **Policy** | Azure Policy 평가 결과 | "태그 누락 리소스 발견" |

### 2.3 Activity Log 보존

```
기본 보존:
  └─ Azure 포털에서 90일간 조회 가능 (무료)
  └─ 90일 이후 → 자동 삭제

장기 보존이 필요하면:
  └─ Diagnostic Setting으로 다른 목적지로 전송
     ├── Log Analytics Workspace → KQL로 고급 분석 (수집 무료!)
     ├── Storage Account → 장기 아카이브 (비용 최소)
     └── Event Hub → 외부 SIEM으로 실시간 스트리밍
```

### 2.4 Activity Log JSON 구조

```json
{
  "time": "2026-02-24T18:30:00.000Z",
  "resourceId": "/subscriptions/xxxx/resourceGroups/myRG/providers/Microsoft.Compute/virtualMachines/myVM",
  "operationName": "Microsoft.Compute/virtualMachines/write",
  "category": "Administrative",
  "resultType": "Success",
  "caller": "admin@example.com",
  "callerIpAddress": "203.0.113.42",
  "properties": {
    "statusCode": "Created",
    "serviceRequestId": "abc-123-def"
  },
  "correlationId": "corr-xyz-789"
}
```

**각 필드가 6하원칙에 매핑:**
- **When**: `time`
- **Who**: `caller`, `callerIpAddress`
- **What**: `operationName`
- **Where**: `resourceId`
- **How**: `properties`
- **결과**: `resultType`

---

## 3. Resource Logs (리소스 로그)

### 3.1 정의

Resource Logs는 **개별 Azure 리소스 내부에서 수행된 작업(Data Plane)**을 기록한다.

```
⚠️ Resource Logs는 기본적으로 수집되지 않는다!
   Diagnostic Setting을 만들어야 로그가 수집된다.

  Activity Log: 자동으로 수집됨 ✅ (설정 불필요)
  Resource Logs: Diagnostic Setting 필요 ⚠️ (직접 설정해야 함)
```

### 3.2 주요 Azure 서비스별 Resource Logs

#### App Service (웹 앱)

| 로그 카테고리 | 설명 | 앞 단원과의 연결 |
|-------------|------|----------------|
| **AppServiceHTTPLogs** | 모든 HTTP 요청/응답 기록 | → 3장: 요청/접근 로그 |
| **AppServiceConsoleLogs** | 컨테이너 stdout/stderr | → 3장: 에러 로그 |
| **AppServiceAppLogs** | 앱 코드의 logger 출력 | → 2장: 로그 레벨 |
| **AppServiceAuditLogs** | FTP/배포 감사 기록 | → 3장: 감사 로그 |
| **AppServicePlatformLogs** | 플랫폼 런타임 이벤트 | → 3장: 가용성 로그 |

```kusto
// KQL: App Service에서 5xx 에러 찾기
AppServiceHTTPLogs
| where TimeGenerated > ago(1h)
| where ScStatus >= 500
| summarize Count = count() by CsUriStem, ScStatus
| order by Count desc
| take 10
```

#### Azure SQL Database

| 로그 카테고리 | 설명 |
|-------------|------|
| **SQLInsights** | 지능형 성능 분석 |
| **AutomaticTuning** | 자동 튜닝 권장/적용 |
| **QueryStoreRuntimeStatistics** | 쿼리 실행 통계 |
| **Errors** | DB 에러 이벤트 |
| **DatabaseWaitStatistics** | 대기 통계 (병목 분석) |
| **Deadlocks** | 교착 상태 발생 |
| **SQLSecurityAuditEvents** | 보안 감사 (접근·쿼리) |

```kusto
// KQL: 느린 쿼리 찾기
AzureDiagnostics
| where ResourceProvider == "MICROSOFT.SQL"
| where Category == "QueryStoreRuntimeStatistics"
| where duration_d > 5000  // 5초 이상
| project TimeGenerated, query_hash_s, duration_d, execution_count_d
| order by duration_d desc
```

#### AKS (Azure Kubernetes Service)

| 로그 카테고리 | 설명 |
|-------------|------|
| **kube-apiserver** | K8s API 서버 로그 |
| **kube-controller-manager** | 컨트롤러 매니저 |
| **kube-scheduler** | 스케줄러 결정 로그 |
| **kube-audit** | K8s 감사 로그 (누가 어떤 API 호출) |
| **kube-audit-admin** | 읽기 제외 감사 로그 |
| **guard** | Entra ID 인증 이벤트 |

#### Key Vault

| 로그 카테고리 | 설명 |
|-------------|------|
| **AuditEvent** | 모든 키/비밀/인증서 접근 기록 |

```kusto
// KQL: Key Vault에서 비밀 접근 기록
AzureDiagnostics
| where ResourceProvider == "MICROSOFT.KEYVAULT"
| where OperationName == "SecretGet"
| project TimeGenerated, identity_claim_upn_s, id_s, CallerIPAddress
| order by TimeGenerated desc
```

> 📌 Key Vault 감사 로그는 **보안상 극히 중요**하다.
> "누가 어떤 비밀을 언제 읽었는가"를 추적할 수 있는 유일한 수단이다.

---

## 4. Entra ID 로그 (테넌트 레벨)

### 4.1 Entra ID 로그의 종류

| 로그 유형 | 테이블명 | 핵심 내용 | 보존 (기본) |
|----------|---------|---------|-----------|
| **로그인 로그** | `SigninLogs` | 대화형 로그인 (사용자가 직접) | 30일 |
| **비대화형 로그인** | `AADNonInteractiveUserSignInLogs` | 토큰 갱신, 백그라운드 인증 | 30일 |
| **서비스 주체 로그인** | `AADServicePrincipalSignInLogs` | 앱/서비스 계정 로그인 | 30일 |
| **감사 로그** | `AuditLogs` | 사용자·그룹·앱 변경 | 30일 |
| **프로비저닝 로그** | `AADProvisioningLogs` | 계정 자동 생성/동기화 | 30일 |

### 4.2 로그인 로그 핵심 필드

```json
{
  "TimeGenerated": "2026-02-24T18:30:00Z",
  "UserPrincipalName": "user@example.com",
  "UserDisplayName": "홍길동",
  "AppDisplayName": "Azure Portal",
  "IPAddress": "203.0.113.42",
  "Location": "KR",
  "Status": {
    "errorCode": 0
  },
  "ConditionalAccessStatus": "success",
  "AuthenticationDetails": [
    {
      "authenticationMethod": "Password",
      "succeeded": true
    },
    {
      "authenticationMethod": "Microsoft Authenticator",
      "succeeded": true
    }
  ],
  "MfaDetail": {
    "authMethod": "PhoneAppNotification"
  },
  "DeviceDetail": {
    "browser": "Chrome 120",
    "operatingSystem": "Windows 11"
  },
  "RiskLevelDuringSignIn": "none",
  "ResultType": 0
}
```

### 4.3 KQL 실전 쿼리 모음

#### 로그인 실패 분석

```kusto
// 최근 24시간 로그인 실패 Top 10 사용자
SigninLogs
| where TimeGenerated > ago(24h)
| where ResultType != 0  // 0 = 성공
| summarize FailCount = count(),
            LastFailure = max(TimeGenerated),
            FailReasons = make_set(ResultDescription)
    by UserPrincipalName, IPAddress
| order by FailCount desc
| take 10
```

#### 비정상 위치 로그인 탐지

```kusto
// 동일 사용자가 30분 내 서로 다른 국가에서 로그인 (불가능 여행)
let timeWindow = 30m;
SigninLogs
| where TimeGenerated > ago(7d)
| where ResultType == 0
| project TimeGenerated, UserPrincipalName, Location, IPAddress
| sort by UserPrincipalName, TimeGenerated asc
| extend PrevLocation = prev(Location), PrevTime = prev(TimeGenerated),
         PrevUser = prev(UserPrincipalName)
| where UserPrincipalName == PrevUser
| where Location != PrevLocation
| where TimeGenerated - PrevTime < timeWindow
| project TimeGenerated, UserPrincipalName, PrevLocation, Location,
          TimeDiff = TimeGenerated - PrevTime
```

#### MFA 없이 로그인한 사용자

```kusto
// MFA 없이 성공한 로그인 (보안 위험)
SigninLogs
| where TimeGenerated > ago(7d)
| where ResultType == 0
| where AuthenticationRequirement == "singleFactorAuthentication"
| where AppDisplayName != "Windows Sign In"
| summarize Count = count() by UserPrincipalName, AppDisplayName
| order by Count desc
```

#### 감사 로그: 중요 변경 추적

```kusto
// 역할 할당 변경 (누가 누구에게 권한을 줬는가)
AuditLogs
| where TimeGenerated > ago(30d)
| where OperationName has "role"
| extend Initiator = tostring(InitiatedBy.user.userPrincipalName)
| extend Target = tostring(TargetResources[0].userPrincipalName)
| extend RoleName = tostring(TargetResources[0].modifiedProperties[0].newValue)
| project TimeGenerated, Initiator, OperationName, Target, RoleName
| order by TimeGenerated desc
```

---

## 5. Diagnostic Settings (진단 설정)

### 5.1 Diagnostic Settings의 역할

```
Azure 리소스의 로그는 Diagnostic Setting 없이는 아무 데도 가지 않는다!

  ┌─────────────────┐
  │  Azure 리소스     │
  │  (App Service,   │
  │   SQL DB, VM...) │
  └────────┬────────┘
           │
     Diagnostic Setting
     "어떤 로그를" + "어디로 보낼지"
           │
     ┌─────┼──────────┬──────────────┐
     ▼     ▼          ▼              ▼
  Log       Storage    Event Hub     Partner
  Analytics Account   (실시간        Solution
  Workspace (장기     스트리밍)      (Datadog,
  (분석)    아카이브)                 Splunk 등)
```

### 5.2 설정 구성 요소

```json
{
  "name": "send-all-to-law",
  "properties": {
    "logs": [
      {
        "categoryGroup": "allLogs",
        "enabled": true
      }
    ],
    "metrics": [
      {
        "category": "AllMetrics",
        "enabled": true
      }
    ],
    "workspaceId": "/subscriptions/xxx/resourcegroups/myRG/providers/Microsoft.OperationalInsights/workspaces/myWorkspace",
    "storageAccountId": "/subscriptions/xxx/resourcegroups/myRG/providers/Microsoft.Storage/storageAccounts/myArchive",
    "eventHubAuthorizationRuleId": "...",
    "eventHubName": "security-logs"
  }
}
```

### 5.3 Category Groups

| Category Group | 포함 범위 | 사용 시나리오 |
|---------------|---------|-------------|
| **allLogs** | 해당 리소스의 모든 로그 카테고리 | 개발/테스트, 전체 파악 |
| **audit** | 감사 관련 로그만 | 규정 준수, 보안 |

> ⚠️ `allLogs`를 프로덕션에서 켜면 비용이 급증할 수 있다.
> 필요한 카테고리만 선택하는 것이 비용 효율적이다.

### 5.4 Azure CLI로 Diagnostic Setting 생성

```bash
# App Service에 Diagnostic Setting 추가
az monitor diagnostic-settings create \
  --name "appservice-to-law" \
  --resource "/subscriptions/xxx/resourceGroups/myRG/providers/Microsoft.Web/sites/myApp" \
  --workspace "/subscriptions/xxx/resourceGroups/myRG/providers/Microsoft.OperationalInsights/workspaces/myWorkspace" \
  --logs '[
    {"categoryGroup": "allLogs", "enabled": true}
  ]' \
  --metrics '[
    {"category": "AllMetrics", "enabled": true}
  ]'
```

```bash
# Activity Log를 Log Analytics로 전송
az monitor diagnostic-settings create \
  --name "activity-to-law" \
  --resource "/subscriptions/xxx" \
  --workspace "/subscriptions/xxx/resourceGroups/myRG/providers/Microsoft.OperationalInsights/workspaces/myWorkspace" \
  --logs '[
    {"category": "Administrative", "enabled": true},
    {"category": "Security", "enabled": true},
    {"category": "ServiceHealth", "enabled": true},
    {"category": "Alert", "enabled": true},
    {"category": "Policy", "enabled": true}
  ]'
```

---

## 6. Log Analytics Workspace

### 6.1 Log Analytics Workspace란?

**Log Analytics Workspace**는 Azure의 중앙 로그 저장소이자 분석 엔진이다. 내부적으로 **Azure Data Explorer (Kusto)** 기반이며, **KQL (Kusto Query Language)** 로 데이터를 분석한다.

```
  Activity Log ─────┐
  Resource Logs ────┤
  Entra ID Logs ────┼──▶ Log Analytics Workspace
  VM Insights ──────┤        │
  App Insights ─────┘        ├── KQL로 분석
                              ├── Azure Workbooks (대시보드)
                              ├── 알림 규칙 (Alert Rules)
                              └── Microsoft Sentinel (SIEM)
```

### 6.2 KQL 기초 문법

```kusto
// KQL은 파이프(|)로 연결하는 데이터 흐름 언어

SigninLogs                          // ① 테이블 선택
| where TimeGenerated > ago(24h)    // ② 필터링 (시간)
| where ResultType != 0            // ③ 필터링 (조건)
| summarize Count = count()         // ④ 집계
    by UserPrincipalName            //    그룹화 기준
| order by Count desc               // ⑤ 정렬
| take 10                           // ⑥ 상위 N개
```

**핵심 연산자:**

| 연산자 | 역할 | 예시 |
|--------|------|------|
| `where` | 행 필터링 | `where Level == "Error"` |
| `project` | 열 선택 | `project TimeGenerated, Message` |
| `summarize` | 집계 | `summarize count() by Category` |
| `extend` | 열 추가 | `extend Duration = EndTime - StartTime` |
| `order by` | 정렬 | `order by TimeGenerated desc` |
| `take` / `limit` | 행 수 제한 | `take 100` |
| `join` | 테이블 결합 | `T1 \| join T2 on UserId` |
| `render` | 차트 생성 | `render timechart` |
| `ago()` | 상대 시간 | `ago(1h)`, `ago(7d)` |
| `has` / `contains` | 문자열 검색 | `where Message has "error"` |

### 6.3 비용 관리

```
Log Analytics 비용 구조:

  ┌────────────────┬──────────────────────────────────────┐
  │  비용 요소       │  설명                                 │
  ├────────────────┼──────────────────────────────────────┤
  │ 데이터 수집      │ 수집된 GB당 과금                       │
  │                 │ Activity Log 수집은 무료!              │  
  │                 │ 처음 5GB/월 무료 (Basic Log 제외)       │
  ├────────────────┼──────────────────────────────────────┤
  │ 데이터 보존      │ 기본 30일 무료                          │
  │                 │ 이후 31~730일은 GB/월 과금               │
  │                 │ Activity Log는 90일까지 무료             │
  ├────────────────┼──────────────────────────────────────┤
  │ 분석 (쿼리)      │ Analytics Logs: 쿼리 무료              │
  │                 │ Basic Logs: 스캔한 GB당 과금            │
  └────────────────┴──────────────────────────────────────┘

비용 최적화 전략:
  ├── 볼륨 큰 로그는 Basic Logs 티어로 전환 (수집 비용 70% 절감)
  ├── 불필요한 로그 카테고리 비활성화
  ├── 장기 보관은 Storage Account로 (Archive 티어)
  ├── Commitment Tier (100GB/일 이상) 사용 시 할인
  └── 데이터 수집 규칙(DCR)으로 수집 전 필터링
```

---

## 7. 실전 설계 패턴

### 7.1 워크스페이스 설계 전략

```
패턴 1: 단일 워크스페이스 (소규모)
  ├── 모든 리소스의 로그를 하나의 Workspace에
  ├── 장점: 간단, 상관관계 분석 용이
  └── 적합: 팀 1~3개, 리소스 50개 미만

패턴 2: 환경별 분리 (중규모)
  ├── Production Workspace
  ├── Staging Workspace
  ├── Dev Workspace
  ├── 장점: 환경 간 데이터 격리, 비용 추적 용이
  └── 적합: 팀 3~10개

패턴 3: 기능별 분리 (대규모)
  ├── Operations Workspace (운영 로그)
  ├── Security Workspace (보안 로그) → Sentinel 연동
  ├── Application Workspace (앱 로그)
  ├── 장점: 접근 제어 세분화, 보안팀 독립 분석
  └── 적합: 대기업, 규제 산업
```

### 7.2 필수 설정 체크리스트

```
□ Activity Log → Log Analytics Workspace 전송 설정 (무료!!)
□ 모든 프로덕션 리소스에 Diagnostic Setting 생성
□ Key Vault AuditEvent 활성화 (비밀 접근 추적)
□ Entra ID 로그 → Log Analytics 전송 (P1/P2 라이선스 필요)
□ AKS kube-audit 활성화 (K8s API 감사)
□ SQL Database 감사 활성화 (SQLSecurityAuditEvents)
□ 보존 기간을 규정에 맞게 설정
□ 비용 알림 설정 (일일 수집량 모니터링)
□ 핵심 알림 규칙 생성 (5xx 에러, 로그인 실패 급증)
□ 장기 아카이브용 Storage Account 연결
```

---

## 정리

```
Azure 리소스 로그:
│
├── Azure Monitor: 로그·메트릭·트레이스 통합 플랫폼
│
├── 3계층 로그:
│   ├── 테넌트: Entra ID (로그인/감사/프로비저닝)
│   ├── 구독: Activity Log (Control Plane, ARM 작업)
│   └── 리소스: Resource Logs (Data Plane, 서비스별 상이)
│
├── Activity Log:
│   ├── 8개 카테고리 (Administrative, Security, Policy 등)
│   ├── 기본 90일 보존, Diagnostic Setting으로 연장
│   └── Log Analytics 수집 무료!
│
├── Resource Logs:
│   ├── ⚠️ Diagnostic Setting 없으면 수집 안 됨!
│   ├── 서비스별 카테고리 상이
│   └── App Service / SQL / AKS / Key Vault 등
│
├── Entra ID 로그:
│   ├── SigninLogs (대화형/비대화형/서비스주체)
│   ├── AuditLogs (변경 추적)
│   └── KQL로 불가능 여행, MFA 미적용 탐지
│
├── Diagnostic Settings:
│   ├── 4가지 목적지: Log Analytics / Storage / Event Hub / Partner
│   ├── categoryGroup으로 간편 설정 (allLogs, audit)
│   └── Azure CLI / Bicep / Terraform으로 자동화
│
└── Log Analytics Workspace:
    ├── KQL (Kusto Query Language)로 분석
    ├── 비용: 수집(GB당) + 보존(30일 이후) + 쿼리(Basic만)
    └── 워크스페이스 설계: 단일 / 환경별 / 기능별
```
