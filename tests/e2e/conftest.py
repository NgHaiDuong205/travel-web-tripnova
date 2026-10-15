"""
Hạ tầng chung cho test E2E (gọi API thật của backend đang chạy).

Nguyên tắc dữ liệu — KHÔNG xoá dữ liệu có sẵn trong DB:
  * Tài khoản test cố định (E2E_USER_EMAIL...): chưa có thì đăng ký, có rồi thì đăng nhập. Không bao giờ xoá.
  * Khách sạn / xe để đặt thử: dùng bản ghi test có tên cố định (tạo nếu chưa có); bật lên lúc test, tắt lại khi xong.
  * Booking tạo ra chỉ được huỷ (đổi trạng thái), không xoá.

Biến môi trường:
  E2E_BASE_URL       (mặc định http://localhost:8080)
  E2E_MAILPIT_URL    (mặc định http://127.0.0.1:8025; không chạy Mailpit -> bỏ qua các kiểm tra email)
  E2E_ADMIN_EMAIL / E2E_ADMIN_PASSWORD   tài khoản ADMIN có sẵn (bắt buộc cho test admin)
  E2E_USER_EMAIL / E2E_USER_PASSWORD     tài khoản khách (tự đăng ký nếu chưa có)
"""
import os
import time
import uuid

import pytest
import requests

BASE_URL = os.getenv('E2E_BASE_URL', 'http://localhost:8080').rstrip('/')
MAILPIT_URL = os.getenv('E2E_MAILPIT_URL', 'http://127.0.0.1:8025').rstrip('/')
ADMIN_EMAIL = os.getenv('E2E_ADMIN_EMAIL', 'claude.test+a2@tripnova.local')
ADMIN_PASSWORD = os.getenv('E2E_ADMIN_PASSWORD', 'secret123')
USER_EMAIL = os.getenv('E2E_USER_EMAIL', 'e2e.user@tripnova.local')
USER_PASSWORD = os.getenv('E2E_USER_PASSWORD', 'secret123')

TEST_HOTEL_NAME = 'Claude Test Hotel B'
TEST_CAR_NAME = 'Claude Test Car'


class Api:
    """Client mỏng quanh requests: tự gắn Bearer, path tương đối từ BASE_URL."""

    def __init__(self, token=None):
        self.token = token
        self.session = requests.Session()

    def request(self, method, path, **kwargs):
        headers = kwargs.pop('headers', {})
        if self.token:
            headers['Authorization'] = 'Bearer ' + self.token
        return self.session.request(method, BASE_URL + path, headers=headers, timeout=120, **kwargs)

    def get(self, path, **kw):
        return self.request('GET', path, **kw)

    def post(self, path, json=None, **kw):
        return self.request('POST', path, json=json, **kw)

    def put(self, path, json=None, **kw):
        return self.request('PUT', path, json=json, **kw)

    def delete(self, path, **kw):
        return self.request('DELETE', path, **kw)


def expect(resp, status):
    """assert mã HTTP, in body khi sai để dễ đọc lỗi; trả JSON (hoặc None)."""
    assert resp.status_code == status, f'{resp.request.method} {resp.url} -> {resp.status_code}: {resp.text[:800]}'
    if resp.content and 'json' in resp.headers.get('Content-Type', ''):
        return resp.json()
    return None


def login(email, password):
    resp = requests.post(BASE_URL + '/api/auth/login/', json={'email': email, 'password': password}, timeout=30)
    return resp


def login_or_register(email, password, full_name):
    resp = login(email, password)
    if resp.status_code == 200:
        return resp.json()
    reg = requests.post(BASE_URL + '/api/auth/register/',
                        json={'email': email, 'password': password, 'fullName': full_name}, timeout=30)
    assert reg.status_code in (200, 201), f'Không đăng ký được {email}: {reg.status_code} {reg.text[:300]}'
    if reg.json().get('verificationRequired'):
        _verify_from_mailpit(email)
    return expect(login(email, password), 200)


