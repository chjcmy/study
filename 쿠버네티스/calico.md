~~~markdown
graph TD;
    Master["Kubernetes Master"] -->|API Server| Node1["Kubernetes Node 1"];
    Master -->|API Server| Node2["Kubernetes Node 2"];
    Node1 -->|Calico| Pod1["Pod 1"];
    Node1 -->|Calico| Pod2["Pod 2"];
    Node2 -->|Calico| Pod3["Pod 3"];
    Node2 -->|Calico| Pod4["Pod 4"];
~~~

