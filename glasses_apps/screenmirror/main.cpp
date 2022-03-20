#define QT_QML_DEBUG



//#include "AtestLensArtist.hpp"


#include <iostream>

#include <QQmlDebuggingEnabler>


//int main(int argc, char * argv[])
//{
//    QGuiApplication app(argc, argv);
//    initializeStyle();
//
//    auto atestLensArtist = new AtestLensArtist(lensName);
//
//    // this says its a subclass of QWindow
//    atestLensArtist->setScreen(QKonaFunctions::connectToGallery(lensGalleryName));
//
//
//
//     
//
//   
////qt::unique_qptr<AtestLensArtist> provideAtestLensArtist()
////{
////    auto atestLensArtist = qt::make_qunique<AtestLensArtist>(lens_name::atest);
////
////    atestLensArtist->setScreen(QKonaFunctions::connectToGallery(lensGalleryName));
////
////    return atestLensArtist;
////}
//}
//
//


#include <QtGui/QGuiApplication>
#include <QtCore/QByteArray>
#include <QtPlatformHeaders/QPlatformHeaderHelper>


//#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQuickWindow>
#include <QtQuickControls2/QQuickStyle>
#include <QQmlContext>

#include <QScreen>
#include "BlackCoralApplication.hpp"
#include "GalleryView2.hpp"
#include "RootViewModel.hpp"
#include "Launcher.h"

#include "ImageStreamHandler.hpp"
#include "Wakelock.hpp"
//#include <QPlatformHeaderHelper>


typedef QScreen* (*connect_to_gallery_fun_t)(const QString &name);
    
QScreen *connectToGallery(const QString &name)
{
    //auto func = reinterpret_cast<connect_to_gallery_fun_t>(QGuiApplication::platformFunction(QByteArrayLiteral("connectToGallery")));
    //Q_ASSERT(func);
    //return func(name);

    return QPlatformHeaderHelper::callPlatformFunction<
        QScreen*, 
        connect_to_gallery_fun_t
    >(
        QByteArrayLiteral("connectToGallery"),
        name
    );
    //        bottomLeftClippedByNSWindowOffsetIdentifier(),window);
    //return QPlatformHeaderHelper::callPlatformFunction<QScreen*, connect_to_gallery_fun_t>( name.toUtf8());
}


namespace thalmic {
    extern void initializeBlackCoralStyle();
    //extern void QmlDirection::registerQml()
}


//thalmic::AssetManager::initialize();
//thalmic::QuickAttachedObject::initialize()  
//thalmic::initializeBlackCoralStyle()    
//
extern void registerResources();
//initializeBlackCoralStyle();
extern void qInitResources_fonts();
extern void qInitResources_views();
extern void qInitResources_styles();
extern void qInitResources_components();
extern void qInitResources_typography();
extern void qInitResources_style_assets();
extern void qInitResources_BlackCoralStyle();
extern void qInitResources_style_asset_aliases();
extern void qInitResources_fonts_qtquickcompiler();
extern void qInitResources_style_assets_qtquickcompiler();
extern void qInitResources_BlackCoralStyle_qtquickcompiler();
extern void qInitResources_style_asset_aliases_qtquickcompiler();


void initializeStyle()
{

//    registerResources();
//    thalmic::initializeBlackCoralStyle();
//
//
//
//    qInitResources_BlackCoralStyle();
//    qInitResources_style_asset_aliases();
//    qInitResources_fonts_qtquickcompiler();
//    qInitResources_style_assets_qtquickcompiler();
//    qInitResources_BlackCoralStyle_qtquickcompiler();
//    qInitResources_style_asset_aliases_qtquickcompiler();
//
//    qInitResources_fonts();
//    qInitResources_views();
//    qInitResources_styles();
//    qInitResources_components();
//    qInitResources_typography();
//    qInitResources_style_assets();
//

    Q_INIT_RESOURCE(AtestAssets);
    Q_INIT_RESOURCE(Atest);
}

namespace {
constexpr auto lensGalleryName = "thalmic.live_lens_gallery";
constexpr auto lensName  = "thalmic.screenmirror.lens";
} // namespace
//

//namespace QKonaFunctions {
//extern QScreen *connectToGallery(const QString &name);
//}

extern QScreen *connectToGallery(const QString &name);


