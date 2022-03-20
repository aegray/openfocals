TEMPLATE = app

QT += quick qml multimedia
CONFIG += quick qtquickcompiler 

#QMAKE_CXXFLAGS += -std=c++0x  -fno-rtti 
QMAKE_CXXFLAGS += -std=c++0x  -fno-rtti  -I ../androidincludes/core/include -I ../androidincludes/native/include
QMAKE_LFLAGS += -lutils -lbinder -lblack_coral_app -lblack_coral_style 

#QMAKE_LFLAGS += -lblack_coral_app -lblack_coral_style 
#-g


HEADERS += Wakelock.hpp ImageStreamHandler.hpp GalleryView2.hpp Launcher.h

SOURCES += main.cpp Wakelock.cpp ImageStreamHandler.cpp Launcher.cpp GalleryView2.cpp

#AtestLensArtist.cpp

RESOURCES += \
    Atest.qrc \
    AtestAssets.qrc 


