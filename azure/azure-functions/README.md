# Azure Functions 완전 정리

**서버리스(Serverless) 컴퓨팅** 서비스로, 코드만 작성하면 Azure가 인프라를 관리합니다.

---

## Azure Functions란?

```
이벤트 발생 → Azure Functions 실행 → 결과 반환
```

- **서버 관리 불필요** — Azure가 자동으로 스케일링, 패치, 배포
- **사용한 만큼만 과금** — 실행 시간 + 실행 횟수 기준
- **이벤트 기반** — HTTP 요청, 타이머, 큐 메시지 등이 트리거

---

## Container Apps vs Functions vs App Service

| 항목 | Azure Functions | Container Apps | App Service |
|------|----------------|---------------|-------------|
| 모델 | 서버리스(함수) | 서버리스(컨테이너) | PaaS(앱) |
| 단위 | 함수(Function) | 컨테이너 | 앱 전체 |
| 스케일링 | 이벤트 기반 자동 | HTTP/이벤트 기반 자동 | 수동/규칙 기반 |
| 0으로 축소 | ✅ (소비 플랜) | ✅ | ❌ (최소 1개) |
| 콜드 스타트 | 있음 (소비 플랜) | 있음 | 없음 |
| 비용 | 실행당 과금 | vCPU/메모리 초당 | 월 고정 |
| 적합한 작업 | 짧은 작업 (≤10분) | 장시간/복잡한 앱 | 풀 앱 |
| 커스터마이징 | 제한적 | Docker 자유롭게 | 보통 |

---

## 호스팅 플랜

| 플랜 | 스케일링 | 실행 제한 | 비용 | 사용 사례 |
|------|---------|----------|------|----------|
| **Consumption** | 자동 (0~200) | 5분 기본, 10분 최대 | 실행당 과금 ⭐ | 간단한 API, 이벤트 처리 |
| **Premium** | 자동 (항상 준비) | 무제한 | Always Ready 인스턴스 과금 | 콜드 스타트 제거, VNet |
| **Dedicated** | App Service Plan | 무제한 | 월정액 | 기존 App Service가 있을 때 |
| **Container Apps** | 자동 (KEDA) | 무제한 | Container 과금 | 마이크로서비스 통합 |

### Consumption 플랜 가격

| 항목 | 무료 포함 | 초과 시 |
|------|----------|---------|
| 실행 횟수 | 100만 회/월 | ₩0.22/백만 실행 |
| 실행 시간 | 40만 GB-초/월 | ₩18.4/백만 GB-초 |

---

## 트리거 (Trigger) 종류

함수가 **언제 실행되는지** 결정합니다.

| 트리거 | 설명 | 사용 사례 |
|--------|------|----------|
| **HTTP** | HTTP 요청 시 실행 | REST API, 웹훅 |
| **Timer** | 스케줄(CRON) 기반 실행 | 배치 작업, 정기 보고서 |
| **Blob Storage** | 파일 업로드 시 실행 | 이미지 처리, 파일 변환 |
| **Queue Storage** | 큐 메시지 도착 시 실행 | 비동기 작업 처리 |
| **Service Bus** | Service Bus 메시지 수신 | 엔터프라이즈 메시징 |
| **Event Hub** | Event Hub 이벤트 수신 | IoT, 로그 스트리밍 |
| **Event Grid** | Event Grid 이벤트 수신 | 리소스 변경 알림 |
| **Cosmos DB** | 문서 변경 시 실행 | 변경 감지, 실시간 처리 |
| **SignalR** | SignalR 메시지 | 실시간 통신 |

---

## 바인딩 (Binding)

함수에서 **외부 리소스를 쉽게 연결**하는 방법입니다.

| 방향 | 설명 |
|------|------|
| **Input Binding** | 외부에서 데이터 읽기 |
| **Output Binding** | 외부로 데이터 쓰기 |
| **Trigger** | 함수를 실행시키는 특수 Input |

```
Trigger (입력)     →  Function  →  Output Binding (출력)
HTTP 요청              처리         Queue에 메시지 전송
Timer CRON             변환         Blob에 파일 저장
Queue 메시지            분석         Cosmos DB에 저장
```

