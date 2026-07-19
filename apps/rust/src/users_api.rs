//! User registration and CRUD (`/api/users`).

use axum::extract::{Extension, Path, State};
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::Deserialize;
use serde::Serialize;

use crate::app::AppState;
use crate::auth::password;
use crate::db::UserWithCreatedAt;
use crate::request_id::RequestId;

const SOURCE: &str = "src/users_api.rs";

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateUserBody {
    pub name: String,
    pub email: String,
    pub password: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateUserBody {
    pub name: String,
    pub email: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserResponse {
    pub id: i64,
    pub name: String,
    pub email: String,
    pub created_at: String,
}

impl From<UserWithCreatedAt> for UserResponse {
    fn from(user: UserWithCreatedAt) -> Self {
        Self {
            id: user.id,
            name: user.name,
            email: user.email,
            created_at: user.created_at,
        }
    }
}

fn is_valid_email(email: &str) -> bool {
    let Some((local, domain)) = email.split_once('@') else {
        return false;
    };
    !local.is_empty() && !domain.is_empty() && domain.contains('.')
}

fn postgres_unavailable() -> Response {
    (
        StatusCode::SERVICE_UNAVAILABLE,
        Json(serde_json::json!({ "error": "Postgres not configured" })),
    )
        .into_response()
}

fn validate_update_body(name: &str, email: &str) -> Option<Response> {
    if name.is_empty() {
        return Some(
            (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": "name is required" })),
            )
                .into_response(),
        );
    }
    if !is_valid_email(email) {
        return Some(
            (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": "email must be valid" })),
            )
                .into_response(),
        );
    }
    None
}

pub async fn list_users(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
) -> Response {
    tracing::info!(
        source = SOURCE,
        controller = "list_users",
        method = "GET",
        path = "/api/users",
        request_id = %request_id.0,
        "list_users request received"
    );
    let Some(pool) = state.pg_pool.as_ref() else {
        return postgres_unavailable();
    };
    match crate::db::list_users(pool, Some(&request_id.0)).await {
        Ok(users) => {
            let count = users.len();
            let responses: Vec<UserResponse> = users.into_iter().map(UserResponse::from).collect();
            tracing::info!(
                source = SOURCE,
                controller = "list_users",
                count = count,
                "list_users succeeded"
            );
            (StatusCode::OK, Json(responses)).into_response()
        }
        Err(err) => {
            tracing::error!(source = SOURCE, error = %err, "list_users failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": err.to_string() })),
            )
                .into_response()
        }
    }
}

pub async fn create_user(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Json(body): Json<CreateUserBody>,
) -> Response {
    tracing::info!(
        source = SOURCE,
        controller = "create_user",
        method = "POST",
        path = "/api/users",
        request_id = %request_id.0,
        "create_user request received"
    );
    let name = body.name.trim();
    let email = body.email.trim();
    if name.is_empty() || email.is_empty() || body.password.len() < 8 {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "name, email, and password (min 8 chars) are required" })),
        )
            .into_response();
    }
    if !is_valid_email(email) {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "email must be valid" })),
        )
            .into_response();
    }
    let Some(pool) = state.pg_pool.as_ref() else {
        return postgres_unavailable();
    };
    let password_hash = match password::hash_password(&body.password) {
        Ok(hash) => hash,
        Err(err) => {
            tracing::warn!(source = SOURCE, error = %err, "password hash failed");
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "password hash failed" })),
            )
                .into_response();
        }
    };
    match crate::db::insert_user_with_password(
        pool,
        name,
        email,
        Some(password_hash.as_str()),
        Some(&request_id.0),
    )
    .await
    {
        Ok(user) => {
            tracing::info!(
                source = SOURCE,
                controller = "create_user",
                id = user.id,
                email = %user.email,
                "create_user succeeded"
            );
            (
                StatusCode::CREATED,
                Json(UserResponse {
                    id: user.id,
                    name: user.name,
                    email: user.email,
                    created_at: user.created_at,
                }),
            )
                .into_response()
        }
        Err(err) => {
            let message = err.to_string();
            let status = if message.contains("duplicate") || message.contains("unique") {
                StatusCode::CONFLICT
            } else {
                StatusCode::INTERNAL_SERVER_ERROR
            };
            tracing::warn!(source = SOURCE, error = %message, "create_user failed");
            (
                status,
                Json(serde_json::json!({ "error": if status == StatusCode::CONFLICT { "Email already registered" } else { message.as_str() } })),
            )
                .into_response()
        }
    }
}

