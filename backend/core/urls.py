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
    # Rooms
    path("homes/<int:home_id>/rooms/", views.RoomListCreateView.as_view(), name="room-list-create"),
    path("homes/<int:home_id>/rooms/<int:room_id>/", views.RoomDetailView.as_view(), name="room-detail"),
    path("homes/<int:home_id>/rooms/<int:room_id>/members/", views.RoomMemberListCreateView.as_view(), name="room-member-list-create"),
    path("homes/<int:home_id>/rooms/<int:room_id>/preferences/", views.RoomPreferenceListCreateView.as_view(), name="room-preference-list-create"),
    # Devices
    path("homes/<int:home_id>/devices/", views.DeviceListCreateView.as_view(), name="device-list-create"),
    path("homes/<int:home_id>/devices/<int:device_id>/", views.DeviceDetailView.as_view(), name="device-detail"),
    path("homes/<int:home_id>/devices/<int:device_id>/capabilities/", views.DeviceCapabilityListCreateView.as_view(), name="device-capability-list-create"),
    
    # Presence & Security
    path("homes/<int:home_id>/presence/", views.PresenceEventListCreateView.as_view(), name="presence-event-list-create"),
    path("homes/<int:home_id>/security/", views.SecurityEventListCreateView.as_view(), name="security-event-list-create"),
    
    # Energy & Bills
    path("homes/<int:home_id>/energy/", views.EnergyUsageRecordListCreateView.as_view(), name="energy-record-list-create"),
    path("homes/<int:home_id>/bills/", views.ElectricityBillListCreateView.as_view(), name="electricity-bill-list-create"),
    
    # Activity & Decision Logs
    path("homes/<int:home_id>/activity-logs/", views.ActivityLogListCreateView.as_view(), name="activity-log-list-create"),
    path("homes/<int:home_id>/decision-logs/", views.DecisionLogListCreateView.as_view(), name="decision-log-list-create"),
    path("homes/<int:home_id>/decision-logs/<int:log_id>/approve/", views.DecisionLogApproveView.as_view(), name="decision-log-approve"),
]
