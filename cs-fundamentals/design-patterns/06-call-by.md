# Call by Value / Reference / Sharing

---

## 3가지 호출 방식

```
Call by Value:     값을 복사하여 전달 (원본 변경 ❌)
Call by Reference: 주소를 전달 (원본 변경 ✅)
Call by Sharing:   객체 참조를 값으로 전달 (Python!) ⭐
```

---

## Call by Value (값에 의한 호출)

```
C, Java(기본형)에서 사용

int a = 10;
modify(a);     // a의 "복사본"을 전달
print(a);      // → 10 (원본 변경 안 됨!)

void modify(int x) {
    x = 99;    // 복사본을 변경 → 원본 무관
}

메모리:
  a: [10] ── 복사 ──→ x: [10]
  a: [10]               x: [99] ← x만 변경됨
```

## Call by Reference (참조에 의한 호출)

```
C++ (참조), C# (ref)에서 사용

int a = 10;
modify(&a);    // a의 "주소"를 전달
print(a);      // → 99 (원본도 변경됨!)

void modify(int& x) {
    x = 99;    // 같은 메모리를 수정 → 원본도 변경
}

메모리:
  a: [10] ←── 같은 주소 ──→ x (a의 별명)
  a: [99]                    x (같은 곳)
```

---

## Call by Sharing (Python의 방식) ⭐

```python
# Python은 "Call by Sharing" (= Call by Object Reference)
# 객체의 참조를 "값으로" 전달

# 핵심 규칙:
# 1. 불변 객체 (int, str, tuple): 원본 변경 ❌ (Value처럼 동작)
# 2. 가변 객체 (list, dict, set): 원본 변경 ✅ (Reference처럼 동작)
```

### 불변 객체 (Immutable)

```python
def modify(x):
    print(f"before: id={id(x)}")  # id: 140001
    x = 99                         # 새 객체 생성! (재바인딩)
    print(f"after:  id={id(x)}")  # id: 140999 ← 다른 객체!

a = 10          # id: 140001
modify(a)       
print(a)        # → 10 (원본 안 바뀜!)

# 이유:
# a ──→ [10] (id: 140001)
# x ──→ [10] (같은 객체)
# x = 99 → x ──→ [99] (새 객체, id: 140999)
# a ──→ [10] (여전히 기존 객체)
```

### 가변 객체 (Mutable)

```python
def modify(lst):
    lst.append(99)   # 같은 객체를 수정 → 원본 변경!
    print(f"id: {id(lst)}")  # id 동일

data = [1, 2, 3]
modify(data)
print(data)    # → [1, 2, 3, 99] (원본도 변경됨!)

# 이유:
# data ──→ [1, 2, 3] (id: 200001)
# lst  ──→ [1, 2, 3] (같은 객체!)
# lst.append(99) → 같은 객체 수정
# data ──→ [1, 2, 3, 99] (원본도 변경됨)
```

### 가변 객체의 재바인딩

```python
def modify(lst):
    lst = [99, 100]   # 재바인딩! (새 객체 생성)
    # lst ──→ [99, 100] (새 객체)

data = [1, 2, 3]
modify(data)
print(data)    # → [1, 2, 3] (원본 안 바뀜!)

# 이유: lst = [...] 은 새 객체에 "재바인딩"한 것
# data는 여전히 기존 객체를 가리킴
```

---

## 언어별 비교

| 언어 | 기본형 | 객체/참조형 |
|------|--------|-----------|
| **Python** | - (모두 객체) | **Call by Sharing** |
| Java | Call by Value | Call by Value (참조의 복사) |
| C | Call by Value | 포인터 전달 (수동) |
| C++ | Call by Value | Call by Reference (& 사용) |
| JavaScript | Call by Value | Call by Sharing |
| Go | Call by Value | 포인터/슬라이스 |

---

## 실무에서 주의점

```python
# ❌ 위험: 함수가 인자를 변경 (부작용)
def add_default_config(config: dict):
    config["timeout"] = 30  # 원본 dict 변경!
    return config

settings = {"host": "localhost"}
new_config = add_default_config(settings)
print(settings)  # → {'host': 'localhost', 'timeout': 30} 의도치 않은 변경!

# ✅ 안전: 복사 후 수정 (방어적 복사)
def add_default_config(config: dict):
    result = config.copy()  # 복사본 생성
    result["timeout"] = 30
    return result

# ✅ 더 안전: deepcopy (중첩 객체도 복사)
import copy
def add_default_config(config: dict):
    result = copy.deepcopy(config)
```

### Pydantic의 불변성

```python
from pydantic import BaseModel

class TenantRequest(BaseModel):
    model_config = {"frozen": True}  # 불변 객체!
    tenant_id: str
    name: str

request = TenantRequest(tenant_id="abc", name="Test")
request.name = "Changed"  # → ValidationError! (불변)
```

---

## 면접 핵심 포인트

```
Q: Python은 Call by Value? Reference?
A: Call by Sharing (객체 참조를 값으로 전달).
   불변 객체는 Value처럼, 가변 객체는 Reference처럼 동작.
   핵심은 "재바인딩 vs 내부 수정"의 차이.

Q: 리스트를 함수에 전달하면 원본이 바뀌는 이유?
A: 가변 객체의 참조가 전달되므로 lst.append()는 
   같은 객체를 수정. 단, lst = [...] 재바인딩은 원본 무관.

Q: 부작용을 방지하려면?
A: 1. 방어적 복사 (copy, deepcopy)
   2. 불변 객체 사용 (tuple, frozenset, Pydantic frozen)
   3. 함수형 프로그래밍 스타일 (입력을 변경하지 않고 새 객체 반환)
```
