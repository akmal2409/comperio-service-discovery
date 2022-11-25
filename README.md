# Comperio - lightweight service discovery tool

Comperio borrowed its name from Latin which literally means "discover" or "find out" because that is exactly what it does.

## System Components
Comperio offers discovery via an HTTP server (REST API) (soon to come), gRPC (soon to come) and via embedded DNS server (soon to come).

Replication - Comperio was designed for high availability and therefore, we made it sure that the data has to propagate to other nodes as soon as possible so that the cluster can withstand the loss of several nodes. For this particular purpose Gossiping protocol has been chosen that provides a best effort delivery at low costs. The whole architecture is completely peer-to-peer without leaders.

# Requirements 
- Java 19
- Maven
