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

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import OSM5NBIClient.OSM5Client;
import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.DeploymentDescriptorStatus;
import io.openslice.model.ExperimentMetadata;
import io.openslice.model.ExperimentOnBoardDescriptor;
import io.openslice.model.MANOprovider;
import io.openslice.model.OnBoardDescriptor;
import io.openslice.model.OnBoardingStatus;
import io.openslice.model.VxFMetadata;
import io.openslice.model.VxFOnBoardedDescriptor;



/**
 * @author ctranoris
 *
 */

@Configuration
public class MANOController {

	/** */
	private static final transient Log logger = LogFactory.getLog(MANOController.class.getName());
	@Autowired
	private MANOClient aMANOClient;
	

//	@Autowired
//	VxFOBDService vxfOBDService;
//	
//	@Autowired
//	VxFService vxfService;
//
//	@Autowired
//	DeploymentDescriptorService deploymentDescriptorService;
//	
//	@Autowired
//	ManoProviderService manoProviderService;
//	
//	@Autowired
//	NSDService nsdService;
//
//	@Autowired
//	NSDOBDService nsdOBDService;
//
//	@Autowired
//	PortalPropertiesService propsService;
//	
//	public MANOController() {
//
//	}
//	
//	@Bean("aMANOController")
//	public MANOController aMANOControllerBean() {
//		return new MANOController();
//	}
//
////	private static String HTTP_SCHEME = "https:";
//
////	public static void setHTTPSCHEME(String url) {
////		logger.info("setHTTPSCHEME url = " + url);
//////		if (url.contains("localhost")) {
//////			HTTP_SCHEME = "http:";
//////		}
////		// HTTP_SCHEME = url + ":";
////	}
//
//	
//	/**
//	 * onBoard a VNF to MANO Provider, as described by this descriptor
//	 * 
//	 * @param vxfobds
//	 * @throws Exception
//	 */
//	public void onBoardVxFToMANOProvider( long vxfobdid ) throws Exception {
//
//		if (vxfOBDService == null) {
//			throw new Exception("vxfOBDService is NULL. Cannot load VxFOnBoardedDescriptor");
//		}
//		
//		VxFOnBoardedDescriptor vxfobd = vxfOBDService.getVxFOnBoardedDescriptorByID(vxfobdid);
//		// PortalRepository portalRepositoryRef = new PortalRepository();
//
//		if (vxfobd == null) {
//			throw new Exception("vxfobd is NULL. Cannot load VxFOnBoardedDescriptor");
//		}
//		
//		if (vxfobd.getVxf() == null) {
//			throw new Exception("vxfobd.getVxf() is NULL. Cannot load VxFOnBoardedDescriptor");
//		}
//		
//		if (vxfobd.getVxf().getName() == null) {
//			throw new Exception("vxfobd.getVxf() is NULL. Cannot load VxFOnBoardedDescriptor");
//		}
//		
//		
//		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
//		// This is the Deployment ID for the portal
//		vxfobd.setDeployId(UUID.randomUUID().toString());
////		VxFMetadata vxf = vxfobd.getVxf();
////		if (vxf == null) {
////			vxf = (VxFMetadata) vxfService.getProductByID(vxfobd.getVxfid());
////		}
//		CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobd.getVxf().getName()+" to "+vxfobd.getOnBoardingStatus());						
//		// Set MANO Provider VxF ID
//		vxfobd.setVxfMANOProviderID( vxfobd.getVxf().getName());
//		// Set onBoarding Date
//		vxfobd.setLastOnboarding(new Date());
//
//		VxFOnBoardedDescriptor vxfobds = vxfOBDService.updateVxFOnBoardedDescriptor(vxfobd);
//		if (vxfobds == null) {
//			throw new Exception("Cannot load VxFOnBoardedDescriptor");
//		}
//
//
//		String pLocation = vxfobd.getVxf().getPackageLocation();
//		logger.info("VxF Package Location: " + pLocation);
//
//		if (!pLocation.contains("http")) {
//			pLocation = propsService.getPropertyByName( "maindomain" ).getValue() + pLocation;
//		}
////		if (!pLocation.contains("http")) {
////			pLocation = "http:" + pLocation;
////			pLocation = pLocation.replace("\\", "/");
////		}					
//		logger.info("PROPER VxF Package Location: " + pLocation);
//
//		
//		// OSM5 START
//		if (vxfobds.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FIVE")) {
//			OSM5Client osm5Client = null;
//			try {
//				osm5Client = new OSM5Client(vxfobds.getObMANOprovider().getApiEndpoint(), vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(), "admin");
//				MANOStatus.setOsm5CommunicationStatusActive(null);													
//			}
//		    catch(Exception e) 
//			{
//				logger.error("onBoardNSDFromMANOProvider, OSM5 fails authentication. Aborting action.");
//				
//				CentralLogger.log( CLevel.INFO, "onBoardNSDFromMANOProvider, OSM5 fails authentication. Aborting action.");
//				MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF OnBoarding action.");																	
//				// Set the reason of the failure
//				vxfobds.setFeedbackMessage("OSM5 communication failed. Aborting VxF OnBoarding action.");
//				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
//				CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus());										
//				// Uncertify if it failed OnBoarding.
//				vxfobds.getVxf().setCertified(false);
//				VxFOnBoardedDescriptor vxfobds_final = vxfOBDService.updateVxFOnBoardedDescriptor(vxfobds);
//				
//				// Uncertify if it failed OnBoarding.
//				BusController.getInstance().onBoardVxFFailed( vxfobds_final );				
//		        return ;
//			}						
//			
//			ResponseEntity<String> response = null;
//			response = osm5Client.createVNFDPackage();
//			if (response == null || response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
//				logger.error("VNFD Package Creation failed.");
//				// Set status
//				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
//				CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus());										
//				// Set the reason of the failure
//				vxfobds.setFeedbackMessage(response.getBody().toString());
//				// Uncertify if it failed OnBoarding.
//				vxfobds.getVxf().setCertified(false);
//				VxFOnBoardedDescriptor vxfobds_final = vxfOBDService.updateVxFOnBoardedDescriptor(vxfobds);
//				BusController.getInstance().onBoardVxFFailed( vxfobds_final );
//				return;				
//			}
//			else
//			{
//				JSONObject obj = new JSONObject(response.getBody());
//				String vnfd_id = obj.getString("id");
//				logger.info(response.getStatusCode()+" replied. The new VNFD Package id is :" + vnfd_id);
//				
//				response = osm5Client.uploadVNFDPackageContent(vnfd_id, pLocation);
//				if (response == null || response.getStatusCode().is4xxClientError()
//						|| response.getStatusCode().is5xxServerError()) {
//					logger.error("Upload of VNFD Package Content failed. Deleting VNFD Package.");
//					// Delete the package from the OSM
//					osm5Client.deleteVNFDPackage(vnfd_id);
//					// Set status
//					vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
//					CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus());											
//					// Set the reason of the failure
//					vxfobds.setFeedbackMessage(response.getBody().toString());
//					// Uncertify if it failed OnBoarding.
//					vxfobds.getVxf().setCertified(false);
//					VxFOnBoardedDescriptor vxfobds_final = vxfOBDService.updateVxFOnBoardedDescriptor(vxfobds);
//					BusController.getInstance().onBoardVxFFailed( vxfobds_final );
//					return;
//				}
//
//				vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
//				CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus());											
//				
//				vxfobds.setFeedbackMessage("OnBoarding Succeeded");
//				// We select by design not to Certify upon OnBoarding but only on final version
//				// is determined.
//				// vxfobds.getVxf().setCertified(true);
//				// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
//				vxfobds.setDeployId(vnfd_id);
//				// What should be the vxf Name. Something like cirros_vnfd.
//				vxfobds.setVxfMANOProviderID( vxfobd.getVxf().getName() );
//				// Set Onboarding date
//				vxfobds.setLastOnboarding(new Date());
//				// Save the changes to vxfobds
//				VxFOnBoardedDescriptor vxfobds_final = vxfOBDService.updateVxFOnBoardedDescriptor(vxfobds);
//				BusController.getInstance().onBoardVxFSucceded( vxfobds_final );
//				
//			}			
//		}		
//		// OSM5 END
//		// OSM4 START		
//		// OSM4 END
//	}

//	public void checkAndDeleteTerminatedOrFailedDeployments() {
//		logger.info("Check and Delete Terminated and Failed Deployments");
//		List<DeploymentDescriptor> DeploymentDescriptorsToDelete = aMANOClient.getDeploymentsToBeDeleted();
//		for (DeploymentDescriptor d : DeploymentDescriptorsToDelete) {
//			// Launch the deployment
//			logger.info("Send to bus control to delete: " + d.getId());
//			aMANOClient.deleteExperiment(d);
//		}
//	}
	
//	public void checkAndDeployExperimentToMANOProvider() {
//		logger.info("This will trigger the check and Deploy Experiments");
//		// Check the database for a new deployment in the next minutes
//		// If there is a deployment to be made and the status is Scheduled
//		List<DeploymentDescriptor> DeploymentDescriptorsToRun = aMANOClient.getDeploymentsToInstantiate();
//		// Foreach deployment found, start the instantiation
//		for (DeploymentDescriptor d : DeploymentDescriptorsToRun) {
//			// Launch the deployment
//			logger.info("Send to bus control to deploy: " + d.getId());
//			aMANOClient.deployExperiment(d);
//		}
//	}
//
	public void checkAndTerminateExperimentToMANOProvider() {
		logger.info("This will trigger the check and Terminate Deployments");
		// Check the database for a deployment to be completed in the next minutes
		// If there is a deployment to be made and the status is Scheduled
		List<DeploymentDescriptor> DeploymentDescriptorsToComplete = aMANOClient.getDeploymentsToBeCompleted();
		// For each deployment
		for (DeploymentDescriptor deployment_descriptor_tmp : DeploymentDescriptorsToComplete) {
			logger.debug("Deployment with id" + deployment_descriptor_tmp.getName() + " with status " + deployment_descriptor_tmp.getStatus() +" is going to be terminated");
			
			// Terminate the deployment
			this.terminateNSFromMANOProvider(deployment_descriptor_tmp.getId() );
		}
	}

