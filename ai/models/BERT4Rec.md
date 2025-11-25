# BERT4Rec (Sequential Recommendation with BERT)

## 📌 개념 (Concept)
SASRec과 비슷하지만, **BERT(Bidirectional Encoder Representations from Transformers)**의 학습 방식을 가져온 모델입니다. 단방향(왼쪽->오른쪽)이 아닌 **양방향(Bidirectional)**으로 문맥을 파악합니다.

- **핵심 원리**: `Cloze Task` (빈칸 채우기)
- **비유**: 
    - SASRec: "아이언맨1, 2를 봤으니 다음은?" (미래 예측)
    - BERT4Rec: "아이언맨1, [빈칸], 어벤져스를 봤네. 빈칸은?" (문맥 파악)

## ⚙️ 구조 (Architecture)
1.  **Embedding**: 아이템 임베딩 + 위치 임베딩
2.  **Transformer Encoder (Bidirectional)**: 
    - SASRec과 달리 Causal Masking을 쓰지 않음 (미래 정보도 봄)
    - 대신 입력 데이터의 일부를 랜덤하게 `[MASK]` 토큰으로 가림
3.  **Prediction**: 가려진 `[MASK]` 부분이 원래 무엇이었는지 맞춤

## 👍 장점 (Pros)
- **깊은 이해**: 앞뒤 문맥을 모두 고려하므로 유저 행동 패턴을 더 깊이 있게 이해함.
- **성능**: 데이터가 충분하다면 SASRec보다 더 높은 성능을 내는 경우가 많음.

## 👎 단점 (Cons)
- **학습 속도**: 빈칸을 채우는 방식으로 학습하느라 SASRec보다 학습 시간이 훨씬 오래 걸림.
- **추론 비용**: 실제 추천(Inference) 시에는 마지막에 `[MASK]`를 붙여서 예측해야 하므로 계산 비용이 듬.

## 💻 코드 예시 (PyTorch)
```python
# SASRec과 거의 동일하지만 Masking 방식이 다름
class BERT4Rec(nn.Module):
    def __init__(self, num_items, max_len):
        super().__init__()
        self.item_emb = nn.Embedding(num_items, 32)
        self.pos_emb = nn.Embedding(max_len, 32)
        # Bidirectional이므로 Causal Mask를 쓰지 않음
        self.encoder = nn.TransformerEncoder(...)

    def forward(self, seq):
        x = self.item_emb(seq) + self.pos_emb(positions)
        # 미래를 가리는 Mask 없이 전체를 다 봄
        out = self.encoder(x) 
        return out
```
