#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../../../../.." && pwd)"
arch="$1"
sdk="${AURORA_SDK_DIR:-$HOME/AuroraOS/sdk/5.2.1.200}"
case "$arch" in aarch64) qmake=qmake-aarch64;; x86_64) qmake=qmake-x86_64;; *) exit 2;; esac
mkdir -p "$root/composeApp/src/auroraMain/native/tasks/build/$arch"
cd "$root/composeApp/src/auroraMain/native/tasks/build/$arch"
export WORKSPACE_DIR="$root"
export VAR_SPECFILE="$root/composeApp/src/auroraMain/native/tasks/tasks.spec"
"$sdk/tools/$qmake" ../../tasks.pro
