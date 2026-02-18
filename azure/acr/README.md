# Azure Container Registry (ACR) 완전 정리

Docker 컨테이너 이미지를 **프라이빗하게 저장/관리/배포**하는 Azure 서비스입니다.

---

## ACR이란?

```
Docker Hub (퍼블릭)    vs    ACR (프라이빗)
├── 누구나 pull 가능         ├── Azure AD 인증 필요
├── 무료 1개 프라이빗        ├── 무제한 프라이빗
└── Rate Limit 있음         └── Azure 네트워크 내 빠른 전송
```

Log-Doctor에서는 **FastAPI 백엔드 이미지**를 ACR에 저장하고 → Container App이 pull 합니다.

---

## SKU (가격 등급)

| SKU | 저장소 | 기능 | 월 비용 |
|-----|--------|------|---------|
| **Basic** | 10 GB | 기본 | ~$5 |
| **Standard** | 100 GB | Webhook, 지역 복제 X | ~$20 |
| **Premium** | 500 GB | 지역 복제, 프라이빗 링크, 콘텐츠 신뢰 | ~$50 |

> Log-Doctor는 **Basic** 으로 충분 (이미지 1개, 소규모)

---

## 핵심 개념

### 레지스트리, 리포지토리, 태그

```
ACR 레지스트리 (logdoctorcr.azurecr.io)
└── 리포지토리 (log-doctor-api)
    ├── latest          ← 태그
    ├── v1.0.0          ← 태그
    └── sha256:abc123   ← 다이제스트 (불변 식별자)
```

- **레지스트리**: ACR 인스턴스 전체 (URL 단위)
- **리포지토리**: 같은 이름의 이미지 모음
- **태그**: 사람이 읽을 수 있는 버전 (mutable!)
- **다이제스트**: 이미지의 SHA256 해시 (immutable)

### 태그 주의사항

```
# ⚠️ latest 태그는 항상 가장 최근 push를 가리킴
# 프로덕션에서는 반드시 버전 태그 사용!

# ❌ Bad (어떤 버전인지 알 수 없음)
docker pull logdoctorcr.azurecr.io/api:latest

# ✅ Good (특정 버전 고정)
docker pull logdoctorcr.azurecr.io/api:v1.2.3
```

---

## 인증 방법

### 1. az acr login (개발자용)

```bash
# Azure CLI 로그인 상태에서 ACR 인증
az acr login --name logdoctorcr

# 내부적으로 docker login이 실행됨
# 토큰은 3시간 유효
```

### 2. Service Principal (CI/CD용)

```bash
# SP 생성
ACR_ID=$(az acr show --name logdoctorcr --query id -o tsv)
az ad sp create-for-rbac \
  --name "acr-push-sp" \
  --scopes $ACR_ID \
  --role AcrPush

# Docker login with SP
docker login logdoctorcr.azurecr.io \
  --username <appId> \
  --password <password>
```

### 3. Managed Identity (Azure 서비스 간)

```
Container App → (Managed Identity) → ACR
                 AcrPull 역할 필요
```

```bicep
// Bicep에서 Container App이 ACR에서 이미지 pull
resource containerApp 'Microsoft.App/containerApps@2023-05-01' = {
  properties: {
    configuration: {
      registries: [
        {
          server: '${acrName}.azurecr.io'
          identity: managedIdentity.id  // MI로 인증
        }
      ]
    }
  }
}
```

### 역할 비교

| 역할 | pull | push | delete | 사용처 |
|------|------|------|--------|--------|
| **AcrPull** | ✅ | ❌ | ❌ | Container App, AKS |
| **AcrPush** | ✅ | ✅ | ❌ | CI/CD 파이프라인 |
| **AcrDelete** | ❌ | ❌ | ✅ | 이미지 정리 |
| **Owner** | ✅ | ✅ | ✅ | 관리자 |

---

## 이미지 빌드 & 배포 전체 흐름

### 로컬 빌드 → ACR Push

```bash
# 1. 이미지 빌드
docker build -t log-doctor-api:v1.0.0 .

# 2. ACR 태그 지정
docker tag log-doctor-api:v1.0.0 logdoctorcr.azurecr.io/log-doctor-api:v1.0.0

# 3. ACR 로그인
az acr login --name logdoctorcr

# 4. Push
docker push logdoctorcr.azurecr.io/log-doctor-api:v1.0.0
```

