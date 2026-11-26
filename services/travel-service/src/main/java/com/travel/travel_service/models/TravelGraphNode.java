package com.travel.travel_service.models;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Travel")
public class TravelGraphNode {

    @Id
    private Long travelId;

    public TravelGraphNode() {
    }

    public Long getTravelId() {
        return travelId;
    }

    public void setTravelId(Long travelId) {
        this.travelId = travelId;
    }
}
