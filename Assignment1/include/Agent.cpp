#include "Agent.h"
#include "Simulation.h"
#include "SelectionPolicy.h"

//FOR PRINT
#include <iostream>
using std::cout;
using std::endl;

Agent::Agent(int agentId, int partyId, SelectionPolicy *selectionPolicy) : mAgentId(agentId), mPartyId(partyId), mCoalition(-1), mSelectionPolicy(selectionPolicy)
{
    // You can change the implementation of the constructor, but not the signature!
}

//Rule of 5
//destructor
 Agent::~Agent(){
    if (mSelectionPolicy) delete mSelectionPolicy;
 } 

//copy constructor
Agent::Agent(const Agent& other):mAgentId(other.mAgentId),mPartyId(other.mPartyId), mCoalition(other.mCoalition), 
mSelectionPolicy(other.mSelectionPolicy -> cloneSp()){} 

//move constructor
Agent::Agent(Agent&& other) : mAgentId(other.mAgentId), mPartyId(other.mPartyId), mCoalition(other.mCoalition), mSelectionPolicy(other.mSelectionPolicy){
    other.mAgentId = 0;
    other.mPartyId = 0;
    other.mCoalition = -1;
    other.mSelectionPolicy = nullptr;
}

//copy assignment operator
Agent& Agent::operator=(const Agent& other) {
    if (this != &other)
    {
        mAgentId = other.mAgentId;
        mPartyId = other.mPartyId;
        mCoalition = other.mCoalition;
        delete mSelectionPolicy;
        mSelectionPolicy=(other.mSelectionPolicy -> cloneSp());
    }
    return *this;
}

//move assignment operator
Agent& Agent::operator=(Agent&& other){
    if(mSelectionPolicy) delete mSelectionPolicy;
    mAgentId = other.mAgentId;
    mPartyId = other.mPartyId;
    mCoalition = other.mCoalition;
    delete mSelectionPolicy;
    mSelectionPolicy = other.mSelectionPolicy;
    other.mCoalition = -1;
    other.mSelectionPolicy = nullptr;
    return *this;
}

int Agent::getId() const
{
    return mAgentId;
}

void Agent::setId(int id)
{
    mAgentId = id;
}

int Agent::getPartyId() const
{
    return mPartyId;
}

void Agent::setPartyId(int id)
{
    mPartyId = id;
}

int Agent::getCoalition() const
{
    return mCoalition;
}

void Agent::setCoalition(int id)
{
    mCoalition = id;
}

void Agent::step(Simulation &sim){

    vector <int> optionalParties;
    Graph graph = sim.getGraph();
    for(Party party:graph.getVertices()){
        bool agentCanOffer = true;
        if(graph.getEdgeWeight(party.getId(), mPartyId) > 0){ // checks if the party is our neighbor.
            if(party.getState() != Joined) {
                vector<int>* idOfAgentsThatOffered = party.getIdOfAgentsThatOffered();
                if (idOfAgentsThatOffered && !(idOfAgentsThatOffered -> empty())) {
                    for(unsigned long int i = 0; i < (idOfAgentsThatOffered -> size()) &&
                     agentCanOffer; i++){ //checks if theres any other agent from our coalition that already offered this party.         
                        if(sim.getCoalitionByAgentId((*idOfAgentsThatOffered)[i]) == mCoalition){
                            agentCanOffer = false;
                        }
                    }
                }
                if(agentCanOffer){
                   optionalParties.push_back(party.getId());
                }
            }
        }
    }

    if(!optionalParties.empty()){ //if the agent has a party to offer, we'll try to offer 
        int selectedPartyId = (*mSelectionPolicy).select(&optionalParties,sim,mPartyId);
        if(sim.getParty(selectedPartyId).getState() == Waiting){ // checking if we need to change selectedParty state to collecting offers.
            (sim.getPartyNotConst(selectedPartyId)).setState(CollectingOffers);
        }
        (sim.getPartyNotConst(selectedPartyId)).addAgent(mAgentId); // adding the agent to the offers vector.
    }
}
