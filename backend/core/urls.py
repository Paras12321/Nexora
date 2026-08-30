from django.urls import path

from core import views

urlpatterns = [
    # Auth
    path("auth/register/", views.RegisterView.as_view(), name="register"),
    path("auth/login/", views.LoginView.as_view(), name="login"),
    path("auth/logout/", views.LogoutView.as_view(), name="logout"),
    path("auth/me/", views.MeView.as_view(), name="me"),
    path("auth/password-reset/", views.PasswordResetRequestView.as_view(), name="password-reset"),
    path("auth/password-reset/confirm/", views.PasswordResetConfirmView.as_view(), name="password-reset-confirm"),
    # Homes
    path("homes/", views.HomeListCreateView.as_view(), name="home-list-create"),
    path("homes/<int:home_id>/", views.HomeDetailView.as_view(), name="home-detail"),
    path("homes/<int:home_id>/members/", views.HomeMemberListCreateView.as_view(), name="home-member-list-create"),
    path("homes/<int:home_id>/members/<int:member_id>/", views.HomeMemberRemoveView.as_view(), name="home-member-remove"),
    path("homes/<int:home_id>/leave/", views.LeaveHomeView.as_view(), name="home-leave"),
]
