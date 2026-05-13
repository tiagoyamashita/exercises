/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_EXERCISES_JAVA_EMBED_URL?: string;
  readonly VITE_EXERCISES_PYTHON_EMBED_URL?: string;
  readonly VITE_EXERCISES_RUST_EMBED_URL?: string;
  /** Legacy: used as Java URL when `VITE_EXERCISES_JAVA_EMBED_URL` is unset */
  readonly VITE_EXERCISES_EMBED_URL?: string;
}
