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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.ExperimentMetadata;
import io.openslice.model.ExperimentOnBoardDescriptor;
import io.openslice.model.Infrastructure;
import io.openslice.model.MANOprovider;
import io.openslice.model.PortalUser;
import io.openslice.model.VxFMetadata;
import io.openslice.model.VxFOnBoardedDescriptor;
import io.openslice.tmf.am642.model.AffectedService;
import io.openslice.tmf.am642.model.AlarmCreate;
import io.openslice.tmf.am642.model.AlarmStateType;
import io.openslice.tmf.am642.model.AlarmType;
import io.openslice.tmf.am642.model.Comment;
import io.openslice.tmf.am642.model.PerceivedSeverityType;
import io.openslice.tmf.am642.model.ProbableCauseType;

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

	@Autowired
	AlarmsService alarmsService;

	@Value("${spring.application.name}")
	private String compname;
	
	@Value("${NFV_CATALOG_NS_LCMCHANGED}")
	private String NFV_CATALOG_NS_LCMCHANGED = "";

	public VxFOnBoardedDescriptor getVxFOnBoardedDescriptorByID(long id) {
		String ret = template.requestBody("activemq:queue:getVxFOnBoardedDescriptorByID", id, String.class);

		VxFOnBoardedDescriptor vxf_obd = null;
		// Map object to VxFOnBoardedDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			vxf_obd = mapper.readValue(ret, new TypeReference<VxFOnBoardedDescriptor>() {
			});
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
		String ret = template.requestBody("activemq:queue:getRunningInstantiatingAndTerminatingDeployments", "",
				String.class);

		List<DeploymentDescriptor> nsds = null;
		// Map object to DeploymentDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			nsds = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>() {
			});
			for (int i = 0; i < nsds.size(); i++) {
				logger.info("The detailed status of " + nsds.get(i).getName() + " is " + nsds.get(i).getDetailedStatus());
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

	public List<Infrastructure> getInfrastructures() {
		String ret = template.requestBody("activemq:queue:getInfrastructures", "", String.class);

		List<Infrastructure> infrastructures = null;
		// Map object to Infrastructure
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			infrastructures = mapper.readValue(ret, new TypeReference<List<Infrastructure>>() {
			});
			for (int i = 0; i < infrastructures.size(); i++) {
				logger.info("Infrastructure " + infrastructures.get(i).getName() + " with VIM id:"
						+ infrastructures.get(i).getVIMid() + " is loaded");
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
		return infrastructures;
	}

	public List<VxFMetadata> getVnfds() {
		String ret = template.requestBody("activemq:queue:getVnfds", "", String.class);
		List<VxFMetadata> vxfs = null;
		// Map object to Infrastructure
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			vxfs = mapper.readValue(ret, new TypeReference<List<VxFMetadata>>() {
			});
			for (int i = 0; i < vxfs.size(); i++) {
				logger.info("VxFMetadata " + vxfs.get(i).getName() + " with VxF id:"
						+ vxfs.get(i).getUuid() + " is loaded");
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
		return vxfs;
	}

	public List<VxFOnBoardedDescriptor> getVxFOnBoardedDescriptors() {
		String ret = template.requestBody("activemq:queue:getVxFOnBoardedDescriptorListDataJson", "", String.class);
		List<VxFOnBoardedDescriptor> vxfobds = null;
		// Map object to Infrastructure
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			vxfobds = mapper.readValue(ret, new TypeReference<List<VxFOnBoardedDescriptor>>() {
			});
			for (int i = 0; i < vxfobds.size(); i++) {
				logger.info("VxFObdDescriptor " + vxfobds.get(i).getId() + " with Deploy id:"
						+ vxfobds.get(i).getDeployId() +','+ vxfobds.get(i).getDeployId() + " is loaded");
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
		return vxfobds;
	}

	public List<ExperimentOnBoardDescriptor> getExperimentOnBoardDescriptors() {
		String ret = template.requestBody("activemq:queue:getExperimentOnBoardDescriptorsDataJson", "", String.class);
		List<ExperimentOnBoardDescriptor> expobds = null;
		// Map object to Infrastructure
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			expobds = mapper.readValue(ret, new TypeReference<List<ExperimentOnBoardDescriptor>>() {
			});
			for (int i = 0; i < expobds.size(); i++) {
				logger.info("ExperimentObdDescriptor " + expobds.get(i).getId() + " with Deploy id:"
						+ expobds.get(i).getDeployId() +','+ expobds.get(i).getDeployId() + " is loaded");
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
		return expobds;
	}

	public List<ExperimentMetadata> getExperiments() {
		String ret = template.requestBody("activemq:queue:getExperiments", "", String.class);
		List<ExperimentMetadata> experiments = null;
		// Map object to Infrastructure
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			experiments = mapper.readValue(ret, new TypeReference<List<ExperimentMetadata>>() {
			});
			for (int i = 0; i < experiments.size(); i++) {
				logger.info("Experiment " + experiments.get(i).getName() + " with Experiment id:"
						+ experiments.get(i).getUuid() + " is loaded");
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
		return experiments;
	}

	public List<DeploymentDescriptor> getDeploymentsToInstantiate() {
		String ret = template.requestBody("activemq:queue:getDeploymentsToInstantiate", "", String.class);

		List<DeploymentDescriptor> DeploymentDescriptorsToRun = null;
		// Map object to Infrastructure
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			DeploymentDescriptorsToRun = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>() {
			});
			// Foreach deployment
			for (DeploymentDescriptor d : DeploymentDescriptorsToRun) {
				// Launch the deployment
				logger.info("The deployment with name  " + d.getName() + " is going to be instantiated");
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
		String ret = template.requestBody("activemq:queue:getDeploymentsToBeCompleted", "", String.class);

		List<DeploymentDescriptor> DeploymentDescriptorsToComplete = null;
		// Map object to DeploymentDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			DeploymentDescriptorsToComplete = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>() {
			});
			for (DeploymentDescriptor d : DeploymentDescriptorsToComplete) {
				// Launch the deployment
				logger.info("The deployment with name  " + d.getName() + " is going to be Completed");
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

	public List<DeploymentDescriptor> getAllDeployments() {
		String ret = template.requestBody("activemq:queue:getAllDeployments", "", String.class);

		List<DeploymentDescriptor> deploymentDescriptors = null;
		// Map object to DeploymentDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			deploymentDescriptors = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>() {
			});
			for (DeploymentDescriptor d : deploymentDescriptors) {
				// Launch the deployment
				logger.info("The deployment with name  " + d.getName() + " was found.");
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
		return deploymentDescriptors;
	}

	public List<DeploymentDescriptor> getDeploymentsToBeDeleted() {
		String ret = template.requestBody("activemq:queue:getDeploymentsToBeDeleted", "", String.class);
		logger.info("Deployments to be deleted: " + ret);
		List<DeploymentDescriptor> DeploymentDescriptorsToDelete = null;
		// Map object to DeploymentDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			DeploymentDescriptorsToDelete = mapper.readValue(ret, new TypeReference<List<DeploymentDescriptor>>() {
			});
			for (DeploymentDescriptor d : DeploymentDescriptorsToDelete) {
				// Launch the deployment
				logger.info("The deployment with name  " + d.getName() + " is going to be Deleted");
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
	public String getDeploymentEagerDataJson(DeploymentDescriptor d) throws JsonProcessingException {

		DeploymentDescriptor dd = this.getDeploymentByIdEager(d.getId());
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Hibernate5Module());
		String res = mapper.writeValueAsString(dd);

		return res;
	}

	// Get the data from the portal api (database)
	public DeploymentDescriptor getDeploymentByIdEager(long Id) {
		String ret = template.requestBody("activemq:queue:getDeploymentByIdEager", Id, String.class);
		logger.debug("Message Received from AMQ on activemq:queue:getDeploymentByIdEager("+Id+") call:" + ret);		
		// FluentProducerTemplate template = contxt.create
		// .createFluentProducerTemplate().to("activemq:queue:getRunningInstantiatingAndTerminatingDeployments?multipleConsumers=true");
		DeploymentDescriptor dd = null;
		// Map object to DeploymentDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			dd = mapper.readValue(ret, DeploymentDescriptor.class);
			if (dd.getExperiment() != null) {
				//dd.setExperiment(this.getExperimentById(dd.getExperiment().getId()));
				dd.setExperiment(this.getNSDById(dd.getExperiment().getId()));	
				
				logger.info("getDeploymentByIdEager: The experiment of the deployment is " + dd.getExperiment().getId());
			}
			dd.getObddescriptor_uuid();
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

	// Get the data from the portal api (database)
	public DeploymentDescriptor getDeploymentByInstanceIdEager(String Id) {
		String ret = template.requestBody("activemq:queue:getDeploymentByInstanceIdEager", Id, String.class);

		logger.debug("Message Received from AMQ:" + ret);
		DeploymentDescriptor dd = null;
		// Map object to DeploymentDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			dd = mapper.readValue(ret, DeploymentDescriptor.class);
			if (dd.getExperiment() != null) {
				//dd.setExperiment(this.getExperimentById(dd.getExperiment().getId()));
				dd.setExperiment(this.getNSDById(dd.getExperiment().getId()));
				logger.info("getDeploymentByInstanceIdEager: The experiment of the deployment is " + dd.getExperiment().toString());
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
		return dd;
	}

	public VxFMetadata getVxFById(long id) {
		logger.info("Trying to get the VxF with id: " + id);
		String ret = template.requestBody("activemq:queue:getVxFByID", id, String.class);
		logger.debug("Message Received from AMQ:" + ret);
		VxFMetadata vxfm = null;
		// Map object to VxFMetadata
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
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

	public VxFMetadata getVxFByUUid(String UUid) 
	{
		String ret = template.requestBody("activemq:queue:getVxFByUUIDDataJson", UUid, String.class);
		logger.debug("Message Received from AMQ:" + ret);
		VxFMetadata vxfm = null;
		// Map object to VxFMetadata
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
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
		String ret = template.requestBody("activemq:queue:getVxFByName", name, String.class);
		logger.debug("Message Received from AMQ:" + ret);
		VxFMetadata vxfm = null;
		// Map object to VxFMetadata
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
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
		String ret = template.requestBody("activemq:queue:getNSDByID", id, String.class);
		logger.debug("Message Received from AMQ on activemq:queue:getNSDByID call for id="+id+" :" + ret);
		ExperimentMetadata expm = null;
		// Map object to ExperimentMetadata
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
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

	// Duplicate of getNSDById
	//private ExperimentMetadata getExperimentById(long id) {
	//	String ret = template.requestBody("activemq:queue:getNSDByID", id, String.class);
	//	logger.debug("Message Received from AMQ:" + ret);
	//	ExperimentMetadata em = null;
	//	// Map object to ExperimentMetadata
	//	try {
	//		ObjectMapper mapper = new ObjectMapper();
	//		logger.debug("From ActiveMQ:" + ret.toString());
	//		em = mapper.readValue(ret, ExperimentMetadata.class);
	//	} catch (JsonParseException e) {
	//		// TODO Auto-generated catch block
	//		e.printStackTrace();
	//		logger.error(e.getMessage());
	//	} catch (JsonMappingException e1) {
	//		// TODO Auto-generated catch block
	//		e1.printStackTrace();
	//		logger.error(e1.getMessage());
	//	} catch (IOException e11) {
	//		// TODO Auto-generated catch block
	//		e11.printStackTrace();
	//		logger.error(e11.getMessage());
	//	}
	//	return em;
	//}

	public String getVxFOnBoardedDescriptorByVxFAndMP(String id, long mp) 
	{		
		String message = id+"##"+mp;
		logger.debug("getVxFOnBoardedDescriptorByVxFAndMP:Message Sent to AMQ:" + message);
		String ret = template.requestBody("activemq:queue:getVxFOnBoardedDescriptorByVxFAndMP", message, String.class);
		logger.debug("Message Received from AMQ:" + ret);
		logger.debug("From ActiveMQ:" + ret.toString());
		return ret.toString();
	}
	
	private VxFOnBoardedDescriptor getVxFOnBoardedDescriptorById(long id) {
		String ret = template.requestBody("activemq:queue:getVxFOBDByID", id, String.class);
		logger.debug("Message Received from AMQ:" + ret);
		VxFOnBoardedDescriptor vxfobd = null;
		// Map object to VxFOnBoardedDescriptor
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
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

	public VxFOnBoardedDescriptor updateVxFOnBoardedDescriptor(VxFOnBoardedDescriptor vxfobd2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String vxfobd_serialized = null;
		try {
			vxfobd_serialized = mapper.writeValueAsString(vxfobd2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + vxfobd_serialized + " to updateVxFOnBoardedDescriptor from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:updateVxFOnBoardedDescriptor", vxfobd_serialized,
				String.class);
		logger.debug("Message Received for updateVxFOnBoardedDescriptor from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		VxFOnBoardedDescriptor vxfd = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			vxfd = mapper.readValue(ret, VxFOnBoardedDescriptor.class);
			// vxfd.setExperiment(this.getExperimentById(vxfd.getExperiment().getId()));
			// logger.info("The experiment of the deployment is
			// "+vxfd.getExperiment().toString());
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

	public Infrastructure addInfrastructure(Infrastructure infrastructure2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String infrastructure_serialized = null;
		try {
			infrastructure_serialized = mapper.writeValueAsString(infrastructure2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + infrastructure_serialized + " to addInfrastructure from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:addInfrastructure", infrastructure_serialized, String.class);
		logger.debug("Message Received for addInstrastructure from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		Infrastructure infrastructure = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			infrastructure = mapper.readValue(ret, Infrastructure.class);
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
		return infrastructure;
	}

	public Infrastructure updateInfrastructure(Infrastructure infrastructure2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String infrastructure_serialized = null;
		try {
			infrastructure_serialized = mapper.writeValueAsString(infrastructure2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + infrastructure_serialized + " to updateInfrastructure from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:updateInfrastructure", infrastructure_serialized, String.class);
		logger.debug("Message Received for addInstrastructure from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		Infrastructure infrastructure = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			infrastructure = mapper.readValue(ret, Infrastructure.class);
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
		return infrastructure;
	}
	
	
	public PortalUser getPortalUserByUsername(String username) {
		
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		
		logger.debug("Sending Message " + username + " to getPortalUserByUsername from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:getPortalUserByUsername", username, String.class);
		logger.debug("Message Received for getPortalUserByUsername from AMQ:" + ret);

		PortalUser portaluser = null;
		try {
			// Map object to PortalUser
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			portaluser = mapper.readValue(ret, PortalUser.class);
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
		return portaluser;
	}	
	
	public VxFMetadata addVxFMetadata(VxFMetadata vxfmetadata2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String vxfmetadata_serialized = null;
		try {
			vxfmetadata_serialized = mapper.writeValueAsString(vxfmetadata2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + vxfmetadata_serialized + " to addVxFMetadata from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:addVxFMetadata", vxfmetadata_serialized, String.class);
		logger.debug("Message Received for addVxFMetadata from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		VxFMetadata vxfmetadata = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			vxfmetadata = mapper.readValue(ret, VxFMetadata.class);
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
		return vxfmetadata;
	}

	public ExperimentMetadata addExperimentMetadata(ExperimentMetadata experimentmetadata2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String experimentmetadata_serialized = null;
		try {
			experimentmetadata_serialized = mapper.writeValueAsString(experimentmetadata2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + experimentmetadata_serialized + " to addExperimentMetadata from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:addExperimentMetadata", experimentmetadata_serialized, String.class);
		logger.debug("Message Received for addExperimentMetadata from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		ExperimentMetadata experimentmetadata = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			experimentmetadata = mapper.readValue(ret, ExperimentMetadata.class);
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
		return experimentmetadata;
	}

	public VxFOnBoardedDescriptor addVxFOnBoardedDescriptor(VxFOnBoardedDescriptor vxfonboardeddescriptor2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String vxfonboardeddescriptor_serialized = null;
		try {
			vxfonboardeddescriptor_serialized = mapper.writeValueAsString(vxfonboardeddescriptor2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + vxfonboardeddescriptor_serialized + " to addVxFOnBoardedDescriptor from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:addVxFOnBoardedDescriptor", vxfonboardeddescriptor_serialized, String.class);
		logger.debug("Message Received for addVxFOnBoardedDescriptor from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		VxFOnBoardedDescriptor vxfonboardeddescriptor = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			vxfonboardeddescriptor = mapper.readValue(ret, VxFOnBoardedDescriptor.class);
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
		return vxfonboardeddescriptor;
	}

	public ExperimentOnBoardDescriptor addExperimentOnBoardedDescriptor(ExperimentOnBoardDescriptor experimentonboardeddescriptor2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String experimentonboardeddescriptor_serialized = null;
		try {
			experimentonboardeddescriptor_serialized = mapper.writeValueAsString(experimentonboardeddescriptor2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + experimentonboardeddescriptor_serialized + " to addExperimentOnBoardDescriptor from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:addExperimentOnBoardedDescriptor", experimentonboardeddescriptor_serialized, String.class);
		logger.debug("Message Received for addExperimentOnBoardedDescriptor from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		ExperimentOnBoardDescriptor experimentonboardeddescriptor = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			experimentonboardeddescriptor = mapper.readValue(ret, ExperimentOnBoardDescriptor.class);
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
		return experimentonboardeddescriptor;
	}

	public ExperimentOnBoardDescriptor updateExperimentOnBoardDescriptor(ExperimentOnBoardDescriptor expobd2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String expobd_serialized = null;
		try {
			expobd_serialized = mapper.writeValueAsString(expobd2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}
		logger.debug("Sending Message " + expobd_serialized + " to updateVxFOnBoardedDescriptor from AMQ:");
		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:updateExperimentOnBoardDescriptor", expobd_serialized,
				String.class);
		logger.debug("Message Received for updateExperimentOnBoardDescriptor from AMQ:" + ret);

		// Get the response and Map object to ExperimentMetadata
		ExperimentOnBoardDescriptor experimentobd = null;
		try {
			// Map object to VxFOnBoardedDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
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
	public DeploymentDescriptor updateDeploymentDescriptor(DeploymentDescriptor dd2send) {
		// Serialize the received object
		ObjectMapper mapper = new ObjectMapper();
		// Registering Hibernate4Module to support lazy objects
		// this will fetch all lazy objects of VxF before marshaling
		// mapper.registerModule(new Hibernate5Module());
		String dd_serialized = null;
		try {
			dd_serialized = mapper.writeValueAsString(dd2send);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			logger.error(e2.getMessage());
		}

		// Send it to activemq endpoint
		String ret = template.requestBody("activemq:queue:updateDeploymentDescriptor", dd_serialized, String.class);
		logger.debug("Message Received from AMQ:" + ret);
		DeploymentDescriptor dd = null;
		// Get the response and Map object to ExperimentMetadata
		try {
			// Map object to DeploymentDescriptor
			mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			dd = mapper.readValue(ret, DeploymentDescriptor.class);
			//dd.setExperiment(this.getExperimentById(dd.getExperiment().getId()));
			dd.setExperiment(this.getNSDById(dd.getExperiment().getId()));
			logger.debug("updateDeploymentDescriptor: The experiment of the deployment is " + dd.getExperiment().getId());
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

	public List<MANOprovider> getMANOproviders() {
		String ret = template.requestBody("activemq:queue:getMANOProviders", "", String.class);

		List<MANOprovider> mps = null;
		// Map object to MANOprovider
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			mps = mapper.readValue(ret, new TypeReference<List<MANOprovider>>() {
			});
			for (int i = 0; i < mps.size(); i++) {
				logger.info("Found EndPoint " + mps.get(i).getApiEndpoint());
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
		return mps;
	}

	public List<MANOprovider> getMANOprovidersForSync() {
		String ret = template.requestBody("activemq:queue:getMANOProvidersForSync", "", String.class);

		List<MANOprovider> mps = null;
		// Map object to MANOprovider
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			mps = mapper.readValue(ret, new TypeReference<List<MANOprovider>>() {
			});
			for (int i = 0; i < mps.size(); i++) {
				logger.info("Found EndPoint " + mps.get(i).getApiEndpoint());
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
		return mps;
	}

	// Update the data from the portal api (database)
	public MANOprovider getMANOproviderByID(long id) {
		String ret = template.requestBody("activemq:queue:getMANOProviderByID", id, String.class);
		// FluentProducerTemplate template = contxt.create
		// .createFluentProducerTemplate().to("activemq:queue:getRunningInstantiatingAndTerminatingDeployments?multipleConsumers=true");

		MANOprovider mp = null;
		// Map object to MANOprovider
		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.debug("From ActiveMQ:" + ret.toString());
			mp = mapper.readValue(ret, MANOprovider.class);
			logger.info("The MANOprovider with name " + mp.getName() + " has endpoint " + mp.getApiEndpoint());
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

	public void deleteExperiment(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.deployment.delete?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	// seda:nsd.deployment

	public void deploymentInstantiationSucceded(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.deployment.instantiation.success?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	public void deploymentInstantiationFailed(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.deployment.instantiation.fail?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	public void deploymentTerminationSucceded(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.deployment.termination.success?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	public void deploymentTerminationFailed(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.deployment.termination.fail?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	// seda:nsd.instance

	public void terminateInstanceSucceded(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.instance.termination.success?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	public void terminateInstanceFailed(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.instance.termination.fail?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	public void deleteInstanceSucceded(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.instance.deletion.success?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	public void deleteInstanceFailed(DeploymentDescriptor deploymentdescriptorid) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.instance.deletion.fail?multipleConsumers=true");
		template.withBody(deploymentdescriptorid).asyncSend();
	}

	/**
	 * Asynchronously sends to the routing bus
	 * (seda:vxf.onboard?multipleConsumers=true) to upload a new vxf
	 * 
	 * @param deployment a {@link VxFMetadata}
	 */
//	public void onBoardVxFAdded(VxFOnBoardedDescriptor obd) {
//		FluentProducerTemplate template = contxt.createFluentProducerTemplate().to("seda:vxf.onboard?multipleConsumers=true");
//		template.withBody( obd ).asyncSend();				
//	}

	public void onBoardVxFFailed(VxFOnBoardedDescriptor vxfobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:vxf.onboard.fail?multipleConsumers=true");
		template.withBody(vxfobds_final).asyncSend();
	}

	public void onBoardVxFSucceded(VxFOnBoardedDescriptor vxfobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:vxf.onboard.success?multipleConsumers=true");
		template.withBody(vxfobds_final).asyncSend();
	}

	public void onBoardNSDFailed(ExperimentOnBoardDescriptor experimentobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.onboard.fail?multipleConsumers=true");
		template.withBody(experimentobds_final).asyncSend();
	}

	public void onBoardNSDSucceded(ExperimentOnBoardDescriptor experimentobds_final) {
		FluentProducerTemplate template = contxt.createFluentProducerTemplate()
				.to("seda:nsd.onboard.success?multipleConsumers=true");
		template.withBody(experimentobds_final).asyncSend();
	}

	/**
	 * 
	 * Compare previous last known action with the last one. We ignore any
	 * intermediate actions
	 * 
	 * @param deployment_tmp
	 * @param previous
	 * @param current
	 * @return
	 */
	public String alertOnScaleOpsList(DeploymentDescriptor deployment_tmp, String previous, String current) {

		try {

			JSONObject prevObj = new JSONObject(previous);

			JSONArray array2 = new JSONArray(current);
			JSONObject currentLastObj = array2.getJSONObject(array2.length() - 1);

			if (!prevObj.get("id").equals(currentLastObj.get("id"))) {
				JSONObject obj2 = currentLastObj;
				if ((obj2.get("lcmOperationType").equals("scale")) && (!obj2.get("operationState").equals("FAILED"))) {

					logger.debug(
							"Sending An AlertCreate with details the body " + obj2.get("operationParams").toString());

					try {
						AlarmCreate a = new AlarmCreate();
						a.setPerceivedSeverity(PerceivedSeverityType.critical.name());
						a.setState(AlarmStateType.raised.name());
						a.setAckState("unacknowledged");
						a.setAlarmRaisedTime(OffsetDateTime.now(ZoneOffset.UTC).toString());
						a.setSourceSystemId(compname);
						a.setAffectedService(new ArrayList<>());
						a.setAlarmType(AlarmType.qualityOfServiceAlarm.name());
						a.setIsRootCause(true);
						a.setProbableCause(ProbableCauseType.thresholdCrossed.name());
						String scaletype = "";
						if (obj2.toString().contains("SCALE_IN")) {
							scaletype = "SCALE_IN";
						} else {
							scaletype = "SCALE_OUT";
						}
						a.setAlarmDetails("DeploymentRequestID=" + deployment_tmp.getId() + ";" + "InstanceId="
								+ deployment_tmp.getInstanceId() + ";" + "scaletype=" + scaletype);
						Comment comment = new Comment();
						comment.setTime(OffsetDateTime.now(ZoneOffset.UTC));
						comment.setSystemId(compname);

						a.setSpecificProblem("action=" + scaletype);

						comment.setComment("Scale Operation " + scaletype + ". " + a.getAlarmDetails());
						a.addCommentItem(comment);

						String response = alarmsService.createAlarm(a);

						logger.debug("Message sent to AlertCreate response=" + response);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				return currentLastObj.toString();

			}
		} catch (JSONException e) {
			logger.info("Crashed during alertOnScaleOpsList" + e.getMessage());
		}
		return previous;
	}
	

	
	/**
	 * @param deployment_tmp 
	 * @return 
	 */
	public String notifyOnLCMChanged(DeploymentDescriptor deployment_tmp) {
		
		
		String body;
		try {
			body = toJsonString( deployment_tmp );
			logger.info("notifyOnLCMChanged create  body = " + body);
			Object response = template.requestBody( NFV_CATALOG_NS_LCMCHANGED, body);
			return response.toString();
		} catch (IOException e) {
			logger.error("Message failed notifyOnLCMChanged");
			e.printStackTrace();
		}
		return null;		
	}

	static String toJsonString(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}
}
