#!/usr/bin/env bash

set -euo pipefail

REPO="Ricket/bedtimeban"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR=""

usage() {
  cat <<'EOF'
Usage:
  ./scripts/install-bedtimeban.sh pr <pr-number> [--target 1.20.1|1.21.1]
  ./scripts/install-bedtimeban.sh release <tag> [--target 1.20.1|1.21.1]

Installs Bedtime Ban jars from GitHub into the local test server mod directories.
Without --target, both supported targets are installed.
EOF
}

cleanup() {
  if [[ -n "${TMP_DIR}" && -d "${TMP_DIR}" ]]; then
    rm -rf "${TMP_DIR}"
  fi
}

fail() {
  echo "Error: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

check_gh_auth() {
  gh auth status >/dev/null 2>&1 || fail "GitHub CLI is not authenticated. Run: gh auth login"
}

target_server_dir() {
  case "$1" in
    1.20.1) echo "${ROOT_DIR}/../server-1.20.1-forge/data/mods" ;;
    1.21.1) echo "${ROOT_DIR}/../server-1.21.1-neoforge/data/mods" ;;
    *) fail "Unsupported target: $1" ;;
  esac
}

ci_artifact_name() {
  case "$1" in
    1.20.1) echo "bedtimeban-ci-mc1.20.1-forge" ;;
    1.21.1) echo "bedtimeban-ci-mc1.21.1-neoforge" ;;
    *) fail "Unsupported target: $1" ;;
  esac
}

release_asset_name() {
  local target="$1"
  local tag="$2"
  local version="${tag#v}"

  case "${target}" in
    1.20.1) echo "bedtimeban-v${version}-mc1.20.1-forge.jar" ;;
    1.21.1) echo "bedtimeban-v${version}-mc1.21.1-neoforge.jar" ;;
    *) fail "Unsupported target: ${target}" ;;
  esac
}

install_jar() {
  local target="$1"
  local jar_path="$2"
  local mods_dir

  mods_dir="$(target_server_dir "${target}")"
  [[ -d "${mods_dir}" ]] || fail "Missing target mods directory: ${mods_dir}"
  [[ -f "${jar_path}" ]] || fail "Jar not found: ${jar_path}"

  find "${mods_dir}" -maxdepth 1 -type f -name 'bedtimeban*.jar' -delete
  cp "${jar_path}" "${mods_dir}/"

  echo "Installed ${target}: ${jar_path##*/} -> ${mods_dir}"
}

find_downloaded_pr_jar() {
  local target="$1"
  local artifact_dir="${TMP_DIR}/$(ci_artifact_name "${target}")"
  local jar_path

  [[ -d "${artifact_dir}" ]] || fail "Downloaded artifact directory not found: ${artifact_dir}"

  jar_path="$(find "${artifact_dir}" -maxdepth 1 -type f -name 'bedtimeban-*.jar' ! -name '*-sources.jar' | head -n 1)"
  [[ -n "${jar_path}" ]] || fail "No Bedtime Ban jar found in ${artifact_dir}"

  echo "${jar_path}"
}

download_pr_artifacts() {
  local pr_number="$1"
  local pr_json head_branch head_sha run_id

  pr_json="$(gh pr view "${pr_number}" --repo "${REPO}" --json headRefName,headRefOid)"
  head_branch="$(printf '%s\n' "${pr_json}" | jq -r '.headRefName')"
  head_sha="$(printf '%s\n' "${pr_json}" | jq -r '.headRefOid')"

  [[ -n "${head_branch}" && "${head_branch}" != "null" ]] || fail "Could not resolve branch for PR #${pr_number}"
  [[ -n "${head_sha}" && "${head_sha}" != "null" ]] || fail "Could not resolve head SHA for PR #${pr_number}"

  run_id="$(gh run list \
    --repo "${REPO}" \
    --workflow "CI" \
    --branch "${head_branch}" \
    --event pull_request \
    --json databaseId,headSha,conclusion,status \
    --limit 20 \
    | jq -r --arg head_sha "${head_sha}" '
        map(select(.headSha == $head_sha and .status == "completed" and .conclusion == "success"))
        | first
        | .databaseId // empty
      ')"

  [[ -n "${run_id}" ]] || fail "No successful CI run found for PR #${pr_number} at ${head_sha}"

  gh run download "${run_id}" --repo "${REPO}" --dir "${TMP_DIR}" >/dev/null
}

download_release_assets() {
  local tag="$1"
  shift
  local patterns=()
  local target asset_name

  mkdir -p "${TMP_DIR}/release"

  for target in "$@"; do
    asset_name="$(release_asset_name "${target}" "${tag}")"
    patterns+=("--pattern" "${asset_name}")
  done

  gh release download "${tag}" --repo "${REPO}" --dir "${TMP_DIR}/release" "${patterns[@]}" >/dev/null
}

find_downloaded_release_jar() {
  local target="$1"
  local tag="$2"
  local jar_path="${TMP_DIR}/release/$(release_asset_name "${target}" "${tag}")"

  [[ -f "${jar_path}" ]] || fail "Downloaded release asset not found: ${jar_path}"
  echo "${jar_path}"
}

parse_args() {
  [[ $# -ge 2 ]] || {
    usage
    exit 1
  }

  MODE="$1"
  REF="$2"
  shift 2

  TARGETS=("1.20.1" "1.21.1")

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --target)
        [[ $# -ge 2 ]] || fail "Missing value for --target"
        case "$2" in
          1.20.1|1.21.1) TARGETS=("$2") ;;
          *) fail "Unsupported target: $2" ;;
        esac
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        fail "Unknown argument: $1"
        ;;
    esac
  done

  case "${MODE}" in
    pr)
      [[ "${REF}" =~ ^[0-9]+$ ]] || fail "PR number must be numeric"
      ;;
    release)
      [[ -n "${REF}" ]] || fail "Release tag is required"
      ;;
    *)
      fail "Unknown mode: ${MODE}"
      ;;
  esac
}

main() {
  local target jar_path

  require_command gh
  require_command jq
  check_gh_auth

  parse_args "$@"

  TMP_DIR="$(mktemp -d /tmp/bedtimeban-install.XXXXXX)"
  trap cleanup EXIT

  case "${MODE}" in
    pr)
      download_pr_artifacts "${REF}"
      for target in "${TARGETS[@]}"; do
        jar_path="$(find_downloaded_pr_jar "${target}")"
        install_jar "${target}" "${jar_path}"
      done
      ;;
    release)
      download_release_assets "${REF}" "${TARGETS[@]}"
      for target in "${TARGETS[@]}"; do
        jar_path="$(find_downloaded_release_jar "${target}" "${REF}")"
        install_jar "${target}" "${jar_path}"
      done
      ;;
  esac
}

main "$@"
