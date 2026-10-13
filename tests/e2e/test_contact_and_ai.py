"""Liên hệ + admin trả lời (email), giới hạn AI Planner ghi vào audit log."""
import time
from datetime import date, timedelta

import pytest

from conftest import expect, unique


def test_contact_reply_sends_email(anon, admin, mailpit):
    email = unique('e2e.contact') + '@tripnova.local'
    subject = unique('E2E subject')
    expect(anon.post('/api/contact/', json={'fullName': 'E2E Contact', 'email': email, 'subject': subject,
                                            'message': 'Xin chào, đây là tin nhắn kiểm thử.'}), 201)
    messages = expect(admin.get('/api/admin/contact-messages/', params={'limit': 50}), 200)
    message = next(m for m in messages if m.get('subject') == subject)
    since = time.time()
    reply = expect(admin.post(f"/api/admin/contact-messages/{message['id']}/reply/",
                              json={'content': 'Cảm ơn bạn.\n\nChúng tôi đã nhận được tin nhắn.'}), 200)
    assert reply['status'] == 'resolved'
    mail = mailpit.wait_for(email, 'Re: ' + subject, since)
    assert 'Chúng tôi đã nhận được tin nhắn.' in mail['Text']
    assert 'đây là tin nhắn kiểm thử' in mail['Text']


def _planner_destination(anon):
    for q in ('Hoi An', 'Da Nang', 'Ha Noi'):
        found = expect(anon.get('/api/destinations/', params={'q': q, 'limit': 1}), 200)
        if found:
            return found[0]
    pytest.skip('Không có điểm đến nào cho AI Planner')


def test_planner_counts_in_audit_log(anon, user, admin):
    me = expect(user.get('/api/auth/me/'), 200)
    params = {'userId': me['id'], 'action': 'ai.itineraries.plan', 'limit': 1}
    before = int(admin.get('/api/admin/audit-logs/', params=params).headers['X-Total-Count'])
    destination = _planner_destination(anon)
    resp = user.post('/api/ai/itineraries/plan/', json={
        'destinationId': destination['id'], 'startDate': (date.today() + timedelta(days=30)).isoformat(),
        'days': 1, 'partySize': 2, 'budget': 'mid', 'pace': 'balanced', 'interests': ['culture'], 'locale': 'vi'})
    if resp.status_code == 429:
        pytest.skip('Tài khoản test đang chạm giới hạn AI Planner — chạy lại sau ít phút')
    plan = expect(resp, 200)
    assert plan['days']
    after = int(admin.get('/api/admin/audit-logs/', params=params).headers['X-Total-Count'])
    assert after == before + 1
