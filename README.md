# 📚 Bookstore Microservice System

This is a microservice-based system for managing books and their reviews and orders, built using Spring Boot. It demonstrates robust inter-service communication using both **Feign Clients** (REST) and **gRPC**.

## 🏗️ Architecture Overview

This project follows a microservices architecture with the following core services:

- **Registry Service** – For service discovery.
- **Gateway Service** – To route and secure external requests.
- **Book Service** – Manages book data.
- **Review Service** – Handles user reviews for books.
- **Order Service** – Handles user orders for books.

Services communicate via:

- 🧩 **Feign Client** (HTTP-based calls)
- ⚡ **gRPC** (for performant binary communication)

## 🤝 Feign Client Integration (Review Service → Book Service)

To enable inter-service communication between Review Service and Book Service, we use Spring Cloud OpenFeign, a declarative REST client. This allows the Review Service to fetch book details (e.g., by ISBN) from the Book Service to validate the book before saving a review.

### ⚙️ Setup with JHipster

When generating your microservice with JHipster, make sure to enable Feign client support:

> **Prompt:**
> _"Do you want to generate a Feign client?"_
> Answer: Yes

This scaffolds the required dependencies and base configuration for Feign in your microservice.

### 🧩 Review Service Integration Steps

To achieve this integration in the review service you need to follow the following steps

#### 🔗 Step 1: Define the Feign Client Interface

In your review service you need to create a BookServiceClient interface that acts as a proxy for calling the Book Service

```bash
## src/main/java/com/prunny/reviewservice/client/BookServiceClient

@FeignClient(name = "bookservice")
public interface BookServiceClient {
    @GetMapping("/api/books/isbn/{isbn}")
    BookDTO getBookByIsbn(@PathVariable("isbn") String isbn);
}
```

#### 🔐 Step 2: Secure Feign Requests with Internal JWT

Since the Book Service is protected by Spring Security, the Review Service must authenticate itself when making requests. To achieve secure inter-service communication, we configure a Feign interceptor to generate and attach a JWT token to each outgoing request.

```bash
## src/main/java/com/prunny/reviewservice/client/UserFeignClientInterceptor.java

@Component
public class UserFeignClientInterceptor implements RequestInterceptor {

    private static final String BASE64_SECRET = "<YOUR_BASE64_ENCODED_SECRET>";

    @Override
    public void apply(RequestTemplate template) {
        String internalJwt = generateInternalJwt();
        template.header("Authorization", "Bearer " + internalJwt);
    }

    private String generateInternalJwt() {
        byte[] keyBytes = Base64.getDecoder().decode(BASE64_SECRET);
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA512");

        return Jwts.builder()
            .claim("auth", List.of("INTERNAL_ADMIN"))
            .setSubject("review-service")
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }
}
```

This approach eliminates the need for user context or SecurityContextHolder, making it ideal for background tasks, scheduled jobs, or service-to-service communication in microservices.

> **Note:** Make sure the BASE64_SECRET matches the secret configured in your Book Service's JWT decoder setup.

#### ⚙️ Step 3: Configure Feign to Use the JWT Interceptor

To wire up the Feign client and ensure that all outgoing HTTP requests from the Review Service to the Book Service include the internally generated JWT token, we define a centralized Feign configuration class.

```bash
## src/main/java/com/prunny/reviewservice/config/FeignConfiguration.java

@Configuration
@EnableFeignClients(basePackages = "com.prunny.reviewservice")
@Import(FeignClientsConfiguration.class)
public class FeignConfiguration {

    @Bean
    feign.Logger.Level feignLoggerLevel() {
        return feign.Logger.Level.BASIC;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new UserFeignClientInterceptor();
    }
}
```

#### 📡 Step 4: Use the Feign Client to Fetch Book Data Inside Your Service Logic

After configuring the Feign client and interceptor, the next step is to leverage it in your service layer—in this case, inside the ReviewService—to fetch book details from the Book Service before saving a review.

```bash
## src/main/java/com/prunny/reviewservice/service/ReviewService.java

public ReviewDTO save(ReviewDTO reviewDTO) {
    LOG.debug("Request to save Review : {}", reviewDTO);
    BookDTO bookDTO = bookServiceClient.getBookByIsbn(reviewDTO.getBookIsbn());
    System.out.println("Book Details " + bookDTO);
    Review review = reviewMapper.toEntity(reviewDTO);
    review = reviewRepository.save(review);
    return reviewMapper.toDto(review);
}
```

