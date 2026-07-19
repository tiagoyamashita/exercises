use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::Json;
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use utoipa::ToSchema;

const SOURCE: &str = "src/items.rs";

#[derive(Deserialize, ToSchema)]
pub struct CreateItemRequest {
    pub name: String,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct ItemResponse {
    pub id: i64,
    pub name: String,
    #[serde(rename = "createdAt")]
    pub created_at: String,
}

#[derive(Serialize, ToSchema)]
pub struct CreateItemResponse {
    pub ok: bool,
    pub name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    #[serde(rename = "requestId", skip_serializing_if = "Option::is_none")]
    pub request_id: Option<String>,
}

#[derive(Deserialize, ToSchema)]
pub struct UpdateItemRequest {
    pub name: String,
}

fn item_row_to_response(row: crate::db::ItemRow) -> ItemResponse {
    ItemResponse {
        id: row.id,
        name: row.name,
        created_at: row.created_at,
    }
}

/// `GET /api/items/{id}` — fetch one row by id.
pub async fn get_item_by_id(
    pool: PgPool,
    item_id: i64,
    request_id: Option<&str>,
) -> impl IntoResponse {
    tracing::info!(
        source = SOURCE,
        controller = "get_item_by_id",
        method = "GET",
        path = "/api/items/{id}",
        id = item_id,
        "get_item_by_id request received"
    );
    match crate::db::find_item_by_id(&pool, item_id, request_id).await {
        Ok(Some(row)) => {
            let response = item_row_to_response(row);
            tracing::info!(
                source = SOURCE,
                controller = "get_item_by_id",
                id = item_id,
                name = %response.name,
                "get_item_by_id succeeded"
            );
            (StatusCode::OK, Json(response)).into_response()
        }
        Ok(None) => {
            tracing::warn!(
                source = SOURCE,
                controller = "get_item_by_id",
                id = item_id,
                "get_item_by_id not found"
            );
            StatusCode::NOT_FOUND.into_response()
        }
        Err(e) => {
            tracing::error!(
                source = SOURCE,
                controller = "get_item_by_id",
                id = item_id,
                error = %e,
                "get_item_by_id failed"
            );
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": e.to_string() })),
            )
                .into_response()
        }
    }
}

async fn update_item_name(
    pool: PgPool,
    item_id: i64,
    body: UpdateItemRequest,
    method: &'static str,
    request_id: Option<&str>,
) -> impl IntoResponse {
    let name = body.name.trim().to_string();
    tracing::info!(
        source = SOURCE,
        controller = "update_item_name",
        method = method,
        path = "/api/items/{id}",
        id = item_id,
        name = %name,
        "update_item_name request received"
    );
    if name.is_empty() {
        tracing::warn!(
            source = SOURCE,
            controller = "update_item_name",
            id = item_id,
            reason = "blank-name",
            "update_item_name validation failed"
        );
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "name must not be blank" })),
        )
            .into_response();
    }
    match crate::db::update_item_name(&pool, item_id, &name, request_id).await {
        Ok(Some(row)) => {
            let response = item_row_to_response(row);
            tracing::info!(
                source = SOURCE,
                controller = "update_item_name",
                id = item_id,
                name = %response.name,
                "update_item_name succeeded"
            );
            (StatusCode::OK, Json(response)).into_response()
        }
        Ok(None) => {
            tracing::warn!(
                source = SOURCE,
                controller = "update_item_name",
                id = item_id,
                name = %name,
                "update_item_name not found"
            );
            StatusCode::NOT_FOUND.into_response()
        }
        Err(e) => {
            tracing::error!(
                source = SOURCE,
                controller = "update_item_name",
                id = item_id,
                error = %e,
                "update_item_name failed"
            );
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": e.to_string() })),
            )
                .into_response()
        }
    }
}

/// `PUT /api/items/{id}` — replace item name.
pub async fn replace_item(
    pool: PgPool,
    item_id: i64,
    body: UpdateItemRequest,
    request_id: Option<&str>,
) -> impl IntoResponse {
    update_item_name(pool, item_id, body, "PUT", request_id).await
}

/// `PATCH /api/items/{id}` — update item name.
pub async fn patch_item(
    pool: PgPool,
    item_id: i64,
    body: UpdateItemRequest,
    request_id: Option<&str>,
) -> impl IntoResponse {
    update_item_name(pool, item_id, body, "PATCH", request_id).await
}

