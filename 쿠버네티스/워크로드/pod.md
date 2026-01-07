	쿠버네티스에서 생성하고 관리할 수 있는 배포 가능한 가장 작은 컴퓨팅 단위이다.

1. **Graceful Termination**: Kubernetes는 Pod를 삭제할 때, Pod 내의 컨테이너가 정상적으로 종료될 수 있도록 시간을 줍니다. 이를 "graceful termination"이라고 합니다. 이 과정에서 컨테이너는 현재 진행 중인 작업을 완료하고, 필요한 정리 작업을 수행합니다. 이로 인해 삭제 시간이 길어질 수 있다.
    
2. **Resource Cleanup**: Pod를 삭제할 때, Kubernetes는 해당 Pod와 관련된 모든 리소스를 정리해야 합니다. 예를 들어, 네트워크 연결을 해제하고, 볼륨을 분리하며, 메모리와 CPU 리소스를 해제합니다. 이 과정이 시간이 걸릴 수 있다.
    
3. **Deployment 관리**: Pod가 Deployment에 의해 관리되고 있는 경우, Deployment는 삭제된 Pod를 대체하기 위해 새로운 Pod를 생성합니다. 이로 인해 삭제 과정이 복잡해질 수 있다.