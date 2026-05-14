# Ecommerce_Microservices

# Order Flow 
```text
REQUEST COMES IN
      │
      ▼ 
① JwtAuthFilter          → reads token, validates it, sets username in SecurityContext
      │
      ▼
② SecurityConfig         → checks "is this route protected?" YES → "is user authenticated?" YES → allow
      │
      ▼
③ OrderController        → receives the request, reads the body into OrderRequestDTO
      │                     extracts username from SecurityContext
      ▼
④ OrderService           → business logic lives here
      │                     loops through each item in the request
      │                     for each item → calls ProductFeignClient
      │                          │
      │                          ▼
      │                    ⑤ ProductFeignClient → makes HTTP call to product-service
      │                          │                 GET /products/1
      │                          │                 GET /products/2
      │                          ▼
      │                    product-service responds with price + stock
      │                          │
      │                     back to OrderService
      │                     validates stock
      │                     calculates subtotal per item
      │                     calculates total price
      │                     builds Order + OrderItems
      │
      ▼
⑥ OrderRepository        → saves to orderdb
      │
      ▼
⑦ ProductFeignClient     → calls product-service again to reduce stock
      │                     PUT /products/1/reduce-stock?quantity=1
      │                     PUT /products/2/reduce-stock?quantity=2
      │
      ▼
⑧ OrderService           → maps saved Order entity to OrderResponseDTO
      │
      ▼
⑨ OrderController        → sends response back to user
      │
      ▼
USER RECEIVES: Below json
```
## JSON:
``` json
{
  "id": 1,
  "username": "madhes",
  "items": [
    { "productId": 1, "quantity": 1, "unitPrice": 75000, "subtotal": 75000 },
    { "productId": 2, "quantity": 2, "unitPrice": 4000,  "subtotal": 8000  }
  ],
  "totalPrice": 83000,
  "status": "PENDING"
}
```

-----------------------------------------------------------------------------
# Why Feign? 
> For connecting different servers for data transmissions normally we use normal Http client codes, build URL,
  parse respones ----- 20-30 lines boilerplate.

## Feign lets you do it with just an interface:
```java
// Feign generates the actual HTTP call at runtime
@FeignClient(name = "product-service", url = "http://localhost:8082")
public interface ProductFeignClient {

    @GetMapping("/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long id);
}
```

### When you call productFeignClient.getProductById(1L) in your service — Feign:

1. Builds the URL: http://localhost:8082/products/1
2. Sends GET request
3. Gets JSON back
4. Converts it to ProductResponse object
5. Returns it to you

You write zero HTTP code. That's the entire point of Feign.

#### ProductResponse — Why It Exists in Order-Service:
```json
// product-service sends back this JSON:
{
  "id": 1,
  "name": "Laptop",
  "price": 75000.00,
  "stock": 10,
  "category": "Electronics",
  "createdAt": "...",
  "updatedAt": "..."
}
```

order-service only cares about 4 of those fields
so ProductResponse only has:
```java
class ProductResponse {
    Long id;
    String name;
    BigDecimal price;   // need this to calculate subtotal
    Integer stock;      // need this to check availability
}
```
>Feign deserializes the JSON into ProductResponse automatically — extra fields like category, createdAt are just ignored

## FeignClientConfig — Why It Exists
> Product-service is protected by JWT. When order-service calls product-service, that call has no Authorization header by default. Product-service will reject it with 403.

> FeignClientConfig intercepts every outgoing Feign call and says: "wait, let me grab the token from the current incoming request and attach it to this outgoing call."

```text
User → [token] → order-service
                      │
                      │ FeignClientConfig grabs that same token
                      │ and attaches it to the outgoing call
                      ▼
               product-service ← [same token] ← order-service
```
--------------------------------------------------------------------------------------------
# Servlet
## What Happens When a Request Hits Your Server ?
 A Request comes from Postman: 
```text
Postman sends:
GET /products
Authorization: Bearer eyJhbGci...
```
Your server receives raw bytes over the network. Someone needs to:
- Parse those bytes into something Java can understand
- Figure out the URL, headers, body
- Route it to the right controller method
- Send a response back

That "someone" is the Servlet.

> A Servlet is Java's standard way of handling HTTP requests. It's been around since 1997.
Think of it as a post office worker:
```text
Opens the envelope (HTTP request)
Reads who it's addressed to (URL)
Processes it
Sends a reply (HTTP response)
```

Spring Boot uses an embedded Tomcat server. Tomcat IS a Servlet container — its entire job is to run Servlets

When request comes in:
```text
HTTP Request (raw bytes)
        │
        ▼
   Tomcat (Servlet Container)
        │  parses the raw bytes into
        ▼
   HttpServletRequest object    ← Java object with methods like
                                   getHeader("Authorization")
                                   getRequestURI()
                                   getMethod()

   HttpServletResponse object   ← Java object you write back to
                                   setStatus(200)
                                   getWriter().write("hello")
```                                   

So HttpServletRequest and HttpServletResponse are just Java representations of the HTTP request and response

----------------------------------------------------------------------
# What is a Filter:

> Before the request reaches your controller, you can intercept it. That's a Filter.
Real world analogy — a nightclub:
```text
People trying to enter (HTTP requests)
        │
        ▼
┌─────────────────┐
│   Security Guy  │  ← Filter 1: checks ID (are you 18+?)
│   at door       │     if no → sends them away
└────────┬────────┘
         │ passes through
         ▼
┌─────────────────┐
│   Ticket Check  │  ← Filter 2: checks if they have a ticket
│                 │     if no → sends them away
└────────┬────────┘
         │ passes through
         ▼
┌─────────────────┐
│   Coat Check    │  ← Filter 3: takes their coat, gives a token
└────────┬────────┘
         │ passes through
         ▼
    Inside the club  ← Your Controller (the actual destination)
```
Each checkpoint is a Filter. They run in sequence. If any filter rejects the request, the request never reaches the club (your controller).
In Spring:
```text
HTTP Request
        │
        ▼
Filter 1 (LoggingFilter)      → logs every incoming request
        │
        ▼
Filter 2 (JwtAuthFilter)      → validates the JWT token
        │
        ▼
Filter 3 (SecurityFilter)     → checks if route is allowed
        │
        ▼
Your Controller               → actually handles the request
```
This sequence of filters is called the Filter Chain.

