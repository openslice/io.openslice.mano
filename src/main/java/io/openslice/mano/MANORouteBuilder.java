/*-
 * ========================LICENSE_START=================================
 * io.openslice.portal.api
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */


package io.openslice.mano;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.openslice.model.ScaleDescriptor;
import io.openslice.model.VxFOnBoardedDescriptor;

/**
 * @author ctranoris
 *
 */
@Component
@Configuration
public class MANORouteBuilder  extends RouteBuilder{
	

	@Autowired
	MANOController aMANOController;


	@Value("${NFV_CATALOG_NSACTIONS_SCALE}")
	private String NFV_CATALOG_NSACTIONS_SCALE ="";
	
	
	public static void main(String[] args) throws Exception {
		//new Main().run(args);				
		CamelContext tempcontext = new DefaultCamelContext();
		try {
		RouteBuilder rb = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from( "timer://getVNFRepoTimer?period=2000&repeatCount=3&daemon=true"  )
        		.log( "Will check VNF repo");
                
                from( "timer://getNSDRepoTimer?period=2000&repeatCount=3&daemon=true"  )
        		.log( "Will check NSD repo");
            }
        };
        tempcontext.addRoutes( rb);
        tempcontext.start();
        Thread.sleep(30000);
		} finally {			
			tempcontext.stop();
        }							
	}
	

	public void configure() {
		
//		/**
//		 * OnBoard New Added VxF
//		 */		
//		//We get the message here and we need to route it to the proper point.
//		//If onboarding is successfull we need to send a Bugzilla message
//		//If it is unsuccessful we need to send another Bugzilla message
//		from("seda:vxf.onboard?multipleConsumers=true")
//		.doTry()
//		.bean( aMANOController,"onBoardVxFToMANOProvider") //returns exception or nothing
//		.log("VNFD Onboarded handled")
//		.doCatch(Exception.class)
//		.log("VNFD Onboarding failed!");
//
//		from("seda:nsd.onboard?multipleConsumers=true")
//		.doTry()
//		.bean( aMANOController,"onBoardNSDToMANOProvider") //returns exception or nothing
//		.log("NSD Onboarded handled")
//		.doCatch(Exception.class)
//		.log("NSD Onboarding failed!");		

//**********************************************************************************************
// 		THESE NOW ARE CALLED DIRECTLY AND ARE NOT REQUIRED ANY MORE 26122019
//		from("seda:nsd.deploy?multipleConsumers=true")
//		.doTry()
//		.bean( aMANOController,"deployNSDToMANOProvider") //returns exception or nothing
//		.log("NS deployed handled").stop()
//		.doCatch(Exception.class)
//		.log("NS deployment failed!"+"${exception.message}").stop();		
//
//		from("seda:nsd.deployment.complete?multipleConsumers=true")
//		.doTry()
//		.bean( aMANOController,"terminateNSFromMANOProvider") //returns exception or nothing
//		.log("NS completed handled")
//		.doCatch(Exception.class)
//		.log("NS completion failed!").stop();
//
//		from("seda:nsd.deployment.delete?multipleConsumers=true")
//		.doTry()
//		.bean( aMANOController,"deleteNSFromMANOProvider") //returns exception or nothing
//		.log("NS deleted handled")
//		.doCatch(Exception.class)
//		.log("NS deletion failed!").stop();
//		**********************************************************************************************
		
		from("timer://checkAndUpdateRunningDeploymentDescriptors?delay=1s&period=60000").bean(  aMANOController,"checkAndUpdateRunningDeploymentDescriptors").stop();
//		from("timer://checkAndUpdateMANOProvidersResources?delay=1s&period=60000").bean(  aMANOController,"checkAndUpdateMANOProvidersResources").stop();
		
		// THESE SEND THE DeploymentDescriptor Object to Bugzilla for status updates		
		// Here we needed to add getDeploymentEagerDataJson from portal.api.service.DeploymentDescriptorService 
		// in order to create the marshalling
		from("seda:nsd.deployment.instantiation.success?multipleConsumers=true")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class )
		.to( "activemq:topic:nsd.deployment.instantiation.success" );

		from("seda:nsd.deployment.instantiation.fail?multipleConsumers=true")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class )
		.to( "activemq:topic:nsd.deployment.instantiation.fail" );		

		from("seda:nsd.deployment.termination.success?multipleConsumers=true")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class )
		.to( "activemq:topic:nsd.deployment.termination.success" );				

		from("seda:nsd.deployment.termination.fail?multipleConsumers=true")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class )
		.to( "activemq:topic:nsd.deployment.termination.fail" );		
		
		from("seda:vxf.onboard.success?multipleConsumers=true")
		.marshal().json( JsonLibrary.Jackson, VxFOnBoardedDescriptor.class, true)
		.convertBodyTo( String.class )
		.to( "activemq:topic:vxf.onboard.success" );
										
		from("activemq:topic:vxf.onboard")
		.log( "activemq:topic:vxf.onboard for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.VxFOnBoardedDescriptor.class, true)
		.bean( aMANOController, "onBoardVxFToMANOProviderByOBD" )
		.to("log:DEBUG?showBody=true&showHeaders=true");
		
		from("activemq:topic:vxf.onboardbyfile")
		.log( "activemq:topic:vxf.onboardbyfile for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.VxFOnBoardedDescriptor.class, true)
		.bean( aMANOController, "onBoardVxFToMANOProviderByFile" )
		.to("log:DEBUG?showBody=true&showHeaders=true");

		from("activemq:topic:vxf.onBoardByCompositeObj")
		.log( "activemq:topic:vxf.onBoardByCompositeObj for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.CompositeVxFOnBoardDescriptor.class, true)
		.bean( aMANOController, "onBoardVxFToMANOProviderByCompositeObj" )
		.to("log:DEBUG?showBody=true&showHeaders=true");
		
		from("activemq:topic:vxf.offboard")
		.log( "activemq:topic:vxf.offboard for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.VxFOnBoardedDescriptor.class, true)
		.doTry()
			.bean( aMANOController, "offBoardVxFFromMANOProvider" ) //Replies with a ResponseInstance 
			.marshal().json( JsonLibrary.Jackson, true)
			.convertBodyTo(String.class)
        	.log("offboarding ok with ${body}")
        .doCatch(Exception.class)
        	.setBody(exceptionMessage())
        	.log("offboard failed with exception ${body}")
        .end();

		from("activemq:topic:nsd.onboardbyfile")
		.log( "activemq:topic:nsd.onboardbyfile for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.ExperimentOnBoardDescriptor.class, true)
		.bean( aMANOController, "onBoardNSDToMANOProviderByFile" )
		.to("log:DEBUG?showBody=true&showHeaders=true");		

		from("activemq:topic:nsd.onBoardByCompositeObj")
		.log( "activemq:topic:nsd.onBoardByCompositeObj for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.CompositeExperimentOnBoardDescriptor.class, true)
		.bean( aMANOController, "onBoardNSDToMANOProviderByCompositeObj" )
		.to("log:DEBUG?showBody=true&showHeaders=true");
		
		from("activemq:topic:nsd.onboard")
		.log( "activemq:topic:nsd.onboard for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.ExperimentOnBoardDescriptor.class, true)
		.bean( aMANOController, "onBoardNSDToMANOProvider" )
		.to("log:DEBUG?showBody=true&showHeaders=true");
		
		from("activemq:topic:nsd.offboard")
		.log( "activemq:topic:nsd.offboard for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.ExperimentOnBoardDescriptor.class, true)
		.doTry()
			.bean( aMANOController, "offBoardNSDFromMANOProvider" ) //Replies with a ResponseInstance 
			.marshal().json( JsonLibrary.Jackson, true)
			.convertBodyTo(String.class)
        	.log("offboarding ok with ${body}")
        .doCatch(Exception.class)
        	.setBody(exceptionMessage())
        	.log("offboard failed with exception ${body}")
        .end();
		
		
		from("activemq:topic:vxf.metadata.retrieve")
		.log("activemq:topic:vxf.metadata.retrieve")
		.choice()
        .when(header("OSMType").isEqualTo("OSMvTEN"))
    		.bean(aMANOController, "mapOSM10VNFD2ProductEagerDataJson")
        .when(header("OSMType").isEqualTo("OSMvELEVEN"))
    		.bean(aMANOController, "mapOSM10VNFD2ProductEagerDataJson")
        .when(header("OSMType").isEqualTo("OSMvTHIRTEEN"))
    		.bean(aMANOController, "mapOSM10VNFD2ProductEagerDataJson");
        //.when(header("OSMType").isEqualTo("GenericSOL005"))
        //	.bean(aMANOController, "mapOSM7VNFD2ProductEagerDataJson");

		from("activemq:topic:ns.metadata.retrieve")
		.log("activemq:topic:ns.metadata.retrieve")
		.choice()
        .when(header("OSMType").isEqualTo("OSMvTEN"))
    		.bean(aMANOController, "mapOSM10NSD2ProductEagerDataJson")
        .when(header("OSMType").isEqualTo("OSMvELEVEN"))
    		.bean(aMANOController, "mapOSM10NSD2ProductEagerDataJson")
        .when(header("OSMType").isEqualTo("OSMvTHIRTEEN"))
    		.bean(aMANOController, "mapOSM10NSD2ProductEagerDataJson");
        //.when(header("OSMType").isEqualTo("GenericSOL005"))
        //	.bean(aMANOController, "mapOSM7NSD2ProductEagerDataJson");

		from("jms:queue:ns.action.run")
		.log("jms:queue:ns.action.run")
		.convertBodyTo( String.class )
		.bean(aMANOController, "performNSInstanceAction")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo(String.class)
		.log("action run ok with ${body}");

		from("jms:queue:ns.scale.run")
		.log("jms:queue:ns.scale.run")
		.convertBodyTo( String.class )
		.bean(aMANOController, "performNSInstanceScale")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo(String.class)
		.log("scale operation run ok with ${body}");
				

		from( NFV_CATALOG_NSACTIONS_SCALE )
		.log(LoggingLevel.INFO, log, NFV_CATALOG_NSACTIONS_SCALE + " message received!")
		.to("log:DEBUG?showBody=true&showHeaders=true")
		.unmarshal()
		.json( JsonLibrary.Jackson, ScaleDescriptor.class, true) 
		.bean(aMANOController, "performNSScale")
		.convertBodyTo(String.class);
		
		
		
		
		
		from("activemq:topic:ns.action.getnslcmdetails")
		.log("activemq:topic:ns.action.getnslcmdetails")
		.convertBodyTo( String.class )
		.bean(aMANOController, "getNSLCMDetails")
		.convertBodyTo(JSONObject.class)
    	.log("action run ok with ${body}");
		
		from("activemq:queue:ns.scalealert")
		.convertBodyTo( String.class )
		.bean(aMANOController, "getScaleAlertMessageBody");
				
	}	
}
