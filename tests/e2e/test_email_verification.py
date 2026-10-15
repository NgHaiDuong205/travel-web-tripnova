"""Đăng ký → email chào mừng kèm link xác thực → chưa xác thực thì không đăng nhập được → gửi lại link →
xác thực → đăng nhập. Quên mật khẩu bằng link trong email cũng coi như đã xác thực email.

Mỗi lần chạy tạo 2 tài khoản test mới (`e2e.verify+...@tripnova.local`); không xoá tài khoản nào.
Cần Mailpit (BE chạy với --spring.mail.host=127.0.0.1 --spring.mail.port=1025).
"""
import re
import time

import requests

from conftest import BASE_URL, expect, login, unique

PASSWORD = 'secret123'


def _register(email):
    resp = requests.post(BASE_URL + '/api/auth/register/',
                         json={'email': email, 'password': PASSWORD, 'fullName': 'E2E Verify'}, timeout=30)
    return expect(resp, 201)


def _token(mail, path):
    match = re.search(re.escape(path) + r'\?token=([^\s"<>&]+)', mail['Text'])
    assert match, f'Không thấy link {path} trong email'
    return match.group(1)


def test_register_requires_email_verification(anon, mailpit):
    email = unique('e2e.verify') + '@tripnova.local'
    since = time.time()
    data = _register(email)
    # Chưa xác thực: không cấp token.
    assert data['verificationRequired'] is True and data['email'] == email
    assert not data.get('accessToken') and not data.get('refreshToken')

    welcome = mailpit.wait_for(email, 'Welcome to TripNova', since)
    assert 'Chào mừng' in welcome['Text'] and 'Xác thực email' in welcome['Text'] and 'Verify email' in welcome['Text']
    assert '/verify-email?token=' in welcome['HTML']
    token = _token(welcome, '/verify-email')

    # Sai mật khẩu vẫn là 401 (không lộ trạng thái xác thực); đúng mật khẩu -> 403 email_not_verified.
    assert login(email, PASSWORD + 'x').status_code == 401
    resp = login(email, PASSWORD)
    assert resp.status_code == 403 and resp.json()['code'] == 'email_not_verified', resp.text

    # Gửi lại theo email (chưa có token): luôn 204; vừa gửi lúc đăng ký nên trong 60 giây không gửi thêm.
    expect(anon.post('/api/auth/resend-verification/', json={'email': email}), 204)
    expect(anon.post('/api/auth/resend-verification/', json={'email': 'khong-ton-tai-e2e@tripnova.local'}), 204)
    time.sleep(2)
    inbox = requests.get(f'{mailpit.url}/api/v1/search', params={'query': f'to:"{email}"'}, timeout=10).json()
    assert len(inbox['messages']) == 1, 'Không được gửi lại trong 60 giây chờ'
    # Không có body + chưa đăng nhập -> 401.
    expect(anon.post('/api/auth/resend-verification/'), 401)

    verified = expect(anon.post('/api/auth/verify-email/', json={'token': token}), 200)
    assert verified['email'] == email
    tokens = expect(login(email, PASSWORD), 200)
    assert tokens['accessToken'] and tokens['user']['email'] == email


def test_reset_password_link_also_verifies_email(anon, mailpit):
    email = unique('e2e.verify') + '@tripnova.local'
    _register(email)
    assert login(email, PASSWORD).status_code == 403

    since = time.time()
    expect(anon.post('/api/auth/forgot-password/', json={'email': email}), 204)
    mail = mailpit.wait_for(email, 'Reset your TripNova password', since)
    token = _token(mail, '/reset-password')
    expect(anon.post('/api/auth/reset-password/', json={'token': token, 'newPassword': 'newsecret123'}), 204)
    # Mở được link trong hộp thư = sở hữu email -> đăng nhập được bằng mật khẩu mới.
    assert login(email, PASSWORD).status_code == 401
    expect(login(email, 'newsecret123'), 200)
    # Link đặt lại chỉ dùng 1 lần.
    expect(anon.post('/api/auth/reset-password/', json={'token': token, 'newPassword': 'other12345'}), 400)


def test_existing_accounts_still_sign_in(user, admin):
    """Tài khoản tạo trước khi bật quy định (e2e.user, admin) vẫn đăng nhập bình thường dù chưa xác thực."""
    expect(user.get('/api/auth/me/'), 200)
    expect(admin.get('/api/auth/me/'), 200)
