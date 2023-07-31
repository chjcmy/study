### 의존성 파일
~~~mod
require (
	google.golang.org/grpc v1.57.0
	google.golang.org/protobuf v1.31.0
)
~~~

### go.server
~~~go
package main

import (
	"context"
	"fmt"
	"log"
	"net/http"

	"github.com/improbable-eng/grpc-web/go/grpcweb"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	pb "example/user/hello/pb/helloworld"
)

type GreeterServer struct {
	pb.UnimplementedGreeterServer
}

func (s *GreeterServer) SayHello(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	fmt.Println("Received request:", in.Name)
	return &pb.HelloReply{Message: "Hello, " + in.Name}, nil
}

func main() {
	// Create a GRPC server.
	server := grpc.NewServer()

	// Register the Greeter service on the server.
	pb.RegisterGreeterServer(server, &GreeterServer{})

	// Enable reflection on the server.
	reflection.Register(server)

	// Create an HTTP/2 server with h2c support and attach the gRPC handler to it.
	httpServer := &http.Server{
		Addr: ":8080", // 웹 브라우저에서 접근할 포트 번호로 변경
		Handler: grpcweb.WrapServer(server,
			grpcweb.WithCorsForRegisteredEndpointsOnly(false),
			grpcweb.WithOriginFunc(func(origin string) bool { return true }),
		),
	}

	log.Println("Starting gRPC-Web server at :8080")
	if err := httpServer.ListenAndServe(); err != nil {
		log.Fatalf("Failed to start gRPC-Web server: %v", err)
	}
}
~~~

