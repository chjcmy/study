# 진단 설정 (Diagnostic Settings) 완전 정리

Azure 리소스의 **로그와 메트릭을 외부 대상으로 전송**하는 설정입니다.

---

## 진단 설정이란?

```
Azure 리소스 (Key Vault, App Service, ...)
    │
    │ 진단 설정 (Diagnostic Settings)
    │
    ├──▶ Log Analytics Workspace  (분석/쿼리)
    ├──▶ Storage Account          (장기 보관/아카이브)
    └──▶ Event Hub                (외부 SIEM/스트리밍)
```

> 모든 Azure 리소스는 자체적으로 **진단 로그**와 **메트릭**을 생성하지만,  
> **진단 설정을 만들어야** 이 데이터를 저장/분석할 수 있습니다.

---

## 로그 vs 메트릭

| 항목 | 로그 (Logs) | 메트릭 (Metrics) |
|------|-------------|-----------------|
| 형태 | 이벤트 기록 (텍스트) | 숫자 값 (시계열) |
| 예시 | "user@email이 비밀 읽기 시도" | "CPU 사용률 73%" |
| 분석 | KQL 쿼리 | 차트/대시보드 |
| 보존 | 설정에 따름 (30~730일) | 93일 (기본) |
| 비용 | 데이터 양에 비례 | 대부분 무료 |

---

## 전송 대상 비교

| 대상 | 용도 | 비용 | 보존 기간 |
|------|------|------|----------|
| **Log Analytics** | 실시간 분석, KQL 쿼리, 알림 | GB당 과금 | 30~730일 |
| **Storage Account** | 장기 보관, 규정 준수, 감사 | 저렴 | 무제한 |
| **Event Hub** | 외부 SIEM(Splunk 등), 실시간 스트리밍 | 처리량 과금 | 설정에 따름 |

> **추천:** 대부분의 경우 **Log Analytics** 하나면 충분  
> 규정 준수가 필요하면 **Storage Account** 추가

---

## 리소스별 로그 카테고리

### Key Vault

| 카테고리 | 설명 |
|---------|------|
| `AuditEvent` | 모든 접근 요청 (성공/실패) |
| `AzurePolicyEvaluationDetails` | Azure Policy 평가 결과 |

### Container Apps

| 카테고리 | 설명 |
|---------|------|
| `ContainerAppConsoleLogs` | 컨테이너 stdout/stderr |
| `ContainerAppSystemLogs` | 시스템 이벤트 (스케일링, 재시작) |

### App Service

| 카테고리 | 설명 |
|---------|------|
| `AppServiceHTTPLogs` | HTTP 요청/응답 로그 |
| `AppServiceConsoleLogs` | 앱 콘솔 출력 |
| `AppServiceAppLogs` | 앱 로그 |
| `AppServiceAuditLogs` | FTP/배포 감사 로그 |
| `AppServicePlatformLogs` | 플랫폼 이벤트 |

### Azure Functions

| 카테고리 | 설명 |
|---------|------|
| `FunctionAppLogs` | 함수 실행 로그 |

### Storage Account

| 카테고리 | 설명 |
|---------|------|
| `StorageRead` | 읽기 작업 |
| `StorageWrite` | 쓰기 작업 |
| `StorageDelete` | 삭제 작업 |

### SQL Database

| 카테고리 | 설명 |
|---------|------|
| `SQLSecurityAuditEvents` | 보안 감사 |
| `QueryStoreRuntimeStatistics` | 쿼리 성능 |

---

## Bicep 코드

### 기본 패턴 — Key Vault 진단 설정

```bicep
// 기존 리소스 참조
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' existing = {
  name: keyVaultName
}

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' existing = {
  name: logAnalyticsName
}

// 진단 설정
resource kvDiagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'diag-${keyVault.name}'
  scope: keyVault  // ⚠️ 어떤 리소스에 대한 설정인지
  properties: {
    workspaceId: logAnalytics.id  // Log Analytics로 전송
    logs: [
      {
        categoryGroup: 'allLogs'    // 모든 로그
        enabled: true
        retentionPolicy: {
          enabled: true
          days: 30
        }
      }
    ]
    metrics: [
      {
        category: 'AllMetrics'      // 모든 메트릭
        enabled: true
        retentionPolicy: {
          enabled: true
          days: 30
        }
      }
    ]
  }
}
```

### ⚠️ 핵심: scope 속성

```bicep
// ✅ scope — 진단 설정의 대상 리소스를 지정
resource diagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'my-diag'
  scope: keyVault  // Key Vault의 진단 로그를 수집
}

// ❌ scope가 없으면 → 리소스 그룹 수준 진단 설정이 됨 (의도와 다름)
```

### 개별 카테고리 지정

```bicep
logs: [
  {
    category: 'AuditEvent'  // 특정 카테고리만
    enabled: true
  }
  {
    category: 'AzurePolicyEvaluationDetails'
    enabled: false  // 불필요한 카테고리 비활성화
  }
]
```

### categoryGroup vs category

| 방식 | 설명 | 예시 |
|------|------|------|
| `categoryGroup` | 카테고리 그룹 전체 | `allLogs`, `audit` |
| `category` | 개별 카테고리 | `AuditEvent`, `ContainerAppConsoleLogs` |

> **주의:** `categoryGroup`과 `category`를 동시에 사용하면 에러!

### 여러 대상으로 전송

