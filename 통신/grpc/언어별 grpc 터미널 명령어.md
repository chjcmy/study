## rust

~~~shell
protoc --rust-grpc_out=./backend/src/proto --plugin=protoc-gen-rust-grpc=/Users/choeseonghyeon/.cargo/bin/grpc_rust_plugin --experimental_allow_proto3_optional main.proto
~~~

## flutter

~~~shell
protoc --dart_out=grpc:frontend/lib/proto main.proto  
~~~