---

## 코드 예시

### Python — HTTP Trigger

```python
import azure.functions as func
import json
import logging

app = func.FunctionApp()

@app.route(route="hello", methods=["GET"])
def hello(req: func.HttpRequest) -> func.HttpResponse:
    name = req.params.get('name', 'World')
    logging.info(f'HTTP trigger function processed - name: {name}')
    
    return func.HttpResponse(
        json.dumps({"message": f"Hello, {name}!"}),
        mimetype="application/json",
        status_code=200
    )

@app.route(route="users", methods=["POST"])
def create_user(req: func.HttpRequest) -> func.HttpResponse:
    try:
        body = req.get_json()
        name = body.get('name')
        email = body.get('email')
        
        # 비즈니스 로직
        return func.HttpResponse(
            json.dumps({"id": "123", "name": name, "email": email}),
            mimetype="application/json",
            status_code=201
        )
    except ValueError:
        return func.HttpResponse("Invalid JSON", status_code=400)
```

### Python — Timer Trigger (CRON)

```python
@app.timer_trigger(schedule="0 0 9 * * *", arg_name="timer")  # 매일 9시
def daily_report(timer: func.TimerRequest) -> None:
    if timer.past_due:
        logging.info('Timer is past due!')
    
    logging.info('Daily report function executed')
    # 보고서 생성 로직
```

**CRON 표현식:**

| 표현식 | 의미 |
|--------|------|
| `0 */5 * * * *` | 5분마다 |
| `0 0 * * * *` | 매시간 정각 |
| `0 0 9 * * *` | 매일 9시 |
| `0 0 0 * * 1` | 매주 월요일 자정 |
| `0 0 0 1 * *` | 매월 1일 자정 |

> 형식: `{초} {분} {시} {일} {월} {요일}`

### Python — Queue Trigger + Blob Output

```python
@app.queue_trigger(arg_name="msg", queue_name="tasks", connection="AzureWebJobsStorage")
@app.blob_output(arg_name="outputblob", path="results/{id}.json", connection="AzureWebJobsStorage")
def process_task(msg: func.QueueMessage, outputblob: func.Out[str]) -> None:
    task = json.loads(msg.get_body().decode())
    
    # 처리
    result = {"taskId": task["id"], "status": "completed"}
    
    # Blob에 결과 저장
    outputblob.set(json.dumps(result))
```

### Python — Key Vault 연동 (Managed Identity)

```python
from azure.identity import DefaultAzureCredential
from azure.keyvault.secrets import SecretClient

@app.route(route="secret/{name}")
def get_secret(req: func.HttpRequest) -> func.HttpResponse:
    secret_name = req.route_params.get('name')
    
    credential = DefaultAzureCredential()
    client = SecretClient(
        vault_url=os.environ["KEY_VAULT_URL"],
        credential=credential
    )
    
    secret = client.get_secret(secret_name)
    
    return func.HttpResponse(
        json.dumps({"name": secret_name, "value": "***"}),
        mimetype="application/json"
    )
```

### C# — HTTP Trigger

```csharp
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using System.Net;

public class HelloFunction
{
    [Function("Hello")]
    public async Task<HttpResponseData> Run(
        [HttpTrigger(AuthorizationLevel.Function, "get")] HttpRequestData req)
    {
        var response = req.CreateResponse(HttpStatusCode.OK);
        await response.WriteAsJsonAsync(new { message = "Hello!" });
        return response;
    }
}
```

### JavaScript — HTTP Trigger

```javascript
const { app } = require('@azure/functions');

app.http('hello', {
    methods: ['GET'],
    handler: async (request, context) => {
        const name = request.query.get('name') || 'World';
        context.log(`HTTP trigger function processed - name: ${name}`);
        
        return {
            jsonBody: { message: `Hello, ${name}!` }
        };
    }
});
```

---

## 프로젝트 구조

### Python (v2 모델)

```
my-function-app/
├── function_app.py          # 모든 함수 정의 (진입점)
├── host.json                # 전역 설정
├── local.settings.json      # 로컬 환경변수 (Git 제외!)
├── requirements.txt         # Python 패키지
└── .funcignore
```

