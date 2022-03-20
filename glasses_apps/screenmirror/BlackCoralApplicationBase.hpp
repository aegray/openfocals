#pragma once

#include <QGuiApplication>

namespace thalmic {

template <typename T = QGuiApplication>
class BlackCoralApplicationBase : public T 
{
    Q_OBJECT

public:
    BlackCoralApplicationBase(int &argc, char **argv, QString *name);
};


}