pub async fn get_user_by_id(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Path(user_id): Path<i64>,
) -> Response {
    tracing::info!(
        source = SOURCE,
        controller = "get_user_by_id",
        method = "GET",
        path = "/api/users/{id}",
        id = user_id,
        "get_user_by_id request received"
    );
    let Some(pool) = state.pg_pool.as_ref() else {
        return postgres_unavailable();
    };
    match crate::db::find_user_with_created_at(pool, user_id, Some(&request_id.0)).await {
        Ok(Some(user)) => {
            tracing::info!(
                source = SOURCE,
                controller = "get_user_by_id",
                id = user_id,
                email = %user.email,
                "get_user_by_id succeeded"
            );
            (StatusCode::OK, Json(UserResponse::from(user))).into_response()
        }
        Ok(None) => {
            tracing::warn!(source = SOURCE, id = user_id, "get_user_by_id not found");
            StatusCode::NOT_FOUND.into_response()
        }
        Err(err) => {
            tracing::error!(source = SOURCE, id = user_id, error = %err, "get_user_by_id failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": err.to_string() })),
            )
                .into_response()
        }
    }
}

async fn update_user_handler(
    state: AppState,
    request_id: &str,
    user_id: i64,
    body: UpdateUserBody,
    method: &'static str,
) -> Response {
    let name = body.name.trim();
    let email = body.email.trim();
    tracing::info!(
        source = SOURCE,
        controller = "update_user",
        method = method,
        path = "/api/users/{id}",
        id = user_id,
        name = %name,
        email = %email,
        "update_user request received"
    );
    if let Some(response) = validate_update_body(name, email) {
        return response;
    }
    let Some(pool) = state.pg_pool.as_ref() else {
        return postgres_unavailable();
    };
    match crate::db::update_user(pool, user_id, name, email, Some(request_id)).await {
        Ok(Some(user)) => {
            tracing::info!(
                source = SOURCE,
                controller = "update_user",
                id = user_id,
                email = %user.email,
                "update_user succeeded"
            );
            (StatusCode::OK, Json(UserResponse::from(user))).into_response()
        }
        Ok(None) => {
            tracing::warn!(source = SOURCE, id = user_id, "update_user not found");
            StatusCode::NOT_FOUND.into_response()
        }
        Err(err) => {
            let message = err.to_string();
            let status = if message.contains("duplicate") || message.contains("unique") {
                StatusCode::CONFLICT
            } else {
                StatusCode::INTERNAL_SERVER_ERROR
            };
            tracing::warn!(source = SOURCE, id = user_id, error = %message, "update_user failed");
            (
                status,
                Json(serde_json::json!({ "error": if status == StatusCode::CONFLICT { "Email already registered" } else { message.as_str() } })),
            )
                .into_response()
        }
    }
}

pub async fn replace_user(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Path(user_id): Path<i64>,
    Json(body): Json<UpdateUserBody>,
) -> Response {
    update_user_handler(state, &request_id.0, user_id, body, "PUT").await
}

pub async fn patch_user(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Path(user_id): Path<i64>,
    Json(body): Json<UpdateUserBody>,
) -> Response {
    update_user_handler(state, &request_id.0, user_id, body, "PATCH").await
}

pub async fn delete_user(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Path(user_id): Path<i64>,
) -> Response {
    tracing::info!(
        source = SOURCE,
        controller = "delete_user",
        method = "DELETE",
        path = "/api/users/{id}",
        id = user_id,
        "delete_user request received"
    );
    let Some(pool) = state.pg_pool.as_ref() else {
        return postgres_unavailable();
    };
    match crate::db::delete_user_by_id(pool, user_id, Some(&request_id.0)).await {
        Ok(true) => {
            tracing::info!(source = SOURCE, id = user_id, "delete_user succeeded");
            StatusCode::NO_CONTENT.into_response()
        }
        Ok(false) => {
            tracing::warn!(source = SOURCE, id = user_id, "delete_user not found");
            StatusCode::NOT_FOUND.into_response()
        }
        Err(err) => {
            tracing::error!(source = SOURCE, id = user_id, error = %err, "delete_user failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": err.to_string() })),
            )
                .into_response()
        }
    }
}
