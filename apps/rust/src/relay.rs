//! Dashboard outbound item relays (mirrors Java `*ItemRelayService` / `PythonReactRelayService`).

use axum::extract::{Extension, State};
use axum::response::{IntoResponse, Json};
use serde::Deserialize;
use serde_json::{Map, Value};
use std::time::Duration;

use crate::app::AppState;
use crate::request_id::RequestId;

const SOURCE: &str = "src/relay.rs";

#[derive(Debug, Deserialize)]
pub struct RelayItemBody {
    pub name: String,
}

fn blank_name_response(request_id: &str) -> Value {
    serde_json::json!({
        "ok": false,
        "error": "name must not be blank",
        "requestId": request_id
    })
}

fn parse_json_body(raw: &str) -> Option<Value> {
    serde_json::from_str(raw).ok()
}

fn post_json_relay(
    relay_target: &str,
    url_key: &str,
    downstream_key: &str,
    uri: &str,
    name: &str,
    request_id: &str,
) -> Value {
    let outbound_id = crate::request_id::resolve_outbound_request_id(Some(request_id));
    let body = serde_json::json!({ "name": name }).to_string();
    tracing::info!(
        source = SOURCE,
        controller = "post_json_relay",
        relay_target = relay_target,
        url = %uri,
        name = %name,
        "post_json_relay calling downstream"
    );
    match ureq::post(uri)
        .set(crate::request_id::REQUEST_ID_HEADER, &outbound_id)
        .set(crate::request_id::ORIGIN_HEADER, crate::request_id::OUTBOUND_ORIGIN)
        .set("Content-Type", "application/json")
        .timeout(Duration::from_secs(15))
        .send_string(&body)
    {
        Ok(resp) => {
            let status = resp.status();
            let raw_body = resp.into_string().unwrap_or_default();
            let ok = (200..300).contains(&status);
            let mut out = Map::new();
            out.insert("ok".into(), Value::Bool(ok));
            out.insert("requestId".into(), Value::String(request_id.to_string()));
            out.insert(url_key.into(), Value::String(uri.to_string()));
            out.insert("status".into(), Value::from(status));
            out.insert("body".into(), Value::String(raw_body.clone()));
            if let Some(parsed) = parse_json_body(&raw_body) {
                out.insert(downstream_key.into(), parsed);
            }
            if ok {
                tracing::info!(
                    source = SOURCE,
                    controller = "post_json_relay",
                    relay_target = relay_target,
                    status = status,
                    "post_json_relay succeeded"
                );
            } else {
                tracing::warn!(
                    source = SOURCE,
                    controller = "post_json_relay",
                    relay_target = relay_target,
                    status = status,
                    "post_json_relay downstream error"
                );
            }
            Value::Object(out)
        }
        Err(ureq::Error::Status(status, resp)) => {
            let raw_body = resp.into_string().unwrap_or_default();
            tracing::warn!(
                source = SOURCE,
                controller = "post_json_relay",
                relay_target = relay_target,
                status = status,
                "post_json_relay failed"
            );
            let mut out = Map::new();
            out.insert("ok".into(), Value::Bool(false));
            out.insert("requestId".into(), Value::String(request_id.to_string()));
            out.insert(url_key.into(), Value::String(uri.to_string()));
            out.insert("status".into(), Value::from(status));
            out.insert("error".into(), Value::String(raw_body));
            Value::Object(out)
        }
        Err(e) => {
            let error = format!("{e}");
            tracing::warn!(
                source = SOURCE,
                controller = "post_json_relay",
                relay_target = relay_target,
                error = %error,
                "post_json_relay failed"
            );
            let mut out = Map::new();
            out.insert("ok".into(), Value::Bool(false));
            out.insert("requestId".into(), Value::String(request_id.to_string()));
            out.insert(url_key.into(), Value::String(uri.to_string()));
            out.insert("error".into(), Value::String(error));
            Value::Object(out)
        }
    }
}

fn relay_base_urls(stack: &crate::stack_ping::StackLinks) -> (String, String, String) {
    let python = stack.python_base_url().trim_end_matches('/').to_string();
    let rust = stack.rust_base_url().trim_end_matches('/').to_string();
    let react = stack.react_node_base_url().trim_end_matches('/').to_string();
    (python, rust, react)
}

pub async fn add_via_python(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Json(body): Json<RelayItemBody>,
) -> impl IntoResponse {
    let name = body.name.trim();
    if name.is_empty() {
        return Json(blank_name_response(&request_id.0));
    }
    let (python, _, _) = relay_base_urls(&state.stack_links);
    let uri = format!("{python}/api/items");
    Json(post_json_relay(
        "webserver-benchmark-python",
        "pythonUrl",
        "python",
        &uri,
        name,
        &request_id.0,
    ))
}

pub async fn add_via_rust(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Json(body): Json<RelayItemBody>,
) -> impl IntoResponse {
    let name = body.name.trim();
    if name.is_empty() {
        return Json(blank_name_response(&request_id.0));
    }
    let (_, rust, _) = relay_base_urls(&state.stack_links);
    let uri = format!("{rust}/api/items");
    Json(post_json_relay(
        "webserver-benchmark-rust",
        "rustUrl",
        "rust",
        &uri,
        name,
        &request_id.0,
    ))
}

pub async fn add_via_react_node(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Json(body): Json<RelayItemBody>,
) -> impl IntoResponse {
    let name = body.name.trim();
    if name.is_empty() {
        return Json(blank_name_response(&request_id.0));
    }
    let (_, _, react) = relay_base_urls(&state.stack_links);
    let uri = format!("{react}/api/items");
    Json(post_json_relay(
        "webserver-benchmark-react-node",
        "reactNodeUrl",
        "reactNode",
        &uri,
        name,
        &request_id.0,
    ))
}

pub async fn relay_python_react(
    State(state): State<AppState>,
    Extension(request_id): Extension<RequestId>,
    Json(body): Json<RelayItemBody>,
) -> impl IntoResponse {
    const RELAY_CHAIN: &str =
        "webserver-benchmark-rust → webserver-benchmark-python → webserver-benchmark-react-node";
    let name = body.name.trim();
    if name.is_empty() {
        return Json(serde_json::json!({
            "ok": false,
            "error": "name must not be blank",
            "requestId": request_id.0,
            "relayChain": RELAY_CHAIN
        }));
    }
    let (python, _, _) = relay_base_urls(&state.stack_links);
    let uri = format!("{python}/api/relay/react");
    let mut result = post_json_relay(
        "webserver-benchmark-python",
        "pythonRelayUrl",
        "downstream",
        &uri,
        name,
        &request_id.0,
    );
    if let Value::Object(ref mut map) = result {
        map.insert("relayChain".into(), Value::String(RELAY_CHAIN.to_string()));
    }
    Json(result)
}

pub async fn hello_from_java() -> impl IntoResponse {
    tracing::info!(
        source = SOURCE,
        controller = "hello_from_java",
        method = "GET",
        path = "/api/hello-from-java",
        "hello_from_java request received"
    );
    Json(serde_json::json!({
        "message": "Hello from Rust",
        "path": "/api/hello-from-java",
        "note": "Called by another stack dashboard or any HTTP client."
    }))
}