### host.json

```json
{
  "version": "2.0",
  "logging": {
    "applicationInsights": {
      "samplingSettings": {
        "isEnabled": true,
        "excludedTypes": "Request"
      }
    }
  },
  "extensions": {
    "http": {
      "routePrefix": "api",     // URL 접두사 (기본: api)
      "maxConcurrentRequests": 100
    }
  }
}
```

### local.settings.json

```json
{
  "IsEncrypted": false,
  "Values": {
    "FUNCTIONS_WORKER_RUNTIME": "python",
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "KEY_VAULT_URL": "https://kv-myapp.vault.azure.net/"
  }
}
```

---

## Durable Functions (내구성 함수)

**장시간 실행, 복잡한 워크플로우**를 위한 확장 기능입니다.

### 패턴

| 패턴 | 설명 | 사용 사례 |
|------|------|----------|
| **Function Chaining** | 순차 실행 (A→B→C) | 파이프라인 처리 |
| **Fan-out/Fan-in** | 병렬 실행 후 결과 합치기 | 대량 데이터 처리 |
| **Monitor** | 주기적으로 상태 확인 | 외부 API 폴링 |
| **Human Interaction** | 사람의 승인 대기 | 결재 프로세스 |
| **Async HTTP API** | 장시간 작업 상태 추적 | 대용량 파일 처리 |

### 예시 — Function Chaining

```python
import azure.functions as func
import azure.durable_functions as df

app = func.FunctionApp()

# 오케스트레이터 함수
@app.orchestration_trigger(context_name="context")
def chained_workflow(context: df.DurableOrchestrationContext):
    result1 = yield context.call_activity("step1", "input")
    result2 = yield context.call_activity("step2", result1)
    result3 = yield context.call_activity("step3", result2)
    return result3

# 액티비티 함수
@app.activity_trigger(input_name="data")
def step1(data: str) -> str:
    return f"Step1 processed: {data}"
```

---

## Bicep 코드

### Function App 배포

```bicep
param location string = resourceGroup().location
param appName string

// App Service Plan (Consumption)
resource hostingPlan 'Microsoft.Web/serverfarms@2022-09-01' = {
  name: 'plan-${appName}'
  location: location
  sku: {
    name: 'Y1'       // Consumption 플랜
    tier: 'Dynamic'
  }
  properties: {
    reserved: true    // Linux
  }
}

// Storage Account (Functions 필수)
resource storage 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: 'st${uniqueString(resourceGroup().id)}'
  location: location
  kind: 'StorageV2'
  sku: { name: 'Standard_LRS' }
}

// Application Insights
resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: 'ai-${appName}'
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalytics.id  // Log Analytics 연결
  }
}

// Function App
resource functionApp 'Microsoft.Web/sites@2022-09-01' = {
  name: 'func-${appName}'
  location: location
  kind: 'functionapp,linux'
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentity.id}': {}
    }
  }
  properties: {
    serverFarmId: hostingPlan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: 'PYTHON|3.11'
      appSettings: [
        { name: 'FUNCTIONS_WORKER_RUNTIME', value: 'python' }
        { name: 'FUNCTIONS_EXTENSION_VERSION', value: '~4' }
        {
          name: 'AzureWebJobsStorage'
          value: 'DefaultEndpointsProtocol=https;AccountName=${storage.name};AccountKey=${storage.listKeys().keys[0].value}'
        }
        {
          name: 'APPINSIGHTS_INSTRUMENTATIONKEY'
          value: appInsights.properties.InstrumentationKey
        }
        { name: 'KEY_VAULT_URL', value: keyVault.properties.vaultUri }
        { name: 'AZURE_CLIENT_ID', value: managedIdentity.properties.clientId }
      ]
    }
  }
}
```

### 런타임별 설정

| 런타임 | linuxFxVersion | WORKER_RUNTIME |
|--------|---------------|----------------|
| Python | `PYTHON\|3.11` | `python` |
| Node.js | `NODE\|20` | `node` |
| .NET | `DOTNET-ISOLATED\|8.0` | `dotnet-isolated` |
| Java | `JAVA\|17` | `java` |
| PowerShell | `POWERSHELL\|7.4` | `powershell` |

