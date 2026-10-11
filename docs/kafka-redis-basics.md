# Kafka & Redis Basics — easy, organized, copy-paste code

A beginner-friendly, self-contained tutorial. Every snippet is standalone — create a fresh Spring Boot project and paste.

---

## PART 1 · REDIS

### 1.1 Mental model (30 seconds)

Redis is an **in-memory key-value store**. You give it a `key`, it gives you a `value` — in microseconds, because everything lives in RAM.

```
SET user:1 "Alice"      →  key: user:1   value: "Alice"
GET user:1              →  "Alice"
```

That's 80% of Redis. The rest is: values come in 5 *shapes*, and keys can have a **TTL** (expire after N seconds).

### 1.2 Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```properties
# application.properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

**That's all the config you need.** Spring Boot auto-creates the connection (Lettuce client). You only add a `RedisTemplate` bean when you want custom serializers.

### 1.3 The template — your gateway to Redis

```java
// Simplest: use the built-in one, no config class needed
@Autowired
private StringRedisTemplate redis;   // keys + values are plain Strings
```

```java
// Or a custom bean (this is what your banking project does)
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory f) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(f);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }
}
```

**The key rule: pick the operations object that matches your value's shape.**

| Value shape | ops object | Redis commands | Use for |
|---|---|---|---|
| One value | `opsForValue()` | `SET GET INCR` | cache, counter, OTP |
| Fields under one key | `opsForHash()` | `HSET HGET` | profile, session, object |
| Ordered list | `opsForList()` | `LPUSH RPOP` | queue, recent items |
| Unique values | `opsForSet()` | `SADD SISMEMBER` | tags, "already seen" |
| Ranked values | `opsForZSet()` | `ZADD ZRANGE` | leaderboard, sorted data |

### 1.4 Code examples (each one is complete)

**Example 1 — cache with TTL (the most common use)**
```java
// SET with 5-minute expiry — perfect for caching
redis.opsForValue().set("product:101", "{\"name\":\"Laptop\",\"price\":999}", 5, TimeUnit.MINUTES);

// GET
String json = redis.opsForValue().get("product:101");
if (json == null) {
    json = loadFromDatabase(101);              // cache miss → load from DB
    redis.opsForValue().set("product:101", json, 5, TimeUnit.MINUTES);
}
```

**Example 2 — atomic counter (rate limiting, views, likes)**
```java
// INCR is atomic — safe under concurrency, no race conditions
Long count = redis.opsForValue().increment("page:home:views");
System.out.println("Views: " + count);

// Rate-limit pattern: start a 60-second window on the FIRST hit
Long n = redis.opsForValue().increment("user:7:requests");
if (n != null && n == 1L) {
    redis.expire("user:7:requests", 60, TimeUnit.SECONDS);
}
if (n > 5) throw new RuntimeException("Too many requests");
```

