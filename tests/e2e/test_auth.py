"""Đăng nhập, phân quyền, quên mật khẩu (kèm email nếu có Mailpit)."""
import time

import requests

from conftest import BASE_URL, USER_EMAIL, USER_PASSWORD, Api, expect, login


def test_login_wrong_password():
    assert login(USER_EMAIL, USER_PASSWORD + 'x').status_code in (400, 401)


def test_me_and_refresh(user):
    me = expect(user.get('/api/auth/me/'), 200)
    assert me['email'] == USER_EMAIL
    tokens = expect(login(USER_EMAIL, USER_PASSWORD), 200)
    refreshed = expect(requests.post(BASE_URL + '/api/auth/refresh-token/',
                                     json={'refreshToken': tokens['refreshToken']}, timeout=30), 200)
    assert refreshed['accessToken'] and refreshed['refreshToken'] != tokens['refreshToken']
    # Refresh token đã xoay vòng: dùng lại token cũ bị từ chối.
    reused = requests.post(BASE_URL + '/api/auth/refresh-token/', json={'refreshToken': tokens['refreshToken']}, timeout=30)
    assert reused.status_code in (400, 401)


def test_permissions(anon, user):
    expect(anon.get('/api/me/dashboard/'), 401)
    expect(anon.get('/api/admin/dashboard/'), 401)
    expect(user.get('/api/admin/dashboard/'), 403)
    expect(user.get('/api/me/dashboard/'), 200)
    expect(Api('not-a-jwt').get('/api/me/dashboard/'), 401)


def test_admin_dashboard(admin):
    expect(admin.get('/api/admin/dashboard/'), 200)
    expect(admin.get('/api/admin/stats/overview/'), 200)


def test_forgot_password_unknown_email_is_silent(anon):
    expect(anon.post('/api/auth/forgot-password/', json={'email': 'khong-ton-tai-e2e@tripnova.local'}), 204)


def test_forgot_password_sends_email(anon, user, mailpit):
    since = time.time()
    expect(anon.post('/api/auth/forgot-password/', json={'email': USER_EMAIL}), 204)
    mail = mailpit.wait_for(USER_EMAIL, 'Reset your TripNova password', since)
    assert '/reset-password?token=' in mail['Text']
    assert 'Đặt mật khẩu mới' in mail['Text'] and 'Choose a new password' in mail['Text']
    assert '/reset-password?token=' in mail['HTML']


def test_resend_verification_email(user, mailpit):
    me = expect(user.get('/api/auth/me/'), 200)
    if me.get('isVerified'):
        return
    since = time.time()
    resp = user.post('/api/auth/resend-verification/')
    if resp.status_code == 400:  # đang trong 60 giây chờ giữa 2 lần gửi
        return
    expect(resp, 204)
    mail = mailpit.wait_for(USER_EMAIL, 'Verify your TripNova email', since)
    assert '/verify-email?token=' in mail['Text']
