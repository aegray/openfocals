#include "Launcher.h"

Launcher::Launcher(QObject *parent) :
    QObject(parent),
    m_process(new QProcess(this))
{
}

QString Launcher::launch(const QString &program)
{
    m_process->start(program);
    m_process->waitForFinished(-1);
    QByteArray bytes = m_process->readAllStandardOutput();
    QString output = QString::fromLocal8Bit(bytes);
    return output;
}

void Launcher::launch_background(const QString &program)
{
	const char *data = program.toLocal8Bit().constData();
	
	system((std::string(data) + std::string(" & ")).c_str());
    //m_process->start(program + " & ");
    //m_process->waitForFinished(-1);
    //QByteArray bytes = m_process->readAllStandardOutput();
    //QString output = QString::fromLocal8Bit(bytes);
}
