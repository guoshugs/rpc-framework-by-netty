RPC Framework by Netty

This project implements a custom RPC (Remote Procedure Call) framework using Netty. It is designed for learning and experimenting with the principles of distributed systems, serialization, network communication, and service registration.

Features

- Custom RPC protocol
- Service registration and discovery
- Netty-based network communication
- Load balancing strategies
- Support for multiple serializers (e.g., Kryo, JSON)
- Annotation-based service configuration

Project Structure

- rpc-api: Contains common interfaces and data transfer objects.
- rpc-core: Core RPC logic including protocol handling, serialization, and transport.
- rpc-demo: Example client and server implementation.
- rpc-common: Shared utilities and constants.

Getting Started

To run the demo:

1. Build the project using Maven:
       mvn clean install
2. Run the demo server:
       java -jar rpc-demo-server.jar
3. Run the demo client:
       java -jar rpc-demo-client.jar

Requirements

- Java 8+
- Maven

License

This project is for educational purposes and is open source under the MIT License.
