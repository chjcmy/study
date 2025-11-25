# SASRec (Self-Attentive Sequential Recommendation)

## 📌 개념 (Concept)
유저 행동의 **"순서(Sequence)"**를 중요하게 생각하는 모델입니다. NLP(자연어 처리)의 **Transformer** 구조를 추천 시스템에 적용하여, "다음에 무엇을 볼지" 예측합니다.

- **핵심 원리**: `Self-Attention` (Transformer Encoder)
- **비유**: 
    - "아이언맨1 -> 아이언맨2 -> ?"
    - 과거의 시청 기록을 문장처럼 읽고, 문맥을 파악하여 다음 단어(영화)를 맞춤.

## ⚙️ 구조 (Architecture)
1.  **Embedding**: 아이템 임베딩 + **위치 임베딩(Positional Embedding)** (순서 정보를 주기 위해)
2.  **Self-Attention Block**: 
    - 각 아이템이 시퀀스 내의 다른 아이템들과 어떤 관계가 있는지 계산 (가중치)
    - **Causal Masking**: 미래의 정보(뒤에 오는 영화)는 보지 못하게 가림
3.  **Prediction**: 마지막 시점의 출력 벡터와 아이템 임베딩을 내적하여 다음 아이템 예측

## 👍 장점 (Pros)
- **순서 파악**: 유저의 취향 변화나 시리즈물 시청 패턴을 기가 막히게 잘 잡음.
- **병렬 처리**: RNN(순환 신경망)보다 학습 속도가 빠름.
- **SOTA**: 현재 추천 시스템 연구 및 실무에서 가장 성능이 좋은 모델 중 하나.

## 👎 단점 (Cons)
- **데이터 요구량**: 시퀀스 데이터가 충분히 쌓여야 성능이 잘 나옴.
- **메모리**: 긴 시퀀스를 처리할 때 메모리를 많이 사용함.

## 💻 코드 예시 (PyTorch)
```python
class SASRec(nn.Module):
    def __init__(self, num_items, max_len):
        super().__init__()
        self.item_emb = nn.Embedding(num_items, 32)
        self.pos_emb = nn.Embedding(max_len, 32)
        self.encoder = nn.TransformerEncoder(...)

    def forward(self, seq):
        # Masking 미래 참조 방지
        mask = generate_square_subsequent_mask(seq.size(1))
        x = self.item_emb(seq) + self.pos_emb(positions)
        out = self.encoder(x, mask=mask)
        return out
```
