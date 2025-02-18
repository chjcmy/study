# 📂 CachedNetworkImage 정리 (Flutter)

## 📝 **1. CachedNetworkImage란 무엇인가요?**
`CachedNetworkImage`는 Flutter에서 **이미지를 캐싱**하여 **인터넷 없이도 빠르게 이미지를 불러올 수 있도록 도와주는 라이브러리**입니다. Android와 iOS는 캐싱 방식에 차이가 있기 때문에 **`cacheKey`**나 **`CacheManager`**를 활용해 캐싱 정책을 통일하는 것이 좋습니다.

---

## 🚀 **2. 기본 사용법**
```dart
import 'package:cached_network_image/cached_network_image.dart';

CachedNetworkImage(
  imageUrl: 'https://example.com/image.jpg',
  placeholder: (context, url) => CircularProgressIndicator(),
  errorWidget: (context, url, error) => Icon(Icons.error),
  fit: BoxFit.cover,
)
```

---

## ⚠️ **3. Android와 iOS의 캐싱 차이점**
| 구분                | Android                                    | iOS                                      |
|--------------------|-----------------------------------------|-----------------------------------------|
| **캐싱 위치**         | 내부 저장소 (`cacheDir`)                 | `NSCachesDirectory` (임시 캐시 폴더)   |
| **캐싱 유지 기간**     | 앱 삭제 전까지 유지                     | 저장 공간 부족 시 자동 삭제             |
| **캐싱 방식**         | 디스크 캐싱 + 메모리 캐싱                | 디스크 캐싱 + 메모리 캐싱                |
| **캐시 정리 시점**     | 수동으로 정리해야 함 (`clearCache()`)   | 시스템이 자동으로 캐시를 정리함        |

---

## 💡 **4. 캐싱 문제 해결 및 플랫폼 동기화 방법**
### ✅ **방법 1: `cacheKey` 지정 (권장)**
```dart
CachedNetworkImage(
  imageUrl: imageUrl,
  cacheKey: 'product_${imageUrl.hashCode}', // 캐시 키 명시
  placeholder: (context, url) => CircularProgressIndicator(),
  errorWidget: (context, url, error) => Icon(Icons.error),
)
```

### ✅ **방법 2: `CacheManager` 사용 (커스텀 캐싱 정책)**
```dart
import 'package:flutter_cache_manager/flutter_cache_manager.dart';

class CustomCacheManager extends CacheManager {
  static const key = 'customCacheKey';
  CustomCacheManager() : super(Config(
    key,
    stalePeriod: Duration(days: 7), // 캐싱 유지 기간
    maxNrOfCacheObjects: 100,       // 최대 캐싱 개수
  ));
}
```
```dart
CachedNetworkImage(
  imageUrl: imageUrl,
  cacheManager: CustomCacheManager(), // 커스텀 캐시 매니저 적용
  placeholder: (context, url) => CircularProgressIndicator(),
  errorWidget: (context, url, error) => Icon(Icons.error),
)
```

---

## 🧹 **5. 캐시 정리 방법 (`clearCache`)**
```dart
Future<void> clearAllCache() async {
  await CachedNetworkImage.evictFromCache(imageUrl); // 특정 이미지 캐시 삭제
  await DefaultCacheManager().emptyCache();         // 모든 캐시 삭제
}
```

---

## 📝 **6. 정리**
| 🚀 추천 방법                | 설명                                    |
|------------------------|---------------------------------------|
| ✅ `cacheKey` 사용       | 플랫폼 간 캐싱 불일치를 방지할 수 있습니다. |
| ✅ `CacheManager` 사용   | 캐싱 정책을 세밀하게 설정할 수 있습니다.  |
| ✅ `clearCache()` 호출   | 오래된 캐시나 불필요한 캐시를 정리할 수 있습니다. |

### 💯 이와 같이 설정하면 Android와 iOS 모두에서 `CachedNetworkImage`를 **일관성 있게** 최적화하여 사용할 수 있이다
