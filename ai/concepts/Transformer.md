# Transformer (Self-Attention)

## 📌 개념 (Concept)
구글이 2017년 "Attention is All You Need" 논문에서 발표한 혁신적인 신경망 구조입니다. 원래는 번역(기계 번역)을 위해 만들어졌지만, 지금은 GPT, BERT 등 모든 최신 AI의 기반이 되었습니다.

- **핵심 역할**: **"문맥(Context)과 관계(Relation) 파악"**
- **Self-Attention**: 문장(또는 시퀀스) 내의 단어들이 서로 어떤 관계가 있는지 스스로(Self) 계산합니다.

## ⚙️ 원리 (추천 시스템 관점)
1.  **Input**: `[아이언맨1, 아이언맨2, 어벤져스]`
2.  **Attention**: 
    - '어벤져스'를 볼 때, 과거의 '아이언맨1'과는 30% 관련이 있고, '아이언맨2'와는 70% 관련이 있구나!
    - 이렇게 **중요한 정보에 더 집중(Attention)**하여 정보를 요약합니다.
3.  **Output**: 각 영화가 전체 시퀀스 내에서 가지는 의미를 담은 새로운 벡터.

## 💡 추천 시스템에서의 활용
- **SASRec**: 과거 시청 기록(Sequence)에서 어떤 영화가 다음 영화 선택에 결정적인 영향을 미쳤는지 파악합니다.
- **BERT4Rec**: 앞뒤 문맥을 모두 고려하여 빈칸(추천)을 채웁니다.
- RNN(순환 신경망)보다 학습 속도가 빠르고, 긴 시퀀스도 잘 기억합니다.

## 💻 코드 예시 (PyTorch)
```python
# Transformer Encoder Layer
encoder_layer = nn.TransformerEncoderLayer(d_model=64, nhead=4)
transformer = nn.TransformerEncoder(encoder_layer, num_layers=2)

# 입력: (시퀀스 길이, 배치 크기, 임베딩 차원)
output = transformer(input_embeddings)
```
