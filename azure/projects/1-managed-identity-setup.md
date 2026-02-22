# 1단계: Managed Identity & RBAC 설정

Azure Managed Identity(관리 ID)는 코드 내에 암호를 저장하지 않고도 Azure 서비스에 안전하게 인증할 수 있게 해주는 Identity 서비스입니다.

## 1. User-Assigned Managed Identity 생성
서비스가 자체적으로 사용할 신분증을 만드는 과정입니다.

1.  Azure Portal에서 **Managed Identities(관리 ID)** 검색
2.  **+ 만들기** 클릭
3.  구독, 리소스 그룹 선택 후 이름을 `logdoctor-id`로 지정
4.  만들기 완료 후 해당 리소스의 **클라이언트 ID**를 확인 (백엔드 `.env` 설정 시 필요)

## 2. 역할 할당 (RBAC)
만든 신분증(`logdoctor-id`)에 실제 자원을 만질 수 있는 열쇠를 주는 과정입니다.

### 방법 A: Azure Portal 사용
1.  구독(Subscription) 화면으로 이동
2.  **액세스 제어(IAM)** -> **+ 추가** -> **역할 할당 추가**
3.  역할: **기여자 (Contributor)** 선택
4.  대상: **관리 ID** 선택 후 `logdoctor-id` 추가 및 저장

### 방법 B: Azure CLI 사용 (추천 🚀)
터미널에서 아래 명령어를 실행하면 UI 없이 바로 할당됩니다.

```bash
# 1. 관리 ID의 Principal ID 확인
az identity show --name logdoctor-id --resource-group [RG_NAME] --query principalId -o tsv

# 2. 권한 부여 (기여자)
az role assignment create \
  --assignee [PRINCIPAL_ID] \
  --role "Contributor" \
  --scope "/subscriptions/[SUB_ID]"
```

---
> [!TIP]
> '기여자' 권한은 리소스 생성/삭제가 가능한 강력한 권한입니다. 운영 환경에서는 'Log Analytics 독자' 등 필요한 최소 권한만 주는 것이 좋습니다.
