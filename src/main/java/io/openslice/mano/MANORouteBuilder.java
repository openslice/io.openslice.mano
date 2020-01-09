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
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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
		
		from("activemq:topic:vxf.onboard")
		.log( "activemq:topic:vxf.onboard for ${body} !" )
		.unmarshal().json( JsonLibrary.Jackson, io.openslice.model.VxFOnBoardedDescriptor.class, true)
		.bean( aMANOController, "onBoardVxFToMANOProvider" )
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
		
		from("seda:vxf.onboard.success?multipleConsumers=true")
		.marshal().json( JsonLibrary.Jackson, VxFOnBoardedDescriptor.class, true)
		.convertBodyTo( String.class )
		.to( "activemq:topic:vxf.onboard.success" );
				
	}


}