void msgOutput(QtMsgType type, const QMessageLogContext &context, const QString &msg)
{
    QByteArray localMsg = msg.toLocal8Bit();
    const char *file = context.file ? context.file : "";
    const char *fun = context.function ? context.function : "";

    switch(type) {
        case QtDebugMsg:
            fprintf(stderr, "Debug: %s (%s:%u, %s)\n", localMsg.constData(), file, context.line, fun);
            break;
        case QtInfoMsg:
            fprintf(stderr, "Info: %s (%s:%u, %s)\n", localMsg.constData(), file, context.line, fun);
            break;
        case QtWarningMsg:
            fprintf(stderr, "Warning: %s (%s:%u, %s)\n", localMsg.constData(), file, context.line, fun);
            break;
        case QtCriticalMsg:
            fprintf(stderr, "Critical: %s (%s:%u, %s)\n", localMsg.constData(), file, context.line, fun);
            break;
        case QtFatalMsg:
            fprintf(stderr, "Critical: %s (%s:%u, %s)\n", localMsg.constData(), file, context.line, fun);
            break;
    }
}

int main(int argc, char *argv[]) 
{

    
    

    //QString name("");
    //QGuiApplication app(argc, argv); //thalmic::BlackCoralApplication app(argc, argv, "");
    thalmic::BlackCoralApplication app(argc, argv, "");
    qmlRegisterType<Launcher>("LauncherApp", 1, 0, "Launcher");
    qmlRegisterType<ImageStreamHandler>("ImageStreamHandler", 1, 0, "ImageStreamHandler");
    qmlRegisterType<Wakelock>("Wakelock", 1, 0, "Wakelock");
    qInstallMessageHandler(msgOutput);
    app.setQuitOnLastWindowClosed(false);
    
    //QQmlDebuggingEnabler enabler;

    //something about thread pool and futures interface...
    initializeStyle();

    // not sure where this comes from
    //QQuickStyle::setStyle(":/DemoStyle");


//
//    QQuickView view;
//    view.setSource(QUrl("qrc:/atest/AtestLensView.qml")); //::fromLocalFile("application.qml"));
//    view.show();

//
//    QQmlApplicationEngine engine;
//    engine.load(QUrl("qrc:/atest/AtestLensView.qml"));
    


//
//
//    //QGuiApplication app(argc, argv);
//    QQmlApplicationEngine engine;
//    //QList<QQmlError> errs;
//    //engine.importPlugin("black_coral_style", "/system/lib/libblack_coral_style.so", &errs);
//    //std::cout << "Errs: " << errs.length() << std::endl;
//    engine.load(QUrl("qrc:/atest/AtestLensView.qml"));


    //thalmic::GalleryView gv(lensName);
    std::cout << "Going to create" << std::endl;
    QString v1 = "screenmirror";
    thalmic::GalleryView2 gv(v1); //QString("bla")); // gv("bla"); //lensName);

    //thalmic::RootViewModel *vm = new thalmic::RootViewModel();
   // gv.rootContext()->setContextProperty("rootViewModel", (QObject*)vm);

    std::cout << "done create" << std::endl;
    //auto gv = new GalleryView(QString("bla")); // gv("bla"); //lensName);
    auto v2 = (QUrl("qrc:/atest/AtestLensView.qml"));
    //auto v2 = (QUrl("http://192.168.1.6:8000/test.qml")); //#qrc:/atest/AtestLensView.qml"));

    std::cout << "loading resources" << std::endl;
    // set rootContext
    //      -> rootViewContext
    gv.setSource(v2); //QUrl("qrc:/atest/AtestLensView.qml"));
    std::cout << "done loading resources" << std::endl;

    //gv.show();

    auto screen = connectToGallery("thalmic.live_lens_gallery");
    std::cout << "got gallery" << std::endl;


    gv.setScreen(screen);
    std::cout << "set screen" << std::endl;



//    QQmlApplicationEngine engine;
//    engine.load(QUrl("qrc:/atest/AtestLensView.qml"));
//    std::cout << "loaded" << std::endl;

    gv.show();


//    //engine.load(QUrl("qrc:/atest/simple.qml"));
//    QObject *topLevel = engine.rootObjects().value(0);
//    QQuickWindow *window = qobject_cast<QQuickWindow *>(topLevel);
//    window->setScreen(connectToGallery("thalmic.live_lens_gallery"));
//    window->show();

//    throw std::runtime_error("done");

    std::cout << "exec" << std::endl;
    return app.exec();
    return 0;
}


//
//int main(int argc, char * argv[])
//{
//    QString name("");
//    BlackCoralApplication app(argc, argv, &name);
//    initializeStyle();
//
//    auto atestLensArtist = new AtestLensArtist(lensName);
//
//    // this says its a subclass of QWindow
//    atestLensArtist->setScreen(connectToGallery(lensGalleryName));
//    
//    return app.exec();
//}
//
