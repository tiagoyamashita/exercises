"""
Placeholder readiness checks for a future /api/items REST API.
These assert successful HTTP responses; they fail until routes are implemented.

One test per HTTP method, one file dedicated to items.
"""

from __future__ import annotations

BASE = "/api/items"


def test_get_items_collection_is_reachable(client):
    r = client.get(BASE)
    assert r.status_code == 200, "GET /api/items should return 200 once the API exists"


def test_post_items_collection_is_reachable(client):
    r = client.post(BASE, json={"title": "Widget"}, content_type="application/json")
    assert r.status_code in (200, 201), "POST /api/items should succeed once the API exists"


def test_get_item_by_id_is_reachable(client):
    r = client.get(f"{BASE}/42")
    assert r.status_code == 200, "GET /api/items/42 should return 200 once the API exists"


def test_put_item_by_id_is_reachable(client):
    r = client.put(f"{BASE}/42", json={"title": "Gadget"}, content_type="application/json")
    assert r.status_code in (200, 204), "PUT /api/items/42 should succeed once the API exists"


def test_patch_item_by_id_is_reachable(client):
    r = client.patch(f"{BASE}/42", json={"title": "Gadget"}, content_type="application/json")
    assert r.status_code in (200, 204), "PATCH /api/items/42 should succeed once the API exists"


def test_delete_item_by_id_is_reachable(client):
    r = client.delete(f"{BASE}/42")
    assert r.status_code in (200, 204), "DELETE /api/items/42 should succeed once the API exists"
