#include <iostream>
#include "ImageStreamHandler.hpp"

#include <QPainter>
#include <QSGSimpleTextureNode>
#include <QQuickWindow>


//#define NO_THREAD



void ThreadHandler::run()
{
    size_t last_avail = p->avail_;
    bool use_first = true;
    while (true)
    {
        p->mutex.lock();
        size_t cur_avail = p->avail_;
        p->mutex.unlock();
        if (cur_avail != last_avail)
        {
            p->mutex.lock();
            last_avail = cur_avail;
            // grab a copy of data
            QString data;
            {
                data = p->last_frame_;
                p->last_frame_ = QString();
            }

            p->mutex.unlock();

            // process / load             
            QByteArray by = QByteArray::fromBase64(data.toUtf8().remove(0, 23)); //QByteArray(data.data(), data.size())); //base64img.toLatin1());
            //QImage *img = use_first ? &p->buf1_ : &p->buf2_;
            //QImage *img = &p->buf1_;
            //: &p->buf2_;

            QImage *img = new QImage();
            bool res = img->loadFromData(by, "JPEG"); //data.data(), data.size(), "JPEG");



            if (res)
            {
                p->mutex.lock();
                use_first = !use_first;
                QImage *vim = p->image_;
                p->image_ = img;
                if (vim)
                    delete vim;
                //p->image_;
                emit ImagePosted();
                p->mutex.unlock();
                std::cout << "Loaded and flipped" << std::endl;
            }
            else
            {
                std::cout << "Failed to load image" << std::endl;
            }
        }
        else
        {
            //std::cout << "Matched: " << cur_avail << " / " << last_avail << std::endl;
        }
    }
}





ImageStreamHandler::ImageStreamHandler(QQuickItem *parent)
    :
#ifdef USE_QUICK
        QQuickItem(parent), 
#else
        QQuickPaintedItem(parent), 
#endif
    workerThread(this)
{
#ifdef USE_QUICK
    setFlag(QQuickItem::ItemHasContents);
#endif

#ifndef NO_THREAD
    QObject::connect(&workerThread, SIGNAL(ImagePosted()), this, SLOT(OnImagePosted()));
    workerThread.start();
#endif
    //worker.moveToThread(&workerThread);
    //workerThread.start();

}

ImageStreamHandler::~ImageStreamHandler()
{
#ifndef NO_THREAD
    workerThread.quit();
    workerThread.wait();
#endif
}

void ImageStreamHandler::clear()
{
    //mutex.lock();
    //last_frame_ = QString();
    //if (image_)
    //{
    //    delete image_;
    //    image_ = nullptr;
    //    
    //}
    ////std::cout << "Got frame: " << avail_ << std::endl;
    //mutex.unlock();
    //update();
}

void ImageStreamHandler::add_frame(const QString &data)
{
#ifndef NO_THREAD
    mutex.lock();
    last_frame_ = data;
    ++avail_;
    std::cout << "Got frame: " << avail_ << std::endl;
    mutex.unlock();
#else
    QByteArray by = QByteArray::fromBase64(data.toUtf8().remove(0, 23)); //QByteArray(data.data(), data.size())); //base64img.toLatin1());
    bool res = buf1_.loadFromData(by, "JPEG"); //data.data(), data.size(), "JPEG");
    if (res)
    {
        image_ = &buf1_;
        update();

    }
#endif

    //QByteArray by = QByteArray::fromBase64(data.toUtf8().remove(0, 23)); //QByteArray(data.data(), data.size())); //base64img.toLatin1());
    //bool res = image_.loadFromData(by, "JPEG"); //data.data(), data.size(), "JPEG");

    //QThread *thread = QThread::create([]{ runSlowCode(); });

    //"data:image/jpeg;base64,"
    //std::cout << "Loaded image: " << res << std::endl;
    //update();
}




#ifndef USE_QUICK

void ImageStreamHandler::paint(QPainter *painter)
{
    if (image_)
    {
        mutex.lock();
        
        painter->drawImage(0, 0, *image_);

        mutex.unlock();
    }
//
//    painter->setPen(pen);
//    painter->setRenderHints(QPainter::Antialiasing, true);
//    painter->drawPie(boundingRect().adjusted(1, 1, -1, -1), 90 * 16, 290 * 16);
}

#else

QSGNode *ImageStreamHandler::updatePaintNode(QSGNode *oldNode, UpdatePaintNodeData *UpdatePaintNodeData)
{

    auto node = oldNode; //dynamic_cast<QSGSimpleTextureNode *>(oldNode);

    if (!oldNode && image_) {
        node = new QSGSimpleTextureNode();
    }

    if (image_)
    {
        //std::cout << "RENDERING" << std::endl;
        mutex.lock();
        QSGSimpleTextureNode *n = static_cast<QSGSimpleTextureNode*>(node);
        QSGTexture *texture = window()->createTextureFromImage(*image_, QQuickWindow::TextureIsOpaque);
        n->setOwnsTexture(true);
        n->setRect(boundingRect());
        n->markDirty(QSGNode::DirtyForceUpdate);
        n->setTexture(texture);
        mutex.unlock();
    }

//    if (node && !image_)
//    {
//        delete node; 
//        node = nullptr;
//    }

    return node;
}




#endif






