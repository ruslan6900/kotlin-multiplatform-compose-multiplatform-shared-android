#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${WORKSPACE_DIR:-}" ]]; then
  echo "WORKSPACE_DIR must point to the KMP repository root" >&2
  exit 2
fi

sdk_dir="${AURORA_SDK_DIR:-$HOME/AuroraOS/sdk/5.2.0.180}"
project_dir="$(cd "$(dirname "$0")" && pwd)"
build_dir="$project_dir/build/aarch64"
spec_file="$project_dir/aurora-kmp-test-route-vpn.spec"

mkdir -p "$build_dir"
cd "$build_dir"
VAR_SPECFILE="$spec_file" "$sdk_dir/tools/qmake-aarch64" ../../connman-test-plugin.pro
# qmake's incremental timestamp handling inside the SDK container may retain
# stale C objects. Force the second make pass so every source edit is linked.
"$sdk_dir/tools/make" -B
