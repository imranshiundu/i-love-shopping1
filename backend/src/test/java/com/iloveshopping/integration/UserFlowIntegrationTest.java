package com.iloveshopping.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iloveshopping.entity.Brand;
import com.iloveshopping.entity.Category;
import com.iloveshopping.entity.Product;
import com.iloveshopping.repository.BrandRepository;
import com.iloveshopping.repository.CategoryRepository;
import com.iloveshopping.repository.ProductRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end user-flow integration test: a shopper registers, signs in,
 * browses the catalog, adds an item to the cart, checks out and can read
 * the created order — against real PostgreSQL, Redis and RabbitMQ
 * containers, through the real HTTP stack.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserFlowIntegrationTest {

    // Fixed host ports below the ephemeral range (32768+): random mapped ports
    // collide with locally allocated source ports under load, killing the
    // first connection attempts (self-connect rejection on loopback).
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("iloveshopping_it")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("stringtype", "unspecified")
            .withCreateContainerCmdModifier(cmd -> cmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(15433), ExposedPort.tcp(5432))));

    // All four RabbitMQ ports stay mapped (AMQPS, AMQP, management HTTP/HTTPS):
    // replacing the whole binding list with only 5672 starves the container's
    // wait strategy and bootstrap fails with ContainerLaunchException.
    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-alpine"))
            .withCreateContainerCmdModifier(cmd -> cmd.withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(16471), ExposedPort.tcp(5671)),
                    new PortBinding(Ports.Binding.bindPort(16773), ExposedPort.tcp(5672)),
                    new PortBinding(Ports.Binding.bindPort(16572), ExposedPort.tcp(15672)),
                    new PortBinding(Ports.Binding.bindPort(16571), ExposedPort.tcp(15671))));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCreateContainerCmdModifier(cmd -> cmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(16380), ExposedPort.tcp(6379))));

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbit.getAmqpPort());
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    BrandRepository brandRepository;
    @Autowired
    ProductRepository productRepository;

    static final ObjectMapper MAPPER = new ObjectMapper();
    static String accessToken;
    static String orderNumber;

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return MAPPER.readTree(response.getBody());
    }

    @Test
    @Order(0)
    void seedOneProduct() {
        // The test schema is created fresh (create-drop, Flyway off), so the
        // flow seeds its own catalog data.
        Category category = categoryRepository.save(Category.builder()
                .name("Integration Category").slug("integration-category")
                .description("Seeded by the user-flow integration test").sortOrder(0).build());
        Brand brand = brandRepository.save(Brand.builder()
                .name("Integration Brand").slug("integration-brand").build());
        productRepository.save(Product.builder()
                .name("Integration Test Product").slug("integration-test-product")
                .description("Product created by the user-flow integration test")
                .price(java.math.BigDecimal.valueOf(250))
                .sku("INT-FLOW-1").stock(50).isActive(true)
                .category(category).brand(brand).build());
    }

    @Test
    @Order(1)
    void catalogIsBrowsableAnonymously() throws Exception {
        ResponseEntity<String> response = rest.getForEntity("/products?page=0&size=5", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode products = body(response).path("data").path("products");
        assertThat(products.size()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    void guestCannotReadOrdersWithoutAuth() {
        // TestRestTemplate prefixes the servlet context path (/api/v1) for us,
        // so paths here are relative to the context root.
        ResponseEntity<String> response = rest.getForEntity("/orders", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void registerAndSignIn() throws Exception {
        String email = "userflow-" + System.currentTimeMillis() + "@example.com";
        ResponseEntity<String> reg = rest.postForEntity("/auth/register",
                new HttpEntity<>(Map.of(
                        "email", email,
                        "password", "FlowPass123!",
                        "name", "Flow User",
                        "captchaToken", "dev-test-secret"), json()), String.class);
        assertThat(reg.getStatusCode().value()).isIn(200, 201);

        ResponseEntity<String> login = rest.postForEntity("/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", "FlowPass123!"), json()), String.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        accessToken = body(login).path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    @Order(4)
    void addToCartAndCheckout() throws Exception {
        assertThat(accessToken).isNotBlank();
        HttpHeaders auth = json();
        auth.setBearerAuth(accessToken);

        ResponseEntity<String> products = rest.exchange("/products?page=0&size=1",
                HttpMethod.GET, new HttpEntity<>(auth), String.class);
        String productId = body(products).path("data").path("products").get(0).path("id").asText();

        ResponseEntity<String> add = rest.exchange("/cart/items",
                HttpMethod.POST, new HttpEntity<>(Map.of("productId", productId, "quantity", 1), auth), String.class);
        assertThat(add.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> cart = rest.exchange("/cart",
                HttpMethod.GET, new HttpEntity<>(auth), String.class);
        assertThat(body(cart).path("data").path("totalItems").asInt()).isEqualTo(1);

        ResponseEntity<String> checkout = rest.exchange("/orders/checkout",
                HttpMethod.POST, new HttpEntity<>(validCheckout(), auth), String.class);
        assertThat(checkout.getStatusCode().value()).isEqualTo(200);
        orderNumber = body(checkout).path("data").path("number").asText();
        assertThat(orderNumber).startsWith("ILS-");
    }

    @Test
    @Order(5)
    void orderIsReadableWithReferenceNumber() throws Exception {
        assertThat(accessToken).isNotBlank();
        HttpHeaders auth = json();
        auth.setBearerAuth(accessToken);
        ResponseEntity<String> order = rest.exchange("/orders/" + orderNumber,
                HttpMethod.GET, new HttpEntity<>(auth), String.class);
        assertThat(order.getStatusCode().value()).isEqualTo(200);
        assertThat(body(order).path("data").path("number").asText()).isEqualTo(orderNumber);
        assertThat(body(order).path("data").path("status").asText()).isIn("PENDING", "PENDING_PAYMENT", "CONFIRMED");
    }

    @Test
    @Order(6)
    void anotherUsersOrderIsNotReadable() {
        assertThat(orderNumber).isNotBlank();
        // A signed-out request must never read someone else's order:
        // the service resolves ownership (user, guest session or admin) and
        // hides foreign orders as 404 so order numbers don't leak existence.
        ResponseEntity<String> anon = rest.getForEntity("/orders/" + orderNumber, String.class);
        assertThat(anon.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @Order(7)
    void adminEndpointsRejectShopper() {
        assertThat(accessToken).isNotBlank();
        HttpHeaders auth = json();
        auth.setBearerAuth(accessToken);
        ResponseEntity<String> stats = rest.exchange("/admin/stats",
                HttpMethod.GET, new HttpEntity<>(auth), String.class);
        assertThat(stats.getStatusCode().value()).isEqualTo(403);
    }

    private HttpHeaders json() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private Map<String, Object> validCheckout() {
        Map<String, Object> address = Map.of(
                "name", "Flow User",
                "line1", "1 Test Lane",
                "city", "Nairobi",
                "state", "Nairobi",
                "postalCode", "00100",
                "country", "KE",
                "phone", "+254700000000");
        return Map.of("shippingAddress", address, "billingAddress", address);
    }
}