### ACR Tasks (클라우드 빌드)

```bash
# 로컬에 Docker가 없어도 ACR에서 직접 빌드!
az acr build \
  --registry logdoctorcr \
  --image log-doctor-api:v1.0.0 \
  --file Dockerfile \
  .

# 장점:
# - 로컬 Docker 불필요
# - Azure 네트워크 내에서 빌드 → 빠름
# - CI/CD에 통합 용이
```

### 자동 빌드 트리거

```bash
# Git commit 시 자동 빌드
az acr task create \
  --name auto-build \
  --registry logdoctorcr \
  --image log-doctor-api:{{.Run.ID}} \
  --context https://github.com/org/repo.git \
  --file Dockerfile \
  --git-access-token $PAT
```

---

## Bicep으로 ACR 배포

```bicep
param location string = resourceGroup().location
param acrName string = 'logdoctorcr'

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: {
    name: 'Basic'     // Basic | Standard | Premium
  }
  properties: {
    adminUserEnabled: false  // ⚠️ 프로덕션에서는 false (MI 사용)
  }
}

// Container App에 AcrPull 역할 부여
resource acrPullRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: acr
  name: guid(acr.id, managedIdentity.id, 'AcrPull')
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      '7f951dda-4ed3-4680-a7ca-43fe172d538d'  // AcrPull
    )
    principalId: managedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

output acrLoginServer string = acr.properties.loginServer
// → "logdoctorcr.azurecr.io"
```

---

## 이미지 관리

### 이미지 목록 확인

```bash
# 리포지토리 목록
az acr repository list --name logdoctorcr

# 특정 리포지토리의 태그 목록
az acr repository show-tags --name logdoctorcr --repository log-doctor-api

# 이미지 상세 정보
az acr repository show-manifests --name logdoctorcr --repository log-doctor-api
```

### 오래된 이미지 정리

```bash
# 태그 없는 이미지 삭제 (dangling)
az acr run --cmd "acr purge --filter 'log-doctor-api:.*' \
  --untagged --ago 30d" \
  --registry logdoctorcr /dev/null

# 특정 태그 삭제
az acr repository delete \
  --name logdoctorcr \
  --image log-doctor-api:v0.1.0 \
  --yes
```

---

## 보안 모범 사례

| 항목 | Bad | Good |
|------|-----|------|
| 인증 | `adminUserEnabled: true` | Managed Identity / SP |
| 태그 | `latest` 사용 | 버전 태그 (`v1.2.3`) |
| 네트워크 | 공개 접근 | Private Endpoint (Premium) |
| 스캔 | 안 함 | Defender for Containers 활성화 |
| 정리 | 수동 | ACR Purge 자동화 |

---

## 트러블슈팅

### 1. Push 권한 오류

```
unauthorized: authentication required
```

```bash
# 해결: ACR 로그인 상태 확인
az acr login --name logdoctorcr

# 또는 SP 자격 증명 확인
docker login logdoctorcr.azurecr.io
```

### 2. Container App에서 Pull 실패

```
ImagePullBackOff: failed to pull image
```

```bash
# 해결 1: MI에 AcrPull 역할 확인
az role assignment list --assignee <MI_PRINCIPAL_ID> --scope <ACR_ID>

# 해결 2: 이미지 경로 확인 (오타 주의)
az acr repository show-tags --name logdoctorcr --repository log-doctor-api
```

### 3. 저장소 용량 초과

```bash
# 현재 사용량 확인
az acr show-usage --name logdoctorcr

# 정리: 오래된 이미지 삭제
az acr purge --filter '.*:.*' --ago 90d --untagged
```

---

## Log-Doctor에서의 사용

```
개발자 PC                    ACR                     Container App
├── docker build        →   logdoctorcr              ├── MI로 pull
├── docker tag          →   .azurecr.io/             ├── 자동 배포
└── docker push         →   log-doctor-api:v1.0.0    └── 서비스 실행
```

1. **개발 시**: 로컬에서 빌드 → ACR push
2. **CI/CD**: GitHub Actions → ACR Tasks 자동 빌드
3. **배포**: Container App이 MI로 ACR에서 이미지 pull
4. **운영**: Defender 스캔 + ACR Purge 자동 정리
