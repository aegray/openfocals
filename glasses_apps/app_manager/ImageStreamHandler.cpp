#include <iostream>
#include "ImageStreamHandler.hpp"

#include <QPainter>
#include <QSGSimpleTextureNode>
#include <QQuickWindow>

//#include <QQuickFramebufferObject>

//
//
//class LogoInFboRenderer : public QQuickFramebufferObject::Renderer {
//    public:
//        LogoInFboRenderer() { }
//
//        void render() {
//            int width = 1, height = 1;
//            glEnable(GL_BLEND);
//            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//            glColor4f(0.0, 1.0, 0.0, 0.8);
//            glBegin(GL_QUADS);
//            glVertex2f(0, 0);
//            glVertex2f(width, 0);
//            glVertex2f(width, height);
//            glVertex2f(0, height);
//            glEnd();
//
//            glLineWidth(2.5);
//            glColor4f(0.0, 0.0, 0.0, 1.0);
//            glBegin(GL_LINES);
//            glVertex2f(0, 0);
//            glVertex2f(width, height);
//            glVertex2f(width, 0);
//            glVertex2f(0, height);
//            glEnd();
//
//            update();
//        }
//
//        QOpenGLFramebufferObject *createFramebufferObject(const QSize &size) {
//            QOpenGLFramebufferObjectFormat format;
//            format.setAttachment(QOpenGLFramebufferObject::CombinedDepthStencil);
//            format.setSamples(4);
//            return new QOpenGLFramebufferObject(size, format);
//        }
//};
//
//QQuickFramebufferObject::Renderer *FboInSGRenderer::createRenderer() const {
//    return new LogoInFboRenderer();
//}
//
//


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
            QImage *img = use_first ? &p->buf1_ : &p->buf2_;

            bool res = img->loadFromData(by, "JPEG"); //data.data(), data.size(), "JPEG");
            if (res)
            {
                p->mutex.lock();
                use_first = !use_first;
                p->image_ = img;
                emit ImagePosted();
                p->mutex.unlock();
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
    QObject::connect(&workerThread, SIGNAL(ImagePosted()), this, SLOT(OnImagePosted()));
    workerThread.start();
    //worker.moveToThread(&workerThread);
    //workerThread.start();

}

ImageStreamHandler::~ImageStreamHandler()
{
    workerThread.quit();
    workerThread.wait();
}

void ImageStreamHandler::add_frame(const QString &data)
{
    mutex.lock();
    last_frame_ = data;
    ++avail_;
    //std::cout << "Got frame: " << avail_ << std::endl;
    mutex.unlock();
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
        mutex.lock();
        QSGSimpleTextureNode *n = static_cast<QSGSimpleTextureNode*>(node);
        QSGTexture *texture = window()->createTextureFromImage(*image_, QQuickWindow::TextureIsOpaque);
        n->setOwnsTexture(true);
        n->setRect(boundingRect());
        n->markDirty(QSGNode::DirtyForceUpdate);
        n->setTexture(texture);
        mutex.unlock();
    }
    return node;
}




#endif






