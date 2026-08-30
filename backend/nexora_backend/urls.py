from django.contrib import admin
from django.urls import include, path

from core import views

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/health/", views.healthcheck, name="healthcheck"),
    path("api/", include("core.urls")),
]
