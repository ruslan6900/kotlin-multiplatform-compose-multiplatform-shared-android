#!/usr/bin/env bash
set -euo pipefail
stage="$1"
arch="$2"
file="$stage/$arch/applications/com.example.aurorakmpdemo.desktop"
python3 - "$file" <<'PYCODE'
import sys
path = sys.argv[1]
with open(path) as f: text = f.read()
text = text.replace('[X-Application]\n', '[X-Application]\nExecDBus=/usr/bin/com.example.aurorakmpdemo --dialog-service\nExportDBusInterfaces=com.example.aurorakmpdemo.DialogAnswer\n', 1)
with open(path, 'w') as f: f.write(text)
PYCODE
cat >> "$file" <<'TASKS'

[X-Task DemoWorker]
Type=worker
Exec=/usr/bin/com.example.aurorakmpdemo.tasks

[X-Task DemoPeriodicWork]
Type=periodic
Exec=/usr/bin/com.example.aurorakmpdemo.tasks

[X-Task DemoPeriodicNotification]
Type=periodic
Exec=/usr/bin/com.example.aurorakmpdemo.tasks
TASKS
docker run --rm --user "$(id -u):$(id -g)" -v "$stage:/project" --entrypoint sh aurora-build-tools:5.2.1.200 -c 'cd "/project/$1" && rpmbuild -ba --build-in-place --target "$1" --define "_topdir /project/$1" com.example.aurorakmpdemo.spec' sh "$arch"
version="$3"
report="$(mktemp)"
trap 'rm -f "$report"' EXIT
docker run --rm -v "$stage/$arch/RPMS/$arch:/packages:ro" --entrypoint sh aurora-build-tools:5.2.1.200 -c 'rpm-validator -p regular "/packages/com.example.aurorakmpdemo-$1-1.$2.rpm"' sh "$version" "$arch" | tee "$report"
if grep -q '(ERROR)' "$report"; then
    echo 'RuntimeManager RPM failed validation' >&2
    exit 1
fi
