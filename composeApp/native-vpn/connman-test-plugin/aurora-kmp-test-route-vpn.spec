Name:           com.example.aurorakmpdemo.test-route-vpn
Version:        0.1.5
Release:        1
Summary:        Aurora ConnMan split-tunnel verification provider
License:        MIT
BuildArch:      %{_arch}
Source0:        %{name}-%{version}.tar.bz2

# ConnMan динамически находит protocol driver; это не публичная shared library
# и она не должна становиться RPM Provides capability.
%global _privatelibs aurora-test-route-proto-plugin
%global __provides_exclude ^(lib%{_privatelibs})[.]so.*$
%global __os_install_post %{nil}

%{!?qtc_qmake5:%define qtc_qmake5 %qmake5}
%{!?qtc_make:%define qtc_make make}

BuildRequires:  connman-devel
BuildRequires:  pkgconfig(glib-2.0)
BuildRequires:  pkgconfig(dbus-1)

%description
Native ConnMan test VPN driver. It routes only 198.51.100.0/24 into a local
TUN sink; DNS and the default route remain unchanged.

%prep
%setup -q

%build
%qtc_qmake5
%qtc_make %{?_smp_mflags}

%install
rm -rf %{buildroot}
%qmake5_install
# По соглашению ConnMan VPN driver называется без qmake-префикса lib.
cp %{buildroot}/usr/lib/connman/plugins-vpn/libaurora-test-route-proto-plugin.so \
   %{buildroot}/usr/lib/connman/plugins-vpn/aurora-test-route-proto-plugin.so
rm -f %{buildroot}/usr/lib/connman/plugins-vpn/libaurora-test-route-proto-plugin.so

%files
%attr(644, root, root) /usr/lib/connman/plugins-vpn/aurora-test-route-proto-plugin.so
%attr(755, root, root) /usr/bin/com.example.aurorakmpdemo.test-route-vpn
%attr(644, root, root) /etc/connman/vpn-plugin/aurora-test-route.conf
