#pragma once



#include <QObject>
#include <QNetworkAccessManager>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QUrl>
#include <QTimer>



class RemoteAppGrabber : public QObject
{
    Q_OBJECT
public:
    RemoteAppGrabber(QObject *parent = 0);

    void checkAppsList();
     
signals:
    void gotAppList(const QList<QString> &);


public slots:
    void replyFinished (QNetworkReply *reply);
    void periodicCheck();

private:
    QNetworkAccessManager *manager_;
    QTimer timer_;
};


