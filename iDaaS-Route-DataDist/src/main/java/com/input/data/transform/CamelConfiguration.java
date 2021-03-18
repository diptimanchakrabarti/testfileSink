/*
 * Copyright 2019 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.input.data.transform;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaEndpoint;
import org.apache.camel.kafkaconnector.file.CamelFileSinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jms.connection.JmsTransactionManager;
//import javax.jms.ConnectionFactory;
import org.springframework.stereotype.Component;
import io.connectedhealth_idaas.eventbuilder.parsers.clinical.FHIRStreamParser;
@Component
public class CamelConfiguration extends RouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

    @Autowired
    private ConfigProperties config;

    @Bean
    private KafkaEndpoint kafkaEndpoint() {
        KafkaEndpoint kafkaEndpoint = new KafkaEndpoint();
        return kafkaEndpoint;
    }

    @Bean
    private KafkaComponent kafkaComponent(KafkaEndpoint kafkaEndpoint) {
        KafkaComponent kafka = new KafkaComponent();
        return kafka;
    }

    @Bean
    private CamelFileSinkConnector CamelFileSinkConnector(){
        CamelFileSinkConnector camelFileSinkConnector=new CamelFileSinkConnector();
        return camelFileSinkConnector;
    }
    @Bean
    public FHIRStreamParser routingEventParser() {
        return new FHIRStreamParser();
    }

    private String getKafkaTopicUri(String topic) {
        return "kafka:" + topic +
                "?brokers=" +
                config.getKafkaBrokers();
    }

    private String getFileSinkPath(String filename){
        String directoryPath="/tmp/filesink/";
        return "file://"+directoryPath+"?filename="+
                filename +"&charset=utf-8";
    }
    /*
     * Kafka implementation based upon https://camel.apache.org/components/latest/kafka-component.html
     *
     */
    @Override
    public void configure() throws Exception {

        /*
         * Audit
         *
         * Direct component within platform to ensure we can centralize logic
         * There are some values we will need to set within every route
         * We are doing this to ensure we dont need to build a series of beans
         * and we keep the processing as lightweight as possible
         *
         */
        from("direct:auditing")
                .setHeader("messageprocesseddate").simple("${date:now:yyyy-MM-dd}")
                .setHeader("messageprocessedtime").simple("${date:now:HH:mm:ss:SSS}")
                .setHeader("processingtype").exchangeProperty("processingtype")
                .setHeader("industrystd").exchangeProperty("industrystd")
                .setHeader("component").exchangeProperty("componentname")
                .setHeader("messagetrigger").exchangeProperty("messagetrigger")
                .setHeader("processname").exchangeProperty("processname")
                .setHeader("auditdetails").exchangeProperty("auditdetails")
                .setHeader("camelID").exchangeProperty("camelID")
                .setHeader("exchangeID").exchangeProperty("exchangeID")
                .setHeader("internalMsgID").exchangeProperty("internalMsgID")
                .setHeader("bodyData").exchangeProperty("bodyData")
                //.convertBodyTo(String.class).to("kafka://localhost:9092?topic=opsMgmt_PlatformTransactions&brokers=localhost:9092")
                .convertBodyTo(String.class)
                .to(getKafkaTopicUri("opsmgmt_platformtransactions"))
        ;
        /*
         *  Logging
         */
        from("direct:logging")
                .log(LoggingLevel.INFO, log, "Transaction Message: [${body}]")
        ;



        /*
         *   FHIR
         */
        // Adverse Events


        // Allergy Intollerance
        from(getKafkaTopicUri("ent_fhirsvr_allergyintollerance"))
                .routeId("AllergyIntollerance-FinalTier")

                // Auditing
                .setProperty("processingtype").constant("data")
                .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
                .setProperty("industrystd").constant("FHIR")
                .setProperty("messagetrigger").constant("AllergyIntollerance")
                .setProperty("component").simple("${routeId}")
                .setProperty("camelID").simple("${camelId}")
                .setProperty("exchangeID").simple("${exchangeId}")
                .setProperty("internalMsgID").simple("${id}")
                .setProperty("bodyData").simple("${body}")
                
                .setProperty("processname").constant("MTier")
                .setProperty("auditdetails").constant("Allergy Intollerance to Enterprise By Data Type middle tier")
                .wireTap("direct:auditing")
                // Enterprise Message By Type
                .convertBodyTo(String.class)
                .to(getFileSinkPath("test_file.txt"))
                //.bean(FHIRStreamParser.class,"buildRoutingEvent(${body})")
                //.to(getKafkaTopicUri("final_fhirsvr_allergyintollerance"))
        ;

    }
}