/// `DELETE /api/items/{id}` — delete item by id.
pub async fn delete_item(
    pool: PgPool,
    item_id: i64,
    request_id: Option<&str>,
) -> impl IntoResponse {
    tracing::info!(
        source = SOURCE,
        controller = "delete_item",
        method = "DELETE",
        path = "/api/items/{id}",
        id = item_id,
        "delete_item request received"
    );
    match crate::db::delete_item_by_id(&pool, item_id, request_id).await {
        Ok(true) => {
            tracing::info!(
                source = SOURCE,
                controller = "delete_item",
                id = item_id,
                "delete_item succeeded"
            );
            StatusCode::NO_CONTENT.into_response()
        }
        Ok(false) => {
            tracing::warn!(
                source = SOURCE,
                controller = "delete_item",
                id = item_id,
                "delete_item not found"
            );
            StatusCode::NOT_FOUND.into_response()
        }
        Err(e) => {
            tracing::error!(
                source = SOURCE,
                controller = "delete_item",
                id = item_id,
                error = %e,
                "delete_item failed"
            );
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": e.to_string() })),
            )
                .into_response()
        }
    }
}

pub async fn list_items(pool: PgPool, request_id: Option<&str>) -> impl IntoResponse {
    tracing::info!(
        source = SOURCE,
        controller = "list_items",
        method = "GET",
        path = "/api/items",
        "list_items request received"
    );
    match crate::db::list_items(&pool, request_id).await {
        Ok(rows) => {
            let count = rows.len();
            let responses: Vec<ItemResponse> = rows
                .into_iter()
                .map(|row| ItemResponse {
                    id: row.id,
                    name: row.name,
                    created_at: row.created_at,
                })
                .collect();
            tracing::info!(
                source = SOURCE,
                controller = "list_items",
                count = count,
                "list_items succeeded"
            );
            tracing::trace!(
                source = SOURCE,
                controller = "list_items",
                items = ?responses,
                "list_items result"
            );
            (StatusCode::OK, Json(responses)).into_response()
        }
        Err(e) => {
            tracing::error!(
                source = SOURCE,
                controller = "list_items",
                error = %e,
                "list_items failed"
            );
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": e.to_string() })),
            )
                .into_response()
        }
    }
}

fn request_id_source_label(source: crate::request_id::RequestIdSource) -> &'static str {
    match source {
        crate::request_id::RequestIdSource::ReceivedHeader => "header",
        crate::request_id::RequestIdSource::Generated => "generated",
    }
}

/// `POST /api/items` with JSON `{"name": "…"}` — inserts into Postgres `items` (Flyway schema from Java).
pub async fn create_item(
    pool: PgPool,
    body: CreateItemRequest,
    request_id: Option<&str>,
    request_origin: Option<&str>,
    request_id_source: crate::request_id::RequestIdSource,
    log_seq: Option<&crate::request_id::RequestLogSeq>,
) -> impl IntoResponse {
    let name = body.name.trim().to_string();
    let id_source = request_id_source_label(request_id_source);
    let seq = log_seq.map(crate::request_id::RequestLogSeq::next).unwrap_or(0);
    tracing::info!(
        source = SOURCE,
        controller = "create_item",
        method = "POST",
        path = "/api/items",
        name = %name,
        request_id_source = id_source,
        request_origin = request_origin.unwrap_or(""),
        log_seq = seq,
        "create_item request received"
    );
    if name.is_empty() {
        tracing::warn!(
            source = SOURCE,
            controller = "create_item",
            name = %body.name,
            reason = "blank-name",
            log_seq = log_seq.map(crate::request_id::RequestLogSeq::next).unwrap_or(0),
            "create_item validation failed"
        );
        return (
            StatusCode::BAD_REQUEST,
            Json(CreateItemResponse {
                ok: false,
                name: String::new(),
                id: None,
                created_at: None,
                error: Some("name must not be blank".into()),
                request_id: request_id.map(str::to_string),
            }),
        )
            .into_response();
    }

    match crate::db::insert_item(&pool, &name, request_id).await {
        Ok(row) => {
            tracing::info!(
                source = SOURCE,
                controller = "create_item",
                request_id_source = id_source,
                request_origin = request_origin.unwrap_or(""),
                id = row.id,
                name = %row.name,
                log_seq = log_seq.map(crate::request_id::RequestLogSeq::next).unwrap_or(0),
                "create_item succeeded"
            );
            (
                StatusCode::CREATED,
                Json(CreateItemResponse {
                    ok: true,
                    name: row.name,
                    id: Some(row.id),
                    created_at: Some(row.created_at),
                    error: None,
                    request_id: request_id.map(str::to_string),
                }),
            )
                .into_response()
        }
        Err(e) => {
            tracing::error!(
                source = SOURCE,
                controller = "create_item",
                name = %name,
                error = %e,
                log_seq = log_seq.map(crate::request_id::RequestLogSeq::next).unwrap_or(0),
                "create_item failed"
            );
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(CreateItemResponse {
                    ok: false,
                    name,
                    id: None,
                    created_at: None,
                    error: Some(e.to_string()),
                    request_id: request_id.map(str::to_string),
                }),
            )
                .into_response()
        }
    }
}
