/*-
 * ========================LICENSE_START=================================
 * io.openslice.manoclient
 * %%
 * Copyright (C) 2019 - 2020 openslice.io
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

import java.io.IOException;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.ExperimentMetadata;
import io.openslice.model.ExperimentOnBoardDescriptor;
import io.openslice.model.MANOprovider;
import io.openslice.model.VxFMetadata;
import io.openslice.model.VxFOnBoardedDescriptor;

import org.springframework.stereotype.Service;

@Service
public class MANOClient {
	
	private static final transient Log logger = LogFactory.getLog(MANOClient.class.getName());

	@Autowired
	CamelContext contxt;

//	@Autowired
//	public void setActx(CamelContext actx) {
//		MANOClient.contxt = actx;
//		logger.info( "MANOClient configure() contxt = " + contxt);
//	}
	
	@Autowired
	ProducerTemplate template;
	
	MANOController aMANOController;

	public VxFOnBoardedDescriptor getVxFOnBoardedDescriptorByID(long id)
	{
		String ret = template.requestBody( "activemq:queue:getVxFOnBoardedDescriptorByID", id, String.class);

		VxFOnBoardedDescriptor vxf_obd = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			vxf_obd = mapper.readValue(ret, new TypeReference<VxFOnBoardedDescriptor>(){});
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return vxf_obd;			
	}
	
	// Get the data from the portal api (database)
	public List<DeploymentDescriptor> getRunningInstantiatingAndTerminatingDeployments() {
		String ret = template.requestBody( "activemq:queue:getRunningInstantiatingAndTerminatingDeployments", "", String.class);
	
		List<DeploymentDescriptor> nsds = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			nsds = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>(){});
			for(int i=0; i<nsds.size();i++)
			{
				logger.info("The detailed status of "+nsds.get(i).getName()+" is "+nsds.get(i).getDetailedStatus());
			}
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return nsds;	
	}

	public List<DeploymentDescriptor> getDeploymentsToInstantiate() {
		String ret = template.requestBody( "activemq:queue:getDeploymentsToInstantiate", "", String.class);
	
		List<DeploymentDescriptor> DeploymentDescriptorsToRun = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			DeploymentDescriptorsToRun = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>(){});
			// Foreach deployment
			for (DeploymentDescriptor d : DeploymentDescriptorsToRun) {
				// Launch the deployment
				logger.info("The deployment with name  "+d.getName()+" is going to be instantiated");				
			}			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());			
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return DeploymentDescriptorsToRun;	
	}

	public List<DeploymentDescriptor> getDeploymentsToBeCompleted() {
		String ret = template.requestBody( "activemq:queue:getDeploymentsToBeCompleted", "", String.class);
	
		List<DeploymentDescriptor> DeploymentDescriptorsToComplete = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			DeploymentDescriptorsToComplete = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>(){});
			for (DeploymentDescriptor d : DeploymentDescriptorsToComplete) {
				// Launch the deployment
				logger.info("The deployment with name  "+d.getName()+" is going to be Completed");
			}			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return DeploymentDescriptorsToComplete;	
	}

	public List<DeploymentDescriptor> getDeploymentsToBeDeleted() {
		String ret = template.requestBody( "activemq:queue:getDeploymentsToBeDeleted", "", String.class);
		logger.info("Deployments to be deleted: "+ret);
		List<DeploymentDescriptor> DeploymentDescriptorsToDelete = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			DeploymentDescriptorsToDelete = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>(){});
			for (DeploymentDescriptor d : DeploymentDescriptorsToDelete) {
				// Launch the deployment
				logger.info("The deployment with name  "+d.getName()+" is going to be Deleted");
			}			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return DeploymentDescriptorsToDelete;	
	}
	
	/**
	 * @param d
	 * @return as json
	 * @throws JsonProcessingException
	 */
	public String getDeploymentEagerDataJson( DeploymentDescriptor d ) throws JsonProcessingException {

		DeploymentDescriptor dd = this.getDeploymentByIdEager( d.getId() );
		ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Hibernate5Module()); 
		String res = mapper.writeValueAsString(dd);
		
		return res;
	}	
		
	// Get the data from the portal api (database)
	public DeploymentDescriptor getDeploymentByIdEager(long Id)
	{
		String ret = template.requestBody( "activemq:queue:getDeploymentByIdEager", Id, String.class);
		//FluentProducerTemplate template = contxt.create .createFluentProducerTemplate().to("activemq:queue:getRunningInstantiatingAndTerminatingDeployments?multipleConsumers=true");
		logger.info("Message Received from AMQ:"+ret);
		DeploymentDescriptor dd = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			dd = mapper.readValue(ret, DeploymentDescriptor.class);
			dd.setExperiment(this.getExperimentById(dd.getExperiment().getId()));
			logger.info("The experiment of the deployment is "+dd.getExperiment().toString());
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return dd;	
	}

	public VxFMetadata getVxFById(long id) {
		String ret = template.requestBody( "activemq:queue:getVxFByID", id, String.class);
		logger.info("Message Received from AMQ:"+ret);
		VxFMetadata vxfm = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			vxfm = mapper.readValue(ret, VxFMetadata.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return vxfm;	
	}

	public VxFMetadata getVxFByName(String name) {
		String ret = template.requestBody( "activemq:queue:getVxFByName", name, String.class);
		logger.info("Message Received from AMQ:"+ret);
		VxFMetadata vxfm = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			vxfm = mapper.readValue(ret, VxFMetadata.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return vxfm;	
	}
	
	public ExperimentMetadata getNSDById(long id) {
		String ret = template.requestBody( "activemq:queue:getNSDByID", id, String.class);
		logger.info("Message Received from AMQ:"+ret);
		ExperimentMetadata expm = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			expm = mapper.readValue(ret, ExperimentMetadata.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return expm;	
	}
	
	private ExperimentMetadata getExperimentById(long id) {
		String ret = template.requestBody( "activemq:queue:getNSDByID", id, String.class);
		logger.info("Message Received from AMQ:"+ret);
		ExperimentMetadata em = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			em = mapper.readValue(ret, ExperimentMetadata.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return em;	
	}
	
	private VxFOnBoardedDescriptor getVxFOnBoardedDescriptorById(long id)
	{
		String ret = template.requestBody( "activemq:queue:getVxFOBDByID", id, String.class);
		logger.info("Message Received from AMQ:"+ret);
		VxFOnBoardedDescriptor vxfobd = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			vxfobd = mapper.readValue(ret, VxFOnBoardedDescriptor.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.error(e1.getMessage());
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
			logger.error(e11.getMessage());
		}
		return vxfobd;			
	}
	
	public VxFOnBoardedDescriptor updateVxFOnBoardedDescriptor(VxFOnBoardedDescriptor vxfobd2send)
	{
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
        //Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
        //mapper.registerModule(new Hibernate5Module()); 
		String vxfobd_serialized = null;
		try {
			vxfobd_serialized = mapper.writeValueAsString( vxfobd2send );
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.info("Sending Message " + vxfobd_serialized + " to updateVxFOnBoardedDescriptor from AMQ:");		
		// Send it to activemq endpoint
		String ret = template.requestBody( "activemq:queue:updateVxFOnBoardedDescriptor",  vxfobd_serialized, String.class);
		logger.info("Message Received for updateVxFOnBoardedDescriptor from AMQ:"+ret);

		// Get the response and Map object to ExperimentMetadata
		VxFOnBoardedDescriptor vxfd = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			vxfd = mapper.readValue(ret, VxFOnBoardedDescriptor.class);
			//vxfd.setExperiment(this.getExperimentById(vxfd.getExperiment().getId()));
			//logger.info("The experiment of the deployment is "+vxfd.getExperiment().toString());
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return vxfd;			
	}
	
	public ExperimentOnBoardDescriptor updateExperimentOnBoardDescriptor(ExperimentOnBoardDescriptor expobd2send)
	{
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
        //Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
        //mapper.registerModule(new Hibernate5Module()); 
		String expobd_serialized = null;
		try {
			expobd_serialized = mapper.writeValueAsString( expobd2send );
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.info("Sending Message " + expobd_serialized + " to updateVxFOnBoardedDescriptor from AMQ:");		
		// Send it to activemq endpoint
		String ret = template.requestBody( "activemq:queue:updateExperimentOnBoardDescriptor",  expobd_serialized, String.class);
		logger.info("Message Received for updateExperimentOnBoardDescriptor from AMQ:"+ret);

		// Get the response and Map object to ExperimentMetadata
		ExperimentOnBoardDescriptor experimentobd = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			experimentobd = mapper.readValue(ret, ExperimentOnBoardDescriptor.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return experimentobd;			
	}
	
	// Update the data in the portal api (database)
	public DeploymentDescriptor updateDeploymentDescriptor(DeploymentDescriptor dd2send)
	{
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
        //Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
        //mapper.registerModule(new Hibernate5Module()); 
		String dd_serialized = null;
		try {
			dd_serialized = mapper.writeValueAsString( dd2send );
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}

		// Send it to activemq endpoint
		String ret = template.requestBody( "activemq:queue:updateDeploymentDescriptor",  dd_serialized, String.class);
		logger.info("Message Received from AMQ:"+ret);
		DeploymentDescriptor dd = null;
		// Get the response and Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			dd = mapper.readValue(ret, DeploymentDescriptor.class);
			dd.setExperiment(this.getExperimentById(dd.getExperiment().getId()));
			logger.info("The experiment of the deployment is "+dd.getExperiment().toString());
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return dd;	
	}
	
	// Update the data from the portal api (database)
	public MANOprovider getMANOproviderByID( long id )
	{
		String ret = template.requestBody( "activemq:queue:getMANOProviderByID", id, String.class);
		//FluentProducerTemplate template = contxt.create .createFluentProducerTemplate().to("activemq:queue:getRunningInstantiatingAndTerminatingDeployments?multipleConsumers=true");
	
		MANOprovider mp = null;
		// Map object to ExperimentMetadata
		try {
			// Map object to ExperimentMetadata
			ObjectMapper mapper = new ObjectMapper();
			logger.info("From ActiveMQ:"+ret.toString());
			mp = mapper.readValue(ret, MANOprovider.class);
			logger.info("The MANOprovider with name "+mp.getName()+" has endpoint "+mp.getApiEndpoint());
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return mp;	
	}
	

//	public void deployExperiment( DeploymentDescriptor  deploymentdescriptor) {		
//		logger.info( "deployExperiment: to(\"seda:nsd.deploy?multipleConsumers=true\")");		
//		FluentProducerTemplate	template = contxt.createFluentProducerTemplate().to("seda:nsd.deploy?multipleConsumers=true");
//		template.withBody( deploymentdescriptor ).asyncSend();	
//	}

	public void deleteExperiment(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.deployment.delete?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	
	// seda:nsd.deployment
	
	public void deploymentInstantiationSucceded(DeploymentDescriptor deploymentdescriptorid)
	{
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.deployment.instantiation.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}
	
	public void deploymentInstantiationFailed(DeploymentDescriptor deploymentdescriptorid)
	{
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.deployment.instantiation.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	public void deploymentTerminationSucceded(DeploymentDescriptor deploymentdescriptorid)
	{
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.deployment.termination.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}
	
	public void deploymentTerminationFailed(DeploymentDescriptor deploymentdescriptorid)
	{
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.deployment.termination.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}
	
	// seda:nsd.instance
	
	public void terminateInstanceSucceded(DeploymentDescriptor deploymentdescriptorid)
	{
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.instance.termination.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	public void terminateInstanceFailed(DeploymentDescriptor deploymentdescriptorid)
	{
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.instance.termination.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	public void deleteInstanceSucceded(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.instance.deletion.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();								
	}
		
	public void deleteInstanceFailed(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.instance.deletion.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();								
	}
	
	/**
	 * Asynchronously sends to the routing bus (seda:vxf.onboard?multipleConsumers=true) to upload a new vxf
	 * @param deployment a {@link VxFMetadata}
	 */
//	public void onBoardVxFAdded(VxFOnBoardedDescriptor obd) {
//		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:vxf.onboard?multipleConsumers=true");
//		template.withBody( obd ).asyncSend();				
//	}

	public void onBoardVxFFailed(VxFOnBoardedDescriptor vxfobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:vxf.onboard.fail?multipleConsumers=true");
		template.withBody( vxfobds_final ).asyncSend();			
	}

	public void onBoardVxFSucceded(VxFOnBoardedDescriptor vxfobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:vxf.onboard.success?multipleConsumers=true");
		template.withBody( vxfobds_final ).asyncSend();				
	}

	public void onBoardNSDFailed(ExperimentOnBoardDescriptor experimentobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.onboard.fail?multipleConsumers=true");
		template.withBody( experimentobds_final ).asyncSend();			
	}

	public void onBoardNSDSucceded(ExperimentOnBoardDescriptor experimentobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.onboard.success?multipleConsumers=true");
		template.withBody( experimentobds_final ).asyncSend();				
	}	
}
