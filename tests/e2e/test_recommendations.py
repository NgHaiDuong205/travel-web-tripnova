"""Gợi ý cá nhân hoá, ghi tương tác, khách sạn tương tự.

Dữ liệu: ghi vài lượt xem cho e2e.user; thêm 1 mục yêu thích (khách sạn test) rồi bỏ đúng mục đó.
"""
import pytest

from conftest import expect

TYPES = {'hotel', 'tour', 'destination', 'landmark', 'car'}
REASONS = {'upcoming_trip', 'liked_destination', 'similar_destination', 'popular'}


def _first_hotel(anon, name=None):
    params = {'page': 1, 'limit': 1}
    if name:
        params['name'] = name
    hotels = expect(anon.get('/api/hotels/', params=params), 200)
    if not hotels:
        pytest.skip('Không có khách sạn')
    return hotels[0]


def test_recommendations_auth_and_validation(anon, user):
    expect(anon.get('/api/ai/recommendations/'), 401)
    expect(user.get('/api/ai/recommendations/', params={'type': 'user'}), 400)
    expect(anon.post('/api/interactions/', json={'entityType': 'hotel', 'entityId': '00000000-0000-0000-0000-000000000000'}), 401)
    expect(user.post('/api/interactions/', json={'entityType': 'user', 'entityId': '00000000-0000-0000-0000-000000000000'}), 400)
    hotel = _first_hotel(anon)
    expect(user.post('/api/interactions/', json={'entityType': 'hotel', 'entityId': hotel['id'], 'action': 'buy'}), 400)
    expect(user.post('/api/interactions/', json={'entityType': 'hotel', 'entityId': hotel['id']}), 204)
    expect(user.post('/api/interactions/', json={'entityType': 'hotel', 'entityId': hotel['id']}), 204)  # trùng: bỏ qua


def test_recommendations_shape_and_mix(user):
    items = expect(user.get('/api/ai/recommendations/', params={'limit': 10}), 200)
    assert 0 < len(items) <= 10
    for item in items:
        assert item['entityType'] in TYPES and item['reason'] in REASONS, item
        assert item['name'] and item['entityId']
        if item['entityType'] != 'destination':
            assert item['destinationName'], item
    # type=all trộn nhiều loại; lọc theo loại chỉ trả đúng loại, xếp theo điểm giảm dần.
    assert len({i['entityType'] for i in items}) >= 2
    hotels = expect(user.get('/api/ai/recommendations/', params={'type': 'hotel', 'limit': 30}), 200)
    assert hotels and all(h['entityType'] == 'hotel' for h in hotels)
    scores = [h['score'] for h in hotels]
    assert scores == sorted(scores, reverse=True)
    # Đa dạng: tối đa 4 khách sạn mỗi điểm đến.
    per_dest = {}
    for h in hotels:
        per_dest[h['destinationId']] = per_dest.get(h['destinationId'], 0) + 1
    assert max(per_dest.values()) <= 4


def test_favorite_is_excluded_and_refreshes(user, test_hotel):
    hotel, _ = test_hotel
    before = {f['itemId'] for f in expect(user.get('/api/favorites/'), 200)}
    fav = None
    try:
        if hotel['id'] not in before:
            fav = expect(user.post('/api/favorites/', json={'itemType': 'hotel', 'itemId': hotel['id']}), 201)
        items = expect(user.get('/api/ai/recommendations/', params={'type': 'hotel', 'limit': 30}), 200)
        assert hotel['id'] not in {i['entityId'] for i in items}, 'Mục đã yêu thích không được gợi ý lại'
        favorites = {f['itemId'] for f in expect(user.get('/api/favorites/'), 200)}
        assert not favorites & {i['entityId'] for i in items}
    finally:
        if fav:
            expect(user.delete(f"/api/favorites/{fav['id']}/"), 204)


def test_similar_hotels(anon):
    expect(anon.get('/api/hotels/00000000-0000-0000-0000-000000000000/similar/'), 404)
    hotel = _first_hotel(anon, 'Da Nang')
    detail = expect(anon.get(f"/api/hotels/{hotel['id']}/"), 200)
    similar = expect(anon.get(f"/api/hotels/{hotel['id']}/similar/", params={'limit': 4}), 200)
    assert 0 < len(similar) <= 4
    assert hotel['id'] not in {s['entityId'] for s in similar}
    for s in similar:
        assert s['entityType'] == 'hotel' and s['reason'] == 'similar' and 0 < s['score'] <= 1
        assert s['destinationName'] == detail.get('destinationName', s['destinationName'])
    again = expect(anon.get(f"/api/hotels/{hotel['id']}/similar/", params={'limit': 4}), 200)
    assert [s['entityId'] for s in again] == [s['entityId'] for s in similar]  # cache ổn định
