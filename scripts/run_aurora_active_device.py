#!/usr/bin/env python3
"""Deploy the demo to the target snapshotted by the Gradle invocation."""
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path

APP_ID = "com.example.aurorakmpdemo"

def main():
    host = os.environ["AURORA_TARGET_HOST"]
    user = os.environ["AURORA_TARGET_USER"]
    port = os.environ["AURORA_TARGET_PORT"]
    key = os.environ.get("AURORA_TARGET_KEY", "")
    name = os.environ["AURORA_TARGET_NAME"]
    if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9.:-]{0,252}", host) or ".." in host:
        raise ValueError("Invalid SSH host")
    if not re.fullmatch(r"[a-z_][a-z0-9_-]{0,31}", user) or not (1 <= int(port) <= 65535):
        raise ValueError("Invalid SSH user or port")
    if key and not Path(key).is_file():
        raise ValueError("SSH key not found")
    options = ["-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=yes"]
    if key:
        options += ["-i", key, "-o", "IdentitiesOnly=yes"]
    target = f"{user}@{host}"
    ssh = ["ssh", *options, "-p", port, target]
    print(f"Aurora target: {name} ({target}:{port})", flush=True)
    arch = subprocess.check_output([*ssh, "uname -m"], text=True).strip()
    if arch not in ("aarch64", "arm64"):
        raise RuntimeError(f"Demo build is aarch64, but selected device reports {arch}")
    packages = sorted(Path(sys.argv[1]).glob(f"{APP_ID}-[0-9]*.aarch64.rpm"))
    packages = [p for p in packages if re.fullmatch(r"[a-zA-Z0-9._+-]+\.rpm", p.name)]
    if len(packages) != 1:
        raise RuntimeError(f"Expected exactly one demo RPM, found {len(packages)} in {sys.argv[1]}")
    # Match the location used by Aurora's own deployment plugin.
    remote = f"/home/{user}/Downloads/{packages[0].name}"
    subprocess.run(["scp", *options, "-P", port, str(packages[0]), f"{target}:{remote}"], check=True)
    install = " ".join(["gdbus call --system --dest ru.omp.APM --object-path /ru/omp/APM", "--method ru.omp.APM.Install", shlex.quote(remote), "'{}'"])
    result = subprocess.run([*ssh, install], check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout, flush=True)
    if "error" in result.stdout.lower():
        raise RuntimeError("Aurora package manager reported an installation error")
    command = f"if runtime-manager-tool Control getRunningApplications | grep -Fq 'Instance Id:       {APP_ID}'; then runtime-manager-tool Control terminate {APP_ID} || exit 1; sleep 3; fi; exec runtime-manager-tool Control startDebug {APP_ID} --nosandbox --output-to-console"
    subprocess.run(["ssh", "-tt", *options, "-p", port, target, command], check=True)

if __name__ == "__main__":
    try:
        main()
    except (ValueError, RuntimeError, subprocess.SubprocessError, OSError) as exc:
        print(f"Aurora launch failed: {exc}", file=sys.stderr)
        sys.exit(1)