	public void checkAndDeleteTerminatedOrFailedDeployments() {
		logger.info("Check and Delete Terminated and Failed Deployments");
		List<DeploymentDescriptor> DeploymentDescriptorsToDelete = aMANOClient.getDeploymentsToBeDeleted();
		for (DeploymentDescriptor d : DeploymentDescriptorsToDelete) {
			// Launch the deployment
			logger.info("Send to bus control to delete: " + d.getId());
			this.deleteNSFromMANOProvider(d.getId());
		}
	}

	public void checkAndDeployExperimentToMANOProvider() {
		logger.info("This will trigger the check and Deploy Experiments");
		// Check the database for a new deployment in the next minutes
		// If there is a deployment to be made and the status is Scheduled
		List<DeploymentDescriptor> DeploymentDescriptorsToRun = aMANOClient.getDeploymentsToInstantiate();
		// Foreach deployment found, start the instantiation
		for (DeploymentDescriptor d : DeploymentDescriptorsToRun) {
			// Launch the deployment
			logger.info("Send to bus control to deploy: " + d.getId());
			this.deployNSDToMANOProvider(d.getId());
		}
	}
	
	public void checkAndUpdateRunningDeploymentDescriptors() {
		logger.info("Update Deployment Descriptors");	
		try
		{
			List<DeploymentDescriptor> runningDeploymentDescriptors = aMANOClient.getRunningInstantiatingAndTerminatingDeployments();
			for(DeploymentDescriptor nsd : runningDeploymentDescriptors)
			{
				System.out.println("NSD name:"+nsd.getName());
			}
			OSM5Client osm5Client = null;
			// For each deployment get the status info and the IPs
			for (int i = 0; i < runningDeploymentDescriptors.size(); i++) {
				DeploymentDescriptor deployment_tmp = aMANOClient.getDeploymentByIdEager(runningDeploymentDescriptors.get(i).getId());
				deployment_tmp.getExperimentFullDetails();
				try {
					// Get the MANO Provider for each deployment			
					long tmp_MANOprovider_id = getExperimOBD( deployment_tmp ).getObMANOprovider().getId();
					MANOprovider sm = aMANOClient.getMANOproviderByID( tmp_MANOprovider_id );
					
					//OSM5 - START
					if (sm.getSupportedMANOplatform().getName().equals("OSM FIVE")) {
						if (osm5Client == null || !osm5Client.getMANOApiEndpoint().equals(sm.getApiEndpoint())) {
							try
							{
								osm5Client = new OSM5Client(sm.getApiEndpoint(), sm.getUsername(), sm.getPassword(), "admin");							
	//							MANOStatus.setOsm5CommunicationStatusActive(null);
							}
							catch(Exception e)
							{
								logger.error("OSM5 fails authentication");
								//CentralLogger.log( CLevel.ERROR, "OSM5 fails authentication");							
	//							MANOStatus.setOsm5CommunicationStatusFailed(null);
								return;
							}
						}
						JSONObject ns_instance_info = osm5Client.getNSInstanceInfo(deployment_tmp.getInstanceId());
						// If the no nsd with the specific id is found, mark the instance as faile to delete.
						if(ns_instance_info == null)
						{
							deployment_tmp.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);
							//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
							logger.info("NS not found in OSM. Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());						
							deployment_tmp.setFeedback("NS instance not present in OSM. Marking as FAILED_OSM_REMOVED");	
							logger.info("Update DeploymentDescriptor Object in 363");							
							DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
							logger.info("NS status change is now "+deploymentdescriptor_final.getStatus());						
							aMANOClient.deleteInstanceFailed(deploymentdescriptor_final);										
						}
						else 
						{
							try {
								logger.info(ns_instance_info.toString());
								if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.RUNNING)
								{
									if(!deployment_tmp.getOperationalStatus().equals(ns_instance_info.getString("operational-status"))||!deployment_tmp.getConfigStatus().equals(ns_instance_info.getString("config-status"))||!deployment_tmp.getDetailedStatus().equals(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", "")))
									{
										logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
										//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
										deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
										deployment_tmp.setConstituentVnfrIps("");
										for (int j = 0; j < ns_instance_info.getJSONArray("constituent-vnfr-ref")
												.length(); j++) 
										{
											if (j > 0) {
												deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps() + ", ");
											}
											ResponseEntity<String> vnf_instance_id_info_response = osm5Client.getVNFInstanceInfoNew(ns_instance_info.getJSONArray("constituent-vnfr-ref").get(j).toString());
											if(!vnf_instance_id_info_response.getStatusCode().is4xxClientError() && !vnf_instance_id_info_response.getStatusCode().is5xxServerError() )
											{
												JSONObject vnf_instance_info = new JSONObject(vnf_instance_id_info_response.getBody());
												try {
													deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps()
															+ vnf_instance_info.getString("ip-address"));
												} catch (JSONException e) {
													logger.error(e.getMessage());
												}																				
											}
											else
											{
												deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps()	+ "Ν/Α");
												logger.error("ERROR gettin constituent-vnfr-ref info. Response:"+vnf_instance_id_info_response.getBody().toString());
											}									
										}
										deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
										aMANOClient.deploymentInstantiationSucceded(deployment_tmp );									
									}
								}
								
								deployment_tmp.setOperationalStatus(ns_instance_info.getString("operational-status"));
								deployment_tmp.setConfigStatus(ns_instance_info.getString("config-status"));
								deployment_tmp.setDetailedStatus(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
								// Depending on the current OSM status, change the portal status.
								if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.INSTANTIATING
										&& deployment_tmp.getOperationalStatus().toLowerCase().equals("running")) 
								{
									deployment_tmp.setStatus(DeploymentDescriptorStatus.RUNNING);
									logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
									//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
									deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
									deployment_tmp.setConstituentVnfrIps("");
									for (int j = 0; j < ns_instance_info.getJSONArray("constituent-vnfr-ref")
											.length(); j++) 
									{
										if (j > 0) {
											deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps() + ", ");
										}
										ResponseEntity<String> vnf_instance_id_info_response = osm5Client.getVNFInstanceInfoNew(ns_instance_info.getJSONArray("constituent-vnfr-ref").get(j).toString());
										if(!vnf_instance_id_info_response.getStatusCode().is4xxClientError() && !vnf_instance_id_info_response.getStatusCode().is5xxServerError() )
										{
											JSONObject vnf_instance_info = new JSONObject(vnf_instance_id_info_response.getBody());
											try {
												deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps()
														+ vnf_instance_info.getString("ip-address"));
											} catch (JSONException e) {
												logger.error(e.getMessage());
											}																				
										}
										else
										{
											deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps()	+ "Ν/Α");
											logger.error("ERROR gettin constituent-vnfr-ref info. Response:"+vnf_instance_id_info_response.getBody().toString());
											//break;
										}									
									}
									deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
									aMANOClient.deploymentInstantiationSucceded(deployment_tmp);
								}
								// deployment_tmp.getStatus() == DeploymentDescriptorStatus.TERMINATING &&
								if (deployment_tmp.getOperationalStatus().toLowerCase().equals("terminated")) {
									// This message changes in OSM5 from "terminating" to "terminated"
									//&& deployment_tmp.getConfigStatus().toLowerCase().equals("terminated")
									//&& deployment_tmp.getDetailedStatus().toLowerCase().equals("done")) {
									deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
									deployment_tmp.setStatus(DeploymentDescriptorStatus.TERMINATED);
									//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());								
									logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
									deployment_tmp.setConstituentVnfrIps("N/A");
									deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
									aMANOClient.deploymentTerminationSucceded(deployment_tmp);
								}
								// if(deployment_tmp.getStatus() != DeploymentDescriptorStatus.FAILED &&
								// deployment_tmp.getOperationalStatus().equals("failed"))
								if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.INSTANTIATING
										&& deployment_tmp.getOperationalStatus().equals("failed")) {
									deployment_tmp.setStatus(DeploymentDescriptorStatus.FAILED);
									//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());								
									logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
									deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
									deployment_tmp.setConstituentVnfrIps("N/A");
									deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
									aMANOClient.deploymentInstantiationFailed(deployment_tmp);
								}
								if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.TERMINATING
										&& deployment_tmp.getOperationalStatus().equals("failed")) {
									deployment_tmp.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);
									//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());								
									logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
									deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
									deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
									aMANOClient.deploymentTerminationFailed(deployment_tmp);
								}
								logger.info("NS status change is now "+deployment_tmp.getStatus());													
							} catch (JSONException e) {
								logger.error(e.getMessage());
							}
						}
					}
					//OSM5 - END												
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
			checkAndDeployExperimentToMANOProvider();
			checkAndTerminateExperimentToMANOProvider();
			checkAndDeleteTerminatedOrFailedDeployments();
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
		
	}
	
	private ExperimentOnBoardDescriptor getExperimOBD(DeploymentDescriptor deployment_tmp) {			
		for (ExperimentOnBoardDescriptor e : deployment_tmp.getExperimentFullDetails().getExperimentOnBoardDescriptors()) {

			return e; //return the first one found
		}
		return null;
	}