#### 🛡️ Step 5: Gracefully Handle Feign Client Errors with a Global Exception Handler

When making remote calls using Feign (e.g., to the Book Service), failures such as 404 Not Found, 403 Forbidden, or 500 Internal Server Error can occur. To avoid leaking raw Feign exception details and to provide a clean, user-friendly API response, we handle these errors centrally using a global exception handler.

```bash
## src/main/java/com/prunny/reviewservice/exception/GlobalExceptionHandler.java

@ExceptionHandler(FeignException.class)
public ResponseEntity<ApiResponse> handleFeignException(FeignException ex) {
    String message = "Remote service error";

    try {
        if (ex.content() != null) {
            JsonNode root = objectMapper.readTree(ex.contentUTF8());

            message = root.path("detail").asText();
            if (message == null || message.isBlank()) {
                message = root.path("message").asText("Remote service error");
            }
        }
    } catch (Exception e) {
        message = "Error parsing remote service response";
    }

    ApiResponse apiResponse = new ApiResponse(message, null);
    return ResponseEntity.status(ex.status()).body(apiResponse);
}
```

## ⚡ gRPC Integration (Order Service → Book Service)

To enable high-performance inter-service communication between the Order Service and Book Service, we integrate gRPC — a modern, efficient Remote Procedure Call (RPC) framework. This allows the Order Service to query book availability, pricing, or metadata in real time before confirming or processing an order.

### ⚙️ Setup with JHipster and gRPC

JHipster doesn’t include gRPC support out of the box, but you can manually set it up by adding grpc dependencies

Add the following to the `<dependencies>` section

```bash
## pom.xl

<!--GRPC -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.69.0</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>1.69.0</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>1.69.0</version>
</dependency>
<dependency> <!-- necessary for Java 9+ -->
    <groupId>org.apache.tomcat</groupId>
    <artifactId>annotations-api</artifactId>
    <version>6.0.53</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>3.1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>4.29.1</version>
</dependency>
```

Add the following to the `<build>` section

```bash
## pom.xl

<build>
    <extensions>
        <!-- Ensure OS compatibility for protoc -->
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.0</version>
        </extension>
    </extensions>
    <plugins>
        <!-- Spring boot / maven  -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

        <!-- PROTO -->
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:3.25.5:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.68.1:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                    <configuration>
                        <sources>
                          <source>${project.build.directory}/generated-sources/protobuf/java</source>
                          <source>${project.build.directory}/generated-sources/protobuf/grpc-java</source>
                        </sources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <version>3.2.0</version>
            <executions>
                <execution>
                    <id>add-source</id>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>add-source</goal>
                    </goals>
                    <configuration>
                        <sources>
                            <source>${project.build.directory}/generated-sources/protobuf/java</source>
                            <source>${project.build.directory}/generated-sources/protobuf/grpc-java</source>
                        </sources>
                    </configuration>
                </execution>
            </executions>
    </plugin>

    </plugins>
</build>
```

#### 📦 Step 1: Define a Shared Proto File

We create a `book_service.proto` file that defines the service contract between the Book and Order services.

```bash
## src/main/proto/book_service.proto

syntax = "proto3";

option java_multiple_files = true;
option java_package = "book";

package book;

service BookService {
  rpc ReduceAvailableCopies (ReduceAvailableCopiesRequest) returns (ReduceAvailableCopiesResponse);
}

message ReduceAvailableCopiesRequest {
  string bookIsbn = 1;
  int32 quantity = 2;
}

message ReduceAvailableCopiesResponse {
  bool success = 1;
  string message = 2;
}
```

> **Note:** For ease of development and configuration, especially in early-stage projects or proof-of-concept builds, we place the .proto file directly under src/main/proto/ in both the Book and Order services.

> In a real-world microservices architecture, it's best practice to centralize proto definitions in a shared repository or internal package registry. This ensures consistency across services and prevents duplication or contract drift.

##### Alternative (Recommended in Teams): Use Precompiled .proto JARs

If your team is managing the shared protos, they can:

1. Generate the Java stubs from the .proto files once
2. Package and publish them as a library to a private Maven or GitHub Packages registry, e.g., book-proto-stubs
3. You just consume the precompiled stubs:

```bash
<dependency>
    <groupId>com.prunny.shared</groupId>
    <artifactId>book-proto-stubs</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 🔁 Step 2: Generate gRPC Stubs

To generate the Java classes (stubs) from the .proto file, run:

```bash
mvn clean install
```

- This will generate the required gRPC files (e.g., BookServiceGrpc.java, BookRequest.java, etc.) into:

```bash
target/generated-sources/protobuf/
```

- In IntelliJ (or your IDE), you must mark this directory as a "Source Root" so the generated stubs can be used in your codebase:
  Right-click → Mark Directory as → Generated Sources Root

#### 🧩 Step 3: Implement the gRPC Server in Book Service

```bash
## src/main/java/com/prunny/book_service/grpc/server/BookGrpcService.java

@GrpcService
public class BookGrpcService extends BookServiceGrpc.BookServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BookGrpcService.class);

    private final BookRepository bookRepository;

    public BookGrpcService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void reduceAvailableCopies(book.ReduceAvailableCopiesRequest request, StreamObserver<book.ReduceAvailableCopiesResponse> responseObserver) {
        log.info("ReduceAvailableCopies request received {}", request.toString());

        try {
            Optional<Book> bookOptional = bookRepository.findByIsbn(request.getBookIsbn());
            if (bookOptional.isEmpty()) {
                ReduceAvailableCopiesResponse response = ReduceAvailableCopiesResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Book not found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Book book = bookOptional.get();
            if (book.getAvailableCopies() < request.getQuantity()) {
                ReduceAvailableCopiesResponse response = ReduceAvailableCopiesResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Not enough stock")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            book.setAvailableCopies(book.getAvailableCopies() - request.getQuantity());
            bookRepository.save(book);

            ReduceAvailableCopiesResponse response = ReduceAvailableCopiesResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Successfully reduced available copies of book")
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error while reducing available copies", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Unexpected error occurred")
                    .withCause(e)
                    .asRuntimeException()
            );
        }
    }

}
```

#### 🚀 Step 4: Create the gRPC Client in Order Service

```bash
@Service
public class BookServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BookServiceGrpcClient.class);
    private final BookServiceGrpc.BookServiceBlockingStub blockingStub;

    public BookServiceGrpcClient(
        @Value("${book.service.address:localhost}") String serverAddress,
        @Value("${book.service.grpc.port:9090}") int serverPort
    ) {
        log.info("Connecting to Billing Service GRPC service at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = NettyChannelBuilder
            .forAddress(serverAddress, serverPort)
            .usePlaintext()
            .build();

        blockingStub = BookServiceGrpc.newBlockingStub(channel);
    }

    public ReduceAvailableCopiesResponse reduceAvailableCopies(String bookIsbn, int quantity) {
        ReduceAvailableCopiesRequest request = ReduceAvailableCopiesRequest.newBuilder().setBookIsbn(bookIsbn).setQuantity(quantity).build();

        ReduceAvailableCopiesResponse response = blockingStub.reduceAvailableCopies(request);

        return response;
    }
}
```

#### 📡 Step 5: Use gRPC Client in Order Service to validate Book availability

```bash
public OrderDTO save(OrderDTO orderDTO) {
        LOG.debug("Request to save Order : {}", orderDTO);
        Order order = orderMapper.toEntity(orderDTO);
        order = orderRepository.save(order);

        ReduceAvailableCopiesResponse bookServiceResponse = bookServiceGrpcClient.reduceAvailableCopies(order.getBookIsbn(), order.getQuantity());
        LOG.info("Received response from book service via GRPC: {}", bookServiceResponse);
        if (!bookServiceResponse.getSuccess()) {
            String msg = bookServiceResponse.getMessage().toLowerCase();
            if (msg.contains("not found")) throw new ResourceNotFoundException(msg);
            if (msg.contains("not enough stock")) throw new InsufficientStockException(msg);
            throw new ResourceNotFoundException(bookServiceResponse.getMessage());
        }

        return orderMapper.toDto(order);
    }
```

## Test Book APIs via Postman

Explore Postman API Documentation: [Online Bookstore (Feign & gRPC) - API Docs](https://documenter.getpostman.com/view/28117952/2sB3BALCFz)
