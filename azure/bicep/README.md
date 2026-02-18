# Bicep 완전 정리

Azure 리소스를 코드로 정의하는 **IaC(Infrastructure as Code)** 도구입니다.  
ARM 템플릿(JSON)의 단점을 보완한 **선언형 DSL(Domain-Specific Language)** 입니다.

---

## Bicep vs ARM vs Terraform

| 항목 | Bicep | ARM (JSON) | Terraform |
|------|-------|-----------|-----------|
| 언어 | 선언형 DSL | JSON | HCL |
| 학습 난이도 | ⭐⭐ 쉬움 | ⭐⭐⭐⭐ 어려움 | ⭐⭐⭐ 보통 |
| Azure 전용 | ✅ 예 | ✅ 예 | ❌ 멀티 클라우드 |
| 상태 파일 | 불필요 | 불필요 | 필요 (tfstate) |
| 모듈화 | ✅ 내장 | ⚠️ 복잡 | ✅ 내장 |
| 코드 라인수 | 짧음 | 3~5배 길음 | 보통 |
| IDE 지원 | VS Code 확장 | VS Code 확장 | VS Code 확장 |

---

## 기본 문법

### 1. 리소스 선언 (resource)

```bicep
resource <심볼이름> '<리소스타입>@<API버전>' = {
  name: '리소스이름'
  location: location
  properties: {
    // 리소스 속성
  }
}
```

**실제 예시:**
```bicep
resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: 'mystorageaccount'
  location: 'koreacentral'
  kind: 'StorageV2'
  sku: {
    name: 'Standard_LRS'
  }
}
```

> **심볼 이름** = Bicep 파일 내에서 참조할 이름 (Azure에 배포되는 이름 X)  
> **name** = Azure에 실제 생성되는 리소스 이름

---

### 2. 파라미터 (param)

```bicep
// 기본 파라미터
@description('설명')
param location string = resourceGroup().location

// 기본값이 없는 필수 파라미터
param appName string

// 허용 값 제한
@allowed(['dev', 'stg', 'prd'])
param environment string = 'dev'

// 최소/최대 제한
@minLength(3)
@maxLength(24)
param storageName string

// 보안 파라미터 (로그에 노출 안됨)
@secure()
param adminPassword string
```

**파라미터 데코레이터 정리:**

| 데코레이터 | 설명 | 예시 |
|-----------|------|------|
| `@description()` | 설명 추가 | `@description('리소스 위치')` |
| `@allowed()` | 허용값 제한 | `@allowed(['dev', 'prd'])` |
| `@minLength()` | 최소 길이 | `@minLength(3)` |
| `@maxLength()` | 최대 길이 | `@maxLength(24)` |
| `@minValue()` | 최소 값 | `@minValue(1)` |
| `@maxValue()` | 최대 값 | `@maxValue(100)` |
| `@secure()` | 보안 (로그 숨김) | 비밀번호, API 키 등 |
| `@metadata()` | 메타데이터 | `@metadata({ example: 'foo' })` |

---

### 3. 변수 (var)

```bicep
// 단순 변수
var storageName = 'st${appName}${environment}'

// 연산 변수
var isProd = environment == 'prd'
var skuName = isProd ? 'Standard_GRS' : 'Standard_LRS'

// 객체 변수
var tags = {
  environment: environment
  project: appName
  createdBy: 'bicep'
}
```

> **param vs var:**  
> `param` = 외부에서 주입 (배포 시 전달)  
> `var` = 내부에서 계산 (파일 내에서만 사용)

---

### 4. 출력값 (output)

```bicep
output storageId string = storageAccount.id
output storageName string = storageAccount.name
output primaryEndpoint string = storageAccount.properties.primaryEndpoints.blob

// 조건부 출력
output keyVaultUri string = deployKeyVault ? keyVault.properties.vaultUri : ''
```

---

### 5. 조건부 배포 (if)

```bicep
// 프로덕션일 때만 배포
resource appInsights 'Microsoft.Insights/components@2020-02-02' = if (isProd) {
  name: 'ai-${appName}'
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
  }
}
```

---

### 6. 반복문 (for)

```bicep
// 배열 반복
param locations array = ['koreacentral', 'japaneast']

resource storageAccounts 'Microsoft.Storage/storageAccounts@2023-01-01' = [for loc in locations: {
  name: 'st${uniqueString(resourceGroup().id, loc)}'
  location: loc
  kind: 'StorageV2'
  sku: { name: 'Standard_LRS' }
}]

// 인덱스 포함 반복
resource nsg 'Microsoft.Network/networkSecurityGroups@2023-04-01' = [for (name, i) in subnetNames: {
  name: 'nsg-${name}'
  location: location
}]

// 범위 반복
resource storages 'Microsoft.Storage/storageAccounts@2023-01-01' = [for i in range(0, 3): {
  name: 'storage${i}'
  location: location
  kind: 'StorageV2'
  sku: { name: 'Standard_LRS' }
}]
```

