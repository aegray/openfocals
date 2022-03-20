



#include "Wakelock.hpp"


Wakelock::Wakelock(QObject *parent) : QObject(parent), timer_(this)
    //binder_(android::defaultServiceManager()->getService(android::String16("thalmic.activity_service")))
{
//
//    if (!binder_)
//    {
//        std::cout << "INVALID" << std::endl;
//        throw std::runtime_error("INVALID");
//    }

   // binder_ = sm->getService(String16("thalmic.activity_service"));
    connect(&timer_, &QTimer::timeout, this, &Wakelock::on_timer);

    timer_.start(2500);
}

void Wakelock::on_timer()
{
    if (enabled_)
    {
        std::cout << "Trying timer" << std::endl;
        //auto x = android::defaultServiceManager()->getService(android::String16("thalmic.activity_service"));
        android::sp<android::IServiceManager> sm = android::defaultServiceManager();
        android::sp<android::IBinder> binder = sm->getService(android::String16("thalmic.activity_service"));


        if (!binder)
        {
            std::cout << "INVALID2" << std::endl;
            throw std::runtime_error("INVALID2");
        }
       
        //{
        //
        //    android::Parcel data;
        //    android::Parcel resp;
        //    data.writeInterfaceToken(android::String16("thalmic.activity_service"));
        //    data.writeInt32(2); // extendedInteraction
        //    binder->transact(4, data, &resp, 0); //, 0);
        //    //std::cout << "WORKED" << std::endl;
        //}

        {
            /// reset inactivity
            android::Parcel data;
            data.writeInterfaceToken(android::String16("thalmic.activity_service"));
            binder->transact(1, data, nullptr, 0); //, 0);

            std::cout << "Wrote wakelock" << std::endl;
        }

//
//        if (!binder_->pingBinder())
//        {
//            binder_ = android::defaultServiceManager()->getService(android::String16("thalmic.activity_service"));
//    
//            if (!binder_ || !binder_->pingBinder())
//            {
//                std::cout << "INVALID2" << std::endl;
//                throw std::runtime_error("INVALID2");
//            }
//        }
// 
//        binder_->transact(1, data, nullptr, 1); //, 0);
    }
}

void Wakelock::set_enabled(bool enabled)
{
    enabled_ = enabled;
    if (enabled != enabled_)
    {
        // 
        android::sp<android::IServiceManager> sm = android::defaultServiceManager();
        android::sp<android::IBinder> binder = sm->getService(android::String16("thalmic.activity_service"));
        if (!binder)
        {
            std::cout << "INVALID2" << std::endl;
            throw std::runtime_error("INVALID2");
        }


        if (!enabled_)
        {
            {
            
                android::Parcel data;
                android::Parcel resp;
                data.writeInterfaceToken(android::String16("thalmic.activity_service"));
                data.writeInt32(0); // extendedInteraction
                binder->transact(4, data, &resp, 0); 
            }
                 

            // need to enable
            //system("echo 'qmlwakelock' > /sys/power/wake_lock");
        }
        else
        {
            {
            
                android::Parcel data;
                android::Parcel resp;
                data.writeInterfaceToken(android::String16("thalmic.activity_service"));
                data.writeInt32(2); // extendedInteraction
                binder->transact(4, data, &resp, 0); 
            }
            //system("echo 'qmlwakelock' > /sys/power/wake_unlock");
        }

        enabled_ = enabled;
    }
}


bool Wakelock::get_enabled() const
{
    return enabled_;
}


