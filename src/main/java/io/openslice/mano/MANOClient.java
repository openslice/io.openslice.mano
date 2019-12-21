package io.openslice.mano;

import java.io.IOException;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.ExperimentMetadata;
import io.openslice.model.MANOprovider;

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
	
	// Get the data from the portal api (database)
	public List<DeploymentDescriptor> getRunningInstantiatingAndTerminatingDeployments() {
		String ret = template.requestBody( "activemq:queue:getRunningInstantiatingAndTerminatingDeployments", "", String.class);
		//FluentProducerTemplate template = contxt.create .createFluentProducerTemplate().to("activemq:queue:getRunningInstantiatingAndTerminatingDeployments?multipleConsumers=true");
	
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
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
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
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
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
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return DeploymentDescriptorsToComplete;	
	}

	public List<DeploymentDescriptor> getDeploymentsToBeDeleted() {
		String ret = template.requestBody( "activemq:queue:getDeploymentsToBeDeleted", "", String.class);
	
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
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return DeploymentDescriptorsToDelete;	
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
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return dd;	
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
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e11) {
			// TODO Auto-generated catch block
			e11.printStackTrace();
		}
		return em;	
	}

	/**
	 * @param d
	 * @return as json
	 * @throws JsonProcessingException
	 */
	public String getDeploymentEagerDataJson( DeploymentDescriptor d ) throws JsonProcessingException {

		DeploymentDescriptor dd = this.getDeploymentByIdEager( d.getId() );
		ObjectMapper mapper = new ObjectMapper();
        //Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
        mapper.registerModule(new Hibernate5Module()); 
		String res = mapper.writeValueAsString( dd );
		
		return res;
	}	
	
	// Update the data in the portal api (database)
	public DeploymentDescriptor updateDeploymentDescriptor(DeploymentDescriptor dd2send)
	{
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
		
		String ret = template.requestBody( "activemq:queue:updateDeploymentDescriptor",  dd_serialized, String.class);
		//FluentProducerTemplate template = contxt.create .createFluentProducerTemplate().to("activemq:queue:getRunningInstantiatingAndTerminatingDeployments?multipleConsumers=true");
		logger.info("Message Received from AMQ:"+ret);
		DeploymentDescriptor dd = null;
		// Map object to ExperimentMetadata
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

	public void completeExperiment(long deploymentdescriptorid) {
		logger.info( "completeExperiment: to(\"seda:nsd.complete?multipleConsumers=true\")");		
		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:nsd.deployment.complete?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();				
	}

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
	
}
