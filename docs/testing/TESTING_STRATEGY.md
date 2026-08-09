# Testing Strategy

This document outlines the testing approach for i-love-shopping.

## Testing Pyramid

```
        /\
       /  \     E2E Tests (Few)
      /____\    
     /    \    Integration Tests (Some)
    /______\   
   /      \  Unit Tests (Many)
  /________\
```

## Test Categories

### 1. Unit Tests (Target: 80%+ coverage)

**Scope**: Individual classes, methods, business logic
**Tools**: JUnit 5, Mockito, AssertJ
**Location**: `src/test/java/com/iloveshopping/`

**Current Coverage**:
- `JwtServiceTest` - Token generation, validation, expiry
- `AuthValidationTest` - DTO validation rules
- `ProductTest` - Entity business logic
- `SecurityTest` - SQL injection, XSS, path traversal detection
- `HealthCheckTest` - Health endpoint responses

**Run**: `./mvnw test`

### 2. Integration Tests (Target: Key flows)

**Scope**: Component interactions, database, external APIs
**Tools**: Testcontainers, Spring Boot Test, RestAssured
**Location**: `src/test/java/com/iloveshopping/integration/`

**Planned Tests**:
- `AuthIntegrationTest` - Registration, login, refresh, 2FA
- `CatalogIntegrationTest` - Product search, filtering, pagination
- `CartIntegrationTest` - Add, update, remove, merge
- `OrderIntegrationTest` - Checkout, payment, cancellation
- `PaymentIntegrationTest` - M-Pesa STK Push, callbacks

**Run**: `./mvnw verify -DskipUnitTests`

### 3. E2E Tests (Target: Critical user journeys)

**Scope**: Full user flows through API
**Tools**: Playwright or Cypress
**Location**: `frontend/tests/` (future)

**Planned Journeys**:
- User registration → email verification → login
- Browse products → add to cart → checkout → M-Pesa payment
- Admin login → create product → publish
- Password reset flow

## Test Data Management

### Test Fixtures
- **Builders**: Fluent builders for entities
- **Factories**: Test data factories for complex objects
- **Fixtures**: JSON files for complex test data

### Database
- **Testcontainers**: PostgreSQL for integration tests
- **H2**: Fast in-memory for unit tests (if needed)
- **Cleanup**: `@Transactional` rollback or `@AfterEach` cleanup

### External Services
- **M-Pesa**: Mock server or sandbox
- **Email**: Mailhog or MockJavaMail
- **OAuth2**: Mock authorization server

## Test Conventions

### Naming
```java
@Test
void shouldGenerateValidAccessTokenWhenUserExists() {
    // Given
    // When
    // Then
}
```

### Structure
```java
@Test
void testName() {
    // Given - Setup test data
    // When - Execute method under test
    // Then - Assert expected results
}
```

### Annotations
```java
@ExtendWith(MockitoExtension.class)     // Unit tests
@SpringBootTest                         // Integration tests
@Testcontainers                         // With Testcontainers
@AutoConfigureMockMvc                   // Web layer tests
@DataJpaTest                            // Repository tests
@WebMvcTest(AuthController.class)       // Controller tests
```

## Continuous Integration

### GitHub Actions / GitLab CI
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Run tests
        run: ./mvnw test
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

### Pre-commit Hooks
```bash
# .git/hooks/pre-commit
#!/bin/bash
./mvnw test -q
```

## Code Coverage

### Targets
| Layer | Target |
|-------|--------|
| Services | 90% |
| Controllers | 80% |
| Repositories | 70% |
| Utilities | 95% |
| Overall | 80% |

### Reports
```bash
# Generate
./mvnw test jacoco:report

# View
open target/site/jacoco/index.html
```

### Exclusions
```xml
<configuration>
  <excludes>
    <exclude>**/config/**</exclude>
    <exclude>**/entity/**</exclude>
    <exclude>**/dto/**</exclude>
    <exclude>**/exception/**</exclude>
  </excludes>
</configuration>
```

## Test Data Builders

```java
public class UserTestBuilder {
    private String email = "test@example.com";
    private String password = "Password123!";
    private String name = "Test User";
    
    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public User build() {
        return User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .name(name)
            .build();
    }
}
```

## Mocking Guidelines

### Do
- Mock external dependencies (HTTP clients, external APIs)
- Use `@MockBean` for Spring beans in integration tests
- Verify interactions with `verify()`

### Don't
- Mock internal classes (test the real implementation)
- Over-specify mocks (use `any()` when appropriate)
- Mock data structures (use real objects)

## Performance Testing

### Load Testing
```bash
# Using k6
k6 run --vus 100 --duration 30s load-test.js
```

### Metrics
- Response time (p50, p95, p99)
- Throughput (req/s)
- Error rate
- Resource utilization (CPU, memory, DB connections)

## Mutation Testing

```bash
# PIT Mutation Testing
./mvnw org.pitest:pitest-maven:mutationCoverage
```

Target: > 70% mutation score

## Test Reporting

### Surefire Reports
```
target/surefire-reports/
├── TEST-com.iloveshopping.security.JwtServiceTest.xml
└── ...

### JaCoCo Coverage
target/site/jacoco/
├── index.html
└── ...

### Surefire Report Plugin
target/surefire-report.html
```

## Test Environment

### Local
```bash
# Start dependencies
docker-compose up -d postgres redis mailhog

# Run tests
./mvnw test
```

### CI/CD
```yaml
services:
  postgres:
    image: postgres:16
    env:
      POSTGRES_DB: test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports: ["5432:5432"]
  redis:
    image: redis:7
    ports: ["6379:6379"]
```

## Future Improvements

- [ ] Contract testing with Pact
- [ ] Chaos engineering with Chaos Mesh
- [ ] Visual regression testing
- [ ] Accessibility testing (axe-core)
- [ ] Performance budgets in CI