# Mini In-Memory Key-Value Store

A lightweight, Redis-inspired key-value store built from scratch in Java. Supports concurrent client connections over TCP, TTL-based key expiration, and LRU eviction — the core mechanics behind real-world caching systems like Redis and Memcached.

## Features

- **Multi-client TCP server** — accepts and handles multiple simultaneous client connections, each on its own thread
- **Core commands** — `SET`, `GET`, `DELETE`, `KEYS`
- **TTL / expiration** — keys can be set with an expiration time (`SET key value EX seconds`); expired keys are efficiently purged using a min-heap rather than scanning the entire dataset
- **LRU eviction** — the store enforces a configurable max size; when full, the least-recently-used key is automatically evicted to make room for new writes
- **Thread-safe** — all store operations are synchronized to safely handle concurrent access from multiple clients

## Architecture

| Component | File | Responsibility |
|---|---|---|
| `Store` | `Store.java` | Core data storage, TTL tracking (min-heap), LRU eviction (access-ordered hash table) |
| `ClientHandler` | `ClientHandler.java` | Parses client commands and translates them into `Store` operations |
| `Server` | `Server.java` | Accepts incoming connections, spins up a handler thread per client, runs a background TTL cleanup thread |

### Design decisions

- **Min-heap for TTL cleanup**: rather than checking every key's expiration on every operation (O(n)), a `PriorityQueue` ordered by expiration time lets the server always know the *next* key to expire, so cleanup work scales with the number of expiring keys, not the total dataset size.
- **LRU via access-ordered `LinkedHashMap`**: leverages Java's built-in access-order tracking (a hash table + doubly linked list under the hood) to maintain recency order in O(1) per access, with eviction handled via `removeEldestEntry`.
- **Per-client threads**: each connected client is handled on its own thread, so one slow or idle client doesn't block others — a simple version of the concurrency model used by real network servers.

## Running it

\`\`\`bash
javac *.java
java Server
\`\`\`

The server starts listening on port `6379` (Redis's default port).

In a separate terminal, connect as a client:

\`\`\`bash
nc localhost 6379
\`\`\`

## Example usage

\`\`\`
SET name Nidhi
OK

GET name
Nidhi

SET session:abc123 loggedin EX 60
OK

GET session:abc123
loggedin

(after 60 seconds)
GET session:abc123
(nil)

KEYS
name
session:abc123
\`\`\`

## Possible extensions

- Persistence (periodic snapshot to disk so data survives a restart)
- A proper client library (rather than raw `nc`)
- Additional commands (`EXPIRE`, `TTL`, `INCR`)