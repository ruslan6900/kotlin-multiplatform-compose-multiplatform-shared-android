Name: aurora-demo-tasks
Version: 0.1
Release: 1
Summary: RuntimeManager demo helper
License: BSD-3-Clause
%{!?qtc_qmake5:%define qtc_qmake5 %qmake5}
%{!?qtc_make:%define qtc_make make}

%description
Helper used by Aurora KMP Demo.
%build
%qtc_qmake5
%qtc_make %{?_smp_mflags}
%install
%files