def _verify_from_mailpit(email):
    """Tài khoản mới phải xác thực email mới đăng nhập được: lấy link trong email chào mừng (Mailpit)."""
    import re
    deadline = time.time() + 20
    while time.time() < deadline:
        try:
            found = requests.get(MAILPIT_URL + '/api/v1/search', params={'query': f'to:"{email}"'}, timeout=10).json()
        except requests.RequestException:
            break
        for msg in found.get('messages', []):
            text = requests.get(MAILPIT_URL + '/api/v1/message/' + msg['ID'], timeout=10).json()['Text']
            match = re.search(r'/verify-email\?token=([^\s"<>&]+)', text)
            if match:
                expect(requests.post(BASE_URL + '/api/auth/verify-email/', json={'token': match.group(1)}, timeout=30), 200)
                return
        time.sleep(0.5)
    pytest.exit(f'{email} cần xác thực email: chạy Mailpit + BE gửi mail vào Mailpit (xem tests/README.md)', returncode=2)


def unique(prefix='e2e'):
    return f'{prefix}-{uuid.uuid4().hex[:10]}'


# --------------------------------------------------------------------------- fixtures

@pytest.fixture(scope='session', autouse=True)
def backend_up():
    try:
        requests.get(BASE_URL + '/api/continents/', timeout=10)
    except requests.RequestException as e:
        pytest.exit(f'Backend không chạy ở {BASE_URL}: {e}', returncode=2)


@pytest.fixture(scope='session')
def anon():
    return Api()


@pytest.fixture(scope='session')
def user():
    data = login_or_register(USER_EMAIL, USER_PASSWORD, 'E2E User')
    api = Api(data['accessToken'])
    api.email = USER_EMAIL
    return api


@pytest.fixture(scope='session')
def admin():
    resp = login(ADMIN_EMAIL, ADMIN_PASSWORD)
    if resp.status_code != 200:
        pytest.skip(f'Không đăng nhập được admin {ADMIN_EMAIL} (đặt E2E_ADMIN_EMAIL / E2E_ADMIN_PASSWORD)')
    data = resp.json()
    roles = data.get('user', {}).get('roles', [])
    if 'ADMIN' not in roles:
        pytest.skip(f'{ADMIN_EMAIL} không có role ADMIN')
    return Api(data['accessToken'])


class Mailpit:
    def __init__(self, url):
        self.url = url

    def wait_for(self, to, subject_contains, since=0.0, timeout=20):
        """Chờ email tới `to` có tiêu đề chứa chuỗi, nhận sau thời điểm `since` (epoch giây)."""
        deadline = time.time() + timeout
        while time.time() < deadline:
            resp = requests.get(self.url + '/api/v1/search', params={'query': f'to:"{to}"'}, timeout=10)
            for msg in resp.json().get('messages', []):
                created = msg.get('Created', '')
                if subject_contains in msg['Subject'] and _epoch(created) >= since - 2:
                    return requests.get(self.url + '/api/v1/message/' + msg['ID'], timeout=10).json()
            time.sleep(0.5)
        raise AssertionError(f'Không thấy email "{subject_contains}" gửi tới {to} trong {timeout}s')


def _epoch(iso):
    from datetime import datetime
    try:
        return datetime.fromisoformat(iso.replace('Z', '+00:00')).timestamp()
    except ValueError:
        return 0


@pytest.fixture(scope='session')
def mailpit():
    try:
        requests.get(MAILPIT_URL + '/api/v1/info', timeout=3).raise_for_status()
    except requests.RequestException:
        pytest.skip(f'Mailpit không chạy ở {MAILPIT_URL} — bỏ qua kiểm tra email '
                    '(chạy backend với --spring.mail.host=127.0.0.1 --spring.mail.port=1025)')
    return Mailpit(MAILPIT_URL)


# ------------------------------------------------------------- dữ liệu test (bật/tắt, không xoá)

def _first_destination(admin_api):
    data = expect(admin_api.get('/api/destinations/', params={'limit': 1}), 200)
    assert data, 'DB chưa có điểm đến nào'
    return data[0]['id']


