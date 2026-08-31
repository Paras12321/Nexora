# NEXORA Android App

## Networking & Data Foundation

This module contains the core Android networking infrastructure, repository pattern implementation, and token management for the NEXORA Home Device Management system.

### Base URL Configuration

The application uses an environment-safe base URL injected into `BuildConfig.BASE_URL` at build time.

- **Default (Android Emulator):** `http://10.0.2.2:8000/api/`
  - In Android emulators, `10.0.2.2` maps to `localhost` on the host development machine running the Django REST backend.
- **Physical Device Testing:** Configure `BASE_URL` in `gradle.properties`:
  ```properties
  BASE_URL=http://<YOUR_LOCAL_IP>:8000/api/
  ```
- **Custom Environment Override via Command Line:**
  ```bash
  ./gradlew assembleDebug -PBASE_URL="http://192.168.1.100:8000/api/"
  ```

### Key Components

1. **`ApiClient` (`com.nexora.app.data.remote.ApiClient`)**
   - Retrofit + OkHttp client setup.
   - Kotlinx Serialization configured with lenient parsing and ignoring unknown JSON keys.
   - Sourced base URL from `BuildConfig.BASE_URL`.

2. **`AuthInterceptor` (`com.nexora.app.data.remote.AuthInterceptor`)**
   - Automatically injects `Authorization: Token <token>` header for requests when an active session token exists in `TokenManager`.
   - Preserves custom or pre-existing Authorization headers.

3. **`TokenManager` (`com.nexora.app.data.local.TokenManager`)**
   - Interface and implementations (`SharedPreferencesTokenManager`, `InMemoryTokenManager`) for token persistence.

4. **`NetworkResult<T>` (`com.nexora.app.data.remote.NetworkResult`)**
   - Sealed class representing `Success(data)`, `Error(networkError)`, and `Loading` states for UI and ViewModels.

5. **`NetworkError` (`com.nexora.app.data.remote.NetworkError`)**
   - Strongly typed error hierarchy handling:
     - `HttpError`: Status codes (400, 401, 403, 500) and backend error JSON bodies (`{"detail": "..."}`).
     - `ConnectivityError`: Network offline, timeout, or DNS resolution issues.
     - `SerializationError`: Corrupted, mismatched, or malformed JSON responses.
     - `UnknownError`: Generic fallback exception handler.

6. **`BaseRepository` (`com.nexora.app.data.repository.BaseRepository`)**
   - Abstract repository class with `safeApiCall` that catches exceptions and maps Retrofit responses into `NetworkResult<T>`.

### Security Guarantees

- **No Hard-coded Production Secrets:** Base URLs and API configurations use dynamic Gradle fields; credentials and auth tokens are stored dynamically at runtime.
- **No Direct MySQL Connection:** All data access goes through the authenticated Django REST API layer over HTTP/HTTPS.
