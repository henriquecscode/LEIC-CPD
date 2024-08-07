# README

To compile our code, with a command line in ***newproj\src\main\java***:

```java
javac MyCLI.java
```

This is a special CLI we did to help with running the program.

```java
java MyCLI
```

After this, it's possible to invoke both the client and server implementations.

```java
java TestClient <node_ap> <operation> [<opnd>]
java Store <IP_mcast_addr> <IP_mcast_port> <node_id>  <Store_port>
```