**Example 3 — Hash (an object's fields)**
```java
HashOperations<String, String, String> h = redis.opsForHash();
h.put("session:abc123", "userId", "7");
h.put("session:abc123", "role", "admin");
h.put("session:abc123", "lastActive", "2026-08-31T10:00:00");

String role = h.get("session:abc123", "role");          // "admin"
Map<String, String> wholeSession = h.entries("session:abc123");
redis.expire("session:abc123", 30, TimeUnit.MINUTES);   // session timeout
```

**Example 4 — List (queue / recent items)**
```java
ListOperations<String, String> l = redis.opsForList();
l.leftPush("notifications:7", "Payment received");
l.leftPush("notifications:7", "Login from new device");

String newest = l.leftPop("notifications:7");           // "Login from new device"
List<String> all = l.range("notifications:7", 0, 9);    // newest 10
```

**Example 5 — Set (uniqueness)**
```java
SetOperations<String, String> s = redis.opsForSet();
s.add("voted:poll:12", "user:1");
s.add("voted:poll:12", "user:1");                       // ignored — already there

boolean alreadyVoted = s.isMember("voted:poll:12", "user:1");  // true
```

**Example 6 — lifecycle: TTL check, delete, atomic expire**
```java
Long ttl = redis.getExpire("product:101");              // seconds left, -1 = never expires
redis.delete("product:101");                            // remove immediately
redis.opsForValue().set("k", "v");                      // no TTL = lives forever (leak!)
```

### 1.5 CLI cheatsheet

```bash
redis-cli SET user:1 Alice
redis-cli GET user:1
redis-cli SET token:9 abc123 EX 300        # SET with 300s TTL
redis-cli TTL token:9                      # seconds remaining
redis-cli INCR page:views
redis-cli HSET session:x role admin
redis-cli KEYS 'user:*'                    # list keys by pattern
redis-cli FLUSHALL                         # DANGER — wipes everything
```

### 1.6 Gotchas

- **TTL is your cleanup.** No TTL = keys accumulate forever.
- **Serializers must match.** If you see `\xAC\xED` garbage in `redis-cli`, your value serializer is JDK default — switch to `StringRedisSerializer`.
- **Redis is not your database.** It's a fast cache/scratchpad. Losing it must not lose business data.
- **Don't store huge blobs.** RAM is expensive; keep values small.

---

## PART 2 · KAFKA

### 2.1 Mental model (30 seconds)

Kafka is a **distributed append-only log**. Producers *append* messages to a **topic**; consumers *read* from the log and keep a bookmark (offset).

The key difference from a queue: **every consumer group reads every message**. One event can feed 3 services, each at its own pace.

### 2.2 Concepts

| Concept | Meaning | Analogy |
|---|---|---|
| **Topic** | A named stream of events | a magazine |
| **Partition** | A topic split into ordered logs | copies of the magazine |
| **Offset** | Your reading position | bookmark |
| **Producer** | Publishes messages | writer |
| **Consumer** | Subscribes and reads | reader |
| **Consumer group** | A set of consumers sharing the topic | a reading club (each member reads different pages) |
| **Broker** | The Kafka server | the publishing house |

**Consumer groups rule:** *different* services = *different* groups = each gets every message. *Same* service scaled out = *same* group = work split between instances.

### 2.3 Setup

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

```properties
# application.properties — copy this block as-is
spring.kafka.bootstrap-servers=localhost:9092

# how messages are written
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# how messages are read back (group id MUST be unique per service)
spring.kafka.consumer.group-id=my-app-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

### 2.4 Producer — publishing

```java
@Service
public class OrderEventPublisher {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void orderCreated(Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", order.getId());
        event.put("customerId", order.getCustomerId());
        event.put("total", order.getTotal());

        // send(topic, key, value)
        // key = orderId → same order always lands in the same partition → order preserved
        kafkaTemplate.send("order.created", order.getId(), event);
    }
}
```

Send a plain string instead? Just use `KafkaTemplate<String, String>` and `send(topic, key, jsonString)`.

### 2.5 Consumer — subscribing

```java
@Service
public class OrderEventListener {

    @KafkaListener(topics = "order.created")
    public void onOrderCreated(@Payload Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        Double total = (Double) event.get("total");
        System.out.println("Processing order " + orderId + " for $" + total);
    }
}
```

That's the whole pattern: **publish with `KafkaTemplate.send`, consume with `@KafkaListener`.**

### 2.6 Sending a typed DTO instead of a Map

```java
// 1. The DTO — must have a no-arg constructor + getters/setters
public class OrderCreatedEvent {
    private String orderId;
    private BigDecimal total;
    // no-arg constructor, getters, setters...
}
```

```java
// 2. Producer — send the object directly
kafkaTemplate.send("order.created", event.getOrderId(), event);
```

```java
// 3. Consumer — same class, deserialized automatically
@KafkaListener(topics = "order.created")
public void onOrderCreated(@Payload OrderCreatedEvent event) {
    System.out.println("Order " + event.getOrderId() + " → " + event.getTotal());
}
```

> If you get deserialization errors with DTOs, the easiest fix is the Map approach (2.4) — zero classes, zero headaches.

### 2.7 CLI cheatsheet

```bash
# list all topics
docker exec -it kafka kafka-topics --bootstrap-server kafka:29092 --list

# watch messages live (debugging gold)
docker exec -it kafka kafka-console-consumer --bootstrap-server kafka:29092 \
  --topic order.created --from-beginning

# publish a test message
docker exec -it kafka kafka-console-producer --bootstrap-server kafka:29092 \
  --topic order.created
```

### 2.8 Gotchas

- **at-least-once delivery** — after a crash, a consumer may see a message twice. Make consumers **idempotent** (e.g. check a unique key / idempotency table).
- **`group-id` must differ between services.** Same group = they steal each other's messages.
- **Ordering is per-key.** Different keys can arrive out of order; same key never is.
- **`earliest`** = a brand-new group reads from the beginning (misses nothing). Existing groups keep their saved offset.

---

## PART 3 · Redis vs Kafka — when to use which

| Question | Redis | Kafka |
|---|---|---|
| Data lives in | RAM (fast, volatile-ish) | Disk log (durable, replayable) |
| Access pattern | read/write one key instantly | append + stream to consumers |
| One message reaches | one caller | every consumer group |
| Latency | microseconds | milliseconds |
| Good for | cache, counter, session, OTP, rate limit | events, SAGA, notifications, audit trail |
| Bad for | durable source of truth, broadcasting | low-latency key lookup |

**Rule of thumb:** *"store it"* → Redis. *"tell other services about it"* → Kafka. *"keep it forever"* → your database.

---

## PART 4 · Mini combined demo (paste into one project)

```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

// --- 1. Producer: publish + cache ---
@Service
public class OrderService {
    @Autowired private KafkaTemplate<String, Object> kafka;
    @Autowired private StringRedisTemplate redis;

    public void placeOrder(String orderId) {
        // tell everyone
        kafka.send("order.created", orderId, Map.of("orderId", orderId, "status", "NEW"));

        // remember locally (5 min)
        redis.opsForValue().set("order:" + orderId, "NEW", 5, TimeUnit.MINUTES);
    }
}

// --- 2. Consumer: react to the event ---
@Service
public class NotificationListener {
    @Autowired private StringRedisTemplate redis;

    @KafkaListener(topics = "order.created")
    public void onOrder(@Payload Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        System.out.println("Send email for order " + orderId);

        // each service reads its own state from Redis if needed
        String status = redis.opsForValue().get("order:" + orderId);
    }
}
```

**The pattern:** Kafka moves the event between services; Redis gives each service fast shared state. That's exactly how your banking project works — 15 topics for the SAGA, Redis for OTPs and fraud counters.
