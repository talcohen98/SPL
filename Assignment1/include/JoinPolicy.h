#pragma once
#include <vector>
#include "Simulation.h"

class JoinPolicy {
public:
    virtual int select(Simulation &sim, std::vector<int>* optionalAgents) const=0;
    virtual JoinPolicy* cloneJp()=0;
    virtual ~JoinPolicy() = default;
};

class MandatesJoinPolicy : public JoinPolicy {
public:
    int select(Simulation &sim, std::vector<int>* optionalAgents) const;
    JoinPolicy* cloneJp();
};

class LastOfferJoinPolicy : public JoinPolicy {
public:
    int select(Simulation &sim, std::vector<int>* optionalAgents) const;
    JoinPolicy* cloneJp();
};