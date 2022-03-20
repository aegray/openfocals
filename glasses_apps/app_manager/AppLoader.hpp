#pragma once

#include <vector>

#include <QTimer>
#include <QScreen>
#include <QString>

#include "GalleryView2.hpp"


class AppLoader : public QObject
{
    Q_OBJECT
public:
    AppLoader(QScreen *screen, QObject *parent = 0);

    void addLens(const QString &name, const QString &qml_url);

public slots:
    void handleAppsList(const QList<QString> &apps);

    void onTimer();


private:
    QString doload_;
    bool got_apps_ = false;
    QList<QString> apps_;
    QScreen *screen_;
    QTimer timer_;
    std::vector<thalmic::GalleryView2*> lenses_;
};
