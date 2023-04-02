#include "Simulation.h"

//FOR PRINT
#include <iostream>
using std::cout;
using std::endl;

Simulation::Simulation(Graph graph, vector<Agent> agents) : mGraph(graph), mAgents(agents) 
{
    // You can change the implementation of the constructor, but not the signature!
    for(unsigned int agent = 0; agent < mAgents.size(); agent++) {
        (mAgents[agent]).setCoalition(agent);
        getPartyNotConst((mAgents[agent]).getPartyId()).setCoalition(agent);
    }
}

void Simulation::step()
{
    for(int party=0; party < mGraph.getNumVertices(); party++) { //making step for each party
        mGraph.getParty(party).step(*this);
    }

    for(Agent& agent: mAgents) { //making step for each agent
        agent.step(*this);
    }
}

bool Simulation::shouldTerminate() const
{
    vector<vector<int>> coalitionParties = getPartiesByCoalitions(); //checks if there are 61 mandates or more
    for(vector<int> singleCoalition: coalitionParties) {
        int sumOfMandatesInCoalition = 0;
        for(int singleParty: singleCoalition) {
            sumOfMandatesInCoalition += mGraph.getMandates(singleParty);
        }
        if(sumOfMandatesInCoalition >= 61) {
            return true;
        }
    }

    int numberOfJoinedParties = 0;
    for(int party=0; party < mGraph.getNumVertices(); party++){ //checks if all parties are part of a coalition
        if((mGraph.getParty(party)).getState() == Joined){
            numberOfJoinedParties++;
        }
    }
    if (numberOfJoinedParties == mGraph.getNumVertices()){
        return true;
    }

    return false;
}

const Graph &Simulation::getGraph() const
{
    return mGraph;
}

const vector<Agent> &Simulation::getAgents() const
{
    return mAgents;
}

const Party &Simulation::getParty(int partyId) const
{
    return mGraph.getParty(partyId);
}

Party &Simulation::getPartyNotConst(int partyId)
{
    return mGraph.getParty(partyId);
}

const int Simulation::getCoalitionByAgentId(int agentId){
    for(Agent& agent:mAgents){
        if (agent.getId() == agentId){
            return agent.getCoalition();
        }
    }
    return -1;
}

const Agent& Simulation::getAgentByAgentId(int agentId){
    for(Agent& agent:mAgents){
        if (agent.getId() == agentId){
            return agent;
        }
    }
    throw std::invalid_argument("There is no agent with this id");
}

void Simulation::addClonedAgent(Agent cAgent){
    mAgents.push_back(cAgent);
}

/// This method returns a "coalition" vector, where each element is a vector of party IDs in the coalition.
/// At the simulation initialization - the result will be [[agent0.partyId], [agent1.partyId], ...]
const vector<vector<int>> Simulation::getPartiesByCoalitions() const //checked
{
    vector<vector<int>> partiesByCoalition;
    for(Agent agent:mAgents){
        unsigned long int agentCoalition = agent.getCoalition();
        bool partyIsAdded = false;
        for(unsigned long int i = 0; i < partiesByCoalition.size() && !partyIsAdded; i++) {
           if(agentCoalition == i){
                partiesByCoalition[i].push_back(agent.getPartyId());
                partyIsAdded = true;
           }
        }
        if(!partyIsAdded) {
            vector<int> newCoalition;
            newCoalition.push_back(agent.getPartyId());
            partiesByCoalition.push_back(newCoalition);
        }
    }
    return partiesByCoalition;
}
