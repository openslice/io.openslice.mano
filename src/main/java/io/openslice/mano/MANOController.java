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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.project.nsd.rev170228.nsd.constituent.vnfd.ConstituentVnfd;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.project.nsd.rev170228.project.nsd.catalog.Nsd;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.vnfd.base.rev170228.vnfd.descriptor.Vdu;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.vnfd.rev170228.vnfd.catalog.Vnfd;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import OSM5Util.OSM5ArchiveExtractor.OSM5NSExtractor;
import OSM5Util.OSM5ArchiveExtractor.OSM5VNFDExtractor;
import OSM5Util.OSM5NSReq.OSM5NSRequirements;
import OSM5Util.OSM5VNFReq.OSM5VNFRequirements;
import OSM7Util.OSM7ArchiveExtractor.OSM7NSExtractor;
import OSM7Util.OSM7ArchiveExtractor.OSM7VNFDExtractor;
import OSM7Util.OSM7NSReq.OSM7NSRequirements;
import OSM7Util.OSM7VNFReq.OSM7VNFRequirements;
import io.openslice.model.CompositeExperimentOnBoardDescriptor;
import io.openslice.model.CompositeVxFOnBoardDescriptor;
import io.openslice.model.ConstituentVxF;
//import OSM5NBIClient.OSM5Client;
import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.DeploymentDescriptorStatus;
import io.openslice.model.ExperimentMetadata;
import io.openslice.model.ExperimentOnBoardDescriptor;
import io.openslice.model.MANOprovider;
import io.openslice.model.OnBoardingStatus;
import io.openslice.model.VFImage;
import io.openslice.model.ValidationStatus;
import io.openslice.model.VxFMetadata;
import io.openslice.model.VxFOnBoardedDescriptor;
import io.openslice.sol005nbi.OSMClient;
import io.openslice.centrallog.client.*;

/**
 * @author ctranoris, ichatzis
 *
 */

@Configuration
public class MANOController {

	/** */
	private static final transient Log logger = LogFactory.getLog(MANOController.class.getName());
	@Autowired
	private MANOClient aMANOClient;	

	@Value("${spring.application.name}")
	private String compname;	

	public void onBoardVxFToMANOProviderByFile( ) throws Exception {
		
	}

	/**
	 * onBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param vxfobds
	 * @throws Exception
	 */
	public void onBoardVxFToMANOProviderByOBD( VxFOnBoardedDescriptor vxfobd ) throws Exception {
		
		vxfobd.setVxf(aMANOClient.getVxFById(vxfobd.getVxfid()));		
		// Update the status and update the vxfobd
		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobd.getVxf().getName()+" to "+vxfobd.getOnBoardingStatus(), compname);
		logger.info("Onboarding status change of VxF "+vxfobd.getVxf().getName()+" to "+vxfobd.getOnBoardingStatus());
		// Set MANO Provider VxF ID
		vxfobd.setVxfMANOProviderID( vxfobd.getVxf().getName());
		// Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = aMANOClient.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}
		//Reload the vxf for the updated object.
		vxfobds.setVxf(aMANOClient.getVxFById(vxfobds.getVxfid()));					
				