# What is FilterChain
> FilterChain is just the object that represents "the rest of the filters after me."
When your filter is done doing its job, it must call:
```java
filterChain.doFilter(request, response);
```
This means: "I'm done, pass the request to the next filter in the chain."
If you DON'T call this — the request stops dead. It never reaches the next filter or the controller. This is how filters BLOCK requests.

```java
// This filter BLOCKS the request — never reaches controller
protected void doFilterInternal(request, response, filterChain) {
    // do some check...
    // if bad: just return, don't call filterChain.doFilter()
    return;  // ← request dies here
}

// This filter ALLOWS the request to continue
protected void doFilterInternal(request, response, filterChain) {
    // do some check...
    filterChain.doFilter(request, response);  // ← passes to next filter
}
```

# What is OncePerRequestFilter
> Spring has a problem. Some filters can run multiple times per request (due to request forwarding internally). For security, you never want your JWT filter to run twice.

OncePerRequestFilter is Spring's solution — it guarantees your filter runs exactly once per HTTP request, no matter what.

That's the only reason you extend it instead of implementing raw Filter. One guarantee, nothing else.

```java
// You extend this
public class JwtAuthFilter extends OncePerRequestFilter {

    // You implement this method
    // Spring calls this exactly once per request
    protected void doFilterInternal(
            HttpServletRequest request,    // the incoming request
            HttpServletResponse response,  // the outgoing response
            FilterChain filterChain)       // the rest of the filter chain
            throws ServletException, IOException {

        // your logic here
    }
}
```

Now Read Your JwtAuthFilter Line by Line
```java
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

    // Step 1: Read the Authorization header from the request
    // request.getHeader() reads any HTTP header by name
    // If user sent "Authorization: Bearer eyJhbGci...", this returns that string
    // If user sent no Authorization header, this returns null
    final String authHeader = request.getHeader("Authorization");

    // Step 2: If no header, or it doesn't start with "Bearer "
    // this is NOT a JWT request — could be a public endpoint
    // Just pass it through to the next filter, don't block it
    // SecurityConfig will decide if it needs auth or not
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);  // pass through
        return;  // stop executing THIS filter's code
    }

    // Step 3: Strip "Bearer " (7 characters) to get the raw JWT
    // "Bearer eyJhbGci..." → "eyJhbGci..."
    String jwt = authHeader.substring(7);

    // Step 4: Extract the username from the JWT payload
    // JWT has 3 parts: header.payload.signature
    // payload contains "sub": "madhes" (the username we stored when generating)
    // jwtService.extractUsername() decodes the payload and reads "sub"
    String username = jwtService.extractUsername(jwt);

    // Step 5: Only proceed if:
    // - username was successfully extracted (token wasn't garbage)
    // - no existing authentication in SecurityContext (don't re-authenticate)
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

        // Step 6: Validate the token
        // checks: is username correct? is token expired?
        if (jwtService.isTokenValid(jwt, username)) {

            // Step 7: Create an authentication object
            // This is Spring Security's way of saying "this request is authenticated"
            // Parameters: (who they are, their password (null for JWT), their roles)
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")));

            // Step 8: Attach extra request details (IP address etc) - optional but good practice
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Step 9: Store the authentication in SecurityContextHolder
            // This is the KEY step — SecurityContext is like a notice board for this request
            // "madhes is authenticated, has ROLE_USER"
            // SecurityConfig reads from here to decide if access is allowed
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    // Step 10: Always pass to the next filter
    // Whether token was valid or not — we pass through
    // If token was invalid, SecurityContext has no authentication
    // SecurityConfig will then reject the request with 403
    filterChain.doFilter(request, response);
}
```
----------------------------------------------------
# What is SecurityContextHolder
Think of it as a notice board attached to the current request's thread.
```text
Thread handling request from madhes:
┌─────────────────────────────────┐
│  SecurityContext (notice board) │
│                                 │
│  Authentication:                │
│    principal: "madhes"          │
│    roles: [ROLE_USER]           │
│    authenticated: true          │
└─────────────────────────────────┘
```
JwtAuthFilter writes to this board: "madhes is authenticated."

SecurityConfig reads from this board: "is anyone authenticated? yes → allow."

OrderController reads from this board: "who is authenticated? madhes → use as username."

```java
// This is how controller reads it
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName(); // returns "madhes"
After the request is done, Spring clears this board automatically. Next request starts fresh.
```

# The full picture:
```text
POST /orders
Authorization: Bearer eyJhbGci...
        │
        ▼
JwtAuthFilter:
  reads header → gets "Bearer eyJhbGci..."
  strips "Bearer " → gets raw JWT
  extracts username → "madhes"
  validates token → valid ✅
  writes to SecurityContext: "madhes is authenticated, ROLE_USER"
  calls filterChain.doFilter() → passes to next
        │
        ▼
SecurityConfig filter:
  reads SecurityContext → "madhes is authenticated" ✅
  checks route "/orders" → needs authentication ✅
  allows request through
        │
        ▼
OrderController:
  reads SecurityContext → auth.getName() → "madhes"
  calls orderService.placeOrder(requestDTO, "madhes")
        │
        ▼
Response sent back to user
SecurityContext cleared for this thread
```