# Wide & Deep Learning

## 📌 개념 (Concept)
구글(Google)에서 제안한 모델로, **"암기(Memorization)"**와 **"일반화(Generalization)"**의 장점을 동시에 취하기 위해 만들어졌습니다. 주로 앱 스토어 추천 등에 사용됩니다.

- **핵심 원리**: `Wide Component` (선형 모델) + `Deep Component` (신경망)
- **비유**: 
    - **Wide**: "A를 산 사람은 B도 사더라" (단순 규칙 암기)
    - **Deep**: "이런 패턴을 가진 사람은 저런 것도 좋아할 거야" (추론 및 일반화)

## ⚙️ 구조 (Architecture)
1.  **Wide Part**: 
    - 입력: 희소한 특성 (Sparse Features, 예: 설치한 앱, 장르)
    - 구조: 선형 회귀 (Linear Regression)
    - 역할: 빈번하게 발생하는 패턴을 기억
2.  **Deep Part**: 
    - 입력: 밀집된 특성 (Dense Features) + 임베딩된 희소 특성
    - 구조: 다층 퍼셉트론 (MLP)
    - 역할: 이전에 본 적 없는 조합을 예측

## 👍 장점 (Pros)
- **균형**: 단순함과 정교함의 균형을 잘 맞춤.
- **실용성**: 대규모 상용 서비스(구글 플레이 등)에서 검증된 성능.
- **부가 정보 활용**: 유저의 나이, 성별, 시간대 등 다양한 Context 정보를 잘 활용함.

## 👎 단점 (Cons)
- **Feature Engineering**: Wide 파트에 들어갈 Cross Product Feature 등을 사람이 어느 정도 설계해야 함.

## 💻 코드 예시 (PyTorch)
```python
class WideAndDeep(nn.Module):
    def __init__(self, num_features, embed_dim=32):
        super().__init__()
        self.wide = nn.Linear(num_features, 1)
        self.deep = nn.Sequential(
            nn.Linear(embed_dim, 64),
            nn.ReLU(),
            nn.Linear(64, 1)
        )

    def forward(self, x_wide, x_deep):
        wide_out = self.wide(x_wide)
        deep_out = self.deep(x_deep)
        return wide_out + deep_out
```