		String manoVersion = vxfobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion, vxfobds.getObMANOprovider().getApiEndpoint(), vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(), vxfobds.getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);
		}
	    catch(Exception e) 
		{
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");				
			CentralLogger.log( CLevel.ERROR, "onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.", compname);
			
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF OnBoarding action.");																	
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(manoVersion + " communication failed. Aborting VxF OnBoarding action.");
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus());
			
			// ?? This should change. Either by an activemq call or we should certify upon Onboarding success.
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			
			//Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed( vxfobds_final );				
	        return ;
		}						
		
		ResponseEntity<String> response = null;
		response = osmClient.createVNFDPackage();
		if (response == null || response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
			logger.error("VNFD Package Creation failed.");
			// Set status
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);										
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(response.getBody().toString());
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			//Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed( vxfobds_final );
			return;				
		}
		else
		{
			JSONObject obj = new JSONObject(response.getBody());
			String vnfd_id = obj.getString("id");
			logger.info(response.getStatusCode()+" replied. The new VNFD Package id is :" + vnfd_id);
			String pLocation = vxfobd.getVxf().getPackageLocation();
			logger.info("Package location to onboard is :" + pLocation);
			response = osmClient.uploadVNFDPackageContent(vnfd_id, pLocation);
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of VNFD Package Content failed. Deleting VNFD Package.");
				// Delete the package from the OSM
				osmClient.deleteVNFDPackage(vnfd_id);
				// Set status
				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);											
				// Set the reason of the failure
				vxfobds.setFeedbackMessage(response.getBody().toString());
				// Uncertify if it failed OnBoarding.
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
				aMANOClient.onBoardVxFFailed( vxfobds_final );
				return;
			}

			vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);											
			
			vxfobds.setFeedbackMessage("OnBoarding Succeeded");
			
			// We select by design not to Certify upon OnBoarding but only on final version is determined.
			// vxfobds.getVxf().setCertified(true);
			
			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID( vxfobd.getVxf().getName() );
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			aMANOClient.onBoardVxFSucceded( vxfobds_final );
			
		}			
	}

	public void onBoardVxFToMANOProviderByCompositeObj( CompositeVxFOnBoardDescriptor compositeobd ) throws Exception {
		
		VxFOnBoardedDescriptor vxfobd = (VxFOnBoardedDescriptor)compositeobd.getObd();
		byte[] allBytes = compositeobd.getAllBytes();
		// Load the related VxFMetadata object
		vxfobd.setVxf(aMANOClient.getVxFById(vxfobd.getVxfid()));		
		// Update the status and update the vxfobd
		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobd.getVxf().getName()+" to "+vxfobd.getOnBoardingStatus(), compname);
		logger.info("Onboarding status change of VxF "+vxfobd.getVxf().getName()+" to "+vxfobd.getOnBoardingStatus());
		// Set MANO Provider VxF ID
		vxfobd.setVxfMANOProviderID( vxfobd.getVxf().getName());
		// Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = aMANOClient.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}
		//Reload the vxf for the updated object.
		vxfobds.setVxf(aMANOClient.getVxFById(vxfobds.getVxfid()));					
				
		String manoVersion = vxfobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			logger.info("manoVersion: " + manoVersion);
			osmClient = OSMClientFactory.getOSMClient(manoVersion, vxfobds.getObMANOprovider().getApiEndpoint(), vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(), vxfobds.getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);
			if ( osmClient == null ) {
				new Exception( "Cannot create osmClient" );
			}
		}
	    catch(Exception e) 
		{
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");				
			CentralLogger.log( CLevel.ERROR, "onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.", compname);
			
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF OnBoarding action.");																	
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(manoVersion + " communication failed. Aborting VxF OnBoarding action.");
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus());
			
			// ?? This should change. Either by an activemq call or we should certify upon Onboarding success.
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			
			//Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed( vxfobds_final );				
	        return ;
		}						
		
		ResponseEntity<String> response = null;
		response = osmClient.createVNFDPackage();
		if (response == null || response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
			logger.error("VNFD Package Creation failed.");
			// Set status
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);										
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(response.getBody().toString());
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			//Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed( vxfobds_final );
			return;				
		}
		else
		{
			JSONObject obj = new JSONObject(response.getBody());
			String vnfd_id = obj.getString("id");
			logger.info(response.getStatusCode()+" replied. The new VNFD Package id is :" + vnfd_id);
			response = osmClient.uploadVNFDPackageContent(vnfd_id, allBytes);
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of VNFD Package Content failed. Deleting VNFD Package.");
				// Delete the package from the OSM
				osmClient.deleteVNFDPackage(vnfd_id);
				// Set status
				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);											
				// Set the reason of the failure
				vxfobds.setFeedbackMessage(response.getBody().toString());
				// Uncertify if it failed OnBoarding.
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
				aMANOClient.onBoardVxFFailed( vxfobds_final );
				return;
			}

			vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);											
			
			vxfobds.setFeedbackMessage("OnBoarding Succeeded");
			
			// We select by design not to Certify upon OnBoarding but only on final version is determined.
			// vxfobds.getVxf().setCertified(true);
			
			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID( vxfobd.getVxf().getName() );
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			aMANOClient.onBoardVxFSucceded( vxfobds_final );				
		}			
	}

	public void onBoardVxFToMANOProvider( VxFOnBoardedDescriptor vxfobd, byte[] allBytes ) throws Exception {
		
		// Load the related VxFMetadata object
		vxfobd.setVxf(aMANOClient.getVxFById(vxfobd.getVxfid()));		
		// Update the status and update the vxfobd
		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobd.getVxf().getName()+" to "+vxfobd.getOnBoardingStatus(), compname);
		logger.info("Onboarding status change of VxF "+vxfobd.getVxf().getName()+" to "+vxfobd.getOnBoardingStatus());
		// Set MANO Provider VxF ID
		vxfobd.setVxfMANOProviderID( vxfobd.getVxf().getName());
		// Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = aMANOClient.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}
		//Reload the vxf for the updated object.
		vxfobds.setVxf(aMANOClient.getVxFById(vxfobds.getVxfid()));					
				
		String manoVersion = vxfobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion, vxfobds.getObMANOprovider().getApiEndpoint(), vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(), vxfobds.getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);
		}
	    catch(Exception e) 
		{
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");				
			CentralLogger.log( CLevel.ERROR, "onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.", compname);
			
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF OnBoarding action.");																	
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(manoVersion + " communication failed. Aborting VxF OnBoarding action.");
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus());
			
			// ?? This should change. Either by an activemq call or we should certify upon Onboarding success.
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			
			//Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed( vxfobds_final );				
	        return ;
		}						
		
		ResponseEntity<String> response = null;
		response = osmClient.createVNFDPackage();
		if (response == null || response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
			logger.error("VNFD Package Creation failed.");
			// Set status
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);										
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(response.getBody().toString());
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			//Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed( vxfobds_final );
			return;				
		}
		else
		{
			JSONObject obj = new JSONObject(response.getBody());
			String vnfd_id = obj.getString("id");
			logger.info(response.getStatusCode()+" replied. The new VNFD Package id is :" + vnfd_id);
			response = osmClient.uploadVNFDPackageContent(vnfd_id, allBytes);
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of VNFD Package Content failed. Deleting VNFD Package.");
				// Delete the package from the OSM
				osmClient.deleteVNFDPackage(vnfd_id);
				// Set status
				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);											
				// Set the reason of the failure
				vxfobds.setFeedbackMessage(response.getBody().toString());
				// Uncertify if it failed OnBoarding.
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
				aMANOClient.onBoardVxFFailed( vxfobds_final );
				return;
			}

			vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			CentralLogger.log( CLevel.INFO, "Onboarding status change of VxF "+vxfobds.getVxf().getName()+" to "+vxfobds.getOnBoardingStatus(), compname);											
			
			vxfobds.setFeedbackMessage("OnBoarding Succeeded");
			
			// We select by design not to Certify upon OnBoarding but only on final version is determined.
			// vxfobds.getVxf().setCertified(true);
			
			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID( vxfobd.getVxf().getName() );
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			aMANOClient.onBoardVxFSucceded( vxfobds_final );				
		}			
	}
	
	/**
	 * offBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param c
	 */
	public ResponseEntity<String> offBoardVxFFromMANOProvider(VxFOnBoardedDescriptor obd)
			throws HttpClientErrorException {
		// TODO Auto-generated method stub
		ResponseEntity<String> response = null;
		String vnfd_id = obd.getDeployId();
		String manoVersion=obd.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;			
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion , obd.getObMANOprovider().getApiEndpoint(), obd.getObMANOprovider().getUsername(), obd.getObMANOprovider().getPassword(), obd.getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);								
		}
	    catch(HttpStatusCodeException e) 
		{
			logger.error("offBoardVxFFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			CentralLogger.log( CLevel.ERROR, "offBoardVxFFromMANOProvider, OSM5 fails authentication. Aborting VxF offboarding action.", compname);
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF offboarding action.");								
	        return ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders())
	                .body(e.getResponseBodyAsString());
		}						
		
		response = osmClient.deleteVNFDPackage(vnfd_id);
		if (obd.getObMANOprovider().getSupportedMANOplatform().getVersion().equals("OSMvTWO")) {
			response = new ResponseEntity<>("Not implemented for OSMvTWO", HttpStatus.CREATED);
		}
		return response;
	}

	public void onBoardNSDToMANOProviderByFile( ) throws Exception {
		
	}
	
	public void onBoardNSDToMANOProviderByCompositeObj(CompositeExperimentOnBoardDescriptor compexpobd) throws Exception {
		
		ExperimentOnBoardDescriptor uexpobd = compexpobd.getObd();
		byte[] allBytes = compexpobd.getAllBytes();
		
		ExperimentMetadata em = uexpobd.getExperiment();
		//if (em == null) {
			em = (ExperimentMetadata) aMANOClient.getNSDById(uexpobd.getExperimentid());
		//}
		uexpobd.setExperiment(em);
		uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);

		// This is the Deployment ID for the portal
		uexpobd.setDeployId(UUID.randomUUID().toString());
		CentralLogger.log( CLevel.INFO, "Onboarding status change of Experiment "+uexpobd.getExperiment().getName()+" to "+uexpobd.getOnBoardingStatus(), compname);
		logger.info("Onboarding status change of Experiment "+uexpobd.getExperiment().getName()+" to "+uexpobd.getOnBoardingStatus());

		//uexpobd.setVxfMANOProviderID(em.getName()); // Possible Error. This probably needs to be
		uexpobd.setExperimentMANOProviderID(em.getName());
		uexpobd.setLastOnboarding(new Date());
				
		ExperimentOnBoardDescriptor uexpobds = aMANOClient.updateExperimentOnBoardDescriptor(uexpobd);
		if (uexpobds == null) {
			throw new Exception("Cannot load NSDOnBoardedDescriptor");
		}
		
		em = uexpobds.getExperiment();
		if (em == null) {
			em = (ExperimentMetadata) aMANOClient.getNSDById(uexpobd.getExperimentid());
		}
		uexpobds.setExperiment(em);
		uexpobds.setObMANOprovider(uexpobd.getObMANOprovider());
		String manoVersion = uexpobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion , uexpobds.getObMANOprovider().getApiEndpoint(), uexpobds.getObMANOprovider().getUsername(), uexpobds.getObMANOprovider().getPassword(), uexpobds.getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);								
		}
	    catch(Exception e) 
		{
			logger.error("onBoardNSDFromMANOProvider, "+ manoVersion +" fails authentication. Aborting action.");
			CentralLogger.log( CLevel.ERROR, "onBoardNSDFromMANOProvider, "+ manoVersion +" fails authentication. Aborting NSD Onboarding action.", compname);
			logger.error("onBoardNSDFromMANOProvider, "+ manoVersion +" fails authentication. Aborting NSD Onboarding action.");
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD Onboarding action.");				
			// Set the reason of the failure
			uexpobds.setFeedbackMessage("OSM communication failed. Aborting NSD Onboarding action.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.ERROR, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed( uexpobds );
			return ;
		}						
		
		ResponseEntity<String> response = null;
		response = osmClient.createNSDPackage();
		if (response == null || response.getStatusCode().is4xxClientError()
				|| response.getStatusCode().is5xxServerError()) {
			logger.error("Creation of NSD Package Content failed. Deleting NSD Package.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
			// Set the reason of the failure
			uexpobds.setFeedbackMessage(response.getBody().toString());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed( uexpobds );
			return;				
		}
		else
		{
			JSONObject obj = new JSONObject(response.getBody());
			String nsd_id = obj.getString("id");
			logger.info(response.getStatusCode()+" replied. The new NSD Package id is :" + nsd_id);
			response = osmClient.uploadNSDPackageContent(nsd_id, allBytes);
			if (response == null || response.getStatusCode().is4xxClientError()	|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
				osmClient.deleteNSDPackage(nsd_id);
				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
				logger.error("Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());
				
				if ( response.getBody() !=null ) {
					uexpobds.setFeedbackMessage(response.getBody().toString());
				}
				logger.error("Onboarding Feedbacj Message of Experiment "+uexpobds.getExperiment().getName()+" is "+uexpobds.getFeedbackMessage());
				// Set Valid to false if it fails OnBoarding
				uexpobds.getExperiment().setValid(false);
				aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
				aMANOClient.onBoardNSDFailed( uexpobds );
				return;
			}
			else
			{
				uexpobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
				CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
				logger.info("Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());
				uexpobds.setFeedbackMessage("NSD Onboarded Successfully");
				// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
				uexpobds.setDeployId(nsd_id);
				// What should be the NSD Name. Something like cirros_nsd.
				uexpobds.setExperimentMANOProviderID(em.getName());
				// Set Onboarding date
				uexpobds.setLastOnboarding(new Date());
				// We decide to set valid when we have the final version. Thus we comment this.
				// uexpobds.getExperiment().setValid(true);
				// Save the changes to vxfobds
				aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
				aMANOClient.onBoardNSDSucceded( uexpobds );
			}
		}
	}
	
	public void onBoardNSDToMANOProvider( ExperimentOnBoardDescriptor uexpobd ) throws Exception {

		ExperimentMetadata em = uexpobd.getExperiment();
		if (em == null) {
			em = (ExperimentMetadata) aMANOClient.getNSDById(uexpobd.getExperimentid());
		}
		uexpobd.setExperiment(em);
		uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);

		// This is the Deployment ID for the portal
		uexpobd.setDeployId(UUID.randomUUID().toString());
		CentralLogger.log( CLevel.INFO, "Onboarding status change of Experiment "+uexpobd.getExperiment().getName()+" to "+uexpobd.getOnBoardingStatus(), compname);
		logger.info("Onboarding status change of Experiment "+uexpobd.getExperiment().getName()+" to "+uexpobd.getOnBoardingStatus());

		//uexpobd.setVxfMANOProviderID(em.getName()); // Possible Error. This probably needs to be
		uexpobd.setExperimentMANOProviderID(em.getName());

		uexpobd.setLastOnboarding(new Date());
				
		ExperimentOnBoardDescriptor uexpobds = aMANOClient.updateExperimentOnBoardDescriptor(uexpobd);
		if (uexpobds == null) {
			throw new Exception("Cannot load NSDOnBoardedDescriptor");
		}
		em = uexpobds.getExperiment();
		if (em == null) {
			em = (ExperimentMetadata) aMANOClient.getNSDById(uexpobd.getExperimentid());
		}
		uexpobds.setExperiment(em);
	
		String manoVersion = uexpobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion , uexpobd.getObMANOprovider().getApiEndpoint(), uexpobd.getObMANOprovider().getUsername(), uexpobd.getObMANOprovider().getPassword(), uexpobd.getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);								
		}
	    catch(Exception e) 
		{
			logger.error("onBoardNSDFromMANOProvider, "+ manoVersion +" fails authentication. Aborting action.");
			CentralLogger.log( CLevel.ERROR, "onBoardNSDFromMANOProvider, "+ manoVersion +" fails authentication. Aborting NSD Onboarding action.", compname);
			logger.error("onBoardNSDFromMANOProvider, "+ manoVersion +" fails authentication. Aborting NSD Onboarding action.");
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD Onboarding action.");				
			// Set the reason of the failure
			uexpobds.setFeedbackMessage("OSM communication failed. Aborting NSD Onboarding action.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.ERROR, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed( uexpobds );
			return ;
		}						
		
		ResponseEntity<String> response = null;
		response = osmClient.createNSDPackage();
		if (response == null || response.getStatusCode().is4xxClientError()
				|| response.getStatusCode().is5xxServerError()) {
			logger.error("Creation of NSD Package Content failed. Deleting NSD Package.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
			// Set the reason of the failure
			uexpobds.setFeedbackMessage(response.getBody().toString());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed( uexpobds );
			return;				
		}
		else
		{
			JSONObject obj = new JSONObject(response.getBody());
			String nsd_id = obj.getString("id");
			logger.info(response.getStatusCode()+" replied. The new NSD Package id is :" + nsd_id);
			String pLocation = uexpobd.getExperiment().getPackageLocation();
			logger.info("Package location to onboard is :" + pLocation);
			response = osmClient.uploadNSDPackageContent(nsd_id, pLocation);
			if (response == null || response.getStatusCode().is4xxClientError()	|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
				osmClient.deleteNSDPackage(nsd_id);
				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
				logger.error("Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());
				uexpobds.setFeedbackMessage(response.getBody().toString());
				logger.error("Onboarding Feedbacj Message of Experiment "+uexpobds.getExperiment().getName()+" is "+uexpobds.getFeedbackMessage());
				// Set Valid to false if it fails OnBoarding
				uexpobds.getExperiment().setValid(false);
				aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
				aMANOClient.onBoardNSDFailed( uexpobds );
				return;
			}
			else
			{
				uexpobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
				CentralLogger.log( CLevel.INFO, "Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus(), compname);
				logger.info("Onboarding Status change of Experiment "+uexpobds.getExperiment().getName()+" to "+uexpobds.getOnBoardingStatus());
				uexpobds.setFeedbackMessage("NSD Onboarded Successfully");
				// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
				uexpobds.setDeployId(nsd_id);
				// What should be the NSD Name. Something like cirros_nsd.
				uexpobds.setExperimentMANOProviderID(em.getName());
				// Set Onboarding date
				uexpobds.setLastOnboarding(new Date());
				// We decide to set valid when we have the final version. Thus we comment this.
				// uexpobds.getExperiment().setValid(true);
				// Save the changes to vxfobds
				aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
				aMANOClient.onBoardNSDSucceded( uexpobds );
			}
		}
	}

	public ResponseEntity<String> offBoardNSDFromMANOProvider(ExperimentOnBoardDescriptor uexpobd) {
		// TODO Auto-generated method stub
		ResponseEntity<String> response = null;
		
		String nsd_id = uexpobd.getDeployId();
		String manoVersion = uexpobd.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;			
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion, uexpobd.getObMANOprovider().getApiEndpoint(), uexpobd.getObMANOprovider().getUsername(), uexpobd.getObMANOprovider().getPassword(), uexpobd.getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);								
		}
	    catch(HttpStatusCodeException e) 
		{
			logger.error("offBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			CentralLogger.log( CLevel.ERROR, "offBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.", compname);
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD offboarding action.");								
	        return ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders())
	                .body(e.getResponseBodyAsString());
		}						
		response = osmClient.deleteNSDPackage(nsd_id);	
		logger.info("offBoardNSDFromMANOProvider, response sent back tou ActiveMQ: " + response.toString());		
		return response;
	}
		
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
		//CentralLogger.log( CLevel.INFO, "Update Deployment Descriptors!!!!", compname);		
		try
		{
			List<DeploymentDescriptor> runningDeploymentDescriptors = aMANOClient.getRunningInstantiatingAndTerminatingDeployments();
			for(DeploymentDescriptor nsd : runningDeploymentDescriptors)
			{
				System.out.println("NSD name:"+nsd.getName());
			}
			OSMClient osmClient = null;
			// For each deployment get the status info and the IPs
			for (int i = 0; i < runningDeploymentDescriptors.size(); i++) {
				DeploymentDescriptor deployment_tmp = aMANOClient.getDeploymentByIdEager(runningDeploymentDescriptors.get(i).getId());
				deployment_tmp.getExperimentFullDetails();
				try {
					// Get the MANO Provider for each deployment			
					long tmp_MANOprovider_id = getExperimOBD( deployment_tmp ).getObMANOprovider().getId();
					MANOprovider sm = aMANOClient.getMANOproviderByID( tmp_MANOprovider_id );
					String manoVersion = sm.getSupportedMANOplatform().getVersion();
					if (osmClient == null || !osmClient.getMANOApiEndpoint().equals(sm.getApiEndpoint())) {
						try
						{
							osmClient = OSMClientFactory.getOSMClient(manoVersion, sm.getApiEndpoint(), sm.getUsername(), sm.getPassword(), sm.getProject());								
							// MANOStatus.setOsm5CommunicationStatusActive(null);
						}
						catch(Exception e)
						{
							logger.error(manoVersion + " fails authentication");
							CentralLogger.log( CLevel.ERROR, manoVersion + " fails authentication", compname);							
							// MANOStatus.setOsm5CommunicationStatusFailed(null);
							return;
						}
					}
					JSONObject ns_instance_info = osmClient.getNSInstanceInfo(deployment_tmp.getInstanceId());
					//JSONObject ns_instance_content_info = osmClient.getNSInstanceContentInfo(deployment_tmp.getInstanceId());
					// If the no nsd with the specific id is found, mark the instance as faile to delete.
					if(ns_instance_info == null)
					{
						deployment_tmp.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);
						CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus(), compname);
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
							//String nsr_string = JSONObject.quote(ns_instance_info.toString());
							deployment_tmp.setNsr(ns_instance_info.toString());
							deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
							logger.info("Setting NSR Info:"+deployment_tmp.getNsr());
							if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.RUNNING)
							{
								JSONObject ns_nslcm_details = osmClient.getNSLCMDetails(deployment_tmp.getNsLcmOpOccId());
								deployment_tmp.setNs_nslcm_details(ns_nslcm_details.toString());																		
								deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
								if(!deployment_tmp.getOperationalStatus().equals(ns_instance_info.getString("operational-status"))||!deployment_tmp.getConfigStatus().equals(ns_instance_info.getString("config-status"))||!deployment_tmp.getDetailedStatus().equals(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", "")))
								{
									logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
									CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus(), compname);
									deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
									deployment_tmp.setConstituentVnfrIps("");
									for (int j = 0; j < ns_instance_info.getJSONArray("constituent-vnfr-ref")
											.length(); j++) 
									{
										if (j > 0) {
											deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps() + ", ");
										}
										ResponseEntity<String> vnf_instance_id_info_response = osmClient.getVNFInstanceInfoNew(ns_instance_info.getJSONArray("constituent-vnfr-ref").get(j).toString());
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
								JSONObject ns_nslcm_details = osmClient.getNSLCMDetails(deployment_tmp.getNsLcmOpOccId());
								deployment_tmp.setNs_nslcm_details(ns_nslcm_details.toString());
								deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);									
								deployment_tmp.setStatus(DeploymentDescriptorStatus.RUNNING);
								logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
								CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus(), compname);
								deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
								deployment_tmp.setConstituentVnfrIps("");
								for (int j = 0; j < ns_instance_info.getJSONArray("constituent-vnfr-ref")
										.length(); j++) 
								{
									if (j > 0) {
										deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps() + ", ");
									}
									ResponseEntity<String> vnf_instance_id_info_response = osmClient.getVNFInstanceInfoNew(ns_instance_info.getJSONArray("constituent-vnfr-ref").get(j).toString());
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
								CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus(), compname);								
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
								CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus(), compname);								
								logger.info("Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus());
								deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status").replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
								deployment_tmp.setConstituentVnfrIps("N/A");
								deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
								aMANOClient.deploymentInstantiationFailed(deployment_tmp);
							}
							if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.TERMINATING
									&& deployment_tmp.getOperationalStatus().equals("failed")) {
								deployment_tmp.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);
								CentralLogger.log( CLevel.INFO, "Status change of deployment "+deployment_tmp.getName()+" to "+deployment_tmp.getStatus(), compname);								
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

	public void deployNSDToMANOProvider(long deploymentdescriptorid) {
		logger.info("Starting deployNSDToMANOProvicer");
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);		
		ExperimentOnBoardDescriptor tmp = getExperimOBD(deploymentdescriptor);
		OSMClient osmClient = null;
		try {
			logger.debug("Connecting to "+tmp.getObMANOprovider().getSupportedMANOplatform().getName()+" MANO Client of version "+tmp.getObMANOprovider().getSupportedMANOplatform().getVersion()+".");
			osmClient = OSMClientFactory.getOSMClient(tmp.getObMANOprovider().getSupportedMANOplatform().getVersion(),
					getExperimOBD(deploymentdescriptor).getObMANOprovider().getApiEndpoint(),
					getExperimOBD(deploymentdescriptor).getObMANOprovider().getUsername(),
					getExperimOBD(deploymentdescriptor).getObMANOprovider().getPassword(),
					getExperimOBD(deploymentdescriptor).getObMANOprovider().getProject());
			//MANOStatus.setOsm5CommunicationStatusActive(null);
		}
		catch(Exception e)
		{
			logger.error("deployNSDToMANOProvider, "+tmp.getObMANOprovider().getSupportedMANOplatform().getName()+" fails authentication! Aborting deployment of NSD.");
			CentralLogger.log( CLevel.ERROR, "deployNSDToMANOProvider, "+tmp.getObMANOprovider().getSupportedMANOplatform().getName()+" fails authentication! Aborting deployment of NSD.", compname);
			//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting deployment of NSD.");								
			// NS instance creation failed
			//deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
			deploymentdescriptor.setFeedback( (new Date()) + tmp.getObMANOprovider().getSupportedMANOplatform().getName() + "  communication failed. Aborting NSD deployment action. " );
			deploymentdescriptor.setOperationalStatus((new Date()) + " communication-failure ");
			deploymentdescriptor = aMANOClient
					.updateDeploymentDescriptor(deploymentdescriptor);
			//aMANOClient.deploymentInstantiationFailed(deploymentdescriptor);
			return;
		}
		NSCreateInstanceRequestPayload nscreateinstancerequestpayload = null;
		String nscreateinstancerequestpayload_json = null;
		if(deploymentdescriptor.getInstantiationconfig() != null)
		{
			nscreateinstancerequestpayload_json = deploymentdescriptor.getInstantiationconfig();
			logger.info("Found and parsed instantiation configuration " + nscreateinstancerequestpayload_json);
		}
		else
		{
			logger.info("Could not find or parse instantiation configuration from user. Getting default configuration");
			nscreateinstancerequestpayload = new NSCreateInstanceRequestPayload(deploymentdescriptor);
			nscreateinstancerequestpayload_json = nscreateinstancerequestpayload.toJSON();
		}
		
		// Get Experiment ID and VIM ID and create NS Instance.
		logger.info("NS Instance creation payload : " + nscreateinstancerequestpayload_json);
		ResponseEntity<String> ns_instance_creation_entity = osmClient.createNSInstance(nscreateinstancerequestpayload_json);
		// The NS Instance ID is set

		// NS instance creation
		if (ns_instance_creation_entity == null || ns_instance_creation_entity.getStatusCode().is4xxClientError()
				|| ns_instance_creation_entity.getStatusCode().is5xxServerError()) {
			// NS instance creation failed
			deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
			CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
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
			//NSInstantiateInstanceRequestPayload nsrequestpayload = new NSInstantiateInstanceRequestPayload(deploymentdescriptor);
			//logger.info("NS Instantiation payload : " + nsrequestpayload.toJSON());


			NSInstantiateInstanceRequestPayload nsrequestpayload = null;
			String nsrequestpayload_json = null;
			if(deploymentdescriptor.getInstantiationconfig() != null)
			{
				nsrequestpayload_json = deploymentdescriptor.getInstantiationconfig();
				logger.info("Found and parsed instantiation configuration "+nsrequestpayload_json);
			}
			else
			{
				logger.info("Could not find or parse instantiation configuration from user. Getting default configuration");
				nsrequestpayload = new NSInstantiateInstanceRequestPayload(deploymentdescriptor);
				nsrequestpayload_json = nscreateinstancerequestpayload.toJSON();
			}
			// Get Experiment ID and VIM ID and create NS Instance.
			logger.info("NS Instance creation payload : " + nsrequestpayload_json);
			
			// Here we need the feedback
			// String nsr_id = osm5Client.instantiateNSInstance(nsd_instance_id, nsrequestpayload.toJSON());
			ResponseEntity<String> instantiate_ns_instance_entity = osmClient.instantiateNSInstance(nsd_instance_id, nsrequestpayload_json);
			if (instantiate_ns_instance_entity == null || instantiate_ns_instance_entity.getStatusCode().is4xxClientError() || instantiate_ns_instance_entity.getStatusCode().is5xxServerError()) {
				// NS Instantiation failed
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
				CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
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
				JSONObject instantiate_ns_instance_entity_json_obj = new JSONObject(instantiate_ns_instance_entity.getBody());
				deploymentdescriptor.setNsLcmOpOccId(instantiate_ns_instance_entity_json_obj.getString("id"));
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.INSTANTIATING);
				CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
				logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());					
				deploymentdescriptor.setFeedback(instantiate_ns_instance_entity.getBody().toString());
				logger.info("NS Instantiation of NS with id" + nsd_instance_id + " started.");
				// Save the changes to DeploymentDescriptor
				aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
				//aMANOClient.deploymentInstantiationSucceded(deploymentdescriptor_final );
			}
		}
		return;
	}

	public void terminateNSFromMANOProvider(long deploymentdescriptorid) {
		logger.info("Starting terminateNSFromMANOProvicer");		
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);
		
		
		logger.info("Current status change before termination is :"+deploymentdescriptor.getStatus());
		if( deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.INSTANTIATING || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.RUNNING || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED )
		{
			try
			{
				MANOprovider tmpMANOProvider = getExperimOBD(deploymentdescriptor).getObMANOprovider();
				OSMClient osmClient = OSMClientFactory.getOSMClient(getExperimOBD(deploymentdescriptor).getObMANOprovider().getSupportedMANOplatform().getVersion(),
						tmpMANOProvider.getApiEndpoint(),
						tmpMANOProvider.getUsername(),
						tmpMANOProvider.getPassword(),
						tmpMANOProvider.getProject());												 
				
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
						ResponseEntity<String> response = osmClient.terminateNSInstanceNew(deploymentdescriptor.getInstanceId()); 
						if (response == null || response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) 
						{
							deploymentdescriptor.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);
							CentralLogger.log( CLevel.ERROR, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
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
							CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
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
				CentralLogger.log( CLevel.ERROR, "terminateNSFromMANOProvider, " + getExperimOBD(deploymentdescriptor).getObMANOprovider().getSupportedMANOplatform().getName() + " fails authentication. Aborting action.", compname);
			}
		}
	}

	public void deleteNSFromMANOProvider(long deploymentdescriptorid) {
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);
		
		
		
		logger.info("Will delete with deploymentdescriptorid : " + deploymentdescriptorid);		
		String aMANOplatform = "";
		try {	
			logger.info("MANOplatform: " + aMANOplatform);			
			aMANOplatform = getExperimOBD(deploymentdescriptor).getObMANOprovider().getSupportedMANOplatform().getVersion();
		}catch (Exception e) {
			aMANOplatform = "UNKNOWN";
		}							
		if(OSMClientFactory.isOSMVersionSupported(aMANOplatform))
		{
			logger.info("Descriptor targets an "+aMANOplatform+" deploymentdescriptorid: " + deploymentdescriptorid);		
			// There can be multiple MANOs for the Experiment. We need to handle that also.
			// After TERMINATION
			boolean force;
			if(deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATED)
			{
				force=false;
			}
			else if(deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATION_FAILED ) //for FAILED OR TERMINATION_FAILED instances
			{
				CentralLogger.log(CLevel.INFO, "Following forcefull deletion. Status of " + deploymentdescriptor.getId() +" is "+ deploymentdescriptor.getStatus(), compname);
				logger.info("Following forcefull deletion. Status of " + deploymentdescriptor.getId() +" is "+ deploymentdescriptor.getStatus());
				force=true;
			}
			else
			{
				logger.info("Skipping deletion. Status of " + deploymentdescriptor.getId() +" is "+ deploymentdescriptor.getStatus());
				return;
			}
			OSMClient osmClient = null;
			try
			{
					osmClient = OSMClientFactory.getOSMClient(aMANOplatform, 							
							getExperimOBD(deploymentdescriptor).getObMANOprovider().getApiEndpoint(),
							getExperimOBD(deploymentdescriptor).getObMANOprovider().getUsername(),
							getExperimOBD(deploymentdescriptor).getObMANOprovider().getPassword(),
							getExperimOBD(deploymentdescriptor).getObMANOprovider().getProject());												 
					//MANOStatus.setOsm5CommunicationStatusActive(null);													
			}
			catch(Exception e)
			{
				logger.error(aMANOplatform+" fails authentication");
				//MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NS deletion action.");													
				CentralLogger.log( CLevel.ERROR, aMANOplatform+" fails authentication", compname);
				deploymentdescriptor.setFeedback(aMANOplatform+" communication failed. Aborting NS deletion action.");				
				logger.error("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
				aMANOClient.deleteInstanceFailed(deploymentdescriptor);				
				return;
			}
			ResponseEntity<String> deletion_response = osmClient.deleteNSInstanceNew(deploymentdescriptor.getInstanceId(),force); 
			if (deletion_response.getStatusCode().is4xxClientError() || deletion_response.getStatusCode().is5xxServerError()) {
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.DELETION_FAILED);
				CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
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
					CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
					logger.info( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());					
					logger.info("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " succeded");					
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
					logger.info("NS status change is now "+deploymentdescriptor_final.getStatus());															
					aMANOClient.deleteInstanceSucceded(deploymentdescriptor_final );				
				}
				if(deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED || deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATION_FAILED)
				{
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);				
					CentralLogger.log( CLevel.INFO, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus(), compname);
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
					CentralLogger.log( CLevel.ERROR, "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus() +" replied with false code "+ deletion_response.getStatusCodeValue() + "and body" + deletion_response.getBody(), compname);
					logger.error( "Status change of deployment "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus() +" replied with false code "+ deletion_response.getStatusCodeValue() + "and body" + deletion_response.getBody());
				}
				catch(Exception e)
				{
					CentralLogger.log( CLevel.ERROR, "Deletion failed with message" + e.getMessage(), compname);
					logger.error("Deletion failed with message" + e.getMessage());
				}
			}
		} else {
			//if this is not a supported OSM then just complete
			logger.info("Descriptor targets an older not supported OSM deploymentdescriptorid: " + deploymentdescriptorid);		
			deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);	
			logger.info( "Status change of deployment " + deploymentdescriptor.getId()+", "+deploymentdescriptor.getName()+" to "+deploymentdescriptor.getStatus());					
			DeploymentDescriptor deploymentdescriptor_final = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
			logger.info("NS status changed is now :" + deploymentdescriptor_final.getStatus());															
		}
	}
		
	public String mapOSM5VNFD2ProductEagerDataJson(String yamlFile) throws JsonProcessingException 
	{
		VxFMetadata vxfMetadata = this.mapOSM5VNFD2Product(yamlFile);
		ObjectMapper mapper = new ObjectMapper();		
		String res = mapper.writeValueAsString( vxfMetadata );
		
		return res;		
	}	
	
	public String mapOSM5NSD2ProductEagerDataJson(String yamlFile) throws JsonProcessingException 
	{
		ExperimentMetadata vxfMetadata = this.mapOSM5NSD2Product(yamlFile);
		ObjectMapper mapper = new ObjectMapper();		
		String res = mapper.writeValueAsString( vxfMetadata );
		
		return res;		
	}

	public String mapOSM7VNFD2ProductEagerDataJson(String yamlFile) throws JsonProcessingException 
	{
		VxFMetadata vxfMetadata = this.mapOSM7VNFD2Product(yamlFile);
		ObjectMapper mapper = new ObjectMapper();		
		String res = mapper.writeValueAsString( vxfMetadata );
		
		return res;		
	}

	public String mapOSM7NSD2ProductEagerDataJson(String yamlFile) throws JsonProcessingException 
	{
		ExperimentMetadata vxfMetadata = this.mapOSM7NSD2Product(yamlFile);
		ObjectMapper mapper = new ObjectMapper();		
		String res = mapper.writeValueAsString( vxfMetadata );
		
		return res;		
	}


	public ExperimentMetadata mapOSM5NSD2Product(String yamlFile)
	{
		ExperimentMetadata prod = new ExperimentMetadata();
		
		// Get the nsd object out of the file info
		osm5.ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd ns;
		try {
			ns = OSM5NSExtractor.extractNsdDescriptorFromYAMLFile(yamlFile);
			
			prod.setName(ns.getAddedId());
			prod.setVersion(ns.getVersion());
			prod.setVendor(ns.getVendor());
			prod.setShortDescription(ns.getName());
			prod.setLongDescription(ns.getDescription());

			for (osm5.ns.yang.nfvo.nsd.rev170228.nsd.constituent.vnfd.ConstituentVnfd v : ns.getConstituentVnfd()) {
				ConstituentVxF cvxf = new ConstituentVxF();
				cvxf.setMembervnfIndex(Integer.parseInt(v.getMemberVnfIndex())); 
				cvxf.setVnfdidRef(v.getVnfdIdRef());				
				VxFMetadata vxf = (VxFMetadata) aMANOClient.getVxFByName(v.getVnfdIdRef());
				cvxf.setVxfref(vxf);
				((ExperimentMetadata) prod).getConstituentVxF().add(cvxf);
			}

			// Get NS Requirements from the nsd			
			OSM5NSRequirements vr = new OSM5NSRequirements(ns);
			// Store the requirements in HTML			
			prod.setDescriptorHTML(vr.toHTML());
			// Store the YAML file			
			prod.setDescriptor(yamlFile);
			prod.setIconsrc(ns.getLogo());									
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prod;	
	}
	
	public ExperimentMetadata mapOSM7NSD2Product(String yamlFile)
	{
		ExperimentMetadata prod = new ExperimentMetadata();
		
		// Get the nsd object out of the file info
		Nsd ns;
		try {
			ns = OSM7NSExtractor.extractNsdDescriptorFromYAMLFile(yamlFile);
			
			prod.setName(ns.getName());
			prod.setVersion(ns.getVersion());
			prod.setVendor(ns.getVendor());
			prod.setShortDescription(ns.getName());
			prod.setLongDescription(ns.getDescription());

			for (ConstituentVnfd v : ns.getConstituentVnfd()) {
				ConstituentVxF cvxf = new ConstituentVxF();
				cvxf.setMembervnfIndex(Integer.parseInt(v.getMemberVnfIndex())); 
				cvxf.setVnfdidRef((String)v.getVnfdIdRef());				
				VxFMetadata vxf = (VxFMetadata) aMANOClient.getVxFByName((String)v.getVnfdIdRef());
				cvxf.setVxfref(vxf);
				((ExperimentMetadata) prod).getConstituentVxF().add(cvxf);
			}

			// Get NS Requirements from the nsd			
			OSM7NSRequirements vr = new OSM7NSRequirements(ns);
			// Store the requirements in HTML			
			prod.setDescriptorHTML(vr.toHTML());
			// Store the YAML file			
			prod.setDescriptor(yamlFile);
			prod.setIconsrc(ns.getLogo());									
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prod;	
	}
	
	public VxFMetadata mapOSM5VNFD2Product(String yamlFile)
	{
		VxFMetadata prod = new VxFMetadata();
		// Get the vnfd object out of the file info
		osm5.ns.yang.nfvo.vnfd.rev170228.vnfd.catalog.Vnfd vnfd;
		try {
			vnfd = OSM5VNFDExtractor.extractVnfdDescriptorFromYAMLFile(yamlFile);
			// Get the name for the db							
			prod.setName(vnfd.getAddedId());
			prod.setVersion(vnfd.getVersion());
			prod.setVendor(vnfd.getVendor());
			prod.setShortDescription(vnfd.getName());
			prod.setLongDescription(vnfd.getDescription());
			
			((VxFMetadata) prod).setValidationStatus( ValidationStatus.UNDER_REVIEW  );
			((VxFMetadata) prod).getVfimagesVDU().clear();//clear previous referenced images
			for (osm5.ns.riftware._1._0.vnfd.base.rev170228.vnfd.descriptor.Vdu vdu : vnfd.getVdu()) {
				String imageName = vdu.getImage();
				if ( ( imageName != null) && (!imageName.equals("")) ){
					VFImage sm  = new VFImage();
					sm.setName( imageName );
					((VxFMetadata) prod).getVfimagesVDU().add( sm );					
				}
			}			
			
			// Get VNF Requirements from the vnfd
			OSM5VNFRequirements vr = new OSM5VNFRequirements(vnfd);
			// Store the requirements in HTML
			prod.setDescriptorHTML(vr.toHTML());
			// Store the YAML file
			prod.setDescriptor(yamlFile);
			prod.setIconsrc(vnfd.getLogo());			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prod;
	}
		
	public VxFMetadata mapOSM7VNFD2Product(String yamlFile)
	{
		VxFMetadata prod = new VxFMetadata();
		// Get the vnfd object out of the file info
		Vnfd vnfd;
		try {
			vnfd = OSM7VNFDExtractor.extractVnfdDescriptorFromYAMLFile(yamlFile);
			// Get the name for the db							
			prod.setName(vnfd.getName());
			prod.setVersion(vnfd.getVersion());
			prod.setVendor(vnfd.getVendor());
			prod.setShortDescription(vnfd.getName());
			prod.setLongDescription(vnfd.getDescription());
			
			((VxFMetadata) prod).setValidationStatus( ValidationStatus.UNDER_REVIEW  );
			((VxFMetadata) prod).getVfimagesVDU().clear();//clear previous referenced images
			for (Vdu vdu : vnfd.getVdu()) {
				String imageName = vdu.getImage();
				if ( ( imageName != null) && (!imageName.equals("")) ){
					VFImage sm  = new VFImage();
					sm.setName( imageName );
					((VxFMetadata) prod).getVfimagesVDU().add( sm );					
				}
			}			
			
			// Get VNF Requirements from the vnfd
			OSM7VNFRequirements vr = new OSM7VNFRequirements(vnfd);
			// Store the requirements in HTML
			prod.setDescriptorHTML(vr.toHTML());
			// Store the YAML file
			prod.setDescriptor(yamlFile);
			
			prod.setIconsrc(vnfd.getLogo());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prod;
	}		
}