---

### 7. 기존 리소스 참조 (existing)

```bicep
// 같은 리소스 그룹의 기존 리소스
resource existingVnet 'Microsoft.Network/virtualNetworks@2023-04-01' existing = {
  name: 'my-vnet'
}

// 다른 리소스 그룹의 기존 리소스
resource existingKv 'Microsoft.KeyVault/vaults@2023-07-01' existing = {
  name: 'my-keyvault'
  scope: resourceGroup('other-rg')
}

// 참조해서 사용
output vnetId string = existingVnet.id
```

> **existing 키워드** = 이미 존재하는 리소스를 참조만 (새로 생성 X)  
> RBAC, 진단설정 등에서 기존 리소스에 설정을 추가할 때 필수

---

## 모듈 시스템

### 모듈이란?

다른 Bicep 파일을 **재사용 가능한 컴포넌트**로 호출하는 방식

### 모듈 호출

```bicep
// modules/storage.bicep 파일을 모듈로 호출
module storage './modules/storage.bicep' = {
  name: 'storageDeployment'           // 배포 이름 (Azure에서 추적용)
  params: {
    location: location                 // 파라미터 전달
    storageName: 'mystorageaccount'
  }
}

// 모듈 출력값 사용
output storageId string = storage.outputs.storageId
```

### 다른 리소스 그룹에 배포

```bicep
module networkModule './modules/network.bicep' = {
  name: 'networkDeployment'
  scope: resourceGroup('network-rg')  // 다른 리소스 그룹 지정
  params: {
    vnetName: 'my-vnet'
  }
}
```

### 조건부 모듈

```bicep
module monitoring './modules/monitoring.bicep' = if (enableMonitoring) {
  name: 'monitoringDeployment'
  params: {
    logAnalyticsId: logAnalytics.outputs.workspaceId
  }
}
```

### 모듈 반복

```bicep
module storages './modules/storage.bicep' = [for loc in locations: {
  name: 'storage-${loc}'
  params: {
    location: loc
  }
}]
```

---

## 내장 함수

### 자주 쓰는 함수

| 함수 | 설명 | 예시 |
|------|------|------|
| `resourceGroup()` | 현재 리소스 그룹 정보 | `.location`, `.id`, `.name` |
| `subscription()` | 현재 구독 정보 | `.subscriptionId`, `.tenantId` |
| `uniqueString()` | 고유 문자열 생성 (13자) | `uniqueString(resourceGroup().id)` |
| `guid()` | GUID 생성 | `guid(resourceGroup().id, 'role')` |
| `toLower()` | 소문자 변환 | `toLower(appName)` |
| `toUpper()` | 대문자 변환 | `toUpper(appName)` |
| `substring()` | 문자열 자르기 | `substring(name, 0, 5)` |
| `replace()` | 문자열 치환 | `replace(name, '-', '')` |
| `concat()` | 문자열 합치기 | `concat('st', appName)` |
| `contains()` | 포함 여부 | `contains(tags, 'env')` |
| `empty()` | 비어있는지 | `empty(myArray)` |
| `length()` | 길이 | `length(myArray)` |
| `json()` | JSON 변환 | `json('0.25')` → 숫자 0.25 |
| `format()` | 서식 지정 | `format('Hello {0}', name)` |
| `environment()` | 클라우드 환경 | `.suffixes.storage` 등 |

### 리소스 ID 함수

```bicep
// 현재 구독의 리소스 정의 ID
var roleId = subscriptionResourceId(
  'Microsoft.Authorization/roleDefinitions',
  '4633458b-17de-408a-b874-0445c86b69e6'
)

// 현재 리소스 그룹의 리소스 ID
var storageId = resourceId(
  'Microsoft.Storage/storageAccounts',
  'mystorageaccount'
)

// 다른 구독의 리소스 ID
var otherStorageId = extensionResourceId(
  '/subscriptions/{subId}/resourceGroups/{rgName}',
  'Microsoft.Storage/storageAccounts',
  'mystorageaccount'
)
```

---

## 스코프 (Scope)

Bicep 배포 대상 범위를 지정합니다.

```bicep
// 리소스 그룹 수준 (기본값)
targetScope = 'resourceGroup'

// 구독 수준 (리소스 그룹 생성 등)
targetScope = 'subscription'

// 관리 그룹 수준
targetScope = 'managementGroup'

// 테넌트 수준
targetScope = 'tenant'
```

**구독 수준 배포 예시:**
```bicep
targetScope = 'subscription'

resource rg 'Microsoft.Resources/resourceGroups@2023-07-01' = {
  name: 'my-rg'
  location: 'koreacentral'
}

module resources './resources.bicep' = {
  name: 'resourcesDeployment'
  scope: rg  // 위에서 만든 리소스 그룹에 배포
  params: { location: rg.location }
}
```

---

## 타입 시스템

### 기본 타입

