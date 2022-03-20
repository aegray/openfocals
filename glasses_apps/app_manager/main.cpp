#define QT_QML_DEBUG



#include <iostream>

#include <QQmlDebuggingEnabler>


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

#include "AppLoader.hpp"
#include "RemoteAppGrabber.hpp"
#include "ImageStreamHandler.hpp"
//#include <QPlatformHeaderHelper>


typedef QScreen* (*connect_to_gallery_fun_t)(const QString &name);
    
QScreen *connectToGallery(const QString &name)
{
    return QPlatformHeaderHelper::callPlatformFunction<
        QScreen*, 
        connect_to_gallery_fun_t
    >(
        QByteArrayLiteral("connectToGallery"),
        name
    );
}

namespace {
constexpr auto lensGalleryName = "thalmic.live_lens_gallery";
} 



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
    thalmic::BlackCoralApplication app(argc, argv, "");
    //qmlRegisterType<Launcher>("LauncherApp", 1, 0, "Launcher");
    qInstallMessageHandler(msgOutput);
    app.setQuitOnLastWindowClosed(false);
    
    qmlRegisterType<ImageStreamHandler>("ImageStreamHandler", 1, 0, "ImageStreamHandler");
    //qmlRegisterType<FboInSGRenderer>("ImageStreamHandler", 1, 0, "ImageStreamHandler");
    
    //QSslConfiguration sslconfig;                                                                        
    //sslconfig.setCaCertificates(QSslCertificate::fromPath("/system/etc/ssl/certs/cacert.pem"));
    //QSslConfiguration::setDefaultConfiguration(sslconfig);

    
    //something about thread pool and futures interface...
    //Q_INIT_RESOURCE(AtestAssets);
    //Q_INIT_RESOURCE(Atest);

    
    // Grab a handle to the main gallery 
    auto screen = connectToGallery(lensGalleryName); //"thalmic.live_lens_gallery");

   
    AppLoader apps(screen);
    RemoteAppGrabber remote;

    QObject::connect(&remote, SIGNAL(gotAppList(const QList<QString> &)), 
                        &apps, SLOT(handleAppsList(const QList<QString> &)));


    remote.checkAppsList();
//    apps.addLens("bla", 
 //       ("http://app.ofocals.com/ofocals/apps/impl?name=2048")); //192.168.1.6:8000/test.qml")); //#qrc:/atest/AtestLensView.qml"));

///
///
/////    RemoteAppGrabber remote;
/////
/////    QObject::connect(&remote, SIGNAL(gotAppList(const QList<QString> &)), 
/////                        &apps, SLOT(handleAppsList(const QList<QString> &)));
///
/////    //std::cout << "done create" << std::endl;
///    thalmic::GalleryView2 gv("bla"); //v1); //QString("bla")); // gv("bla"); //lensName);
/////    //auto gv = new thalmic::GalleryView2(QString("bla")); // gv("bla"); //lensName);
/////    //auto v2 = (QUrl("qrc:/atest/AtestLensView.qml"));
/////    //auto v2 = (QUrl("http://192.168.1.6:8000/test.qml")); //#qrc:/atest/AtestLensView.qml"));
/////    //auto v2 = (QUrl("http://app.ofocals.com/ofocals/apps/2048")); //#impl?name=2048")); //192.168.1.6:8000/test.qml")); //#qrc:/atest/AtestLensView.qml"));
///    auto v2 = (QUrl("http://app.ofocals.com/ofocals/apps/impl?name=2048")); //192.168.1.6:8000/test.qml")); //#qrc:/atest/AtestLensView.qml"));
///    //auto v2 = (QUrl("file:/data/app/game_2048.qml")); //https://cloud.ofocals.com/ofocals/apps/impl?name=2048")); //192.168.1.6:8000/test.qml")); //#qrc:/atest/AtestLensView.qml"));
///
///    //std::cout << "loading resources" << std::endl;
///    // set rootContext
///    //      -> rootViewContext
///    gv.setSource(v2); //QUrl("qrc:/atest/AtestLensView.qml"));
///    //std::cout << "done loading resources" << std::endl;
///
///    //gv.show();
///    std::cout << "got gallery" << std::endl;
///
///
///    gv.setScreen(screen);
///    //std::cout << "set screen" << std::endl;
///
///
///
/////    QQmlApplicationEngine engine;
/////    engine.load(QUrl("qrc:/atest/AtestLensView.qml"));
/////    std::cout << "loaded" << std::endl;
///
///    gv.show();
///
///
/////    //engine.load(QUrl("qrc:/atest/simple.qml"));
/////    QObject *topLevel = engine.rootObjects().value(0);
/////    QQuickWindow *window = qobject_cast<QQuickWindow *>(topLevel);
/////    window->setScreen(connectToGallery("thalmic.live_lens_gallery"));
/////    window->show();
///
/////    throw std::runtime_error("done");
///
    std::cout << "exec" << std::endl;
    return app.exec();
}

