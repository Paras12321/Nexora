# NEXORA Backend

Django REST Framework backend for the NEXORA smart-home platform.

## Stack

- Django
- Django REST Framework
- MySQL
- Python-dotenv
- django-cors-headers

## Local setup

1. Create a virtual environment:

   ```bash
   cd backend
   python3 -m venv .venv
   source .venv/bin/activate
   ```

2. Install dependencies:

   ```bash
   pip install -r requirements.txt
   ```

3. Create a local environment file:

   ```bash
   cp .env.example .env
   ```

4. Ensure MySQL is running and create the database:

   ```sql
   CREATE DATABASE nexora_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

5. Apply database migrations:

   ```bash
   python manage.py migrate
   ```

6. Start the development server:

   ```bash
   python manage.py runserver 0.0.0.0:8000
   ```

## Useful commands

```bash
python manage.py check
python manage.py test
python manage.py makemigrations --check --dry-run
python manage.py migrate
```

## Health endpoint

The API includes a lightweight status endpoint:

- GET /api/health/

Example response:

```json
{"status": "ok", "service": "nexora-backend"}
```

## Notes

- This foundation intentionally excludes authentication, business models, and AI integration.
- Secrets and production settings must be kept outside the repository and managed via environment variables.