| 타입 | 설명 | 예시 |
|------|------|------|
| `string` | 문자열 | `'hello'` |
| `int` | 정수 | `42` |
| `bool` | 불리언 | `true`, `false` |
| `object` | 객체 | `{ key: 'value' }` |
| `array` | 배열 | `['a', 'b', 'c']` |

### 사용자 정의 타입 (Bicep v0.12+)

```bicep
// 타입 정의
type storageConfig = {
  name: string
  sku: 'Standard_LRS' | 'Standard_GRS' | 'Premium_LRS'
  kind: 'StorageV2' | 'BlobStorage'
}

// 사용
param config storageConfig
```

---

## CLI 명령어

### 배포 명령어

```bash
# 리소스 그룹에 배포
az deployment group create \
  --resource-group <rg> \
  --template-file main.bicep \
  --parameters appName='myapp' environment='dev'

# 파라미터 파일 사용
az deployment group create \
  --resource-group <rg> \
  --template-file main.bicep \
  --parameters @parameters.json

# What-if (변경사항 미리 확인, 실제 배포 X)
az deployment group what-if \
  --resource-group <rg> \
  --template-file main.bicep

# 구독 수준 배포
az deployment sub create \
  --location koreacentral \
  --template-file main.bicep
```

### 변환 / 빌드

```bash
# Bicep → ARM JSON 변환
az bicep build --file main.bicep

# ARM JSON → Bicep 변환 (역변환)
az bicep decompile --file template.json

# Bicep 설치/업그레이드
az bicep install
az bicep upgrade
az bicep version
```

### 파라미터 파일 (parameters.json)

```json
{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "appName": { "value": "myapp" },
    "environment": { "value": "dev" },
    "adminPassword": { "value": "S3cur3P@ss!" }
  }
}
```

### .bicepparam 파일 (Bicep 네이티브 방식)

```bicep
using './main.bicep'

param appName = 'myapp'
param environment = 'dev'
param adminPassword = 'S3cur3P@ss!'
```

---

## 네이밍 규칙 (Best Practice)

| 리소스 | 접두사 | 예시 |
|--------|--------|------|
| Resource Group | `rg-` | `rg-myapp-dev` |
| Storage Account | `st` | `stmyappdev` (특수문자 불가) |
| Key Vault | `kv-` | `kv-myapp-dev` |
| Virtual Network | `vnet-` | `vnet-myapp-dev` |
| Subnet | `snet-` | `snet-backend` |
| NSG | `nsg-` | `nsg-backend` |
| App Service | `app-` | `app-myapp-dev` |
| Function App | `func-` | `func-myapp-dev` |
| Container Registry | `acr` | `acrmyappdev` |
| Container App | `ca-` | `ca-myapp-dev` |
| Log Analytics | `law-` | `law-myapp-dev` |
| Managed Identity | `mi-` | `mi-myapp-dev` |

---

## 실전 패턴

### 1. 환경별 분기

```bicep
param environment string = 'dev'

var configMap = {
  dev: {
    sku: 'Standard_LRS'
    instanceCount: 1
  }
  prd: {
    sku: 'Standard_GRS'
    instanceCount: 3
  }
}

var config = configMap[environment]
```

### 2. 태그 표준화

```bicep
param tags object = {}

var defaultTags = {
  environment: environment
  project: appName
  managedBy: 'bicep'
  createdDate: utcNow('yyyy-MM-dd')
}

var mergedTags = union(defaultTags, tags)
```

### 3. 의존성 관리

```bicep
// 암시적 의존성 (속성 참조로 자동 해결)
resource app 'Microsoft.App/containerApps@2023-05-01' = {
  properties: {
    managedEnvironmentId: acaEnv.id  // acaEnv가 먼저 배포됨
  }
}

// 명시적 의존성 (속성 참조 없을 때)
resource roleAssignment '...' = {
  dependsOn: [
    keyVault
    managedIdentity
  ]
}
```

> **Best Practice:** 가능하면 **암시적 의존성** 사용 (속성 참조)  
> `dependsOn`은 꼭 필요한 경우에만

---

## 디버깅

### 에러 해결 팁

| 에러 | 원인 | 해결 |
|------|------|------|
| `BCP035` | 필수 속성 누락 | 리소스 문서 확인 후 추가 |
| `BCP036` | 타입 불일치 | string/int 등 타입 확인 |
| `BCP037` | 존재하지 않는 속성 | API 버전별 지원 속성 확인 |
| `BCP062` | existing 리소스 못찾음 | 이름/리소스그룹 확인 |
| `InvalidTemplate` | 배포 시 에러 | `what-if` 먼저 실행 |
| `Conflict` | 리소스가 이미 존재 | 이름 변경 또는 증분 배포 확인 |

```bash
# 디버깅에 유용한 명령어
az deployment group create ... --debug           # 상세 로그
az deployment group create ... --verbose          # 자세한 출력
az deployment group show --name <배포이름> -g <rg> # 배포 결과 확인
az deployment operation group list -g <rg> --name <배포이름>  # 개별 작업 확인
```
