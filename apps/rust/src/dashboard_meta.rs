//! Dashboard metadata (`GET /api/dashboard-meta`).

use axum::response::IntoResponse;
use axum::Json;

const SOURCE: &str = "src/dashboard_meta.rs";

pub async fn meta() -> impl IntoResponse {
    tracing::info!(
        source = SOURCE,
        controller = "meta",
        method = "GET",
        path = "/api/dashboard-meta",
        "dashboard_meta request received"
    );
    let body = serde_json::json!({
        "title": "Rust APP",
        "template": "landing.html",
        "path": "/",
        "version": 10,
        "features": "sidebar,connectivity-ping,ping-all,session-auth,user-create,user-list,item-list,item-create,openapi,stack-ping,kafka-user-publish"
    });
    tracing::info!(
        source = SOURCE,
        controller = "meta",
        version = body["version"],
        "dashboard_meta succeeded"
    );
    Json(body)
}
