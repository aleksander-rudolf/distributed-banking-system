# Load Balancer

We have two Nginx reverse proxies running on two different machines (Server 1 is running on an OVH VPS, Server 2 is running on an Amazon EC2 instance)

## Server 1

Operates `server_i` and `server_j` locally.  
All requests for `server_k` and `server_read` to Server #2.

## Server 2

Operates `server_k` and `server_read` locally.  
All requests for `server_i` and `server_j` to Server #1.

## Ports

### Client

TCP port `559` is the `write` port. (Servers I, J, and K)  
TCP port `560` is the `read` port. (Read Server)

### Server

TCP port `2025` is `Server J`.  
TCP port `2026` is `Server I`.  
TCP port `2027` is `Server K`.  
TCP port `2028` is `Server Read`.
