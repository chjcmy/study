### 환경설정
~~~yaml
dependencies:  
flutter:  
sdk: flutter  
grpc: ^3.2.3  
protobuf: ^3.0.0
~~~

### dart 언어 proto type 만들기
~~~bash
flutter pub run protoc --dart_out=grpc:lib/example /lib/proto/example.proto
~~~

### main.dart
~~~dart
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:grpc/grpc_web.dart';
import 'package:untitled/proto/helloworld/helloworld.pbgrpc.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late GrpcWebClientChannel channel;

  @override
  void initState() {
    super.initState();
    // gRPC-Web 프록시 서버의 주소로 변경해야 합니다.
    channel = GrpcWebClientChannel.xhr(Uri.parse('http://0.0.0.0:8080'));
  }

  @override
  void dispose() {
    channel.shutdown();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('gRPC-Web Flutter Client'),
      ),
      body: Center(
        child: ElevatedButton(
          onPressed: () async {
            final client = GreeterClient(channel);
            const name = 'John'; // 원하는 이름으로 변경하세요.
            final request = HelloRequest()..name = name;

            try {
              final response = await client.sayHello(request);
              print(response.message);
            } catch (error) {
              if (kDebugMode) {
                print('Error occurred: $error');
              }
            }
          },
          child: const Text('Say Hello to gRPC-Web Server'),
        ),
      ),
    );
  }
}
~~~

