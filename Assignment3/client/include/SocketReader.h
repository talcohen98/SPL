#pragma once

#include <string>
#include <iostream>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <connectionHandler.h>
#include <map>
#include <list>
#include "../include/event.h"

using std::queue;
using std::string;
using std::mutex;
using std::unique_lock;

class SocketReader{
    private:
        ConnectionHandler& connectionHandler;
        queue<string>& messageQueue;
        unique_lock<mutex>& monitor;
        std::condition_variable& condVar;
        bool shouldTerminate; // boolean field that indicates when we stop reading
        int uniqueId; // used for subscribe frame
        std::map<string, std::map<int, string>> userSubscriptions;
        int uniqueReceipt;
        string connectedUser;
        // std::map<string,std::map<string,std::queue<Event>>> data; // the data struc suggested in the assignment for summary

    public:
        SocketReader(ConnectionHandler& connectionHandler, queue<string>& messageQueue,  unique_lock<mutex>& mutex, std::condition_variable& _condVar);

        void readMessages();
        bool getShouldTerminate() const;
        string prepareMessage(string &s);
        string mapToStr(std::map<std::string, std::string> map);
};