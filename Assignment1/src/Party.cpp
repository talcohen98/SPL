#include "Party.h"
#include <vector>
using std::vector;
#include "Agent.h"
#include "JoinPolicy.h"

//FOR PRINT
#include <iostream>
using std::cout;
using std::endl;

Party::Party(int id, string name, int mandates, JoinPolicy *jp) : mId(id), mName(name), mMandates(mandates), mJoinPolicy(jp), mState(Waiting), IdOfAgentsThatOffered(new vector<int>), mTimer(0), mCoalition(-1)
{
    // You can change the implementation of the constructor, but not the signature!
}

// Rule of 5
// destructor
Party:: ~Party()
{
    if (mJoinPolicy) delete mJoinPolicy;
    delete IdOfAgentsThatOffered;
}

// copy constructor
Party::Party(const Party &other) : mId(other.mId), mName(other.mName), mMandates(other.mMandates), mJoinPolicy(other.mJoinPolicy -> cloneJp()), 
mState(other.mState), IdOfAgentsThatOffered(new vector<int>(*(other.IdOfAgentsThatOffered))), mTimer(other.mTimer), mCoalition(other.mCoalition) {}

// move constructor
Party::Party(Party &&other) : mId(other.mId), mName(other.mName), mMandates(other.mMandates), mJoinPolicy(other.mJoinPolicy),
   mState(other.mState), IdOfAgentsThatOffered(other.IdOfAgentsThatOffered), mTimer(other.mTimer), mCoalition(other.mCoalition)
{
    mId = other.mId;
    other.mName = "";
    other.mMandates = 0;
    other.mJoinPolicy = nullptr;
    other.mState = Waiting;
    other.IdOfAgentsThatOffered = nullptr;
    other.mTimer = 0;
    other.mCoalition = -1;
}

// copy assignment operator
Party& Party::operator=(const Party &other)
{
    if (this != &other)
    {
        mId = other.mId;
        mName = other.mName;
        mMandates = other.mMandates;
        delete mJoinPolicy;
        mJoinPolicy = (other.mJoinPolicy -> cloneJp());
        mState = other.mState;
        delete IdOfAgentsThatOffered;
        IdOfAgentsThatOffered = other.IdOfAgentsThatOffered;
        mTimer = other.mTimer;
        mCoalition = other.mCoalition;
    }
    return *this;
}

// move assignment operator
Party& Party::operator=(Party &&other)
{
    if(mJoinPolicy) delete mJoinPolicy;
    mId = other.mId;
    mName = other.mName;
    mMandates = other.mMandates;
    delete mJoinPolicy;
    mJoinPolicy = other.mJoinPolicy;
    other.mJoinPolicy = nullptr;
    mState = other.mState;
    delete IdOfAgentsThatOffered;
    IdOfAgentsThatOffered = other.IdOfAgentsThatOffered;
    other.IdOfAgentsThatOffered = nullptr;
    mTimer = other.mTimer;
    mCoalition = other.mCoalition;
    other.mCoalition = -1;
    return *this;
}

State Party::getState() const
{
    return mState;
}

void Party::setState(State state)
{
    mState = state;
}

int Party::getMandates() const
{
    return mMandates;
}

const string & Party::getName() const
{
    return mName;
}

int Party::getId() const
{
    return mId;
}

vector<int> *Party::getIdOfAgentsThatOffered() const{
    return IdOfAgentsThatOffered;
}

int Party::getCoalition() const{
    return mCoalition;
}

void Party::setCoalition(int id){
    mCoalition = id;
}

void Party::addAgent(int offeredAgentId){
    IdOfAgentsThatOffered->push_back(offeredAgentId);
}

void Party::step(Simulation &s){
    if(mState == CollectingOffers){
            if(mTimer >= 2){
                mCoalition = (*mJoinPolicy).select(s, IdOfAgentsThatOffered);
                mState = Joined;
                if(!IdOfAgentsThatOffered->empty()){
                    for(long unsigned int i = 0; i < IdOfAgentsThatOffered->size(); i++){
                        Agent agent = s.getAgentByAgentId((*IdOfAgentsThatOffered)[i]);
                        if(agent.getCoalition() == mCoalition){
                            Agent clonedAgent(agent);
                            clonedAgent.setId((s.getAgents()).size());
                            clonedAgent.setPartyId(mId);
                            s.addClonedAgent(clonedAgent);
                        }
                    }
                }
            } 
            else{
                mTimer++;
            }
    }
}
