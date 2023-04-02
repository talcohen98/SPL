#pragma once
#include <vector>
#include "Simulation.h"

class SelectionPolicy { 
public:
    virtual int select(std::vector<int>* optionalParties, Simulation &sim, int partyIdOfOfferingAgent) const=0;
    virtual SelectionPolicy* cloneSp()=0;
    virtual ~SelectionPolicy() = default;
};

class MandatesSelectionPolicy: public SelectionPolicy{ 
public:
    int select(std::vector<int>* optionalParties, Simulation &sim, int partyIdOfOfferingAgent) const;
    SelectionPolicy* cloneSp();
};

class EdgeWeightSelectionPolicy: public SelectionPolicy{ 
public:
    int select(std::vector<int>* optionalParties, Simulation &sim, int partyIdOfOfferingAgent) const;
    SelectionPolicy* cloneSp();
};