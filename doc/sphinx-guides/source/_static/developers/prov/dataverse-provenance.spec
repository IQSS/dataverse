Name: dataverse-provenance
Summary: Dataverse Provenance System
Version: 0.1
Release: 1
License: BSD
Group: Applications/Internet
URL: https://github.com/ProvTools/prov-cpl
Source: https://github.com/ProvTools/prov-cpl/archive/8150ee315abc21712b49da2bf4cfdbf308eef1d7.tar.gz
Source1: https://github.com/nlohmann/json/releases/download/v3.0.1/json.hpp
Packager: Dataverse <support@dataverse.org>
BuildRoot: %{_tmppath}/%{name}-%{version}
BuildArch: x86_64

%description
Core Provenance Library with a REST API for use with Dataverse for collecting data provenance.

%prep
%setup -n prov-cpl-8150ee315abc21712b49da2bf4cfdbf308eef1d7
cp ~/rpmbuild/SOURCES/json.hpp ~/rpmbuild/BUILD/prov-cpl-8150ee315abc21712b49da2bf4cfdbf308eef1d7/include

%build
source /opt/rh/devtoolset-7/enable
%{__make}
cd bindings/python
%{__make}

%install
#%{_libdir}" is /usr/lib64, for example
DESTROOT="%{buildroot}"
DESTDIR="%{buildroot}/%{_libdir}"
mkdir -p $DESTDIR
SRCROOT="%{buildroot}/../../BUILD/prov-cpl-8150ee315abc21712b49da2bf4cfdbf308eef1d7"
SRCDIR="%{buildroot}/../../BUILD/prov-cpl-8150ee315abc21712b49da2bf4cfdbf308eef1d7/test/standalone-test/build/debug"
cp $SRCDIR/libcpl.so $DESTDIR
cp $SRCDIR/libcpl.3.so $DESTDIR
cp $SRCDIR/libcpl-odbc.3.0.so $DESTDIR
cp $SRCDIR/libcpl-odbc.3.so $DESTDIR
cp $SRCDIR/libcpl-odbc.so $DESTDIR
cp $SRCDIR/libcpl.3.0.so $DESTDIR
cp $SRCDIR/libcpl.3.so $DESTDIR
cp $SRCDIR/libcpl.so $DESTDIR
INCLUDE_DEST="%{buildroot}/usr/local/include"
mkdir -p $INCLUDE_DEST/backends
PYTHON64_DEST="%{buildroot}/usr/lib64/python2.7/site-packages"
mkdir -p $PYTHON64_DEST
PYTHONLIB_DEST="%{buildroot}/usr/lib/python2.7/site-packages"
mkdir -p $PYTHONLIB_DEST
cp $SRCROOT/include/backends/cpl-odbc.h $INCLUDE_DEST/backends
cp $SRCROOT/include/cpl-db-backend.h $INCLUDE_DEST
cp $SRCROOT/include/cpl-exception.h $INCLUDE_DEST
cp $SRCROOT/include/cpl.h $INCLUDE_DEST
cp $SRCROOT/include/cplxx.h $INCLUDE_DEST
PYTHON_SRC="$SRCROOT/bindings/python/CPL/build/debug"
CPLDIRECT_SRC="$SRCROOT/bindings/python/CPLDirect/build/debug"
cp $PYTHON_SRC/_CPLDirect.so $PYTHON64_DEST
cp $PYTHON_SRC/CPL.py $PYTHONLIB_DEST
cp $CPLDIRECT_SRC/CPLDirect.py $PYTHON64_DEST
CPL_REST_DEST="%{buildroot}//usr/local/dataverse-provenance"
mkdir -p $CPL_REST_DEST
cp $SRCROOT/bindings/python/RestAPI/cpl-rest.py $CPL_REST_DEST

%clean
%{__rm} -rf %{buildroot}

%files
%{_libdir}/libcpl-odbc.3.0.so
%{_libdir}/libcpl-odbc.3.so
%{_libdir}/libcpl-odbc.so
%{_libdir}/libcpl.3.0.so
%{_libdir}/libcpl.3.so
%{_libdir}/libcpl.so
/usr/local/dataverse-provenance/cpl-rest.py
/usr/local/include/backends/cpl-odbc.h
/usr/local/include/cpl-db-backend.h
/usr/local/include/cpl-exception.h
/usr/local/include/cpl.h
/usr/local/include/cplxx.h
/usr/lib/python2.7/site-packages/CPL.py
/usr/lib64/python2.7/site-packages/CPLDirect.py
/usr/lib64/python2.7/site-packages/_CPLDirect.so
%exclude /usr/lib64/python2.7/site-packages/CPLDirect.pyc
%exclude /usr/lib64/python2.7/site-packages/CPLDirect.pyo
%exclude /usr/lib/python2.7/site-packages/CPL.pyc
%exclude /usr/lib/python2.7/site-packages/CPL.pyo
%exclude /usr/local/dataverse-provenance/cpl-rest.pyc
%exclude /usr/local/dataverse-provenance/cpl-rest.pyo

%changelog
* Wed Feb 7 2018 Philip Durbin <philip_durbin@harvard.edu> 0.1-1
- initial package
