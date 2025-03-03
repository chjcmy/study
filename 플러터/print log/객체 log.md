log에서 객체를 불러오면 상속 받은 toString 을 불러온다

```dart
@override

String toString() {

return 'Product(name: ${name.en}, brand: ${brand.en}, price: $price)';

}
```
