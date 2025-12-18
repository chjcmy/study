~~~markdown
graph TD;
    Master["Kubernetes Master"] -->|API Server| Node1["Kubernetes Node 1"];
    Master -->|API Server| Node2["Kubernetes Node 2"];
    Node1 -->|Calico| Pod1["Pod 1"];
    Node1 -->|Calico| Pod2["Pod 2"];
    Node2 -->|Calico| Pod3["Pod 3"];
    Node2 -->|Calico| Pod4["Pod 4"];
~~~

1. **간단하고 확장 가능한 네트워킹**: 칼리코는 간단한 설정으로 확장 가능한 네트워킹을 제공한다.
2. **보안 강화**: 네트워크 정책을 통해 특정 포드 간의 트래픽을 제어할 수 있어 보안을 강화할 수 있한다.
3. **고성능**: BGP(Border Gateway Protocol)를 사용하여 고성능 네트워킹을 제공한다.
4. **유연성**: 다양한 네트워크 환경에 쉽게 통합할 수 있다.