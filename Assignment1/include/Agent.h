#pragma once

#include <vector>
#include "Graph.h"

class SelectionPolicy;

class Agent
{
public:
    Agent(int agentId, int partyId, SelectionPolicy *selectionPolicy);

    //Rule of 5
    ~Agent(); //destructor
    Agent(const Agent& other); //copy constructor
    Agent(Agent&& other); //move constructor
    Agent& operator=(const Agent& other); //copy assignment operator
    Agent& operator=(Agent&& other); //move assignment operator

    int getPartyId() const;
    void setPartyId(int id);
    int getId() const;
    void setId(int id);
    void step(Simulation &);
    int getCoalition() const;
    void setCoalition(int id);

private:
    int mAgentId;
    int mPartyId;
    int mCoalition;
    SelectionPolicy *mSelectionPolicy;
};
