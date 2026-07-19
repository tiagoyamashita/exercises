//! Demo endpoint for routing and HTTP verb tests (`/testingendpoint`).

use axum::body::Bytes;
use axum::http::{header, HeaderValue, StatusCode};
use axum::response::IntoResponse;

const PATH: &str = "/testingendpoint";

pub async fn get() -> impl IntoResponse {
    (
        [(
            header::CONTENT_TYPE,
            HeaderValue::from_static("text/plain; charset=utf-8"),
        )],
        format!("{PATH} GET"),
    )
}

pub async fn post(_body: Option<Bytes>) -> impl IntoResponse {
    (
        [(
            header::CONTENT_TYPE,
            HeaderValue::from_static("text/plain; charset=utf-8"),
        )],
        format!("{PATH} POST"),
    )
}

pub async fn put(_body: Option<Bytes>) -> impl IntoResponse {
    (
        [(
            header::CONTENT_TYPE,
            HeaderValue::from_static("text/plain; charset=utf-8"),
        )],
        format!("{PATH} PUT"),
    )
}

pub async fn patch(_body: Option<Bytes>) -> impl IntoResponse {
    (
        [(
            header::CONTENT_TYPE,
            HeaderValue::from_static("text/plain; charset=utf-8"),
        )],
        format!("{PATH} PATCH"),
    )
}

pub async fn delete() -> impl IntoResponse {
    (
        [(
            header::CONTENT_TYPE,
            HeaderValue::from_static("text/plain; charset=utf-8"),
        )],
        format!("{PATH} DELETE"),
    )
}

pub async fn options() -> impl IntoResponse {
    (
        StatusCode::OK,
        [(
            header::ALLOW,
            HeaderValue::from_static("GET, POST, PUT, PATCH, DELETE, OPTIONS"),
        )],
    )
}
