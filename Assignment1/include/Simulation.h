#pragma once

#include <vector>

#include "Graph.h"
#include "Agent.h"

using std::string;
using std::vector;

class Simulation
{
public:
    Simulation(Graph g, vector<Agent> agents);

    void step();
    bool shouldTerminate() const;

    const Graph &getGraph() const;
    const vector<Agent> &getAgents() const;
    const Party &getParty(int partyId) const;
    Party &getPartyNotConst(int partyId);
    const vector<vector<int>> getPartiesByCoalitions() const;
    const int getCoalitionByAgentId(int agentId);
    const Agent& getAgentByAgentId(int agentId);
    void addClonedAgent(Agent cAgent);

private:
    Graph mGraph;
    vector<Agent> mAgents;
};
