//! OpenAPI spec + Swagger UI (utoipa), `/api/items` only — mirrors Java springdoc scope.

use crate::items::{CreateItemRequest, CreateItemResponse, ItemResponse, UpdateItemRequest};
use serde::Serialize;
use utoipa::OpenApi;

#[derive(Serialize, utoipa::ToSchema)]
pub struct ApiError {
    pub error: String,
}

/// `GET /api/items` — list all rows from the shared `items` table.
#[allow(dead_code)]
#[utoipa::path(
    get,
    path = "/api/items",
    tag = "Items",
    params(
        ("X-Request-ID" = Option<String>, Header, description = "Correlation id for logs and Postgres trace; generated if omitted; echoed in response"),
        ("X-Request-Origin" = Option<String>, Header, description = "Upstream service when relayed (e.g. webserver-benchmark-java); logged as request_origin for tracing")
    ),
    responses(
        (status = 200, description = "All items", body = [ItemResponse]),
        (status = 503, description = "Postgres not configured", body = ApiError),
        (status = 500, description = "Database error", body = ApiError)
    )
)]
fn items_list() {}

/// `POST /api/items` with JSON `{"name": "…"}` — insert a row into the shared `items` table.
#[allow(dead_code)]
#[utoipa::path(
    post,
    path = "/api/items",
    tag = "Items",
    request_body = CreateItemRequest,
    params(
        ("X-Request-ID" = Option<String>, Header, description = "Correlation id for logs and Postgres trace; generated if omitted; echoed in response"),
        ("X-Request-Origin" = Option<String>, Header, description = "Upstream service when relayed (e.g. webserver-benchmark-java); logged as request_origin for tracing")
    ),
    responses(
        (status = 201, description = "Created", body = CreateItemResponse),
        (status = 400, description = "Blank name", body = CreateItemResponse),
        (status = 503, description = "Postgres not configured", body = CreateItemResponse),
        (status = 500, description = "Database error", body = CreateItemResponse)
    )
)]
fn items_create() {}

/// `GET /api/items/{id}` — fetch one row by id.
#[allow(dead_code)]
#[utoipa::path(
    get,
    path = "/api/items/{id}",
    tag = "Items",
    params(
        ("id" = i64, Path, description = "Item id"),
        ("X-Request-ID" = Option<String>, Header, description = "Correlation id for logs and Postgres trace")
    ),
    responses(
        (status = 200, description = "Item", body = ItemResponse),
        (status = 404, description = "Not found"),
        (status = 503, description = "Postgres not configured", body = ApiError),
        (status = 500, description = "Database error", body = ApiError)
    )
)]
fn items_get_by_id() {}

/// `PUT /api/items/{id}` — replace item name.
#[allow(dead_code)]
#[utoipa::path(
    put,
    path = "/api/items/{id}",
    tag = "Items",
    request_body = UpdateItemRequest,
    params(
        ("id" = i64, Path, description = "Item id"),
        ("X-Request-ID" = Option<String>, Header, description = "Correlation id for logs and Postgres trace")
    ),
    responses(
        (status = 200, description = "Updated item", body = ItemResponse),
        (status = 400, description = "Blank name", body = ApiError),
        (status = 404, description = "Not found"),
        (status = 503, description = "Postgres not configured", body = ApiError),
        (status = 500, description = "Database error", body = ApiError)
    )
)]
fn items_replace() {}

/// `PATCH /api/items/{id}` — update item name.
#[allow(dead_code)]
#[utoipa::path(
    patch,
    path = "/api/items/{id}",
    tag = "Items",
    request_body = UpdateItemRequest,
    params(
        ("id" = i64, Path, description = "Item id"),
        ("X-Request-ID" = Option<String>, Header, description = "Correlation id for logs and Postgres trace")
    ),
    responses(
        (status = 200, description = "Updated item", body = ItemResponse),
        (status = 400, description = "Blank name", body = ApiError),
        (status = 404, description = "Not found"),
        (status = 503, description = "Postgres not configured", body = ApiError),
        (status = 500, description = "Database error", body = ApiError)
    )
)]
fn items_patch() {}

/// `DELETE /api/items/{id}` — delete item by id.
#[allow(dead_code)]
#[utoipa::path(
    delete,
    path = "/api/items/{id}",
    tag = "Items",
    params(
        ("id" = i64, Path, description = "Item id"),
        ("X-Request-ID" = Option<String>, Header, description = "Correlation id for logs and Postgres trace")
    ),
    responses(
        (status = 204, description = "Deleted"),
        (status = 404, description = "Not found"),
        (status = 503, description = "Postgres not configured", body = ApiError),
        (status = 500, description = "Database error", body = ApiError)
    )
)]
fn items_delete() {}

#[derive(OpenApi)]
#[openapi(
    paths(items_list, items_create, items_get_by_id, items_replace, items_patch, items_delete),
    components(schemas(ItemResponse, CreateItemRequest, UpdateItemRequest, CreateItemResponse, ApiError)),
    tags(
        (name = "Items", description = "Shared PostgreSQL `items` table (Flyway schema from Java)")
    ),
    info(
        title = "WebServer BenchMark Rust API",
        version = "1.0",
        description = "REST CRUD for `/api/items`. Dashboard, observability, and stack-ping routes are excluded."
    )
)]
pub struct ApiDoc;
