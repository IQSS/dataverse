Name: libcpl
Summary: Core Provenance Library
Version: 3.0
Release: 1
License: BSD
Group: Applications/Internet
URL: https://github.com/ProvTools/prov-cpl
Source: https://github.com/ProvTools/prov-cpl/archive/v3.0-beta.tar.gz
Source1: https://github.com/nlohmann/json/releases/download/v3.0.1/json.hpp
Packager: Dataverse <support@dataverse.org>
BuildRoot: %{_tmppath}/%{name}-%{version}
BuildArch: x86_64

%description
Core Provenance Library for collecting data provenance with multiple language bindings.

%prep
%setup -n prov-cpl-%{version}-beta
cp ~/rpmbuild/SOURCES/json.hpp ~/rpmbuild/BUILD/prov-cpl-%{version}-beta/include

%build
source /opt/rh/devtoolset-7/enable
%{__make}

%install
#%{_libdir}" is /usr/lib64, for example
DESTDIR="%{buildroot}/%{_libdir}"
mkdir -p $DESTDIR
SRCDIR="%{buildroot}/../../BUILD/prov-cpl-%{version}-beta/test/standalone-test/build/debug"
cp $SRCDIR/libcpl.so $DESTDIR
cp $SRCDIR/libcpl.3.so $DESTDIR
cp $SRCDIR/libcpl-odbc.3.0.so $DESTDIR
cp $SRCDIR/libcpl-odbc.3.so $DESTDIR
cp $SRCDIR/libcpl-odbc.so $DESTDIR
cp $SRCDIR/libcpl.3.0.so $DESTDIR
cp $SRCDIR/libcpl.3.so $DESTDIR
cp $SRCDIR/libcpl.so $DESTDIR

%clean
%{__rm} -rf %{buildroot}

%files
%{_libdir}/libcpl-odbc.3.0.so
%{_libdir}/libcpl-odbc.3.so
%{_libdir}/libcpl-odbc.so
%{_libdir}/libcpl.3.0.so
%{_libdir}/libcpl.3.so
%{_libdir}/libcpl.so

%changelog
* Mon Feb 5 2018 Philip Durbin <philip_durbin@harvard.edu> 3.0-beta-1
- initial package
