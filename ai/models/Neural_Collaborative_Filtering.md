# Neural Collaborative Filtering (NCF)

## 📌 개념 (Concept)
MF의 선형적인 한계를 극복하기 위해 **"딥러닝(Deep Learning)"**을 도입한 모델입니다. MF의 장점과 신경망의 장점을 결합했습니다.

- **핵심 원리**: `GMF` (Generalized Matrix Factorization) + `MLP` (Multi-Layer Perceptron)
- **비유**: 
    - MF처럼 단순한 취향도 보고 (GMF)
    - 딥러닝으로 복잡하고 미묘한 취향도 파악 (MLP)
    - 두 결과를 합쳐서 최종 판단

## ⚙️ 구조 (Architecture)
1.  **GMF Part**: 유저/아이템 임베딩의 요소별 곱 (Element-wise Product)
2.  **MLP Part**: 유저/아이템 임베딩을 이어 붙여서(Concat) 여러 층의 신경망 통과
3.  **NeuMF Layer**: GMF와 MLP의 출력을 합쳐서(Concat) 최종 예측

## 👍 장점 (Pros)
- **정확도**: 비선형적인 관계를 학습하여 MF보다 높은 정확도를 보임.
- **유연성**: 다양한 구조로 확장이 가능함.

## 👎 단점 (Cons)
- **학습 속도**: MF보다 연산량이 많아 학습이 느림.
- **복잡성**: 하이퍼파라미터(레이어 수, 노드 수 등) 튜닝이 필요함.

## 💻 코드 예시 (PyTorch)
```python
class NCF(nn.Module):
    def __init__(self, num_users, num_items, embed_dim=32):
        super().__init__()
        # GMF
        self.gmf_user = nn.Embedding(num_users, embed_dim)
        self.gmf_item = nn.Embedding(num_items, embed_dim)
        # MLP
        self.mlp_user = nn.Embedding(num_users, embed_dim)
        self.mlp_item = nn.Embedding(num_items, embed_dim)
        self.mlp = nn.Sequential(
            nn.Linear(embed_dim*2, 64),
            nn.ReLU(),
            nn.Linear(64, 32),
            nn.ReLU()
        )
        self.predict = nn.Linear(embed_dim + 32, 1)

    def forward(self, users, items):
        # GMF
        gmf_out = self.gmf_user(users) * self.gmf_item(items)
        # MLP
        mlp_in = torch.cat([self.mlp_user(users), self.mlp_item(items)], dim=1)
        mlp_out = self.mlp(mlp_in)
        # Concat
        return self.predict(torch.cat([gmf_out, mlp_out], dim=1))
```
