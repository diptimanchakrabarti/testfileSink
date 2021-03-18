package com.input.data.transform;

import io.connectedhealth_idaas.eventbuilder.events.platform.RoutingEvent;
import io.connectedhealth_idaas.eventbuilder.parsers.clinical.FHIRStreamParser;

public class BuildeventTransform {

 public String returnFHIRAllergy(String msgBody) {
     System.out.println(msgBody);
     FHIRStreamParser parser = new FHIRStreamParser();
     RoutingEvent msgRoutingEvent = parser.buildRoutingEvent(msgBody);
     return msgRoutingEvent.toString();
    }


}
