	컴퓨터는 반복 작업을 수행하기에 이상적인 존재다

```kotlin
fun condition(i: Int) = i < 100  
  
fun main() {  
    var i = 0  
    while (condition(i)) {  
        print(".")  
        i += 10  
    }  
}
```

