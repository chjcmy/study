# ACE (Azure Container Apps) Managed Identity 설정

ACE 배포 시 시스템 할당 관리 ID(System-Assigned Managed Identity)를 활성화하고, 백엔드 애플리케이션이 이를 인식하도록 설정하는 인프라 코드(Bicep) 가이드입니다.

## 📝 Bicep 구성 예시 (aca.bicep)

```bicep
// Backend Container App 리소스 정의
resource acaBackend 'Microsoft.App/containerApps@2023-05-01' = {
  name: 'ca-backend-dev'
  location: location
  identity: {
    type: 'SystemAssigned' // ✅ 시스템 할당 관리 ID 활성화
  }
  properties: {
    managedEnvironmentId: acaEnv.id
    configuration: {
      ingress: {
        external: true
        targetPort: 8000
      }
    }
    template: {
      containers: [
        {
          name: 'backend'
          image: 'your-acr.azurecr.io/backend:latest'
          env: [
            {
              name: 'AUTH_METHOD'
              value: 'managed_identity' // ✅ 코드가 관리 ID를 선택하도록 환경 변수 주입
            }
          ]
        }
      ]
    }
  }
}
```

## ⚙️ 작동 원리
1.  **Identity 활성화**: `type: 'SystemAssigned'`를 통해 이 컨테이너는 고유한 '신분증'을 갖게 됩니다.
2.  **환경 변수 주입**: `AUTH_METHOD`를 `managed_identity`로 설정하여, 코드 내부의 `DefaultAzureCredential`이 로컬 자격 증명이 아닌 Azure 서버 자격 증명을 사용하도록 유도합니다.
3.  **권한 부여**: 배포 후, 이 관리 ID의 `PrincipalId`에 대해 Cosmos DB나 구독 조회 권한(Contributor 등)을 Azure Portal이나 RBAC 설정을 통해 부여해야 합니다.

---
> [!TIP]
> **성능 팁**: Managed Identity 사용 시 토큰 갱신은 Azure 플랫폼이 알아서 처리하므로 애플리케이션은 성능에만 집중할 수 있습니다.
