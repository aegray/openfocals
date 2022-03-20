TEMPLATE = app

QT += quick qml core gui 
#quickwidgets
CONFIG += quick qtquickcompiler qmltypes

QMAKE_CXXFLAGS += -std=c++0x  -fno-rtti 
#-g

QMAKE_LFLAGS += -lblack_coral_app -lblack_coral_style 
#-g


HEADERS += ImageStreamHandler.hpp GalleryView2.hpp GalleryView.hpp RemoteAppGrabber.hpp AppLoader.hpp

SOURCES += ImageStreamHandler.cpp main.cpp RemoteAppGrabber.cpp AppLoader.cpp GalleryView2.cpp 


#RESOURCES += \
#    Atest.qrc \
#    AtestAssets.qrc 