//	public void onBoardNSDToMANOProvider( long uexpobdid ) throws Exception {
//		ExperimentOnBoardDescriptor uexpobd = nsdOBDService.getExperimentOnBoardDescriptorByID(uexpobdid);
//		
//		if (uexpobd == null) {
//			throw new Exception("uexpobd is NULL. Cannot load VxFOnBoardedDescriptor");
//		}
//		
//		
//		uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
//		CentralLogger.log( CLevel.INFO, "Onboarding status change of Experiment "+uexpobd.getExperiment().getName()+" to "+uexpobd.getOnBoardingStatus());													
//		// This is the Deployment ID for the portal
//		uexpobd.setDeployId(UUID.randomUUID().toString());
//		ExperimentMetadata em = uexpobd.getExperiment();
//		if (em == null) {
//			em = (ExperimentMetadata) nsdService.getProductByID(uexpobd.getExperimentid());
//		}
//
//		/**
//		 * The following is not OK. When we submit to OSMClient the createOnBoardPackage
//		 * we just get a response something like response = {"output":
//		 * {"transaction-id": "b2718ef9-4391-4a9e-97ad-826593d5d332"}} which does not
//		 * provide any information. The OSM RIFTIO API says that we could get
//		 * information about onboarding (create or update) jobs see
//		 * https://open.riftio.com/documentation/riftware/4.4/a/api/orchestration/pkt-mgmt/rw-pkg-mgmt-download-jobs.htm
//		 * with /api/operational/download-jobs, but this does not return pending jobs.
//		 * So the only solution is to ask again OSM if something is installed or not, so
//		 * for now the client (the portal ) must check via the
//		 * getVxFOnBoardedDescriptorByIdCheckMANOProvider giving the VNF ID in OSM. OSM
//		 * uses the ID of the yaml description Thus we asume that the vxf name can be
//		 * equal to the VNF ID in the portal, and we use it for now as the OSM ID. Later
//		 * in future, either OSM API provide more usefull response or we extract info
//		 * from the VNFD package
//		 * 
//		 */
//
//		uexpobd.setVxfMANOProviderID(em.getName()); // Possible Error. This probably needs to be
//													// setExperimentMANOProviderID(em.getName())
//
//		uexpobd.setLastOnboarding(new Date());
//
//		ExperimentOnBoardDescriptor uexpobds = nsdOBDService.updateExperimentOnBoardDescriptor(uexpobd);
//		if (uexpobds == null) {
//			throw new Exception("Cannot load NSDOnBoardedDescriptor");
//		}
//
//		String pLocation = em.getPackageLocation();
//		logger.info("NSD Package Location: " + pLocation);
//		if (!pLocation.contains("http")) {
//			pLocation = propsService.getPropertyByName( "maindomain" ).getValue() + pLocation;
//		}
////		if (!pLocation.contains("http")) {
////			pLocation = "http:" + pLocation;
////			pLocation = pLocation.replace("\\", "/");
////		}				
//
//	
//		
//		// OSM5 - START
//		if (uexpobds.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FIVE")) {
//			ResponseEntity<String> response = null;
//			OSM5Client osm5Client = null;
//			try {
//				osm5Client = new OSM5Client(uexpobd.getObMANOprovider().getApiEndpoint(), uexpobd.getObMANOprovider().getUsername(), uexpobd.getObMANOprovider().getPassword(), "admin");
//				MANOStatus.setOsm5CommunicationStatusActive(null);								
//			}
//		    catch(Exception e) 
//			{
//				logger.error("onBoardNSDFromMANOProvider, OSM5 fails authentication. Aborting action.");
//				CentralLogger.log( CLevel.ERROR, "onBoardNSDFromMANOProvider, OSM5 fails authentication. Aborting NSD Onboarding action.");
//				MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD Onboarding action.");				
//				// Set the reason of the failure
//				uexpobds.setFeedbackMessage("OSM communication failed. Aborting NSD Onboarding action.");
//				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
//				CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());																	
//				// Set Valid to false if it fails OnBoarding
//				uexpobds.getExperiment().setValid(false);
//				ExperimentOnBoardDescriptor uexpobd_final = nsdOBDService.updateExperimentOnBoardDescriptor(uexpobds);
//				BusController.getInstance().onBoardNSDFailed( uexpobds );
//				return ;
//			}						
//			
//			response = osm5Client.createNSDPackage();
//			if (response == null || response.getStatusCode().is4xxClientError()
//					|| response.getStatusCode().is5xxServerError()) {
//				logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
//				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
//				CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());																	
//				// Set the reason of the failure
//				uexpobds.setFeedbackMessage(response.getBody().toString());
//				// Set Valid to false if it fails OnBoarding
//				uexpobds.getExperiment().setValid(false);
//				ExperimentOnBoardDescriptor uexpobd_final = nsdOBDService.updateExperimentOnBoardDescriptor(uexpobds);
//				BusController.getInstance().onBoardNSDFailed( uexpobds );
//				return;				
//			}
//			else
//			{
//				JSONObject obj = new JSONObject(response.getBody());
//				String nsd_id = obj.getString("id");
//				logger.info(response.getStatusCode()+" replied. The new NSD Package id is :" + nsd_id);
//				logger.error("Uploading NSD Archive from URL " + pLocation);
//				logger.error("************************");
//				response = osm5Client.uploadNSDPackageContent(nsd_id, pLocation);
//				if (response == null || response.getStatusCode().is4xxClientError()
//						|| response.getStatusCode().is5xxServerError()) {
//					logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
//					osm5Client.deleteNSDPackage(nsd_id);
//					uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
//					CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());																						
//					uexpobds.setFeedbackMessage(response.getBody().toString());
//					// Set Valid to false if it fails OnBoarding
//					uexpobds.getExperiment().setValid(false);
//					ExperimentOnBoardDescriptor uexpobd_final = nsdOBDService.updateExperimentOnBoardDescriptor(uexpobds);
//					BusController.getInstance().onBoardNSDFailed( uexpobds );
//					return;
//				}
//				else
//				{
//					uexpobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
//					CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());																						
//					uexpobds.setFeedbackMessage("NSD Onboarded Successfully");
//					// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
//					uexpobds.setDeployId(nsd_id);
//					// What should be the NSD Name. Something like cirros_nsd.
//					uexpobds.setExperimentMANOProviderID(em.getName());
//					// Set Onboarding date
//					uexpobds.setLastOnboarding(new Date());
//					// We decide to set valid when we have the final version. Thus we comment this.
//					// uexpobds.getExperiment().setValid(true);
//					// Save the changes to vxfobds
//					ExperimentOnBoardDescriptor uexpobd_final = nsdOBDService.updateExperimentOnBoardDescriptor(uexpobds);
//					BusController.getInstance().onBoardNSDSucceded( uexpobds );
//				}
//			}
//		}
//		// OSM5 - END
//	}
//
//	/**
//	 * offBoard a VNF to MANO Provider, as described by this descriptor
//	 * 
//	 * @param c
//	 */
//	public ResponseEntity<String> offBoardVxFFromMANOProvider(VxFOnBoardedDescriptor obd)
//			throws HttpClientErrorException {
//		// TODO Auto-generated method stub
//		ResponseEntity<String> response = null;
//		// OSM5 START
//		if (obd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FIVE")) {
//			String vnfd_id = obd.getDeployId();
//			OSM5Client osm5Client = null;			
//			try {
//				osm5Client = new OSM5Client(obd.getObMANOprovider().getApiEndpoint(), obd.getObMANOprovider().getUsername(), obd.getObMANOprovider().getPassword(), "admin");
//				MANOStatus.setOsm5CommunicationStatusActive(null);								
//			}
//		    catch(HttpStatusCodeException e) 
//			{
//				logger.error("offBoardVxFFromMANOProvider, OSM5 fails authentication. Aborting action.");
//				CentralLogger.log( CLevel.ERROR, "offBoardVxFFromMANOProvider, OSM5 fails authentication. Aborting VxF offboarding action.");
//				MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF offboarding action.");								
//		        return ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders())
//		                .body(e.getResponseBodyAsString());
//			}						
//			
//			response = osm5Client.deleteVNFDPackage(vnfd_id);
//		}
//		// OSM5 END
//		if (obd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM TWO")) {
//			response = new ResponseEntity<>("Not implemented for OSMvTWO", HttpStatus.CREATED);
//		}
//		return response;
//	}
//
//	private void checkVxFStatus(VxFOnBoardedDescriptor obd) throws Exception {
//
//		CamelContext tempcontext = new DefaultCamelContext();
//		MANOController mcontroller = this;
//		try {
//			RouteBuilder rb = new RouteBuilder() {
//				@Override
//				public void configure() throws Exception {
//					from("timer://getVNFRepoTimer?delay=2000&period=3000&repeatCount=6&daemon=true")
//							.log("Will check VNF repo").setBody().constant(obd)
//							.bean(mcontroller, "getVxFStatusFromOSM2Client");
//				}
//			};
//			tempcontext.addRoutes(rb);
//			tempcontext.start();
//			Thread.sleep(30000);
//		} finally {
//			tempcontext.stop();
//		}
//
//	}
//
//	
//	public ResponseEntity<String> offBoardNSDFromMANOProvider(ExperimentOnBoardDescriptor uexpobd) {
//		// TODO Auto-generated method stub
//		ResponseEntity<String> response = null;
//		
//		// OSM5 START
//		if (uexpobd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FIVE")) {
//			String nsd_id = uexpobd.getDeployId();
//			OSM5Client osm5Client = null;			
//			try {
//				osm5Client = new OSM5Client(uexpobd.getObMANOprovider().getApiEndpoint(), uexpobd.getObMANOprovider().getUsername(), uexpobd.getObMANOprovider().getPassword(), "admin");
//				MANOStatus.setOsm5CommunicationStatusActive(null);								
//			}
//		    catch(HttpStatusCodeException e) 
//			{
//				logger.error("offBoardNSDFromMANOProvider, OSM5 fails authentication. Aborting action.");
//				CentralLogger.log( CLevel.ERROR, "offBoardNSDFromMANOProvider, OSM5 fails authentication. Aborting action.");
//				MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD offboarding action.");								
//		        return ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders())
//		                .body(e.getResponseBodyAsString());
//			}						
//			response = osm5Client.deleteNSDPackage(nsd_id);
//		}
//		// OSM5 END
//		
//		// return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
//		return response;
//	}
//

	public void deployNSDToMANOProvider(long deploymentdescriptorid) {
		logger.info("Starting deployNSDToMANOProvicer");
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);		
		// OSM5 - START
		ExperimentOnBoardDescriptor tmp = getExperimOBD(deploymentdescriptor);
		if ( tmp.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FIVE")) {
			// There can be multiple MANOs for the Experiment. We need to handle that also.
			OSM5Client osm5Client = null;
			try {
				 osm5Client = new OSM5Client(
						getExperimOBD(deploymentdescriptor).getObMANOprovider().getApiEndpoint(),
						getExperimOBD(deploymentdescriptor).getObMANOprovider().getUsername(),
						getExperimOBD(deploymentdescriptor).getObMANOprovider().getPassword(),
						"admin");
					//MANOStatus.setOsm5CommunicationStatusActive(null);													
			}
			catch(Exception e)
			{
				logger.error("deployNSDToMANOProvider, OSM5 fails authentication! Aborting deployment of NSD.");
				//CentralLogger.log( CLevel.ERROR, "deployNSDToMANOProvider, OSM5 fails authentication! Aborting deployment of NSD.");
				//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting deployment of NSD.");								
				// NS instance creation failed
				deploymentdescriptor.setFeedback("OSM5 communication failed. Aborting NSD deployment action.");
				aMANOClient.deploymentInstantiationFailed(deploymentdescriptor);
				return;
			}

			NSCreateInstanceRequestPayload nscreateinstancerequestpayload = new NSCreateInstanceRequestPayload(deploymentdescriptor);
			// Get Experiment ID and VIM ID and create NS Instance.
			logger.info("NS Instance creation payload : " + nscreateinstancerequestpayload.toJSON());
			ResponseEntity<String> ns_instance_creation_entity = osm5Client
					.createNSInstance(nscreateinstancerequestpayload.toJSON());
			// The NS Instance ID is set

			// NS instance creation
			if (ns_instance_creation_entity == null || ns_instance_creation_entity.getStatusCode().is4xxClientError()
					|| ns_instance_creation_entity.getStatusCode().is5xxServerError()) {
				// NS instance creation failed
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
				//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
				logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());								
				deploymentdescriptor.setFeedback(ns_instance_creation_entity.getBody().toString());
				logger.info("Update DeploymentDescriptor Object in 785");
				DeploymentDescriptor deploymentdescriptor_final = aMANOClient
						.updateDeploymentDescriptor(deploymentdescriptor);
				aMANOClient.deploymentInstantiationFailed(deploymentdescriptor_final);
				logger.info("NS Instance creation failed with response: "
						+ ns_instance_creation_entity.getBody().toString());
			} else {
				// String nsr_id =
				// osm5Client.instantiateNSInstance(nsd_instance_id,deploymentdescriptor.getName(),deploymentdescriptor.getInfrastructureForAll().getVIMid(),
				// deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getDeployId());
				JSONObject ns_instance_creation_entity_json_obj = new JSONObject(ns_instance_creation_entity.getBody());
				String nsd_instance_id = ns_instance_creation_entity_json_obj.getString("id");
				deploymentdescriptor.setInstanceId(nsd_instance_id);
				// Instantiate NS Instance
				NSInstantiateInstanceRequestPayload nsrequestpayload = new NSInstantiateInstanceRequestPayload(deploymentdescriptor);
				logger.info("NS Instantiation payload : " + nsrequestpayload.toJSON());
				// Here we need the feedback
				// String nsr_id = osm5Client.instantiateNSInstance(nsd_instance_id,
				// nsrequestpayload.toJSON());
				ResponseEntity<String> instantiate_ns_instance_entity = osm5Client.instantiateNSInstance(nsd_instance_id, nsrequestpayload.toJSON());
				if (instantiate_ns_instance_entity == null || instantiate_ns_instance_entity.getStatusCode().is4xxClientError() || instantiate_ns_instance_entity.getStatusCode().is5xxServerError()) {
					// NS Instantiation failed
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
					//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
					logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());										
					deploymentdescriptor.setFeedback(instantiate_ns_instance_entity.getBody().toString());
					logger.info("NS Instantiation failed. Status Code:"
							+ instantiate_ns_instance_entity.getStatusCode().toString() + ", Payload:"
							+ ns_instance_creation_entity.getBody().toString());
					// Save the changes to DeploymentDescriptor
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient
							.updateDeploymentDescriptor(deploymentdescriptor);
					aMANOClient.deploymentInstantiationFailed(deploymentdescriptor_final );
				} else {
					// NS Instantiation starts
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.INSTANTIATING);
					//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
					logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());					
					deploymentdescriptor.setFeedback(instantiate_ns_instance_entity.getBody().toString());
					logger.info("NS Instantiation of NS with id" + nsd_instance_id + " started.");
					// Save the changes to DeploymentDescriptor
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient
							.updateDeploymentDescriptor(deploymentdescriptor);
					//aMANOClient.deploymentInstantiationSucceded(deploymentdescriptor_final );
				}
			}
		}
		// OSM5 - END		
		return;
	}

	public void terminateNSFromMANOProvider(long deploymentdescriptorid) {
		logger.info("Starting terminateNSFromMANOProvicer");		
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);
		
		
		// OSM5 START
		if ( getExperimOBD(deploymentdescriptor).getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FIVE")) {
			 //deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATING ||			
			logger.info("Current status change before termination is :"+deploymentdescriptor.getStatus());
			if( deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.INSTANTIATING || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.RUNNING || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED )
			{
				try
				{
					MANOprovider tmpMANOProvider = getExperimOBD(deploymentdescriptor).getObMANOprovider();
					OSM5Client osm5Client = new OSM5Client(
							tmpMANOProvider.getApiEndpoint(),
							tmpMANOProvider.getUsername(),
							tmpMANOProvider.getPassword(),
							"admin");
					
					//MANOStatus.setOsm5CommunicationStatusActive(null);				
//					JSONObject ns_instance_info = osm5Client.getNSInstanceInfo(deploymentdescriptor.getInstanceId());
//					if (ns_instance_info != null) 
//					{
//						logger.info(ns_instance_info.toString());
//						logger.info("Status change of deployment " + deploymentdescriptor.getName() + " with status " + deploymentdescriptor.getStatus());						
//						CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());						
//						deploymentdescriptor.setOperationalStatus(ns_instance_info.getString("operational-status"));
//						deploymentdescriptor.setConfigStatus(ns_instance_info.getString("config-status"));
//						deploymentdescriptor.setDetailedStatus(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
//						if( deploymentdescriptor.getOperationalStatus() != "terminating" && deploymentdescriptor.getOperationalStatus() != "terminated" )
//						{
							ResponseEntity<String> response = osm5Client.terminateNSInstanceNew(deploymentdescriptor.getInstanceId()); 
							if (response == null || response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) 
							{
								deploymentdescriptor.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);
								//CentralLogger.log( CLevel.ERROR, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
								logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());								
								deploymentdescriptor.setFeedback(response.getBody().toString());				
								logger.error("Termination of NS instance " + deploymentdescriptor.getInstanceId() + " failed");	
								logger.info("Update DeploymentDescriptor Object in 877");								
								DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
								logger.info("NS status change is now "+deploymentdescriptor_final.getStatus());																			
								aMANOClient.terminateInstanceFailed(deploymentdescriptor_final );				
							}
							else
							{
								// NS Termination succeeded
								deploymentdescriptor.setStatus(DeploymentDescriptorStatus.TERMINATING);
								//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
								logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
								deploymentdescriptor.setConstituentVnfrIps("N/A");
								logger.info("Termination of NS " + deploymentdescriptor.getInstanceId() + " with name "+ deploymentdescriptor.getName() +" succeded");
								DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
								logger.info("NS status change is now "+deploymentdescriptor_final.getStatus());																			
								aMANOClient.terminateInstanceSucceded(deploymentdescriptor_final );				
							}
//						}
//					}
//					else
//					{
//						CentralLogger.log( CLevel.INFO, "Deployment "+deploymentdescriptor.getName()+" not found in OSM. Deletion skipped.");
//						logger.error("Deployment "+deploymentdescriptor.getName()+" not found in OSM. Deletion skipped.");							
//					}
				}
				catch(Exception e)
				{
					//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD termination action.");													
					//CentralLogger.log( CLevel.ERROR, "terminateNSFromMANOProvider, OSM5 fails authentication. Aborting action.");
				}
			}
		}
		// OSM5 END
	}

	public void deleteNSFromMANOProvider(long deploymentdescriptorid) {
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);

		logger.info("Will delete with deploymentdescriptorid : " + deploymentdescriptorid);		
		String aMANOplatform = "";
		try {	
			logger.info("MANOplatform: " + aMANOplatform);			
			aMANOplatform = getExperimOBD(deploymentdescriptor).getObMANOprovider().getSupportedMANOplatform().getName();
		}catch (Exception e) {
			aMANOplatform = "UNKNOWN";
		}							
		// OSM5 START
		if ( aMANOplatform.equals("OSM FIVE")) {
			logger.info("Descriptor targets an OSM FIVE deploymentdescriptorid: " + deploymentdescriptorid);		
			// There can be multiple MANOs for the Experiment. We need to handle that also.
			// After TERMINATION
			boolean force;
			if(deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATED)
			{
				force=false;
			}
			else if(deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATION_FAILED ) //for FAILED OR TERMINATION_FAILED instances
			{
				//CentralLogger.log(CLevel.INFO, "Following forcefull deletion. Status of " + deploymentdescriptor.getId() +" is "+ deploymentdescriptor.getStatus());
				logger.info("Following forcefull deletion. Status of " + deploymentdescriptor.getId() +" is "+ deploymentdescriptor.getStatus());
				force=true;
			}
			else
			{
				logger.info("Skipping deletion. Status of " + deploymentdescriptor.getId() +" is "+ deploymentdescriptor.getStatus());
				return;
			}
			OSM5Client osm5Client = null;
			try
			{
				 osm5Client = new OSM5Client(
						 getExperimOBD(deploymentdescriptor).getObMANOprovider().getApiEndpoint(),
						 getExperimOBD(deploymentdescriptor).getObMANOprovider().getUsername(),
						 getExperimOBD(deploymentdescriptor).getObMANOprovider().getPassword(),
						"admin");	
					//MANOStatus.setOsm5CommunicationStatusActive(null);													
			}
			catch(Exception e)
			{
				logger.error("OSM5 fails authentication");
				//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NS deletion action.");													
				//CentralLogger.log( CLevel.ERROR, "OSM5 fails authentication");
				deploymentdescriptor.setFeedback("OSM5 communication failed. Aborting NS deletion action.");				
				logger.error("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
				aMANOClient.deleteInstanceFailed(deploymentdescriptor);				
				return;
			}
			ResponseEntity<String> deletion_response = osm5Client.deleteNSInstanceNew(deploymentdescriptor.getInstanceId(),force); 
			if (deletion_response.getStatusCode().is4xxClientError() || deletion_response.getStatusCode().is5xxServerError()) {
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.DELETION_FAILED);
				//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
				logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());				
				deploymentdescriptor.setFeedback(deletion_response.getBody().toString());				
				logger.error("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
				logger.info("Update DeploymentDescriptor Object in 969");				
				DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
				logger.info("NS status change is now "+deploymentdescriptor_final.getStatus());															
				aMANOClient.deleteInstanceFailed(deploymentdescriptor_final );				
			}
			else if (deletion_response.getStatusCode().is2xxSuccessful())
			{
				if(deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATED)
				{
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.COMPLETED);
					//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
					logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());					
					logger.info("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " succeded");					
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
					logger.info("NS status change is now "+deploymentdescriptor_final.getStatus());															
					aMANOClient.deleteInstanceSucceded(deploymentdescriptor_final );				
				}
				if(deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATION_FAILED)
				{
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);				
					//CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());
					logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());					
					logger.info("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " succeded");					
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
					logger.info("NS status change is now "+deploymentdescriptor_final.getStatus());															
					aMANOClient.deleteInstanceSucceded(deploymentdescriptor_final );				
				}
			}
			else 
			{
				try
				{
					//CentralLogger.log( CLevel.ERROR, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus() +" replied with false code "+ deletion_response.getStatusCodeValue() + "and body" + deletion_response.getBody());
					logger.error( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus() +" replied with false code "+ deletion_response.getStatusCodeValue() + "and body" + deletion_response.getBody());
				}
				catch(Exception e)
				{
					//CentralLogger.log( CLevel.ERROR, "Deletion failed with message" + e.getMessage());
					logger.error("Deletion failed with message" + e.getMessage());
				}
			}
		} else {
			//if this is not a suported OSM then just complete
			logger.info("Descriptor targets an older not supported OSM deploymentdescriptorid: " + deploymentdescriptorid);		
			deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);	
			logger.info( "Status change of deployment " + deploymentdescriptor.getId()+", "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());					
			DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
			logger.info("NS status changed is now :" + deploymentdescriptor_final.getStatus());															
		}
	}
	// OSM5 END
	
}