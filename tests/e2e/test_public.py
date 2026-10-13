"""API công khai: chỉ đọc, không ghi gì vào DB."""
from conftest import expect


def test_lookup(anon):
    assert expect(anon.get('/api/continents/'), 200)
    assert isinstance(expect(anon.get('/api/amenities/'), 200), list)
    expect(anon.get('/api/contact/info/'), 200)


def test_hotels_paging(anon):
    resp = anon.get('/api/hotels/', params={'page': 1, 'limit': 5})
    hotels = expect(resp, 200)
    assert len(hotels) <= 5
    assert int(resp.headers['X-Total-Count']) >= len(hotels)
    if hotels:
        detail = expect(anon.get(f"/api/hotels/{hotels[0]['id']}/"), 200)
        assert detail['id'] == hotels[0]['id']
        assert isinstance(expect(anon.get(f"/api/hotels/{hotels[0]['id']}/room-types/"), 200), list)


def test_hotel_not_found(anon):
    expect(anon.get('/api/hotels/00000000-0000-0000-0000-000000000000/'), 404)


def test_destinations_and_landmarks(anon):
    resp = anon.get('/api/destinations/', params={'limit': 3})
    destinations = expect(resp, 200)
    assert 'X-Total-Count' in resp.headers
    if destinations:
        expect(anon.get(f"/api/destinations/{destinations[0]['id']}/"), 200)
        expect(anon.get(f"/api/destinations/{destinations[0]['id']}/landmarks/"), 200)
    expect(anon.get('/api/destinations/', params={'category': 'nope'}), 400)
    expect(anon.get('/api/landmarks/', params={'limit': 3}), 200)


def test_catalogs(anon):
    for path in ('/api/cars/', '/api/flights/', '/api/tours/'):
        resp = anon.get(path, params={'limit': 3})
        expect(resp, 200)
        assert 'X-Total-Count' in resp.headers, path
    expect(anon.get('/api/cars/filters/'), 200)
    expect(anon.get('/api/flights/filters/'), 200)


def test_search(anon):
    data = expect(anon.get('/api/search/', params={'q': 'hoi an', 'limit': 5}), 200)
    assert data is not None
    expect(anon.get('/api/search/suggest/', params={'q': 'ha'}), 200)


def test_posts_public(anon):
    expect(anon.get('/api/posts/', params={'limit': 3}), 200)
