#include "../include/JoinPolicy.h"

//FOR PRINT
#include <iostream>
using std::cout;
using std::endl;

int MandatesJoinPolicy::select(Simulation &sim, std::vector<int>* optionalAgents) const{
    int maxMandates = 0;
    int coalitionWithMaxMandates = -1;
    vector<int>::iterator agentIt;
    for(agentIt = optionalAgents->begin(); agentIt != optionalAgents->end(); agentIt++){
        int coalitionOfAgent = sim.getCoalitionByAgentId(*agentIt);
        for(vector<int> singleCoalition:sim.getPartiesByCoalitions()){ // need to check if we can do it without calling getPartiesByCoalitions
            int coalitionId = (sim.getParty(singleCoalition[0])).getCoalition();
            if(coalitionOfAgent == coalitionId){
                int sumOfMandatesInCoalition = 0;
                for(int partyId:singleCoalition){
                    sumOfMandatesInCoalition = sumOfMandatesInCoalition + (sim.getParty(partyId)).getMandates();
                }
                if (sumOfMandatesInCoalition > maxMandates){
                    maxMandates = sumOfMandatesInCoalition;
                    coalitionWithMaxMandates = coalitionId;
                }
            }
        }
    }
    return coalitionWithMaxMandates;
}

JoinPolicy* MandatesJoinPolicy::cloneJp(){
    return new MandatesJoinPolicy();
}

int LastOfferJoinPolicy::select(Simulation &sim, std::vector<int>* optionalAgents) const{
    int n = (*optionalAgents).size();
    int idOfLastAgentThatOffered = (*optionalAgents)[n-1];
    int coalitionOfAgent = sim.getCoalitionByAgentId(idOfLastAgentThatOffered);
    return coalitionOfAgent;
}

JoinPolicy* LastOfferJoinPolicy::cloneJp(){
    return new LastOfferJoinPolicy();
}