#include <QtQuick/QQuickPaintedItem>
#include <QtQml>
#include <QObject>
#include <QImage>


#include <QtQuick/QQuickItem>


#define USE_QUICK
//
//class ThreadHandler : public QObject
//{
//    Q_OBJECT
//public:
//    ThreadHandler(ImageStreamHandler *p) : p(p) { }
//
//    Q_SLOT void doWork() 
//    {
//
//    }
//
//
//
//private:
//    ImageStreamHandler *p;
//};
//



//
//class FboInSGRenderer : public QQuickFramebufferObject {
//    Q_OBJECT
//public:
//    Renderer *createRenderer() const;
//};
//


class ImageStreamHandler;

class ThreadHandler : public QThread
{
    Q_OBJECT
public:
    ThreadHandler(ImageStreamHandler *p) : p(p) { }
    void run();

signals:
    void ImagePosted();



private:
    ImageStreamHandler *p;
};


#ifdef USE_QUICK
class ImageStreamHandler : public QQuickItem
#else
class ImageStreamHandler : public QQuickPaintedItem
#endif
{
    Q_OBJECT
    //Q_PROPERTY(QString name READ name WRITE setName)
    //Q_PROPERTY(QColor color READ color WRITE setColor)
    //QML_ELEMENT

    //ThreadHandler worker;
    //QThread workerThread;
    ThreadHandler workerThread;
    QMutex mutex;

public:
    ImageStreamHandler(QQuickItem *parent = 0);
    ~ImageStreamHandler();


    //QString name() const;
    //void setName(const QString &name);

    Q_INVOKABLE void add_frame(const QString &data);

    Q_INVOKABLE void clear();


    //QColor color() const;
    //void setColor(const QColor &color);

#ifndef USE_QUICK
    void paint(QPainter *painter);
#else
    QSGNode *updatePaintNode(QSGNode *, UpdatePaintNodeData *);
#endif

public slots:
    void OnImagePosted()
    {
        update();
    }

private:
    friend class ThreadHandler;
    //QString m_name;
    size_t avail_ = 0;

    // which buffer is loading
    //bool buf1_ = false;
    //bool loading_ = false;

    QImage buf1_, buf2_;

    QImage *image_ = nullptr;
	QString last_frame_;
};



// on new data - 
//      set 
