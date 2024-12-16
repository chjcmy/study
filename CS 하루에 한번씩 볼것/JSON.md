JSON은 경량의 데이터 교환 형식으로, 사람이 읽고 쓰기 쉽고 기계가 파싱하고 생성하기 쉽습니다.

## 기본 구조

- JSON은 두 가지 구조를 기본으로 합니다:
    
    1. 이름/값 쌍의 집합 (객체)
    2. 값의 순서화된 리스트 (배열)
    

## 데이터 타입

JSON은 다음과 같은 데이터 타입을 지원합니다:

- 숫자 (Number)
- 문자열 (String)
- 불리언 (Boolean)
- 배열 (Array)
- 객체 (Object)
- null

## 문법

1. 객체: 중괄호 {}로 표현json
```json
    `{"name": "John", "age": 30}`
```
    
2. 배열: 대괄호 []로 표현json
    ```json
    ["apple", "banana", "cherry"]
    ```

    
3. 값: 큰따옴표로 묶인 문자열, 숫자, true/false, null, 객체, 배열json
    
    ```json
{   
	"name": "John",
	"age": 30,  
	"isStudent": false,  
	"grades": [85, 90, 78],  
	"address": null 
	}
```
    

## 특징

- 언어 독립적: 대부분의 프로그래밍 언어에서 사용 가능
- 자기 서술적 (self-describing): 데이터 구조를 쉽게 이해할 수 있음
- 계층적 구조: 복잡한 데이터 구조 표현 가능

## 사용 사례

- API 응답 형식
- 설정 파일
- 데이터 저장 및 전송

#JSON #데이터포맷 #웹개발 #API #데이터교환 #JavaScript