# Azure Container Apps 완전 정리

**서버리스 컨테이너** 실행 환경 — Kubernetes처럼 Docker 컨테이너를 실행하지만, 인프라를 직접 관리하지 않습니다.

---

## Container Apps란?

```
VM              vs    AKS (K8s)         vs    Container Apps
├── 직접 관리        ├── K8s 지식 필요       ├── 서버리스
├── OS 패치 필요     ├── 클러스터 관리        ├── 자동 스케일링
└── 높은 제어권      └── 높은 유연성          └── 단순한 설정
```

Log-Doctor에서는 **FastAPI 백엔드를 Container App에 배포**합니다.

---

## 핵심 개념

### Environment (환경)

```
Container Apps Environment (shared)
├── Container App 1 (log-doctor-api)
├── Container App 2 (worker)     ← 같은 가상 네트워크 공유
└── Log Analytics Workspace      ← 로그 자동 전송
```

- 여러 Container App이 **같은 환경을 공유**
- 같은 환경 내 앱들은 **같은 VNet**에서 통신
- 환경당 하나의 **Log Analytics Workspace** 연결

### Container App 구조

```
Container App
├── Configuration (설정)
│   ├── Ingress (HTTP 트래픽 설정)
│   ├── Secrets (환경 변수 비밀)
│   ├── Registries (ACR 연결)
│   └── Scale Rules (스케일링 규칙)
│
└── Template (앱 정의)
    ├── Containers (컨테이너 이미지)
    ├── Init Containers (초기화 컨테이너)
    ├── Volumes (파일 저장소)
    └── Revision (불변 스냅샷)
```

---

## Revision (리비전) 개념

```
v1 (Revision 1) ─── 100% 트래픽
     │
     │ 새 이미지 배포
     ▼
v1 (Revision 1) ─── 80% 트래픽
v2 (Revision 2) ─── 20% 트래픽   ← 카나리 배포!
     │
     │ 검증 완료
     ▼
v2 (Revision 2) ─── 100% 트래픽
v1 (Revision 1) ─── 비활성화
```

- **Revision**: 컨테이너 앱의 **불변 스냅샷**
- 이미지나 환경변수 변경 시 **새 Revision 생성**
- **트래픽 분할**로 카나리/블루-그린 배포 가능

### Revision 모드

| 모드 | 동작 | 사용 시나리오 |
|------|------|-------------|
| **Single** | 최신 Revision만 활성 | 일반적인 배포 |
| **Multiple** | 여러 Revision 동시 활성 | 카나리/A-B 테스트 |

---

## Ingress (트래픽 설정)

```
인터넷                        Container Apps Environment
  │                          ┌─────────────────────────────┐
  │   external: true         │                             │
  ├─── HTTPS ──────────────→ │  Container App (API)        │
  │   port: 8000             │  ├── Container 1            │
  │                          │  └── Container 2 (replica)  │
  │                          │                             │
  │   external: false        │  Container App (Worker)     │
  │   (내부만 접근 가능)      │  └── 환경 내에서만 접근     │
  │                          └─────────────────────────────┘
```

| 속성 | 설명 | 예시 |
|------|------|------|
| `external` | 외부 노출 여부 | `true` = 인터넷 접근 가능 |
| `targetPort` | 컨테이너 포트 | `8000` (FastAPI 기본) |
| `transport` | 프로토콜 | `http`, `http2`, `tcp` |
| `allowInsecure` | HTTP 허용 | `false` (HTTPS만) |

---

## 스케일링 (Auto Scale)

### 스케일링 규칙

```
0 ← 최소 (비용 절감, 콜드 스타트 있음)
│
├── HTTP 요청수 기반: 동시 요청 10개당 +1 replica
├── CPU 기반: 70% 초과 시 +1 replica
├── 메모리 기반: 80% 초과 시 +1 replica
├── KEDA 기반: 큐 메시지, 이벤트 등
│
10 ← 최대
```

### Bicep 스케일링 설정

```bicep
scale: {
  minReplicas: 0    // 0 = 트래픽 없으면 완전 종료 (비용 $0)
  maxReplicas: 10
  rules: [
    {
      name: 'http-rule'
      http: {
        metadata: {
          concurrentRequests: '10'  // 동시 요청 10개당 +1
        }
      }
    }
  ]
}
```

> **주의**: `minReplicas: 0`이면 첫 요청 시 **콜드 스타트** (수 초 지연)

---

## 환경 변수 & 비밀

### 일반 환경 변수

```bicep
env: [
  { name: 'APP_ENV', value: 'production' }
  { name: 'LOG_LEVEL', value: 'info' }
]
```

### Secret 참조

```bicep
// 1. Container App에 Secret 등록
configuration: {
  secrets: [
    { name: 'cosmos-connection', value: cosmosConnectionString }
    { name: 'client-secret', value: clientSecret }
  ]
}

// 2. 컨테이너에서 참조
template: {
  containers: [
    {
      env: [
        { name: 'COSMOS_URL', secretRef: 'cosmos-connection' }
        { name: 'CLIENT_SECRET', secretRef: 'client-secret' }
      ]
    }
  ]
}
```

### Key Vault 참조 (권장)

```bicep
// Secret을 직접 넣지 않고 Key Vault 참조
configuration: {
  secrets: [
    {
      name: 'cosmos-key'
      keyVaultUrl: 'https://logdoctorvault.vault.azure.net/secrets/cosmos-key'
      identity: managedIdentity.id
    }
  ]
}
```

