"""Chấm điểm rủi ro gian lận: đặt xe (khách test) → giao dịch được chấm điểm sau khi tạo đơn → admin xem / đánh dấu.

Dữ liệu: 1 đơn thuê xe trên xe test, huỷ ngay (chỉ huỷ, không xoá).
"""
import time
from datetime import date, datetime, timedelta

from conftest import expect

REASON_CODES = {'new_account', 'high_amount', 'amount_spike', 'order_velocity', 'failed_payments', 'cancellations',
                'unverified_email', 'shared_ip'}


def _score_for(admin, order_code, timeout=10):
    deadline = time.time() + timeout
    while time.time() < deadline:
        found = expect(admin.get('/api/admin/ai/fraud-scores/', params={'q': order_code}), 200)
        if found:
            return found[0]
        time.sleep(0.5)
    raise AssertionError(f'Đơn {order_code} chưa được chấm điểm')


def test_fraud_permissions_and_validation(user, admin):
    expect(user.get('/api/admin/ai/fraud-scores/'), 403)
    expect(admin.get('/api/admin/ai/fraud-scores/', params={'level': 'extreme'}), 400)
    expect(admin.get('/api/admin/ai/fraud-scores/', params={'action': 'delete'}), 400)
    stats = expect(admin.get('/api/admin/ai/fraud-scores/stats/'), 200)
    assert set(stats) == {'low', 'medium', 'high', 'toReview'}
    expect(admin.get('/api/admin/ai/fraud-scores/00000000-0000-0000-0000-000000000000/'), 404)


def test_new_order_is_scored_and_reviewable(user, admin, test_car):
    pickup = datetime.combine(date.today() + timedelta(days=40 + datetime.now().second % 20), datetime.min.time()).replace(hour=9)
    booking = expect(user.post('/api/car-bookings/', json={
        'carId': test_car['id'], 'pickupDate': pickup.isoformat(), 'returnDate': (pickup + timedelta(days=1)).isoformat(),
        'driverLicenseNo': 'E2E-123456', 'paymentMethod': 'credit_card'}), 201)
    try:
        payment = expect(user.get(f"/api/payments/{booking['paymentId']}/"), 200)
        score = _score_for(admin, payment['orderCode'])
        assert score['paymentId'] == booking['paymentId'] and score['userEmail'] == user.email
        assert 0 <= score['riskScore'] <= 1 and score['riskLevel'] in ('low', 'medium', 'high')
        assert {r['code'] for r in score['reasons']} <= REASON_CODES
        # Điểm = 1 - ∏(1 - w) của các lý do.
        keep = 1.0
        for r in score['reasons']:
            keep *= 1 - r['weight']
        assert abs(score['riskScore'] - round(1 - keep, 3)) < 0.002
        assert score['actionTaken'] == ('allow' if score['riskLevel'] == 'low' else 'review')

        expect(admin.put(f"/api/admin/ai/fraud-scores/{score['id']}/", json={'actionTaken': 'allow-all'}), 400)
        cleared = expect(admin.put(f"/api/admin/ai/fraud-scores/{score['id']}/", json={'actionTaken': 'cleared'}), 200)
        assert cleared['actionTaken'] == 'cleared'
        listed = expect(admin.get('/api/admin/ai/fraud-scores/', params={'action': 'cleared', 'q': payment['orderCode']}), 200)
        assert [s['id'] for s in listed] == [score['id']]
        # Chấm bù không tạo điểm trùng cho giao dịch đã có điểm.
        expect(admin.post('/api/admin/ai/fraud-scores/backfill/'), 200)
        again = expect(admin.get('/api/admin/ai/fraud-scores/', params={'q': payment['orderCode']}), 200)
        assert len(again) == 1
    finally:
        expect(user.post(f"/api/car-bookings/{booking['id']}/cancel/", json={'reason': 'E2E fraud test'}), 200)
