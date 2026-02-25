# Log Analytics Workspace 완전 정리

Azure Monitor의 로그 데이터를 **중앙에서 수집/저장/분석**하는 서비스입니다.

---

## Log Analytics란?

```
Azure 리소스들                Log Analytics Workspace           분석
├── Container App      →    ┌─────────────────────────┐     ├── KQL 쿼리
├── Function App       →    │  로그 저장소 (테이블)       │  →  ├── 대시보드
├── Key Vault          →    │  ├── ContainerAppLogs    │     ├── 알림 규칙
├── Cosmos DB          →    │  ├── FunctionAppLogs     │     └── Workbook
└── 진단 설정            →    │  └── AzureActivity       │
                            └─────────────────────────┘
```

Log-Doctor에서는 **고객 Azure 리소스의 로그를 수집**하여 분석합니다.

---

## 핵심 개념

### 테이블 구조

```
Log Analytics Workspace
├── 기본 테이블 (Azure 자동 생성)
│   ├── AzureActivity          ← 리소스 생성/삭제/수정 이벤트
│   ├── AzureDiagnostics       ← 리소스 진단 로그
│   ├── AzureMetrics           ← 리소스 메트릭
│   ├── Heartbeat              ← 에이전트 상태 체크
│   └── Usage                  ← 사용량 데이터
│
├── 솔루션 테이블
│   ├── ContainerAppConsoleLogs_CL    ← Container App 콘솔
│   ├── ContainerAppSystemLogs_CL     ← Container App 시스템
│   └── AppExceptions                ← Application Insights 예외
│
└── 커스텀 테이블
    └── LogDoctorMetrics_CL          ← DCR로 수집한 커스텀 로그
```

### 데이터 보존

| 보존 기간 | 비용 | 사용 시나리오 |
|-----------|------|-------------|
| 30일 | 기본 무료 | 개발/테스트 |
| 90일 | 저렴 | 일반 운영 |
| 365일 | 보통 | 컴플라이언스 |
| 730일 (최대) | 비쌈 | 감사/규제 |

> **비용 팁**: Interactive 보존 (빠른 쿼리) + Archive 보존 (저비용 장기) 조합

---

## KQL (Kusto Query Language)

Log Analytics의 **쿼리 언어**. SQL과 비슷하지만 **파이프라인 기반**.

### 기본 문법

```kql
// SQL: SELECT * FROM logs WHERE level = 'error' ORDER BY time DESC LIMIT 10
// KQL:
AzureDiagnostics
| where Level == "Error"
| order by TimeGenerated desc
| take 10
```

### 자주 쓰는 연산자

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `where` | 필터링 | `where Level == "Error"` |
| `project` | 컬럼 선택 | `project TimeGenerated, Message` |
| `summarize` | 집계 | `summarize count() by Level` |
| `order by` | 정렬 | `order by TimeGenerated desc` |
| `take` | N개 제한 | `take 100` |
| `extend` | 컬럼 추가 | `extend Hour = bin(TimeGenerated, 1h)` |
| `join` | 테이블 조인 | `join kind=inner OtherTable on Key` |
| `render` | 시각화 | `render timechart` |

### 실전 KQL 예시

#### Container App 에러 분석

```kql
// 최근 24시간 에러 로그
ContainerAppConsoleLogs_CL
| where TimeGenerated > ago(24h)
| where Log_s contains "ERROR" or Log_s contains "Exception"
| project TimeGenerated, ContainerAppName_s, Log_s
| order by TimeGenerated desc
```

#### 시간대별 요청 수

```kql
// 1시간 단위 요청 수 차트
ContainerAppConsoleLogs_CL
| where TimeGenerated > ago(7d)
| where Log_s contains "HTTP"
| summarize RequestCount = count() by bin(TimeGenerated, 1h)
| render timechart
```

#### 에러율 계산

```kql
// 시간대별 에러율
ContainerAppConsoleLogs_CL
| where TimeGenerated > ago(24h)
| extend IsError = iff(Log_s contains "ERROR", 1, 0)
| summarize
    Total = count(),
    Errors = sum(IsError),
    ErrorRate = round(100.0 * sum(IsError) / count(), 2)
  by bin(TimeGenerated, 1h)
| order by TimeGenerated desc
```

#### Azure 활동 로그 (누가 뭘 했나)

```kql
AzureActivity
| where TimeGenerated > ago(7d)
| where OperationNameValue contains "Microsoft.App"
| project TimeGenerated, Caller, OperationNameValue, ActivityStatusValue
| order by TimeGenerated desc
```

#### 리소스 비용 분석

```kql
// 테이블별 데이터 수집량 (비용 최적화)
Usage
| where TimeGenerated > ago(30d)
| summarize DataGB = sum(Quantity) / 1024 by DataType
| order by DataGB desc
```

---

## Bicep 배포

```bicep
param location string = resourceGroup().location
param workspaceName string = 'log-doctor-logs'

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: workspaceName
  location: location
  properties: {
    sku: {
      name: 'PerGB2018'     // 사용량 기반 과금
    }
    retentionInDays: 90     // 30, 60, 90, 120, ..., 730
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true  // 리소스 RBAC
    }
  }
}

// Container App Environment에 연결
resource appEnv 'Microsoft.App/managedEnvironments@2023-05-01' = {
  name: 'log-doctor-env'
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
  }
}

output workspaceId string = logAnalytics.id
output customerId string = logAnalytics.properties.customerId
```

