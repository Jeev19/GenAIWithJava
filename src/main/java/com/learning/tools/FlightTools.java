package com.learning.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FlightTools {

    @Tool("Retrieves the flight status of a given flight")
    String getFlightStatus(String flightId) {
        return flightId + " status: " + "IN THE AIR.  2 hours from destination";
    }

    @Tool("Returns the type of aircraft")
    String getAircraftType(String flightId) {
        return flightId + " type: " + "Boeing 747";
    }

    @Tool("Returns the cost of a flight")
    String getFlightCost(String flightId) {         // Name of method is helpful context for an LLM to choose a method
        return "1234.00 USD";
    }

}
