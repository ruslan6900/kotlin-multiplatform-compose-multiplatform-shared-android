#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${WORKSPACE_DIR:-}" ]]; then
  echo "WORKSPACE_DIR must point to the KMP repository root" >&2
  exit 2
fi

sdk_dir="${AURORA_SDK_DIR:-$HOME/AuroraOS/sdk/5.2.0.180}"
package_name="com.example.aurorakmpdemo"
package_version="0.0.3"
system_rpm_dir="$WORKSPACE_DIR/composeApp/system-rpm"
normal_rpm_dir="$WORKSPACE_DIR/composeApp/build/rpm/debug/aarch64"
native_vpn_dir="$WORKSPACE_DIR/composeApp/native-vpn/connman-test-plugin/build/aarch64"
top_dir="$system_rpm_dir/.rpmbuild/aarch64"
source_dir="$top_dir/SOURCES/$package_name-$package_version"

# Завершаем работу до упаковки, а не создаём молча RPM без одного из трёх VPN
# слоёв: ConnMan plugin, процесса provider или D-Bus bridge.
for required in \
  "$normal_rpm_dir/$package_name" \
  "$normal_rpm_dir/applications/$package_name.desktop" \
  "$native_vpn_dir/plugin/libaurora-test-route-proto-plugin.so" \
  "$native_vpn_dir/provider/$package_name.test-route-vpn" \
  "$native_vpn_dir/bridge/com.example.aurorakmpdemo-vpn-bridge"; do
  [[ -f "$required" ]] || { echo "Required build artifact is absent: $required" >&2; exit 3; }
done

# Это воспроизводимый результат сборки. Каталог создаётся из исходников при
# каждом запуске и намеренно исключён из контроля версий.
rm -rf "$top_dir"
mkdir -p "$top_dir"/{BUILD,BUILDROOT,RPMS,SOURCES,SPECS,SRPMS} \
  "$source_dir/app" "$source_dir/vpn" "$source_dir/bridge"

cp "$normal_rpm_dir/$package_name" "$source_dir/app/"
cp -a "$normal_rpm_dir/applications" "$source_dir/app/"
cp -a "$normal_rpm_dir/icons" "$source_dir/app/"
cp -a "$normal_rpm_dir/resources" "$source_dir/app/"
cp -a "$normal_rpm_dir/lib" "$source_dir/app/"
cp "$native_vpn_dir/plugin/libaurora-test-route-proto-plugin.so" "$source_dir/vpn/aurora-test-route-proto-plugin.so"
cp "$native_vpn_dir/provider/$package_name.test-route-vpn" "$source_dir/vpn/$package_name.test-route-vpn"
cp "$WORKSPACE_DIR/composeApp/native-vpn/connman-test-plugin/provider/aurora-test-route.conf" "$source_dir/vpn/"
cp "$native_vpn_dir/bridge/com.example.aurorakmpdemo-vpn-bridge" "$source_dir/bridge/"
cp "$WORKSPACE_DIR/composeApp/native-vpn/connman-test-plugin/bridge/com.example.aurorakmpdemo-vpn-bridge.service" "$source_dir/bridge/"
cp "$WORKSPACE_DIR/composeApp/native-vpn/connman-test-plugin/bridge/com.example.aurorakmpdemo.VpnBridge.conf" "$source_dir/bridge/"

# rpmbuild получает один source archive, поэтому видит KMP-приложение и
# нативные VPN-файлы как единый payload пакета.
tar -C "$top_dir/SOURCES" -cjf "$top_dir/SOURCES/$package_name-$package_version.tar.bz2" "$package_name-$package_version"
cp "$system_rpm_dir/aurora-kmp-demo-system.spec" "$top_dir/SPECS/$package_name.spec"

"$sdk_dir/tools/qmake-aarch64" -query >/dev/null
# Здесь только создание пакета. Подпись выполняет отдельный доверенный release-
# процесс, который не должен получать ключ через этот репозиторий.
docker run -i --rm -h "$(hostname)" \
  $("$sdk_dir/tools/get-user-id-for-docker") \
  -v "$WORKSPACE_DIR":/workspace \
  -w /workspace \
  aurora-build-tools:5.2.0.180 \
  rpmbuild -bb --target aarch64-meego-linux-gnu \
    --define "_topdir /workspace/composeApp/system-rpm/.rpmbuild/aarch64" \
    /workspace/composeApp/system-rpm/.rpmbuild/aarch64/SPECS/$package_name.spec
