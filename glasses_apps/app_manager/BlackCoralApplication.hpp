#pragma once


#include "BlackCoralApplicationBase.hpp"


namespace thalmic {

class BlackCoralApplication : public BlackCoralApplicationBase<>
{
    Q_OBJECT

public:
    BlackCoralApplication(int &argc, char **argv, QString const &name);
};


}


