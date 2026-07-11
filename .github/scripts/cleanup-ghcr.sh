#!/usr/bin/env bash
set -euo pipefail

USERNAME="${1:-LeninTF}"
REPO="${2:-Trazzo}"
KEEP_LATEST="${3:-10}"
DRY_RUN="${4:-true}"
PACKAGES=("trazzo-back" "trazzo-front")

if ! command -v gh &>/dev/null; then
  echo "Error: gh CLI no encontrado. Instala desde https://cli.github.com/" >&2
  exit 1
fi

if ! command -v jq &>/dev/null; then
  echo "Error: jq no encontrado. Instálalo (apt install jq, brew install jq, etc.)" >&2
  exit 1
fi

for PKG in "${PACKAGES[@]}"; do
  echo ""
  echo "========== Procesando: $PKG =========="

  # GHCR siempre normaliza a minúsculas en la API
  REPO_LOWER=$(echo "$REPO" | tr '[:upper:]' '[:lower:]')
  ENCODED_PKG="$REPO_LOWER/$PKG"
  API_URL="/users/$USERNAME/packages/container/${ENCODED_PKG//\//%2F}/versions"

  VERSIONS=$(gh api "$API_URL" --paginate 2>&1) || {
    echo "  Error al consultar API. Autentícate con 'gh auth login' (scope: read:packages, delete:packages)."
    exit 1
  }

  COUNT=$(echo "$VERSIONS" | jq -r 'length')
  if [[ "$COUNT" -eq 0 ]]; then
    echo "  No se encontraron versiones."
    continue
  fi

  echo "  Total versiones: $COUNT"

  SORTED=$(echo "$VERSIONS" | jq -r 'sort_by(.created_at) | reverse | .[] | @base64')
  TO_KEEP=$(echo "$SORTED" | head -n "$KEEP_LATEST")
  TO_DELETE=$(echo "$SORTED" | tail -n +$((KEEP_LATEST + 1)))

  echo "  A mantener: $(echo "$TO_KEEP" | wc -l) versión(es)"
  for ENTRY in $TO_KEEP; do
    _jq() { echo "$ENTRY" | base64 -d | jq -r "$1"; }
    ID=$(_jq '.id')
    CREATED=$(_jq '.created_at')
    TAGS=$(_jq '.metadata.container.tags | join(", ")')
    echo "    [$ID] $CREATED | tags: $TAGS"
  done

  DELETE_COUNT=$(echo "$TO_DELETE" | wc -l)
  if [[ "$DELETE_COUNT" -eq 0 ]]; then
    echo "  No hay versiones para eliminar."
    continue
  fi

  echo "  A eliminar: $DELETE_COUNT versión(es)"
  for ENTRY in $TO_DELETE; do
    _jq() { echo "$ENTRY" | base64 -d | jq -r "$1"; }
    ID=$(_jq '.id')
    CREATED=$(_jq '.created_at')
    TAGS=$(_jq '.metadata.container.tags | join(", ")')
    echo "    [$ID] $CREATED | tags: $TAGS"
  done

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "  [DRY-RUN] No se eliminó nada. Pasa DRY_RUN=false para borrar."
    continue
  fi

  read -rp "  Eliminar estas $DELETE_COUNT versiones? (si/no): " CONFIRM
  if [[ "$CONFIRM" != "si" ]]; then
    echo "  Omitido."
    continue
  fi

  for ENTRY in $TO_DELETE; do
    _jq() { echo "$ENTRY" | base64 -d | jq -r "$1"; }
    ID=$(_jq '.id')
    CREATED=$(_jq '.created_at')
    gh api --method DELETE "$API_URL/$ID" --silent
    echo "  Eliminado [$ID] $CREATED"
  done
done

echo ""
echo "=== Proceso completado ==="