```bicep
resource diagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'diag-multi-target'
  scope: keyVault
  properties: {
    workspaceId: logAnalytics.id           // Log Analytics
    storageAccountId: storageAccount.id    // Storage Account
    eventHubAuthorizationRuleId: eventHubRuleId  // Event Hub
    eventHubName: eventHubName
    logs: [
      { categoryGroup: 'allLogs', enabled: true }
    ]
  }
}
```

### 모듈화 — 재사용 가능한 진단 설정 모듈

```bicep
// modules/diagnostic-settings.bicep
@description('대상 리소스 ID')
param targetResourceId string

@description('Log Analytics Workspace ID')
param workspaceId string

@description('설정 이름')
param settingName string = 'default-diagnostics'

resource diagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: settingName
  scope: /* 동적으로 지정 불가 — 리소스별 모듈 필요 */
  properties: {
    workspaceId: workspaceId
    logs: [
      { categoryGroup: 'allLogs', enabled: true }
    ]
    metrics: [
      { category: 'AllMetrics', enabled: true }
    ]
  }
}
```

> **주의:** `scope`는 리소스 참조만 가능하므로, 리소스 타입별로 모듈을 만드는 것이 일반적

---

## Data Collection Rule (DCR) — 차세대 수집 방법

진단 설정의 한계를 보완한 **새로운 데이터 수집 방식**입니다.

### 진단 설정 vs DCR

| 항목 | 진단 설정 | DCR |
|------|----------|-----|
| 세대 | 1세대 | 2세대 (최신) |
| 필터링 | 카테고리 ON/OFF만 | KQL 변환 가능 |
| 비용 최적화 | 제한적 | 불필요 데이터 필터링 |
| 대상 리소스 | Azure 리소스만 | VM, Arc, 커스텀 앱 |
| 설정 방식 | 리소스별 개별 설정 | 규칙 기반 중앙 관리 |

### DCR Bicep 예시

```bicep
resource dcr 'Microsoft.Insights/dataCollectionRules@2022-06-01' = {
  name: 'dcr-${appName}'
  location: location
  properties: {
    dataSources: {
      performanceCounters: [
        {
          name: 'perfCounter'
          streams: ['Microsoft-Perf']
          samplingFrequencyInSeconds: 60
          counterSpecifiers: [
            '\\Processor(_Total)\\% Processor Time'
            '\\Memory\\Available Bytes'
          ]
        }
      ]
    }
    destinations: {
      logAnalytics: [
        {
          workspaceResourceId: logAnalytics.id
          name: 'logAnalyticsDest'
        }
      ]
    }
    dataFlows: [
      {
        streams: ['Microsoft-Perf']
        destinations: ['logAnalyticsDest']
        transformKql: 'source | where CounterName == "% Processor Time"'
      }
    ]
  }
}
```

---

## CLI 명령어

### 진단 설정 관리

```bash
# 진단 설정 생성 (Key Vault → Log Analytics)
az monitor diagnostic-settings create \
  --name diag-kv \
  --resource /subscriptions/{subId}/resourceGroups/{rg}/providers/Microsoft.KeyVault/vaults/{kvName} \
  --workspace /subscriptions/{subId}/resourceGroups/{rg}/providers/Microsoft.OperationalInsights/workspaces/{lawName} \
  --logs '[{"categoryGroup": "allLogs", "enabled": true}]' \
  --metrics '[{"category": "AllMetrics", "enabled": true}]'

# 진단 설정 조회
az monitor diagnostic-settings list \
  --resource /subscriptions/{subId}/resourceGroups/{rg}/providers/Microsoft.KeyVault/vaults/{kvName}

# 진단 설정 삭제
az monitor diagnostic-settings delete \
  --name diag-kv \
  --resource <리소스ID>

# 사용 가능한 로그 카테고리 확인
az monitor diagnostic-settings categories list \
  --resource <리소스ID> -o table
```

---

## KQL (Kusto Query Language) 기본

진단 로그를 **Log Analytics에서 쿼리**하는 언어입니다.

```kusto
// Key Vault 접근 로그 조회
AzureDiagnostics
| where ResourceProvider == "MICROSOFT.KEYVAULT"
| where OperationName == "SecretGet"
| project TimeGenerated, CallerIPAddress, OperationName, ResultType
| order by TimeGenerated desc
| take 50

// 실패한 접근 시도
AzureDiagnostics
| where ResourceProvider == "MICROSOFT.KEYVAULT"
| where ResultType != "Success"
| summarize count() by OperationName, ResultType
| order by count_ desc

// Container App 로그
ContainerAppConsoleLogs_CL
| where ContainerAppName_s == "my-app"
| project TimeGenerated, Log_s
| order by TimeGenerated desc
| take 100

// 시간별 요청 수
AzureDiagnostics
| where TimeGenerated > ago(24h)
| summarize count() by bin(TimeGenerated, 1h)
| render timechart
```

---

## Best Practice

1. **모든 프로덕션 리소스에 진단 설정** — 최소 Key Vault, App Service, DB
2. **Log Analytics 중앙화** — 하나의 Workspace에 모든 로그 통합
3. **보존 기간 설정** — 개발: 30일, 프로덕션: 90일+
4. **규정 준수 시 Storage 추가** — 장기 보관용
5. **불필요 로그 제외** — 비용 절감을 위해 필요한 카테고리만 활성화
6. **알림 규칙 설정** — 비정상 접근, 에러 급증 시 알림
7. **Azure Policy로 자동화** — 새 리소스에 자동으로 진단 설정 적용