@pytest.fixture(scope='session')
def test_hotel(admin):
    """Khách sạn test: bật trong lúc chạy, tắt lại khi xong. Trả (hotel, roomType)."""
    found = [h for h in expect(admin.get('/api/admin/hotels/', params={'q': TEST_HOTEL_NAME, 'limit': 20}), 200)
             if h['name'] == TEST_HOTEL_NAME]
    if found:
        hotel = expect(admin.get(f"/api/admin/hotels/{found[0]['id']}/"), 200)
    else:
        hotel = expect(admin.post('/api/admin/hotels/', json={
            'destinationId': _first_destination(admin), 'name': TEST_HOTEL_NAME, 'address': 'E2E street 1',
            'starRating': 3, 'description': 'Khách sạn dùng cho test tự động — không hiển thị cho khách.',
        }), 201)
    room_types = [rt for rt in expect(admin.get(f"/api/admin/hotels/{hotel['id']}/room-types/"), 200)
                  if rt.get('isActive', True)]
    if not room_types:
        rt = expect(admin.post(f"/api/admin/hotels/{hotel['id']}/room-types/", json={
            'name': 'E2E Double', 'maxOccupancy': 2, 'pricePerNight': 80}), 201)
        room_types = [rt]
    room_type = room_types[0]
    rooms = expect(admin.get(f"/api/admin/hotels/{hotel['id']}/rooms/"), 200)
    if not any(r.get('roomTypeId') == room_type['id'] for r in rooms):
        for number in ('E201', 'E202'):
            expect(admin.post(f"/api/admin/hotels/{hotel['id']}/rooms/",
                              json={'roomTypeId': room_type['id'], 'roomNumber': number, 'floor': 2}), 201)
    was_active = hotel.get('isActive')
    _set_hotel_active(admin, hotel, True)
    yield hotel, room_type
    if not was_active:
        _set_hotel_active(admin, hotel, False)


def _set_hotel_active(admin, hotel, active):
    body = {k: hotel.get(k) for k in (
        'destinationId', 'name', 'description', 'address', 'starRating', 'checkInTime', 'checkOutTime', 'phone',
        'email', 'cancellationPolicy', 'cancellationHours', 'breakfastIncluded', 'petFriendly', 'coverImageUrl',
        'latitude', 'longitude', 'managedById')}
    for key in ('checkInTime', 'checkOutTime'):
        if body.get(key):
            body[key] = body[key][:5]
    body['isActive'] = active
    expect(admin.put(f"/api/admin/hotels/{hotel['id']}/", json=body), 200)


@pytest.fixture(scope='session')
def test_car(admin):
    """Xe tự lái dùng cho test: bật trong lúc chạy, tắt lại khi xong."""
    found = [c for c in expect(admin.get('/api/admin/cars/', params={'q': TEST_CAR_NAME, 'limit': 20}), 200)
             if c['name'] == TEST_CAR_NAME]
    if found:
        car = expect(admin.get(f"/api/admin/cars/{found[0]['id']}/"), 200)
    else:
        car = expect(admin.post('/api/admin/cars/', json={
            'destinationId': _first_destination(admin), 'name': TEST_CAR_NAME, 'brand': 'Test', 'model': 'E2E',
            'licensePlate': 'E2E-' + uuid.uuid4().hex[:6].upper(), 'carType': 'sedan', 'seats': 4,
            'pricePerDay': 40, 'withDriver': False, 'isActive': False}), 201)
    was_active = car.get('isActive')
    _set_car_active(admin, car, True)
    yield car
    if not was_active:
        _set_car_active(admin, car, False)


def _set_car_active(admin, car, active):
    body = {k: car.get(k) for k in (
        'destinationId', 'name', 'brand', 'model', 'licensePlate', 'carType', 'seats', 'transmission', 'fuelType',
        'pricePerDay', 'withDriver', 'pickupLocation', 'description', 'coverImageUrl')}
    body['isActive'] = active
    expect(admin.put(f"/api/admin/cars/{car['id']}/", json=body), 200)