---

## Bicep 전체 배포 예시

```bicep
param location string = resourceGroup().location
param envName string = 'log-doctor-env'
param appName string = 'log-doctor-api'
param acrName string = 'logdoctorcr'
param imageName string = 'log-doctor-api'
param imageTag string = 'v1.0.0'

// 1. Container Apps Environment
resource appEnv 'Microsoft.App/managedEnvironments@2023-05-01' = {
  name: envName
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

// 2. Container App
resource app 'Microsoft.App/containerApps@2023-05-01' = {
  name: appName
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentity.id}': {}
    }
  }
  properties: {
    managedEnvironmentId: appEnv.id
    configuration: {
      // ACR 연결
      registries: [
        {
          server: '${acrName}.azurecr.io'
          identity: managedIdentity.id
        }
      ]
      // HTTP 설정
      ingress: {
        external: true
        targetPort: 8000
        transport: 'http'
        allowInsecure: false
      }
      // 비밀
      secrets: [
        { name: 'cosmos-key', value: cosmosKey }
      ]
    }
    template: {
      containers: [
        {
          name: appName
          image: '${acrName}.azurecr.io/${imageName}:${imageTag}'
          resources: {
            cpu: json('0.5')     // 0.25, 0.5, 0.75, 1.0, ...
            memory: '1Gi'        // 0.5Gi, 1Gi, 1.5Gi, 2Gi, ...
          }
          env: [
            { name: 'APP_ENV', value: 'production' }
            { name: 'COSMOS_KEY', secretRef: 'cosmos-key' }
          ]
        }
      ]
      scale: {
        minReplicas: 1       // 프로덕션은 1 이상 권장
        maxReplicas: 5
      }
    }
  }
}

output appUrl string = 'https://${app.properties.configuration.ingress.fqdn}'
```

---

## 리소스 할당 가이드

| CPU | 메모리 | 사용 사례 |
|-----|--------|----------|
| 0.25 vCPU | 0.5 Gi | 경량 API, 헬스체크 |
| **0.5 vCPU** | **1 Gi** | **일반 API (Log-Doctor)** |
| 1 vCPU | 2 Gi | 데이터 처리 |
| 2 vCPU | 4 Gi | ML 추론, 대용량 처리 |

> CPU와 메모리는 **조합 제한**이 있음: 0.25 CPU → 최대 0.5Gi

---

## 로그 확인

### Azure Portal

```
Container App → Monitoring → Log stream (실시간)
Container App → Monitoring → Logs (KQL 쿼리)
```

### CLI

```bash
# 실시간 로그 스트리밍
az containerapp logs show \
  --name log-doctor-api \
  --resource-group log-doctor-rg \
  --follow

# 시스템 로그 (스케일링, 에러 등)
az containerapp logs show \
  --name log-doctor-api \
  --resource-group log-doctor-rg \
  --type system
```

### KQL (Log Analytics에서)

```kql
// 최근 에러 로그
ContainerAppConsoleLogs_CL
| where ContainerAppName_s == "log-doctor-api"
| where Log_s contains "ERROR"
| order by TimeGenerated desc
| take 50

// 스케일링 이벤트
ContainerAppSystemLogs_CL
| where Type_s == "Microsoft.App/managedEnvironments"
| where Reason_s == "ScaledUp" or Reason_s == "ScaledDown"
| order by TimeGenerated desc
```

---

## Container Apps vs 다른 서비스

| 기능 | Container Apps | App Service | AKS |
|------|---------------|-------------|-----|
| 복잡도 | ⭐ 낮음 | ⭐ 낮음 | ⭐⭐⭐ 높음 |
| Docker 지원 | ✅ | ✅ | ✅ |
| 0으로 스케일링 | ✅ | ❌ | ✅ (KEDA) |
| 카나리 배포 | ✅ | ❌ (슬롯 사용) | ✅ |
| Dapr 지원 | ✅ | ❌ | ✅ |
| 비용 | **사용량 기반** | 고정 | 클러스터 비용 |
| K8s 지식 | 불필요 | 불필요 | 필요 |

> Log-Doctor는 **Container Apps**가 최적: 서버리스 + Docker + 자동 스케일링 + 간단한 설정

---

## 트러블슈팅

### 1. 앱 시작 실패

```
ContainerAppProvisioningState: Failed
Reason: ContainerCrashing
```

```bash
# 로그 확인
az containerapp logs show --name log-doctor-api -g log-doctor-rg

# 흔한 원인:
# 1. targetPort가 앱의 실제 포트와 불일치
# 2. 환경변수 누락 (COSMOS_URL 등)
# 3. 이미지 pull 실패 (ACR 인증)
```

### 2. 콜드 스타트

```
# minReplicas: 0이면 첫 요청이 오래 걸림
# 해결: 프로덕션은 minReplicas: 1
scale: {
  minReplicas: 1  // 항상 1개는 실행
}
```

### 3. CORS 에러

```python
# FastAPI에서 CORS 설정 필요 (Container App이 아닌 앱 코드에서)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.BACKEND_CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

### 4. HTTPS 리다이렉트 문제

```python
# Container App 뒤에서 실행 시 X-Forwarded-Proto 신뢰 필요
# FastAPI: uvicorn에 --proxy-headers 추가
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers"]
```