---

## CLI 명령어

### 로컬 개발

```bash
# Azure Functions Core Tools 설치
npm install -g azure-functions-core-tools@4

# 프로젝트 초기화
func init my-function-app --worker-runtime python

# 함수 추가
func new --name hello --template "HTTP trigger"

# 로컬 실행
func start

# 로컬 테스트
curl http://localhost:7071/api/hello?name=Azure
```

### Azure 배포

```bash
# Function App 생성
az functionapp create \
  --name func-myapp \
  --resource-group my-rg \
  --storage-account stmyapp \
  --consumption-plan-location koreacentral \
  --runtime python \
  --runtime-version 3.11 \
  --os-type Linux \
  --functions-version 4

# 코드 배포
func azure functionapp publish func-myapp

# 또는 zip 배포
az functionapp deployment source config-zip \
  --name func-myapp \
  --resource-group my-rg \
  --src app.zip

# 함수 목록
az functionapp function list --name func-myapp --resource-group my-rg -o table

# 로그 스트리밍
az functionapp log tail --name func-myapp --resource-group my-rg

# 앱 설정 조회
az functionapp config appsettings list --name func-myapp --resource-group my-rg

# 앱 설정 추가
az functionapp config appsettings set \
  --name func-myapp \
  --resource-group my-rg \
  --settings "KEY_VAULT_URL=https://kv-myapp.vault.azure.net/"
```

---

## 인증/보안

### Function Key (기본 인증)

| 레벨 | 설명 |
|------|------|
| Anonymous | 인증 없음 (누구나 접근) |
| Function | 함수별 키 필요 |
| Admin | 마스터 키 필요 (관리 작업) |

```python
# AuthorizationLevel 설정
@app.route(route="public", auth_level=func.AuthLevel.ANONYMOUS)
def public_api(req): ...

@app.route(route="private", auth_level=func.AuthLevel.FUNCTION)
def private_api(req): ...
```

```bash
# 호출 시 키 전달
curl "https://func-myapp.azurewebsites.net/api/private?code=<function-key>"
```

### Easy Auth (Azure AD 인증)

```bash
# Azure AD 인증 활성화
az webapp auth update \
  --name func-myapp \
  --resource-group my-rg \
  --enabled true \
  --action LoginWithAzureActiveDirectory
```

---

## 모니터링

### Application Insights

Functions에 Application Insights를 연결하면:

| 기능 | 설명 |
|------|------|
| 라이브 메트릭 | 실시간 요청/응답 모니터링 |
| 실패 분석 | 에러 자동 감지 및 분석 |
| 성능 맵 | 종속성 호출 시각화 |
| 사용자 정의 메트릭 | 커스텀 텔레메트리 |

### KQL 쿼리 — 함수 모니터링

```kusto
// 함수별 실행 횟수 및 평균 실행 시간
requests
| where cloud_RoleName == "func-myapp"
| summarize count(), avg(duration) by name
| order by count_ desc

// 실패한 함수 호출
requests
| where success == false
| project timestamp, name, resultCode, duration
| order by timestamp desc
| take 20

// 콜드 스타트 감지
customMetrics
| where name == "FunctionExecutionTimeMs"
| where value > 5000  // 5초 이상
| project timestamp, value
```

---

## Best Practice

1. **함수는 짧게** — 단일 책임, 5분 이내 완료 목표
2. **상태 저장 금지** — 함수는 Stateless, 상태는 외부 저장소 사용
3. **Managed Identity** — 연결 문자열 대신 Managed Identity 사용
4. **Application Insights** — 반드시 연결, 모니터링/디버깅 필수
5. **Key Vault 연동** — 비밀은 `@Microsoft.KeyVault()` 참조 사용
6. **로컬 테스트** — `func start`로 배포 전 반드시 로컬 테스트
7. **환경 분리** — dev/stg/prd 슬롯 또는 별도 Function App
8. **콜드 스타트 대비** — Premium 플랜 또는 최소 인스턴스 설정
