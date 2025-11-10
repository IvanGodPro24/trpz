# HTTP Server

This project implements an **HTTP server** with support for:

- basic HTTP requests (GET, POST, PUT, DELETE),
- **keep-alive** connections,
- **custom CHTML templating engine**,
- **event system** (EventBus),
- **metrics** (RPS, average response time),
- **peer-to-peer synchronization** between nodes,
- data persistence in **MongoDB**.

---

## Project Structure

```text
src/
├─ main/
│  ├─ java/
│  │  ├─ builder/                – HTTP response building
│  │  ├─ chtml/                  – Template engine for *.chtml files
│  │  ├─ db/                     – MongoDB repositories
│  │  ├─ events/                 – Event system (RequestEvent, ResponseEvent)
│  │  ├─ factory/                – Classes for creating different HTTP response types
│  │  ├─ http/                   – Request parser and response serializer
│  │  ├─ httpserver/             – Main
│  │  ├─ logging/                – Simple logger
│  │  ├─ mediator/               – Mediator between server and handler
│  │  ├─ metrics/                – Metrics collection and persistence
│  │  ├─ model/                  – HttpRequest and HttpResponse models
│  │  ├─ p2p/                    – Peer-to-peer exchange between nodes
│  │  ├─ server/                 – Controllers, router, server logic
│  │  └─ state/                  – Server state (Initializing, Open, Closing)
│  └─ resources/
│     ├─ templates/
│     │  ├─ home.chtml
│     │  ├─ contact.chtml
│     │  └─ contact_response.chtml
└─
```

---

## Environment Setup

1. Install **Java 17+**

   ```bash
   java -version
   ```

2. Use **MongoDB Atlas**.  
   Save the URI in a `.env` file (example in `.env.example`):

   ```env
   MONGODB_URI=mongodb+srv://username:password@cluster0.mongodb.net
   MONGODB_DATABASE=mydatabase

   # Keep-alive settings
   KEEP_ALIVE_TIMEOUT_MS=10000
   MAX_REQUESTS_PER_CONNECTION=100

   # Metrics collection interval
   INTERVAL_SECONDS=60

   # Peer network synchronization settings
   PEER_SYNC_INTERVAL_SECONDS=600
   PEER_SYNC_LAST_N=200

   ```

3. Add required **Maven** dependencies:

   ```xml
    <dependencies>
        <dependency>
            <groupId>org.mongodb</groupId>
            <artifactId>mongodb-driver-sync</artifactId>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.9</version>
        </dependency>

        <dependency>
            <groupId>org.json</groupId>
            <artifactId>json</artifactId>
            <version>20240303</version>
        </dependency>

        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.5.13</version>
        </dependency>

        <dependency>
            <groupId>io.github.cdimascio</groupId>
            <artifactId>dotenv-java</artifactId>
            <version>3.0.0</version>
        </dependency>
    </dependencies>

   ```

---

## Running the Project

### Via IDE (IntelliJ IDEA)

1. Open the project.
2. Run the class:

   ```java
   httpserver.Main
   ```

3. The server will start two nodes:
   - `PeerA` on port `8080`
   - `PeerB` on port `8081`

---

## Server Endpoints

- **Home page:**  
  [http://localhost:8080/home](http://localhost:8080/home)

- **Contact form:**  
  [http://localhost:8080/contact](http://localhost:8080/contact)

- **Metrics:**  
  [http://localhost:8080/metrics](http://localhost:8080/metrics)

- **Peer list:**  
  [http://localhost:8080/peers](http://localhost:8080/peers)

---

## Testing MongoDB Connection

1. Ensure MongoDB is running.
2. The `MongoDBConnection` class performs automatic connection during `PeerNode` creation.
3. Check the console — you should see:

   ```text
   MongoDB initialized successfully. Database: mydatabase
   ```

---

## CHTML Template Engine

Templates are located in `src/main/resources/templates/`  
Example — `home.chtml`:

```html
<html>
  <body>
    <h1>Hello, {{ name }}</h1>
    <p>There are {{ count }} items.</p>

    {% if items %}
    <ul>
      {% for it in items %}
      <li>{{ it }}</li>
      {% endfor %}
    </ul>
    {% endif %}
  </body>
</html>
```

---

## P2P Mode

Each `PeerNode` is a separate server that synchronizes request statistics with other nodes.

In `Main.java`:

```java
PeerNode nodeA = new PeerNode("PeerA", 8080, List.of("localhost:8081"));
PeerNode nodeB = new PeerNode("PeerB", 8081, List.of("localhost:8080"));
nodeA.start();
nodeB.start();
```

Nodes exchange statistics via `/sync/stats` and persist data in MongoDB.

---

## Stopping Nodes

The server can be stopped from the console or by calling:

```java
nodeA.stop();
nodeB.stop();
```

---

## License

MIT License © 2025

---
