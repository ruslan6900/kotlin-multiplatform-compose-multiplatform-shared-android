Name:           com.example.aurorakmpdemo
Version:        0.0.3
Release:        1
Summary:        Aurora KMP demo with the split-tunnel ConnMan verification plugin
License:        BSD-3-Clause
BuildArch:      aarch64
Source0:        %{name}-%{version}.tar.bz2

# This is deliberately a system-oriented spec: it owns the ConnMan plugin
# path and its provider. A production RPM must be built with an Aurora
# system/extended signing profile and pass the corresponding validator.
%global __provides_exclude_from ^%{_datadir}/%{name}/lib/.*$
%global __requires_exclude ^(libmaliit-glib.*)$

%description
Development-only combined KMP application and ConnMan split-tunnel VPN plugin.

%prep
%setup -q

%build

%install
rm -rf %{buildroot}

install -D -m 0755 app/com.example.aurorakmpdemo %{buildroot}%{_bindir}/%{name}
install -D -m 0755 vpn/com.example.aurorakmpdemo.test-route-vpn %{buildroot}%{_bindir}/com.example.aurorakmpdemo.test-route-vpn
install -D -m 0644 vpn/aurora-test-route-proto-plugin.so %{buildroot}/usr/lib/connman/plugins-vpn/aurora-test-route-proto-plugin.so
install -D -m 0644 vpn/aurora-test-route.conf %{buildroot}/etc/connman/vpn-plugin/aurora-test-route.conf
install -D -m 0755 bridge/com.example.aurorakmpdemo-vpn-bridge %{buildroot}/usr/libexec/com.example.aurorakmpdemo-vpn-bridge
install -D -m 0644 bridge/com.example.aurorakmpdemo-vpn-bridge.service %{buildroot}%{_unitdir}/com.example.aurorakmpdemo-vpn-bridge.service
install -D -m 0644 bridge/com.example.aurorakmpdemo.VpnBridge.conf %{buildroot}/etc/dbus-1/system.d/com.example.aurorakmpdemo.VpnBridge.conf

mkdir -p %{buildroot}%{_datadir}/applications %{buildroot}%{_datadir}/icons %{buildroot}%{_datadir}/%{name}
cp -a app/applications/. %{buildroot}%{_datadir}/applications/
# The KMP packaging task creates the desktop entry without VPN because this
# custom system RPM, not the ordinary application RPM, owns the ConnMan
# integration.  Sailjail grants system-bus access from this declaration.
sed -i 's/^Permissions=.*/Permissions=Internet;UserDirs;VPN/' %{buildroot}%{_datadir}/applications/%{name}.desktop
cp -a app/icons/. %{buildroot}%{_datadir}/icons/
cp -a app/resources %{buildroot}%{_datadir}/%{name}/
mkdir -p %{buildroot}%{_datadir}/%{name}/lib
cp -a app/lib/. %{buildroot}%{_datadir}/%{name}/lib/

%files
%defattr(-,root,root,-)
%attr(0755,root,root) %{_bindir}/%{name}
%attr(0755,root,root) %{_bindir}/com.example.aurorakmpdemo.test-route-vpn
%attr(0644,root,root) /usr/lib/connman/plugins-vpn/aurora-test-route-proto-plugin.so
%attr(0644,root,root) /etc/connman/vpn-plugin/aurora-test-route.conf
%attr(0755,root,root) /usr/libexec/com.example.aurorakmpdemo-vpn-bridge
%attr(0644,root,root) %{_unitdir}/com.example.aurorakmpdemo-vpn-bridge.service
%attr(0644,root,root) /etc/dbus-1/system.d/com.example.aurorakmpdemo.VpnBridge.conf
%{_datadir}/applications/%{name}.desktop
%{_datadir}/icons/hicolor/*/apps/%{name}.png
%{_datadir}/%{name}/resources/*
%{_datadir}/%{name}/lib/*