---

## 진단 설정 연결

```bicep
// Key Vault 로그를 Log Analytics로 전송
resource kvDiagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'kv-to-logs'
  scope: keyVault
  properties: {
    workspaceId: logAnalytics.id
    logs: [
      {
        category: 'AuditEvent'
        enabled: true
        retentionPolicy: { enabled: false }
      }
    ]
    metrics: [
      {
        category: 'AllMetrics'
        enabled: true
      }
    ]
  }
}
```

---

## 알림 규칙 (Alert Rules)

```
KQL 쿼리 → 조건 충족 → 알림 발송
                        ├── 이메일
                        ├── SMS
                        ├── Webhook
                        └── Azure Function
```

```bicep
// 에러 5개 이상이면 알림
resource alertRule 'Microsoft.Insights/scheduledQueryRules@2022-06-15' = {
  name: 'high-error-rate'
  location: location
  properties: {
    severity: 1  // 0(Critical) ~ 4(Verbose)
    evaluationFrequency: 'PT5M'   // 5분마다 체크
    windowSize: 'PT15M'           // 15분 윈도우
    criteria: {
      allOf: [
        {
          query: '''
            ContainerAppConsoleLogs_CL
            | where Log_s contains "ERROR"
            | summarize ErrorCount = count()
          '''
          threshold: 5
          operator: 'GreaterThan'
          timeAggregation: 'Count'
        }
      ]
    }
    actions: {
      actionGroups: [ actionGroup.id ]
    }
  }
}
```

---

## 비용 최적화

### 데이터 수집 줄이기

```kql
// 어떤 테이블이 가장 많은 데이터를 수집하는지 확인
Usage
| where TimeGenerated > ago(30d)
| where IsBillable == true
| summarize DataGB = sum(Quantity) / 1024 by DataType
| order by DataGB desc
```

### 비용 절감 전략

| 전략 | 효과 | 방법 |
|------|------|------|
| 불필요한 로그 끄기 | 높음 | 진단 설정에서 카테고리 선택 |
| 보존 기간 줄이기 | 높음 | 90일 → 30일 |
| Archive 계층 사용 | 보통 | Interactive → Archive |
| 데이터 변환 | 보통 | DCR에서 필터링 후 수집 |
| 커밋먼트 계층 | 높음 | 일일 100GB+ 시 할인 |

---

## DCR (Data Collection Rule)

Log-Doctor의 핵심 — **어떤 데이터를 어떻게 수집할지** 정의합니다.

```
Azure 리소스                DCR (Data Collection Rule)         Workspace
├── VM 로그          →     ┌──────────────────────────┐    →  테이블에 저장
├── 앱 로그          →     │ 1. 데이터 소스 정의       │
├── 커스텀 로그      →     │ 2. 변환 (KQL로 필터링)    │
                           │ 3. 대상 지정              │
                           └──────────────────────────┘
```

```bicep
resource dcr 'Microsoft.Insights/dataCollectionRules@2022-06-01' = {
  name: 'log-doctor-dcr'
  location: location
  properties: {
    dataSources: {
      // 수집할 데이터 소스 정의
    }
    destinations: {
      logAnalytics: [
        {
          workspaceResourceId: logAnalytics.id
          name: 'workspace'
        }
      ]
    }
    dataFlows: [
      {
        streams: [ 'Custom-LogDoctorMetrics_CL' ]
        destinations: [ 'workspace' ]
        transformKql: 'source | where Level != "Debug"'  // Debug 로그 제외
        outputStream: 'Custom-LogDoctorMetrics_CL'
      }
    ]
  }
}
```

---

## 트러블슈팅

### 1. 쿼리 결과가 안 나옴

```bash
# 원인 1: 데이터 수집 지연 (최대 5분)
# → 잠시 기다리기

# 원인 2: 진단 설정 미연결
az monitor diagnostic-settings list --resource <RESOURCE_ID>

# 원인 3: 테이블 이름 오타
# "_CL" 접미사 (Custom Log) 확인
```

### 2. 비용 급증

```kql
// 일별 수집량 확인
Usage
| where TimeGenerated > ago(30d)
| where IsBillable == true
| summarize DailyGB = sum(Quantity) / 1024 by bin(TimeGenerated, 1d)
| render columnchart
```

### 3. 권한 문제

```bash
# Log Analytics Reader 역할 필요
az role assignment create \
  --assignee <USER_OR_MI_ID> \
  --role "Log Analytics Reader" \
  --scope <WORKSPACE_ID>
```

---

## Log-Doctor에서의 활용

```
고객 Azure 구독                    Log Analytics
├── VM 진단 로그          →       ├── 로그 수집
├── Container App 로그    →       ├── KQL로 분석
├── Key Vault 감사 로그   →       ├── 이상 감지
└── Activity 로그         →       └── 리포트 생성 → Teams 앱에 표시
```

1. **에이전트**: 고객 구독에 DCR을 배포하여 로그 수집 설정
2. **분석**: KQL로 로그 패턴 분석, 비용 절감 포인트 발견
3. **리포트**: 분석 결과를 Teams 앱 대시보드에 표시
4. **알림**: 이상 감지 시 자동 알림
