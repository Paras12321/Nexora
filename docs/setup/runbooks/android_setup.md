# Android Development Setup & Networking Configuration

## Backend Base URL Configuration

The NEXORA Android application connects to the Django REST Framework backend API.

### Environment-Safe Development URLs
- **Android Emulator:** `http://10.0.2.2:8000/api/` (default in `gradle.properties`)
- **Physical Device:** Set `BASE_URL=http://<HOST_IP>:8000/api/` in `android/gradle.properties`

### Running Android Unit Tests
Unit tests for the networking layer use `MockWebServer` and run without needing a connected device:
```bash
cd android
./gradlew test
```
