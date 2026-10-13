"""
Đặt khách sạn / thuê xe -> thanh toán giả lập -> hoá đơn (+ email PDF) -> huỷ.
Chỉ dùng khách sạn / xe test (fixture test_hotel, test_car); booking được huỷ cuối test, không xoá.
"""
import random
import time
from datetime import date, datetime, timedelta

from conftest import expect


def _future_day(start=120, span=400):
    # Ngày xa + ngẫu nhiên để các lần chạy không đụng lịch nhau.
    return date.today() + timedelta(days=start + random.randint(0, span))


def _pay(user, payment_id, success=True):
    return expect(user.post(f'/api/payments/{payment_id}/mock-confirm/', json={'success': success}), 200)


def _invoice_for_order(user, order_id):
    for invoice in expect(user.get('/api/me/invoices/', params={'limit': 50}), 200):
        if invoice['orderId'] == order_id:
            return invoice
    raise AssertionError(f'Không thấy hoá đơn của order {order_id}')


def test_hotel_booking_pay_invoice_cancel(user, admin, test_hotel, request):
    hotel, room_type = test_hotel
    check_in = _future_day()
    body = {'hotelId': hotel['id'], 'roomTypeId': room_type['id'], 'checkIn': check_in.isoformat(),
            'checkOut': (check_in + timedelta(days=2)).isoformat(), 'adults': 1, 'paymentMethod': 'credit_card',
            'specialRequests': 'E2E test'}
    expect(user.post('/api/hotel-bookings/', json={**body, 'adults': 0}), 400)

    created = expect(user.post('/api/hotel-bookings/', json=body), 201)
    assert created['holdExpiresAt'] and created['paymentId']
    booking = expect(user.get(f"/api/hotel-bookings/{created['bookingId']}/"), 200)
    assert booking['status'] == 'pending'
    since = time.time()
    payment = _pay(user, created['paymentId'])
    assert payment['status'] == 'success'
    booking = expect(user.get(f"/api/hotel-bookings/{created['bookingId']}/"), 200)
    assert booking['status'] == 'confirmed'

    invoice = _invoice_for_order(user, booking['orderId'])
    pdf = user.get(f"/api/me/invoices/{invoice['id']}/download/")
    assert pdf.status_code == 200 and pdf.content[:4] == b'%PDF'

    mailpit = _optional_mailpit(request)
    if mailpit:
        mail = mailpit.wait_for(user.email, invoice['invoiceNumber'], since)
        assert [a['FileName'] for a in mail['Attachments']] == [invoice['invoiceNumber'] + '.pdf']
        # Admin gửi lại hoá đơn -> thêm 1 email.
        since = time.time()
        expect(admin.post(f"/api/admin/invoices/{invoice['id']}/resend/"), 200)
        mailpit.wait_for(user.email, invoice['invoiceNumber'], since)

    cancelled = expect(user.post(f"/api/hotel-bookings/{booking['id']}/cancel/", json={'reason': 'E2E test'}), 200)
    assert cancelled['status'] in ('cancelled', 'refunded')


def test_hotel_booking_payment_failed_releases(user, test_hotel):
    hotel, room_type = test_hotel
    check_in = _future_day()
    created = expect(user.post('/api/hotel-bookings/', json={
        'hotelId': hotel['id'], 'roomTypeId': room_type['id'], 'checkIn': check_in.isoformat(),
        'checkOut': (check_in + timedelta(days=1)).isoformat(), 'adults': 1, 'paymentMethod': 'momo'}), 201)
    payment = _pay(user, created['paymentId'], success=False)
    assert payment['status'] == 'failed'
    booking = expect(user.get(f"/api/hotel-bookings/{created['bookingId']}/"), 200)
    assert booking['status'] == 'cancelled'


def test_car_booking_pay_cancel(user, test_car):
    pickup = datetime.combine(_future_day(), datetime.min.time()).replace(hour=10)
    body = {'carId': test_car['id'], 'pickupDate': pickup.isoformat(),
            'returnDate': (pickup + timedelta(days=2)).isoformat(), 'driverLicenseNo': 'E2E-123456',
            'paymentMethod': 'credit_card'}
    expect(user.post('/api/car-bookings/', json={**body, 'returnDate': pickup.isoformat()}), 400)

    booking = expect(user.post('/api/car-bookings/', json=body), 201)
    assert booking['status'] == 'pending' and booking['rentalDays'] == 2
    # Cùng xe, trùng giờ -> bị chặn khi booking kia đang giữ chỗ.
    expect(user.post('/api/car-bookings/', json=body), 409)
    _pay(user, booking['paymentId'])
    booking = expect(user.get(f"/api/car-bookings/{booking['id']}/"), 200)
    assert booking['status'] == 'confirmed'
    mine = expect(user.get('/api/me/car-bookings/', params={'status': 'upcoming', 'limit': 50}), 200)
    assert any(b['id'] == booking['id'] for b in mine)
    cancelled = expect(user.post(f"/api/car-bookings/{booking['id']}/cancel/", json={'reason': 'E2E test'}), 200)
    assert cancelled['status'] in ('cancelled', 'refunded')


def _optional_mailpit(request):
    try:
        return request.getfixturevalue('mailpit')
    except Exception:  # pytest.skip trong fixture -> chạy tiếp phần không cần email
        return None
