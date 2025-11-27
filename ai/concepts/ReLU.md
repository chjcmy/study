# ReLU (Rectified Linear Unit)

## 📌 개념 (Concept)
딥러닝에서 가장 많이 사용되는 **활성화 함수(Activation Function)**입니다. 입력값이 0보다 작으면 0으로, 0보다 크면 입력값 그대로 출력하는 아주 단순한 함수입니다.

- **수식**: 
    - 함수값: $f(x) = \max(0, x)$
    - 미분값(Gradient): $f'(x) = \begin{cases} 1 & \text{if } x > 0 \\ 0 & \text{if } x \le 0 \end{cases}$
- **핵심 역할**: **"비선형성(Non-linearity) 부여"** 및 **"학습 효율성 증대"**

## ⚙️ 왜 쓸까? (Why ReLU?)
1.  **Vanishing Gradient 문제 해결**: 
    - 예전에는 Sigmoid 함수를 많이 썼는데, 층이 깊어질수록 미분값(기울기)이 0에 가까워져서 학습이 안 되는 문제가 있었습니다.
    - ReLU는 양수 구간에서 미분값이 항상 1이므로, 깊은 신경망에서도 신호가 죽지 않고 잘 전달됩니다.
2.  **계산 속도**: 
    - 지수 함수($e^x$)를 쓰는 Sigmoid나 Tanh보다 계산이 훨씬 빠릅니다. (단순 비교 연산만 하면 됨)
3.  **희소성(Sparsity)**:
    - 음수 입력은 모두 0이 되므로, 불필요한 뉴런을 끄는 효과가 있어 모델이 가벼워집니다.

## ⚠️ 단점 (Dying ReLU)
- 입력값이 계속 음수면 미분값이 0이 되어, 해당 뉴런이 영원히 죽어버리는(업데이트가 안 되는) 현상이 발생할 수 있습니다.
- 이를 보완하기 위해 `Leaky ReLU`, `ELU` 같은 변형들이 나왔습니다.

## 💻 코드 예시 (PyTorch)
```python
import torch.nn as nn

# 1. 레이어로 정의해서 쓰기
relu = nn.ReLU()
output = relu(input_tensor)

# 2. 모델 안에 넣기
model = nn.Sequential(
    nn.Linear(64, 32),
    nn.ReLU(),  # 여기서 비선형성 추가!
    nn.Linear(32, 10)
)
```
