#include "../include/SelectionPolicy.h"
#include <vector>

//FOR PRINT
#include <iostream>
using std::cout;
using std::endl;

int MandatesSelectionPolicy::select(std::vector<int>* optionalParties, Simulation &sim, int partyIdOfOfferingAgent) const{
    int maxMandatesPartyId = -1; 
    int maxMandates = 0;
    for(int partyId:*optionalParties){
        if((sim.getParty(partyId)).getMandates() > maxMandates){
            maxMandates = (sim.getParty(partyId)).getMandates();
            maxMandatesPartyId = partyId;
        }   
    }
    return maxMandatesPartyId;
}

SelectionPolicy* MandatesSelectionPolicy::cloneSp(){
    return new MandatesSelectionPolicy();
}

int EdgeWeightSelectionPolicy::select(std::vector<int>* optionalParties, Simulation &sim, int partyIdOfOfferingAgent) const {
     int maxEdgeWeightPartyId = -1;
     int maxEdgeWeight = 0;
     for(int partyId:(*optionalParties)){
        int edgeWeight = sim.getGraph().getEdgeWeight(partyId,partyIdOfOfferingAgent);
        if(edgeWeight > maxEdgeWeight){
            maxEdgeWeight = edgeWeight;
            maxEdgeWeightPartyId = partyId; 
        }
     }
     return maxEdgeWeightPartyId;
}

SelectionPolicy* EdgeWeightSelectionPolicy::cloneSp(){
    return new EdgeWeightSelectionPolicy();
}