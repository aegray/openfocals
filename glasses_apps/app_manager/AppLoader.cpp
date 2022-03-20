
#include <QCoreApplication>
#include <iostream>

#include "AppLoader.hpp"



AppLoader::AppLoader(QScreen *screen, QObject *parent) 
    : QObject(parent), screen_(screen)
{
    //connect(&timer_, SIGNAL(timeout()), this, SLOT(onTimer()));
//    timer_.setSingleShot(true);
    //timer_.start(10*1000);
}

void AppLoader::onTimer() 
{
    if (doload_.isEmpty()) {
        doload_ = "a";
    addLens("bla", 
        ("http://app.ofocals.com/ofocals/apps/impl?name=2048")); //192.168.1.6:8000/test.qml")); //#qrc:/atest/AtestLensView.qml"));
    }
}

void AppLoader::handleAppsList(const QList<QString> &apps)
{
    if (!got_apps_)
    {
//        onTimer();
        got_apps_ = true;
        //apps_ = apps;
        for (QString appname : apps) 
        {
            QString v2;
            v2.append(appname);
            addLens(v2, QString("http://app.ofocals.com/ofocals/apps/impl?name=") + v2);
            apps_.append(v2);
        }
    } 
    else 
    {
        // we already got the app list - check if anything changed, and if it did, then 
        // kill ourself and let android restart us to reload all the lenses
        bool anydiff = false;
        if (apps_.size() == apps.size())
        {
            for (int i = 0; i < apps.size(); ++i)
            {
                anydiff |= (apps_[i] != apps[i]);
            }
        }
        else 
        {
            anydiff = true;
        }

        if (anydiff)
        {
            std::cout << "Got change in apps list - quitting to reload" << std::endl;
            QCoreApplication::quit();
        }
    }
}


void AppLoader::addLens(const QString &iname, const QString &iqml_url)
{
    QString name;
    name.append(iname);

    QString qml_url;
    qml_url.append(iqml_url);

    std::cout << "Adding lens for: " << name.toStdString() << " Url=" << qml_url.toStdString() << std::endl;
    thalmic::GalleryView2 *gv = new thalmic::GalleryView2(QString("lens_") + name);
    std::cout << "Created gallery view" << std::endl;
    gv->setSource(QUrl(qml_url));
    std::cout << "Set source" << std::endl;
    gv->setScreen(screen_);
    std::cout << "Set screen" << std::endl;
    gv->show();
    std::cout << "Showed" << std::endl;
    lenses_.push_back(gv);
    std::cout << "Pushd to vector" << std::endl;
}

