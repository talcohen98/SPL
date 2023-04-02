#pragma once
#include <string>
#include <vector>

using std::string;
using std::vector;

class JoinPolicy;
class Simulation;

enum State
{
    Waiting,
    CollectingOffers,
    Joined
};

class Party
{
public:
    Party(int id, string name, int mandates, JoinPolicy *); 

//Rule of 5
    ~Party(); //destructor
    Party(const Party& other); //copy constructor
    Party(Party&& other); //move constructor
    Party& operator=(const Party& other); //copy assignment operator
    Party& operator=(Party&& other); //move assignment operator

    State getState() const;
    void setState(State state);
    int getMandates() const;
    void step(Simulation &s);
    const string &getName() const;
    int getId() const;
    int getCoalition() const;
    void setCoalition(int id);
    void addAgent(int offeredAgentId);
    vector<int>* getIdOfAgentsThatOffered() const;

private:
    int mId;
    string mName;
    int mMandates;
    JoinPolicy *mJoinPolicy;
    State mState;
    vector<int>* IdOfAgentsThatOffered;
    int mTimer;
    int mCoalition;

};
