# Async Aggregator Demo - SSE Pattern

This project demonstrates an asynchronous aggregation pattern using Server-Sent Events (SSE) for real-time updates.

## Architecture

The system consists of three types of services:

1. **Client** (port 8082): Web UI built with Spring MVC and Thymeleaf
   - Provides a form to trigger aggregation requests
   - Opens SSE connection to receive live updates from the aggregator
   - Displays journal notes sorted by date (descending)

2. **Aggregator** (port 8080): Spring WebFlux-based orchestrator
   - Receives journal aggregation requests from the client
   - Calls three resource services in parallel
   - Receives callbacks from resources
   - Streams results to clients via SSE

3. **Resource Services** (3 instances): Spring WebFlux-based data providers
   - Each instance generates sample journal notes
   - Accepts or rejects requests based on delay parameter (-1 = reject)
   - Calls back to aggregator after specified delay

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Spring WebFlux (for aggregator and resource services)
- Spring MVC + Thymeleaf (for client)
- Maven
- Docker & Docker Compose

## Data Flow (SSE Pattern - Option C)

1. Client submits request to aggregator with `patientId` and `delays`
2. Aggregator generates a `correlationId` and calls all 3 resources in parallel
3. Client receives response with `respondents` count and `correlationId`
4. Client opens SSE connection: `GET /aggregate/stream?correlationId=...`
5. Resources process requests (with delays) and POST callbacks to aggregator
6. Aggregator receives callbacks and pushes them to client via SSE
7. Client receives SSE events and updates UI with journal notes

## Building and Running

### Prerequisites

- Docker and Docker Compose installed
- Java 17 (for local development)
- Maven (for local development)

### Run with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Access the client UI
# Open browser to: http://localhost:8082
```

### Local Development (without Docker)

Build all modules:
```bash
mvn clean package
```

Run services in separate terminals:

```bash
# Terminal 1 - Aggregator
cd aggregator
mvn spring-boot:run

# Terminal 2-4 - Resource services
cd resource
RESOURCE_ID=resource-1 SERVER_PORT=8081 mvn spring-boot:run
RESOURCE_ID=resource-2 SERVER_PORT=8083 mvn spring-boot:run
RESOURCE_ID=resource-3 SERVER_PORT=8084 mvn spring-boot:run

# Terminal 5 - Client
cd client
mvn spring-boot:run
```

Note: For local development, update the URLs in `application.yml` files accordingly.

## Usage

1. Open the client UI at `http://localhost:8082`
2. Enter a Patient ID (e.g., `patient-123`)
3. Enter delays as comma-separated values (e.g., `1000,2000,3000`)
   - Use `-1` to simulate a resource rejecting the request
   - Use `0` for immediate response
   - Use positive numbers for delay in milliseconds
4. Click "Call Aggregator"
5. Watch as journal notes arrive in real-time via SSE
6. Notes are automatically sorted by date (newest first)

## Example Delay Patterns

- `1000,2000,3000` - All three resources accept with 1s, 2s, and 3s delays
- `-1,1000,2000` - First resource rejects, other two accept
- `0,0,0` - All resources respond immediately
- `5000,10000,15000` - Longer delays to demonstrate async behavior

## API Endpoints

### Aggregator Service (port 8080)

- `POST /aggregate/journals` - Initiate journal aggregation
  ```json
  {
    "patientId": "patient-123",
    "delays": "1000,2000,3000"
  }
  ```
  Response:
  ```json
  {
    "respondents": 3,
    "correlationId": "uuid"
  }
  ```

- `GET /aggregate/stream?correlationId={id}` - SSE endpoint for receiving callbacks
  
- `POST /aggregate/callback` - Endpoint for resources to post callbacks
  ```json
  {
    "source": "resource-1",
    "patientId": "patient-123",
    "correlationId": "uuid",
    "delayMs": 1000,
    "status": "ok",
    "notes": [...]
  }
  ```

### Resource Service (port 8081)

- `POST /journals` - Process journal request
  ```json
  {
    "patientId": "patient-123",
    "delay": 1000,
    "callbackUrl": "http://aggregator:8080/aggregate/callback",
    "correlationId": "uuid"
  }
  ```
  Returns:
  - `200 OK` if delay >= 0 (accepts request)
  - `401 Unauthorized` if delay == -1 (rejects request)

## Project Structure

```
async-aggregator/
├── aggregator/          # Aggregator service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── resource/            # Resource service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── client/              # Client UI
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml   # Orchestration
└── pom.xml             # Parent POM
```

## Stopping the Services

```bash
# Stop and remove containers
docker-compose down

# Stop and remove containers with volumes
docker-compose down -v
```

## Notes

- All services use in-memory storage only
- SSE connections are managed per correlationId
- The client automatically closes SSE connections when complete
- CORS is enabled on the aggregator to allow browser connections
