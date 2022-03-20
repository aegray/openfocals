#include <QJsonDocument>
#include <QJsonArray>
#include <QJsonObject>

#include <iostream>

#include "RemoteAppGrabber.hpp"

RemoteAppGrabber::RemoteAppGrabber(QObject *parent) 
    : QObject(parent)
{
    manager_ = new QNetworkAccessManager(this);

    connect(manager_, SIGNAL(finished(QNetworkReply*)),
            this, SLOT(replyFinished(QNetworkReply*)));
    
    connect(&timer_, SIGNAL(timeout()), this, SLOT(periodicCheck()));
    timer_.start(30*1000);
}
   

void RemoteAppGrabber::periodicCheck()
{
    checkAppsList();
}


void RemoteAppGrabber::checkAppsList() 
{
    QSslConfiguration sslconfig;                                                                        
    sslconfig.setCaCertificates(QSslCertificate::fromPath("/system/etc/ssl/certs/cacert.pem"));
    QNetworkRequest req = QNetworkRequest(
            QUrl(QString("https://cloud.ofocals.com/ofocals/apps/list")));
    req.setSslConfiguration(sslconfig);
    manager_->get(req); 
}



void RemoteAppGrabber::replyFinished (QNetworkReply *reply)
{
    std::cout << "Got reply" << std::endl;
    if(reply->error())
    {
        qDebug() << "ERROR!";
        qDebug() << reply->errorString();
    }
    else
    {
        if (reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt() == 200)
        {
            auto data = reply->readAll();
            qDebug() << "Got data: " << data;

            QJsonDocument loadDoc(QJsonDocument::fromJson(data));

            if (loadDoc.isArray())
            {
                QJsonArray arr = loadDoc.array();

                QList<QString> apps;


                for (int i = 0; i < arr.size(); ++i) 
                {
                    QJsonObject o = arr[i].toObject();
                    QString name = o["name"].toString();
                    //QString description = o["description"].toString();
                    //QString url = "https://cloud.ofocals.com/ofocals/apps/impl?name=" + name;

                    apps.append(name);

                    std::cout << "Got app: " << name.toStdString() << std::endl;
                }

                emit(gotAppList(apps));
            }
        }
        else
        {
            std::cout << "Got invalid response" << std::endl;    
        }
        //qDebug() << reply->header(QNetworkRequest::ContentTypeHeader).toString();
        //qDebug() << reply->header(QNetworkRequest::LastModifiedHeader).toDateTime().toString();
        //qDebug() << reply->header(QNetworkRequest::ContentLengthHeader).toULongLong();
        //qDebug() << reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        //qDebug() << reply->attribute(QNetworkRequest::HttpReasonPhraseAttribute).toString();

        //qDebug() << reply->readAll();
        //QFile *file = new QFile("C:/Qt/Dummy/downloaded.txt");
        //if(file->open(QFile::Append))
        //{
        //    file->write(reply->readAll());
        //    file->flush();
        //    file->close();
        //}
        //delete file;
    }

    reply->deleteLater();
    //QCoreApplication::quit();
}








