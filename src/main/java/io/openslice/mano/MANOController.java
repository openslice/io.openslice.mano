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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.nsd.Df;
import org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.nsd.df.VnfProfile;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.vnfd.rev170228.vnfd.catalog.Vnfd;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import io.openslice.model.CompositeExperimentOnBoardDescriptor;
import io.openslice.model.CompositeVxFOnBoardDescriptor;
import io.openslice.model.ConstituentVxF;
import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.DeploymentDescriptorStatus;
import io.openslice.model.DeploymentDescriptorVxFInstanceInfo;
import io.openslice.model.ExperimentMetadata;
import io.openslice.model.ExperimentOnBoardDescriptor;
import io.openslice.model.Infrastructure;
import io.openslice.model.InfrastructureStatus;
import io.openslice.model.MANOprovider;
import io.openslice.model.OnBoardingStatus;
import io.openslice.model.PackagingFormat;
import io.openslice.model.ScaleDescriptor;
import io.openslice.model.VFImage;
import io.openslice.model.ValidationStatus;
import io.openslice.model.VxFMetadata;
import io.openslice.model.VxFOnBoardedDescriptor;
import io.openslice.sol005nbi.OSMClient;
import io.openslice.sol005nbi.ΑNSActionRequestPayload;
import jakarta.transaction.Transactional;
import io.openslice.sol005nbi.ANSScaleRequestPayload;
import io.openslice.centrallog.client.*;

import OSM10Util.OSM10ArchiveExtractor.OSM10NSExtractor;
import OSM10Util.OSM10ArchiveExtractor.OSM10VNFDExtractor;
import OSM10Util.OSM10NSReq.OSM10NSRequirements;
import OSM10Util.OSM10VNFReq.OSM10VNFRequirements;

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
	
	@Autowired
	private CentralLogger centralLogger;

	public void onBoardVxFToMANOProviderByFile() throws Exception {

	}

	/**
	 * onBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param vxfobds
	 * @throws Exception
	 */
	public void onBoardVxFToMANOProviderByOBD(VxFOnBoardedDescriptor vxfobd) throws Exception {

		vxfobd.setVxf(aMANOClient.getVxFById(vxfobd.getVxfid()));
		// Update the status and update the vxfobd
		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		centralLogger.log(CLevel.INFO,
				"Onboarding status change of VxF " + vxfobd.getVxf().getName() + " to " + vxfobd.getOnBoardingStatus(),
				compname);
		logger.info(
				"Onboarding status change of VxF " + vxfobd.getVxf().getName() + " to " + vxfobd.getOnBoardingStatus());
		// Set MANO Provider VxF ID
		vxfobd.setVxfMANOProviderID(vxfobd.getVxf().getName());
		// Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = aMANOClient.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}
		// Reload the vxf for the updated object.
		vxfobds.setVxf(aMANOClient.getVxFById(vxfobds.getVxfid()));

		String manoVersion = vxfobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion, vxfobds.getObMANOprovider().getApiEndpoint(),
					vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(),
					vxfobds.getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (Exception e) {
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			centralLogger.log(CLevel.ERROR,
					"onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.", compname);

			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF OnBoarding
			// action.");
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(manoVersion + " communication failed. Aborting VxF OnBoarding action.");
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus());

			// ?? This should change. Either by an activemq call or we should certify upon
			// Onboarding success.
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);

			// Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed(vxfobds_final);
			return;
		}

		ResponseEntity<String> response = null;
		response = osmClient.createVNFDPackage();
		if (response == null || response.getStatusCode().is4xxClientError()
				|| response.getStatusCode().is5xxServerError()) {
			logger.error("VNFD Package Creation failed.");
			// Set status
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(response.getBody().toString());
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			// Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed(vxfobds_final);
			return;
		} else {
			JSONObject obj = new JSONObject(response.getBody());
			String vnfd_id = obj.getString("id");
			logger.info(response.getStatusCode() + " replied. The new VNFD Package id is :" + vnfd_id);
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
				centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
						+ vxfobds.getOnBoardingStatus(), compname);
				// Set the reason of the failure
				vxfobds.setFeedbackMessage(response.getBody().toString());
				// Uncertify if it failed OnBoarding.
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
				aMANOClient.onBoardVxFFailed(vxfobds_final);
				return;
			}

			vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);

			vxfobds.setFeedbackMessage("OnBoarding Succeeded");

			// We select by design not to Certify upon OnBoarding but only on final version
			// is determined.
			// vxfobds.getVxf().setCertified(true);

			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID(vxfobd.getVxf().getName());
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			aMANOClient.onBoardVxFSucceded(vxfobds_final);

		}
	}

	public void onBoardVxFToMANOProviderByCompositeObj(CompositeVxFOnBoardDescriptor compositeobd) throws Exception {

		VxFOnBoardedDescriptor vxfobd = (VxFOnBoardedDescriptor) compositeobd.getObd();
		byte[] allBytes = compositeobd.getAllBytes();
		// Load the related VxFMetadata object
		vxfobd.setVxf(aMANOClient.getVxFById(vxfobd.getVxfid()));
		// Update the status and update the vxfobd
		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		centralLogger.log(CLevel.INFO,
				"Onboarding status change of VxF " + vxfobd.getVxf().getName() + " to " + vxfobd.getOnBoardingStatus(),
				compname);
		logger.info(
				"Onboarding status change of VxF " + vxfobd.getVxf().getName() + " to " + vxfobd.getOnBoardingStatus());
		// Set MANO Provider VxF ID
		vxfobd.setVxfMANOProviderID(vxfobd.getVxf().getName());
		// Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = aMANOClient.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}
		// Reload the vxf for the updated object.
		vxfobds.setVxf(aMANOClient.getVxFById(vxfobds.getVxfid()));

		String manoVersion = vxfobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			logger.info("manoVersion: " + manoVersion);
			osmClient = OSMClientFactory.getOSMClient(manoVersion, vxfobds.getObMANOprovider().getApiEndpoint(),
					vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(),
					vxfobds.getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
			if (osmClient == null) {
				new Exception("Cannot create osmClient");
			}
		} catch (Exception e) {
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			centralLogger.log(CLevel.ERROR,
					"onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.", compname);

			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF OnBoarding
			// action.");
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(manoVersion + " communication failed. Aborting VxF OnBoarding action.");
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus());

			// ?? This should change. Either by an activemq call or we should certify upon
			// Onboarding success.
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);

			// Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed(vxfobds_final);
			return;
		}

		ResponseEntity<String> response = null;
		response = osmClient.createVNFDPackage();
		if (response == null || response.getStatusCode().is4xxClientError()
				|| response.getStatusCode().is5xxServerError()) {
			logger.error("VNFD Package Creation failed.");
			// Set status
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(response.getBody().toString());
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			// Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed(vxfobds_final);
			return;
		} else {
			JSONObject obj = new JSONObject(response.getBody());
			String vnfd_id = obj.getString("id");
			logger.info(response.getStatusCode() + " replied. The new VNFD Package id is :" + vnfd_id);
			response = osmClient.uploadVNFDPackageContent(vnfd_id, allBytes);
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of VNFD Package Content failed. Deleting VNFD Package.");
				// Delete the package from the OSM
				osmClient.deleteVNFDPackage(vnfd_id);
				// Set status
				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
						+ vxfobds.getOnBoardingStatus(), compname);
				// Set the reason of the failure
				vxfobds.setFeedbackMessage(response.getBody().toString());
				// Uncertify if it failed OnBoarding.
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
				aMANOClient.onBoardVxFFailed(vxfobds_final);
				return;
			}

			vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);

			vxfobds.setFeedbackMessage("OnBoarding Succeeded");

			// We select by design not to Certify upon OnBoarding but only on final version
			// is determined.
			// vxfobds.getVxf().setCertified(true);

			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID(vxfobd.getVxf().getName());
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			aMANOClient.onBoardVxFSucceded(vxfobds_final);
		}
	}

	public void onBoardVxFToMANOProvider(VxFOnBoardedDescriptor vxfobd, byte[] allBytes) throws Exception {

		// Load the related VxFMetadata object
		vxfobd.setVxf(aMANOClient.getVxFById(vxfobd.getVxfid()));
		// Update the status and update the vxfobd
		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		centralLogger.log(CLevel.INFO,
				"Onboarding status change of VxF " + vxfobd.getVxf().getName() + " to " + vxfobd.getOnBoardingStatus(),
				compname);
		logger.info(
				"Onboarding status change of VxF " + vxfobd.getVxf().getName() + " to " + vxfobd.getOnBoardingStatus());
		// Set MANO Provider VxF ID
		vxfobd.setVxfMANOProviderID(vxfobd.getVxf().getName());
		// Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = aMANOClient.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}
		// Reload the vxf for the updated object.
		vxfobds.setVxf(aMANOClient.getVxFById(vxfobds.getVxfid()));

		String manoVersion = vxfobds.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion, vxfobds.getObMANOprovider().getApiEndpoint(),
					vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(),
					vxfobds.getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (Exception e) {
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			centralLogger.log(CLevel.ERROR,
					"onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.", compname);

			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF OnBoarding
			// action.");
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(manoVersion + " communication failed. Aborting VxF OnBoarding action.");
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus());

			// ?? This should change. Either by an activemq call or we should certify upon
			// Onboarding success.
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);

			// Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed(vxfobds_final);
			return;
		}

		ResponseEntity<String> response = null;
		response = osmClient.createVNFDPackage();
		if (response == null || response.getStatusCode().is4xxClientError()
				|| response.getStatusCode().is5xxServerError()) {
			logger.error("VNFD Package Creation failed.");
			// Set status
			vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);
			// Set the reason of the failure
			vxfobds.setFeedbackMessage(response.getBody().toString());
			// Uncertify if it failed OnBoarding.
			vxfobds.getVxf().setCertified(false);
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			// Send a message for OnBoarding Failure due to osm connection failure
			aMANOClient.onBoardVxFFailed(vxfobds_final);
			return;
		} else {
			JSONObject obj = new JSONObject(response.getBody());
			String vnfd_id = obj.getString("id");
			logger.info(response.getStatusCode() + " replied. The new VNFD Package id is :" + vnfd_id);
			response = osmClient.uploadVNFDPackageContent(vnfd_id, allBytes);
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of VNFD Package Content failed. Deleting VNFD Package.");
				// Delete the package from the OSM
				osmClient.deleteVNFDPackage(vnfd_id);
				// Set status
				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
						+ vxfobds.getOnBoardingStatus(), compname);
				// Set the reason of the failure
				vxfobds.setFeedbackMessage(response.getBody().toString());
				// Uncertify if it failed OnBoarding.
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
				aMANOClient.onBoardVxFFailed(vxfobds_final);
				return;
			}

			vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			centralLogger.log(CLevel.INFO, "Onboarding status change of VxF " + vxfobds.getVxf().getName() + " to "
					+ vxfobds.getOnBoardingStatus(), compname);

			vxfobds.setFeedbackMessage("OnBoarding Succeeded");

			// We select by design not to Certify upon OnBoarding but only on final version
			// is determined.
			// vxfobds.getVxf().setCertified(true);

			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID(vxfobd.getVxf().getName());
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = aMANOClient.updateVxFOnBoardedDescriptor(vxfobds);
			aMANOClient.onBoardVxFSucceded(vxfobds_final);
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
		String manoVersion = obd.getObMANOprovider().getSupportedMANOplatform().getVersion();
		OSMClient osmClient = null;
		try {
			osmClient = OSMClientFactory.getOSMClient(manoVersion, obd.getObMANOprovider().getApiEndpoint(),
					obd.getObMANOprovider().getUsername(), obd.getObMANOprovider().getPassword(),
					obd.getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (HttpStatusCodeException e) {
			logger.error("offBoardVxFFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			centralLogger.log(CLevel.ERROR,
					"offBoardVxFFromMANOProvider, OSM5 fails authentication. Aborting VxF offboarding action.",
					compname);
			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting VxF offboarding
			// action.");
			return ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders())
					.body(e.getResponseBodyAsString());
		}

		response = osmClient.deleteVNFDPackage(vnfd_id);
		if (obd.getObMANOprovider().getSupportedMANOplatform().getVersion().equals("OSMvTWO")) {
			response = new ResponseEntity<>("Not implemented for OSMvTWO", HttpStatus.CREATED);
		}
		return response;
	}

	public void onBoardNSDToMANOProviderByFile() throws Exception {

	}

	public void onBoardNSDToMANOProviderByCompositeObj(CompositeExperimentOnBoardDescriptor compexpobd)
			throws Exception {

		ExperimentOnBoardDescriptor uexpobd = compexpobd.getObd();
		byte[] allBytes = compexpobd.getAllBytes();

		ExperimentMetadata em = uexpobd.getExperiment();
		// if (em == null) {
		em = (ExperimentMetadata) aMANOClient.getNSDById(uexpobd.getExperimentid());
		// }
		uexpobd.setExperiment(em);
		uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);

		// This is the Deployment ID for the portal
		uexpobd.setDeployId(UUID.randomUUID().toString());
		centralLogger.log(CLevel.INFO, "Onboarding status change of Experiment " + uexpobd.getExperiment().getName()
				+ " to " + uexpobd.getOnBoardingStatus(), compname);
		logger.info("Onboarding status change of Experiment " + uexpobd.getExperiment().getName() + " to "
				+ uexpobd.getOnBoardingStatus());

		// uexpobd.setVxfMANOProviderID(em.getName()); // Possible Error. This probably
		// needs to be
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
			osmClient = OSMClientFactory.getOSMClient(manoVersion, uexpobds.getObMANOprovider().getApiEndpoint(),
					uexpobds.getObMANOprovider().getUsername(), uexpobds.getObMANOprovider().getPassword(),
					uexpobds.getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (Exception e) {
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			centralLogger.log(CLevel.ERROR, "onBoardNSDFromMANOProvider, " + manoVersion
					+ " fails authentication. Aborting NSD Onboarding action.", compname);
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion
					+ " fails authentication. Aborting NSD Onboarding action.");
			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD Onboarding
			// action.");
			// Set the reason of the failure
			uexpobds.setFeedbackMessage("OSM communication failed. Aborting NSD Onboarding action.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.ERROR, "Onboarding Status change of Experiment "
					+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding Status change of Experiment " + uexpobds.getExperiment().getName() + " to "
					+ uexpobds.getOnBoardingStatus());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed(uexpobds);
			return;
		}

		ResponseEntity<String> response = null;
		response = osmClient.createNSDPackage();
		if (response == null || response.getStatusCode().is4xxClientError()
				|| response.getStatusCode().is5xxServerError()) {
			logger.error("Creation of NSD Package Content failed. Deleting NSD Package.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding Status change of Experiment "
					+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
			// Set the reason of the failure
			uexpobds.setFeedbackMessage(response.getBody().toString());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed(uexpobds);
			return;
		} else {
			JSONObject obj = new JSONObject(response.getBody());
			String nsd_id = obj.getString("id");
			logger.info(response.getStatusCode() + " replied. The new NSD Package id is :" + nsd_id);
			response = osmClient.uploadNSDPackageContent(nsd_id, allBytes);
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
				osmClient.deleteNSDPackage(nsd_id);
				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				centralLogger.log(CLevel.INFO, "Onboarding Status change of Experiment "
						+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
				logger.error("Onboarding Status change of Experiment " + uexpobds.getExperiment().getName() + " to "
						+ uexpobds.getOnBoardingStatus());

				if (response.getBody() != null) {
					uexpobds.setFeedbackMessage(response.getBody().toString());
				}
				logger.error("Onboarding Feedbacj Message of Experiment " + uexpobds.getExperiment().getName() + " is "
						+ uexpobds.getFeedbackMessage());
				// Set Valid to false if it fails OnBoarding
				uexpobds.getExperiment().setValid(false);
				aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
				aMANOClient.onBoardNSDFailed(uexpobds);
				return;
			} else {
				uexpobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
				centralLogger.log(CLevel.INFO, "Onboarding Status change of Experiment "
						+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
				logger.info("Onboarding Status change of Experiment " + uexpobds.getExperiment().getName() + " to "
						+ uexpobds.getOnBoardingStatus());
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
				aMANOClient.onBoardNSDSucceded(uexpobds);
			}
		}
	}

	public void onBoardNSDToMANOProvider(ExperimentOnBoardDescriptor uexpobd) throws Exception {

		ExperimentMetadata em = uexpobd.getExperiment();
		if (em == null) {
			em = (ExperimentMetadata) aMANOClient.getNSDById(uexpobd.getExperimentid());
		}
		uexpobd.setExperiment(em);
		uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);

		// This is the Deployment ID for the portal
		uexpobd.setDeployId(UUID.randomUUID().toString());
		centralLogger.log(CLevel.INFO, "Onboarding status change of Experiment " + uexpobd.getExperiment().getName()
				+ " to " + uexpobd.getOnBoardingStatus(), compname);
		logger.info("Onboarding status change of Experiment " + uexpobd.getExperiment().getName() + " to "
				+ uexpobd.getOnBoardingStatus());

		// uexpobd.setVxfMANOProviderID(em.getName()); // Possible Error. This probably
		// needs to be
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
			osmClient = OSMClientFactory.getOSMClient(manoVersion, uexpobd.getObMANOprovider().getApiEndpoint(),
					uexpobd.getObMANOprovider().getUsername(), uexpobd.getObMANOprovider().getPassword(),
					uexpobd.getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (Exception e) {
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			centralLogger.log(CLevel.ERROR, "onBoardNSDFromMANOProvider, " + manoVersion
					+ " fails authentication. Aborting NSD Onboarding action.", compname);
			logger.error("onBoardNSDFromMANOProvider, " + manoVersion
					+ " fails authentication. Aborting NSD Onboarding action.");
			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD Onboarding
			// action.");
			// Set the reason of the failure
			uexpobds.setFeedbackMessage("OSM communication failed. Aborting NSD Onboarding action.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.ERROR, "Onboarding Status change of Experiment "
					+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
			logger.error("Onboarding Status change of Experiment " + uexpobds.getExperiment().getName() + " to "
					+ uexpobds.getOnBoardingStatus());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed(uexpobds);
			return;
		}

		ResponseEntity<String> response = null;
		response = osmClient.createNSDPackage();
		if (response == null || response.getStatusCode().is4xxClientError()
				|| response.getStatusCode().is5xxServerError()) {
			logger.error("Creation of NSD Package Content failed. Deleting NSD Package.");
			uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Onboarding Status change of Experiment "
					+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
			// Set the reason of the failure
			uexpobds.setFeedbackMessage(response.getBody().toString());
			// Set Valid to false if it fails OnBoarding
			uexpobds.getExperiment().setValid(false);
			aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
			aMANOClient.onBoardNSDFailed(uexpobds);
			return;
		} else {
			JSONObject obj = new JSONObject(response.getBody());
			String nsd_id = obj.getString("id");
			logger.info(response.getStatusCode() + " replied. The new NSD Package id is :" + nsd_id);
			String pLocation = uexpobd.getExperiment().getPackageLocation();
			logger.info("Package location to onboard is :" + pLocation);
			response = osmClient.uploadNSDPackageContent(nsd_id, pLocation);
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
				osmClient.deleteNSDPackage(nsd_id);
				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				centralLogger.log(CLevel.INFO, "Onboarding Status change of Experiment "
						+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
				logger.error("Onboarding Status change of Experiment " + uexpobds.getExperiment().getName() + " to "
						+ uexpobds.getOnBoardingStatus());
				uexpobds.setFeedbackMessage(response.getBody().toString());
				logger.error("Onboarding Feedbacj Message of Experiment " + uexpobds.getExperiment().getName() + " is "
						+ uexpobds.getFeedbackMessage());
				// Set Valid to false if it fails OnBoarding
				uexpobds.getExperiment().setValid(false);
				aMANOClient.updateExperimentOnBoardDescriptor(uexpobds);
				aMANOClient.onBoardNSDFailed(uexpobds);
				return;
			} else {
				uexpobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
				centralLogger.log(CLevel.INFO, "Onboarding Status change of Experiment "
						+ uexpobds.getExperiment().getName() + " to " + uexpobds.getOnBoardingStatus(), compname);
				logger.info("Onboarding Status change of Experiment " + uexpobds.getExperiment().getName() + " to "
						+ uexpobds.getOnBoardingStatus());
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
				aMANOClient.onBoardNSDSucceded(uexpobds);
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
			osmClient = OSMClientFactory.getOSMClient(manoVersion, uexpobd.getObMANOprovider().getApiEndpoint(),
					uexpobd.getObMANOprovider().getUsername(), uexpobd.getObMANOprovider().getPassword(),
					uexpobd.getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (HttpStatusCodeException e) {
			logger.error("offBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.");
			centralLogger.log(CLevel.ERROR,
					"offBoardNSDFromMANOProvider, " + manoVersion + " fails authentication. Aborting action.",
					compname);
			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NSD offboarding
			// action.");
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
			logger.info("Deployment with id" + deployment_descriptor_tmp.getName() + " with status "
					+ deployment_descriptor_tmp.getStatus() + " is going to be terminated");

			// Terminate the deployment
			this.terminateNSFromMANOProvider(deployment_descriptor_tmp.getId());
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
	
	public void getScaleAlertMessageBody(String Body)
	{
		logger.info("Scale Alert Body "+Body);
	}	
	
	@Transactional
	public void checkAndUpdateMANOProvidersResources() {

		// Get MANO Providers
		List<MANOprovider> mps = aMANOClient.getMANOprovidersForSync();
		// For each MANO Provider
		for (int i = 0; i < mps.size(); i++) 
		{
			//******************************************************************
			MANOprovider mp = aMANOClient.getMANOproviderByID(mps.get(i).getId());
			String manoVersion = mp.getSupportedMANOplatform().getVersion();			
			OSMClient osmClient = null;		
						
			try {
				osmClient = OSMClientFactory.getOSMClient(manoVersion, mp.getApiEndpoint(),
						mp.getUsername(), mp.getPassword(), mp.getProject());
			} catch (Exception e) {
				logger.error(manoVersion + " fails authentication. Details: " + mp.getName() + " " + mp.getApiEndpoint() );
				centralLogger.log(CLevel.ERROR, manoVersion + " fails authentication. Details: " + mp.getName() + " " + mp.getApiEndpoint() , compname);
				continue;
			}
			synchronizeVIMs(osmClient, mp);
			synchronizeVNFDs(osmClient, mp);
			synchronizeNSDs(osmClient, mp);
			//synchronizeDeployments(osmClient, mp);
		}
		
	}

	private void synchronizeVIMs(OSMClient osmClient, MANOprovider mp)
	{
		logger.info("Synchronize VIMs for MANOProvider "+mp.getName());
		//******************************************************************
		// Get available VIMs from the portal's Database
		List<Infrastructure> infrastructures = aMANOClient.getInfrastructures();
		for(int j = 0; j < infrastructures.size(); j++)
		{
			logger.debug("Found VIM with id:"+infrastructures.get(j).toJSON());
			//centralLogger.log( CLevel.INFO, "Synchronize VIM with id:"+infrastructures.get(j).getVIMid() , compname);
		}				
		//******************************************************************
		// Get VIMs from OSM MANO.
		ResponseEntity<String> vims_list_entity = osmClient.getVIMs();
		VIMCreateRequestPayload[] vim_osm_array;
		if (vims_list_entity == null || vims_list_entity.getStatusCode().is4xxClientError()
				|| vims_list_entity.getStatusCode().is5xxServerError()) {
			logger.error("VIMs List Get Request failed. Status Code:" + vims_list_entity.getStatusCode().toString()
					+ ", Payload:" + vims_list_entity.getBody().toString());
		} else {
			logger.debug("Got VIM list for MANOProvider "+mp.getName()+": "+vims_list_entity.getBody());
			ObjectMapper mapper = new ObjectMapper();
			try {
				vim_osm_array = (VIMCreateRequestPayload[]) mapper.readValue(vims_list_entity.getBody(), VIMCreateRequestPayload[].class);
				for(VIMCreateRequestPayload vim : vim_osm_array)
				{
					// Εδώ θα συγκρίνουμε αυτό που λάβαμε απο τη βάση με αυτό που λάβαμε απο το osm και θα το ανανεώσουμε στη βάση.
					logger.debug("VIM to JSON:"+vim.toJSON());
					boolean exists_in_db = false;
					for(Infrastructure dbvim : infrastructures)
					{
						if(dbvim.getVIMid().equals(vim.get_id()))
						{
							logger.info("VIM "+vim.get_id()+" already exists");
							exists_in_db = true;
						}
					}
					if(exists_in_db == false)
					{
						//Map osm vim to db vim object
						Infrastructure newInfrastructure = new Infrastructure();
						newInfrastructure.setVIMid(vim.get_id());
						newInfrastructure.setName(vim.getName());
						newInfrastructure.setOrganization(vim.getName());
						newInfrastructure.setDatacentername(vim.getDatacenter());
						newInfrastructure.setMp(mp);
						newInfrastructure.setDatacentername(mp.getName());
						newInfrastructure.setInfrastructureStatus(InfrastructureStatus.OSM_PRESENT);
						newInfrastructure.setDateCreated(new Date());
						//newInfrastructure.setMANOProvider(mps.get(i).getId());
						//Add object to db
						aMANOClient.addInfrastructure(newInfrastructure);
						logger.info("VIM "+vim.get_id()+" added");
					}
				}
				
			} catch (IllegalStateException | IOException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
				return;
			}	
			
			// Check for orphaned 
			for(Infrastructure infrastructure : infrastructures)
			{
				boolean exists_in_osm = false;
				// For each object
				for(VIMCreateRequestPayload vim : vim_osm_array)
				{
					// Εδώ θα συγκρίνουμε αυτό που λάβαμε απο τη βάση με αυτό που λάβαμε απο το osm και θα το ανανεώσουμε στη βάση.
					logger.debug("VIM to JSON:"+vim.toJSON());
					if(infrastructure.getVIMid().equals(vim.get_id()))
					{
						logger.info("VIM "+vim.get_id()+" still exists in osm");
						exists_in_osm = true;
						infrastructure.setInfrastructureStatus(InfrastructureStatus.OSM_PRESENT);
						infrastructure = aMANOClient.updateInfrastructure(infrastructure);
						if(infrastructure != null)
						{
							logger.info("synchronizeVIMs: Infrastructure " + infrastructure.getVIMid() + " updated Infrastructure status to OSM_PRESENT");
						}
						else
						{
							logger.warn("synchronizeVIMs: Infrastructure " + infrastructure.getVIMid() + " update to Infrastructure status to OSM_PRESENT FAILED");	
						}
					}
				}
				try
				{
					if(exists_in_osm == false && infrastructure.getMp().getName().equals(mp.getName()) && infrastructure.getMp().getProject().equals(mp.getProject()))
					{
						logger.debug("VIM with id "+ infrastructure.getVIMid()+" does not exist and MP name '"+infrastructure.getMp().getName()+"'='"+mp.getName()+"' and project '"+infrastructure.getMp().getProject()+"'='"+mp.getProject()+"'");
						exists_in_osm = false;
						infrastructure.setInfrastructureStatus(InfrastructureStatus.OSM_MISSING);
						infrastructure = aMANOClient.updateInfrastructure(infrastructure);
						if(infrastructure != null)
						{
							logger.info("synchronizeVIMs: Infrastructure " + infrastructure.getVIMid() + " updated Infrastructure status to OSM_MISSING");
						}
						else
						{
							logger.warn("synchronizeVIMs: Infrastructure " + infrastructure.getVIMid() + " update to Infrastructure status to OSM_MISSING FAILED");	
						}
					}
				}
				catch(Exception e)
				{
					logger.error("Possible missing MP for VIM with id "+infrastructure.getVIMid()+". VIM OSM Presence check failed and skipped.");;
				}
			}						
			
		}						
	}
	
	private void synchronizeVNFDs(OSMClient osmClient, MANOprovider mp)
	{
		//******************************************************************
		// Get ExperimentMetadata available from the portal's Database
		//List<VxFMetadata> vxfMetadatas = aMANOClient.getVnfds();
		List<VxFOnBoardedDescriptor> vxFOnBoardedDescriptors = aMANOClient.getVxFOnBoardedDescriptors();
		
		//******************************************************************
		// Get VNFDs from OSM MANO.
		ResponseEntity<String> vnfds_list_entity = osmClient.getVNFDescriptorsList();
		if (vnfds_list_entity == null || vnfds_list_entity.getStatusCode().is4xxClientError()
				|| vnfds_list_entity.getStatusCode().is5xxServerError()) {
			logger.error("VNFDs List Get Request failed. Status Code:" + vnfds_list_entity.getStatusCode().toString()
					+ ", Payload:" + vnfds_list_entity.getBody().toString());
		} else {
			logger.debug("Got VNFD list "+vnfds_list_entity.getBody());			
			if(mp.getSupportedMANOplatform().getVersion().equals("OSMvTEN"))
			{					
				synchronizeVNFDsOSM10(vxFOnBoardedDescriptors, vnfds_list_entity, mp);
			}
			if(mp.getSupportedMANOplatform().getVersion().equals("OSMvELEVEN"))
			{					
				synchronizeVNFDsOSM11(vxFOnBoardedDescriptors, vnfds_list_entity, mp);
			}
			if(mp.getSupportedMANOplatform().getVersion().equals("OSMvTHIRTEEN"))
			{					
				synchronizeVNFDsOSM13(vxFOnBoardedDescriptors, vnfds_list_entity, mp);
			}
		}		
	}
	

	private void synchronizeVNFDsOSM10(List<VxFOnBoardedDescriptor> vxFOnBoardedDescriptors, ResponseEntity<String> vnfds_list_entity, MANOprovider mp)
	{
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Parse the json list of objects
			org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[] vnfd_array = (org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[]) mapper.readValue(vnfds_list_entity.getBody(), org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[].class);
			// For each object
			for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd : vnfd_array)
			{
				String jsonInString=null;
				ObjectMapper mapper2 = new ObjectMapper();
				mapper2.setSerializationInclusion(Include.NON_NULL);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(vnfd);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
										
				// Compare db derived data with osm derived data and update the database.
				logger.debug("VNFD to JSON:"+jsonInString);
				logger.info("VNFD " + vnfd.getId() + " added");						

				// Get the mapped ExperimentMetadata object
				VxFMetadata prod = mapOSM10VNFD2ProductFromJSON(vnfd);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(prod);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				logger.debug("Prod created:"+jsonInString);	
				
				// Now that we have the product
				// Check if the VxFMetadata uuid already exists in the database
				
				boolean exists_in_db = false;
				for(VxFOnBoardedDescriptor dbvxfobd : vxFOnBoardedDescriptors)
				{
					if(dbvxfobd.getDeployId().equals(vnfd.getId()))
					{
						logger.info("VNFD " + vnfd.getId() + " already exists");
						exists_in_db = true;
					}
				}
				if(exists_in_db == false)
				{
					logger.info("VNFD " + vnfd.getId() + " does not exist. Adding to db.");
					exists_in_db = true;
					//Map osm vim to db vim object
					VxFMetadata newVxFMetadata = new VxFMetadata();
					newVxFMetadata.setUuid(vnfd.getId());
					// Combine the vnfd name with the OSM name.
					newVxFMetadata.setName(vnfd.getProductName());
					newVxFMetadata.setValidationStatus(ValidationStatus.COMPLETED);
					newVxFMetadata.setDateCreated(new Date());
					newVxFMetadata.setDateUpdated(new Date());
					newVxFMetadata.setShortDescription(vnfd.getProductName()+"@"+mp.getName());
					newVxFMetadata.setPackagingFormat(PackagingFormat.OSMvTEN);
					//Get the manoServiceOwner to add it							
					newVxFMetadata.setOwner(aMANOClient.getPortalUserByUsername("manoService"));
					
					// Get VNF Requirements from the vnfd
					OSM10VNFRequirements vnfreq = new OSM10VNFRequirements(vnfd);
					// Store the requirements in HTML
					newVxFMetadata.setDescriptorHTML(vnfreq.toHTML());
					// Store the YAML file
					newVxFMetadata.setDescriptor(vnfds_list_entity.getBody());
					
					//Add VxFMetadata object to db and get the generated object
					newVxFMetadata = aMANOClient.addVxFMetadata(newVxFMetadata);
					logger.info("VxF " + vnfd.getId() + " added with VxFMetadata id="+newVxFMetadata.getId());
					
					//Create the OnboardedDescriptor
					VxFOnBoardedDescriptor newVxFOnBoardedDescriptor = new VxFOnBoardedDescriptor(newVxFMetadata);
					newVxFOnBoardedDescriptor.setDeployId(vnfd.getId());
					newVxFOnBoardedDescriptor.setFeedbackMessage("Automatically Retrieved from OSM");
					newVxFOnBoardedDescriptor.setLastOnboarding(new Date());
					newVxFOnBoardedDescriptor.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
					newVxFOnBoardedDescriptor.setUuid(vnfd.getId());
					newVxFOnBoardedDescriptor.setVxfMANOProviderID(vnfd.getProductName());
					newVxFOnBoardedDescriptor.setObMANOprovider(mp);
					newVxFOnBoardedDescriptor.setVxf(aMANOClient.getVxFById(newVxFMetadata.getId()));
					//Add VxFOnBoardedDescriptor object to db and get the generated object
					newVxFOnBoardedDescriptor=aMANOClient.addVxFOnBoardedDescriptor(newVxFOnBoardedDescriptor);
					logger.info("VxFOnBoardedDescriptor " + newVxFOnBoardedDescriptor.getId() + " added");
					
				}
			}
			// Check for orphaned 
			for(VxFOnBoardedDescriptor dbvxfobd : vxFOnBoardedDescriptors)
			{
				boolean exists_in_osm = false;
				// For each object
				for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd : vnfd_array)
				{
					if(dbvxfobd.getDeployId().equals(vnfd.getId()))
					{
						logger.info("VNFD " + vnfd.getId() + " exists in osm");
						exists_in_osm = true;
					}
				}
				logger.debug(dbvxfobd.getDeployId()+" does not exist and MP name '"+dbvxfobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbvxfobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'?");
				if(exists_in_osm == false && dbvxfobd.getObMANOprovider().getName().equals(mp.getName()) && dbvxfobd.getObMANOprovider().getProject().equals(mp.getProject()))
				{
					logger.debug(dbvxfobd.getDeployId()+" does not exist and MP name '"+dbvxfobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbvxfobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'");
					dbvxfobd.setOnBoardingStatus(OnBoardingStatus.OSM_MISSING);
					dbvxfobd = aMANOClient.updateVxFOnBoardedDescriptor(dbvxfobd);
					logger.info("synchronizeVNFDsOSM10 : VxFOnBoardedDescriptor " + dbvxfobd.getId() + " updated OnboardingStatus to OSM_MISSING");
				}
			}
		} catch (IllegalStateException | IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}			
	}
	
	private void synchronizeVNFDsOSM11(List<VxFOnBoardedDescriptor> vxFOnBoardedDescriptors, ResponseEntity<String> vnfds_list_entity, MANOprovider mp)
	{
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Parse the json list of objects
			org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[] vnfd_array = (org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[]) mapper.readValue(vnfds_list_entity.getBody(), org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[].class);
			// For each object
			for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd : vnfd_array)
			{
				String jsonInString=null;
				ObjectMapper mapper2 = new ObjectMapper();
				mapper2.setSerializationInclusion(Include.NON_NULL);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(vnfd);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
										
				// Compare db derived data with osm derived data and update the database.
				logger.debug("VNFD to JSON:"+jsonInString);
				logger.info("VNFD " + vnfd.getId() + " added");						

				// Get the mapped ExperimentMetadata object
				VxFMetadata prod = mapOSM10VNFD2ProductFromJSON(vnfd);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(prod);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				logger.debug("Prod created:"+jsonInString);	
				
				// Now that we have the product
				// Check if the VxFMetadata uuid already exists in the database
				
				boolean exists_in_db = false;
				for(VxFOnBoardedDescriptor dbvxfobd : vxFOnBoardedDescriptors)
				{
					if(dbvxfobd.getDeployId().equals(vnfd.getId()))
					{
						logger.info("VNFD " + vnfd.getId() + " already exists");
						exists_in_db = true;
					}
				}
				if(exists_in_db == false)
				{
					logger.info("VNFD " + vnfd.getId() + " does not exist. Adding to db.");
					exists_in_db = true;
					//Map osm vim to db vim object
					VxFMetadata newVxFMetadata = new VxFMetadata();
					newVxFMetadata.setUuid(vnfd.getId());
					// Combine the vnfd name with the OSM name.
					newVxFMetadata.setName(vnfd.getProductName());
					newVxFMetadata.setValidationStatus(ValidationStatus.COMPLETED);
					newVxFMetadata.setDateCreated(new Date());
					newVxFMetadata.setDateUpdated(new Date());
					newVxFMetadata.setShortDescription(vnfd.getProductName()+"@"+mp.getName());
					newVxFMetadata.setPackagingFormat(PackagingFormat.OSMvELEVEN);
					//Get the manoServiceOwner to add it							
					newVxFMetadata.setOwner(aMANOClient.getPortalUserByUsername("manoService"));
					
					// Get VNF Requirements from the vnfd
					OSM10VNFRequirements vnfreq = new OSM10VNFRequirements(vnfd);
					// Store the requirements in HTML
					newVxFMetadata.setDescriptorHTML(vnfreq.toHTML());
					// Store the YAML file
					newVxFMetadata.setDescriptor(vnfds_list_entity.getBody());
					
					//Add VxFMetadata object to db and get the generated object
					newVxFMetadata = aMANOClient.addVxFMetadata(newVxFMetadata);
					logger.info("VxF " + vnfd.getId() + " added with VxFMetadata id="+newVxFMetadata.getId());
					
					//Create the OnboardedDescriptor
					VxFOnBoardedDescriptor newVxFOnBoardedDescriptor = new VxFOnBoardedDescriptor(newVxFMetadata);
					newVxFOnBoardedDescriptor.setDeployId(vnfd.getId());
					newVxFOnBoardedDescriptor.setFeedbackMessage("Automatically Retrieved from OSM");
					newVxFOnBoardedDescriptor.setLastOnboarding(new Date());
					newVxFOnBoardedDescriptor.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
					newVxFOnBoardedDescriptor.setUuid(vnfd.getId());
					newVxFOnBoardedDescriptor.setVxfMANOProviderID(vnfd.getProductName());
					newVxFOnBoardedDescriptor.setObMANOprovider(mp);
					newVxFOnBoardedDescriptor.setVxf(aMANOClient.getVxFById(newVxFMetadata.getId()));
					//Add VxFOnBoardedDescriptor object to db and get the generated object
					newVxFOnBoardedDescriptor=aMANOClient.addVxFOnBoardedDescriptor(newVxFOnBoardedDescriptor);
					logger.info("VxFOnBoardedDescriptor " + newVxFOnBoardedDescriptor.getId() + " added");
					
				}
			}
			// Check for orphaned 
			for(VxFOnBoardedDescriptor dbvxfobd : vxFOnBoardedDescriptors)
			{
				boolean exists_in_osm = false;
				// For each object
				for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd : vnfd_array)
				{
					if(dbvxfobd.getDeployId().equals(vnfd.getId()))
					{
						logger.info("VNFD " + vnfd.getId() + " exists in osm");
						exists_in_osm = true;
					}
				}
				if(exists_in_osm == false && dbvxfobd.getObMANOprovider().getName().equals(mp.getName()) && dbvxfobd.getObMANOprovider().getProject().equals(mp.getProject()))
				{
					logger.debug(dbvxfobd.getDeployId()+" does not exist and MP name '"+dbvxfobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbvxfobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'");
					dbvxfobd.setOnBoardingStatus(OnBoardingStatus.OSM_MISSING);
					dbvxfobd = aMANOClient.updateVxFOnBoardedDescriptor(dbvxfobd);
					logger.info("synchronizeVNFDsOSM11 : VxFOnBoardedDescriptor " + dbvxfobd.getId() + " updated OnboardingStatus to OSM_MISSING");
				}
			}
		} catch (IllegalStateException | IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}			
	}

	private void synchronizeVNFDsOSM13(List<VxFOnBoardedDescriptor> vxFOnBoardedDescriptors, ResponseEntity<String> vnfds_list_entity, MANOprovider mp)
	{
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Parse the json list of objects
			org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[] vnfd_array = (org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[]) mapper.readValue(vnfds_list_entity.getBody(), org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd[].class);
			// For each object
			for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd : vnfd_array)
			{
				String jsonInString=null;
				ObjectMapper mapper2 = new ObjectMapper();
				mapper2.setSerializationInclusion(Include.NON_NULL);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(vnfd);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
										
				// Compare db derived data with osm derived data and update the database.
				logger.debug("VNFD to JSON:"+jsonInString);
				logger.info("VNFD " + vnfd.getId() + " added");						

				// Get the mapped ExperimentMetadata object
				VxFMetadata prod = mapOSM10VNFD2ProductFromJSON(vnfd);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(prod);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				logger.debug("Prod created:"+jsonInString);	
				
				// Now that we have the product
				// Check if the VxFMetadata uuid already exists in the database
				
				boolean exists_in_db = false;
				for(VxFOnBoardedDescriptor dbvxfobd : vxFOnBoardedDescriptors)
				{
					if(dbvxfobd.getDeployId().equals(vnfd.getId()))
					{
						logger.info("VNFD " + vnfd.getId() + " already exists");
						exists_in_db = true;
					}
				}
				if(exists_in_db == false)
				{
					logger.info("VNFD " + vnfd.getId() + " does not exist. Adding to db.");
					exists_in_db = true;
					//Map osm vim to db vim object
					VxFMetadata newVxFMetadata = new VxFMetadata();
					newVxFMetadata.setUuid(vnfd.getId());
					// Combine the vnfd name with the OSM name.
					newVxFMetadata.setName(vnfd.getProductName());
					newVxFMetadata.setValidationStatus(ValidationStatus.COMPLETED);
					newVxFMetadata.setDateCreated(new Date());
					newVxFMetadata.setDateUpdated(new Date());
					newVxFMetadata.setShortDescription(vnfd.getProductName()+"@"+mp.getName());
					newVxFMetadata.setPackagingFormat(PackagingFormat.OSMvTHIRTEEN);
					//Get the manoServiceOwner to add it							
					newVxFMetadata.setOwner(aMANOClient.getPortalUserByUsername("manoService"));
					
					// Get VNF Requirements from the vnfd
					OSM10VNFRequirements vnfreq = new OSM10VNFRequirements(vnfd);
					// Store the requirements in HTML
					newVxFMetadata.setDescriptorHTML(vnfreq.toHTML());
					// Store the YAML file
					newVxFMetadata.setDescriptor(vnfds_list_entity.getBody());
					
					//Add VxFMetadata object to db and get the generated object
					newVxFMetadata = aMANOClient.addVxFMetadata(newVxFMetadata);
					logger.info("VxF " + vnfd.getId() + " added with VxFMetadata id="+newVxFMetadata.getId());
					
					//Create the OnboardedDescriptor
					VxFOnBoardedDescriptor newVxFOnBoardedDescriptor = new VxFOnBoardedDescriptor(newVxFMetadata);
					newVxFOnBoardedDescriptor.setDeployId(vnfd.getId());
					newVxFOnBoardedDescriptor.setFeedbackMessage("Automatically Retrieved from OSM");
					newVxFOnBoardedDescriptor.setLastOnboarding(new Date());
					newVxFOnBoardedDescriptor.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
					newVxFOnBoardedDescriptor.setUuid(vnfd.getId());
					newVxFOnBoardedDescriptor.setVxfMANOProviderID(vnfd.getProductName());
					newVxFOnBoardedDescriptor.setObMANOprovider(mp);
					newVxFOnBoardedDescriptor.setVxf(aMANOClient.getVxFById(newVxFMetadata.getId()));
					//Add VxFOnBoardedDescriptor object to db and get the generated object
					newVxFOnBoardedDescriptor=aMANOClient.addVxFOnBoardedDescriptor(newVxFOnBoardedDescriptor);
					logger.info("VxFOnBoardedDescriptor " + newVxFOnBoardedDescriptor.getId() + " added");
					
				}
			}
			// Check for orphaned 
			for(VxFOnBoardedDescriptor dbvxfobd : vxFOnBoardedDescriptors)
			{
				boolean exists_in_osm = false;
				// For each object
				for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd : vnfd_array)
				{
					if(dbvxfobd.getDeployId().equals(vnfd.getId()))
					{
						logger.info("VNFD " + vnfd.getId() + " exists in osm");
						exists_in_osm = true;
					}
				}
				if(exists_in_osm == false && dbvxfobd.getObMANOprovider().getName().equals(mp.getName()) && dbvxfobd.getObMANOprovider().getProject().equals(mp.getProject()))
				{
					logger.debug(dbvxfobd.getDeployId()+" does not exist and MP name '"+dbvxfobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbvxfobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'");
					dbvxfobd.setOnBoardingStatus(OnBoardingStatus.OSM_MISSING);
					dbvxfobd = aMANOClient.updateVxFOnBoardedDescriptor(dbvxfobd);
					logger.info("synchronizeVNFDsOSM11 : VxFOnBoardedDescriptor " + dbvxfobd.getId() + " updated OnboardingStatus to OSM_MISSING");
				}
			}
		} catch (IllegalStateException | IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}			
	}
	
	private void synchronizeNSDs(OSMClient osmClient, MANOprovider mp)
	{
		//******************************************************************
		// Get ExperimentMetadata available from the portal's Database
		//List<ExperimentMetadata> experimentMetadatas = aMANOClient.getExperiments();
		List<ExperimentOnBoardDescriptor> experimentOnboardDescriptors = aMANOClient.getExperimentOnBoardDescriptors();
		//******************************************************************
		// Get NSDs from OSM MANO.
		ResponseEntity<String> nsds_list_entity = osmClient.getNSDescriptorsList();
		if (nsds_list_entity == null || nsds_list_entity.getStatusCode().is4xxClientError()
				|| nsds_list_entity.getStatusCode().is5xxServerError()) {
			logger.error("NSDs List Get Request failed. Status Code:" + nsds_list_entity.getStatusCode().toString()
					+ ", Payload:" + nsds_list_entity.getBody().toString());
		} else {
			logger.debug("Got NSD list "+nsds_list_entity.getBody());
			logger.info("Got MP Version:"+mp.getSupportedMANOplatform().getVersion());
		
			if(mp.getSupportedMANOplatform().getVersion().equals("OSMvTEN"))
			{					
				synchronizeNSDsOSM10(experimentOnboardDescriptors, nsds_list_entity, mp);
			}
			if(mp.getSupportedMANOplatform().getVersion().equals("OSMvELEVEN"))
			{					
				synchronizeNSDsOSM11(experimentOnboardDescriptors, nsds_list_entity, mp);
			}
			if(mp.getSupportedMANOplatform().getVersion().equals("OSMvTHIRTEEN"))
			{					
				synchronizeNSDsOSM11(experimentOnboardDescriptors, nsds_list_entity, mp);
			}
		}		
	}

	
	
	private void synchronizeNSDsOSM10(List<ExperimentOnBoardDescriptor> experimentOnBoardDescriptors, ResponseEntity<String> nsds_list_entity, MANOprovider mp)
	{
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Parse the json list of objects
			org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[] nsd_array = (org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[]) mapper.readValue(nsds_list_entity.getBody(), org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[].class);
			// For each object
			for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd nsd : nsd_array)
			{
				String jsonInString=null;
				ObjectMapper mapper2 = new ObjectMapper();
				mapper2.setSerializationInclusion(Include.NON_NULL);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(nsd);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
										
				// Εδώ θα συγκρίνουμε αυτό που λάβαμε απο τη βάση με αυτό που λάβαμε απο το osm και θα το ανανεώσουμε στη βάση.
				logger.debug("NSD to JSON:"+jsonInString);
				logger.info("NSD " + nsd.getInvariantId() + " added");						

				// Get the mapped ExperimentMetadata object
				ExperimentMetadata prod = mapOSM10NSD2ProductFromJSON(nsd,mp);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(prod);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				logger.debug("Prod created:"+jsonInString);	
				
				// Now that we have the product
				// Check if the ExperimentMetadata uuid already exists in the database
				
				boolean exists_in_db = false;
				for(ExperimentOnBoardDescriptor dbexpobd : experimentOnBoardDescriptors)
				{
					if(dbexpobd.getDeployId().equals(nsd.getInvariantId()))
					{
						logger.info("NSD " + nsd.getInvariantId() + " already exists");
						exists_in_db = true;
					}
				}						
				if(exists_in_db == false)
				{
					logger.info("NSD " + nsd.getInvariantId() + " does not exist. Adding to db.");
					exists_in_db = true;
					//Map osm vim to db vim object
					ExperimentMetadata newExperimentMetadata = new ExperimentMetadata();
					newExperimentMetadata.setUuid(nsd.getInvariantId());
					// Combine the vnfd name with the OSM name.
					newExperimentMetadata.setName(nsd.getName());
					newExperimentMetadata.setValidationStatus(ValidationStatus.COMPLETED);
					newExperimentMetadata.setDateCreated(new Date());
					newExperimentMetadata.setDateUpdated(new Date());
					newExperimentMetadata.setShortDescription(nsd.getName()+"@"+mp.getName());
					newExperimentMetadata.setPackagingFormat(PackagingFormat.OSMvTEN);
					//Get the manoServiceOwner to add it							
					newExperimentMetadata.setOwner(aMANOClient.getPortalUserByUsername("manoService"));
					
					// Get VNF Requirements from the vnfd
					OSM10NSRequirements nsdreq = new OSM10NSRequirements(nsd);
					// Store the requirements in HTML
					newExperimentMetadata.setDescriptorHTML(nsdreq.toHTML());
					// Store the YAML file
					newExperimentMetadata.setDescriptor(nsds_list_entity.getBody());
					
					for (Df v : nsd.getDf().values()) {
						for( VnfProfile q : v.getVnfProfile().values())
						{
							ConstituentVxF cvxf = new ConstituentVxF();
							try {
								cvxf.setMembervnfIndex(q.getId());
								
							} catch ( NumberFormatException e) {
								cvxf.setMembervnfIndex( "0" );
							}
							cvxf.setVnfdidRef((String) q.getVnfdId());
							String vxfuuid = aMANOClient.getVxFOnBoardedDescriptorByVxFAndMP(q.getVnfdId(), mp.getId());
							VxFMetadata vxf = (VxFMetadata) aMANOClient.getVxFByUUid(vxfuuid);
							cvxf.setVxfref(vxf);
							((ExperimentMetadata) newExperimentMetadata).getConstituentVxF().add(cvxf);					
						}
					}					
					//Add VxFMetadata object to db and get the generated object
					newExperimentMetadata = aMANOClient.addExperimentMetadata(newExperimentMetadata);
					logger.info("Experiment " + nsd.getId() + " added with ExperimentMetadata id="+newExperimentMetadata.getId());
					
					//Create the OnboardedDescriptor
					ExperimentOnBoardDescriptor newExperimentOnBoardedDescriptor = new ExperimentOnBoardDescriptor(newExperimentMetadata);
					newExperimentOnBoardedDescriptor.setDeployId(nsd.getInvariantId());
					newExperimentOnBoardedDescriptor.setFeedbackMessage("Automatically Retrieved from OSM");
					newExperimentOnBoardedDescriptor.setLastOnboarding(new Date());
					newExperimentOnBoardedDescriptor.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
					newExperimentOnBoardedDescriptor.setUuid(nsd.getInvariantId());
					newExperimentOnBoardedDescriptor.setExperimentMANOProviderID(nsd.getName());
					newExperimentOnBoardedDescriptor.setObMANOprovider(mp);
					newExperimentOnBoardedDescriptor.setExperiment(aMANOClient.getNSDById(newExperimentMetadata.getId()));					
					//Add VxFOnBoardedDescriptor object to db and get the generated object
					newExperimentOnBoardedDescriptor=aMANOClient.addExperimentOnBoardedDescriptor(newExperimentOnBoardedDescriptor);
					logger.info("ExperimentOnBoardedDescriptor " + newExperimentOnBoardedDescriptor.getId() + " added");
				}						
			}					
			// Check for orphaned 
			for(ExperimentOnBoardDescriptor dbexpobd : experimentOnBoardDescriptors)
			{
				boolean exists_in_osm = false;
				// For each object
				for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd nsd : nsd_array)
				{
					logger.debug("getDeployId()= " + dbexpobd.getDeployId() + "?=nsd.getInvariantId()" +nsd.getInvariantId());
					if(dbexpobd.getDeployId().equals(nsd.getInvariantId()))
					{
						logger.info("NSD " + dbexpobd.getDeployId() + " already exists");
						exists_in_osm = true;
					}
				}
				logger.debug(dbexpobd.getDeployId()+" does not exist and MP name '"+dbexpobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbexpobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'?");
				if(exists_in_osm == false && dbexpobd.getObMANOprovider().getName().equals(mp.getName()) && dbexpobd.getObMANOprovider().getProject().equals(mp.getProject()))
				{
					logger.debug(dbexpobd.getDeployId()+" does not exist and MP name '"+dbexpobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbexpobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'");
					logger.info("synchronizeNSDsOSM10 : ExperimentOnBoardedDescriptor " + dbexpobd.getId() + " not found. Updating OnboardingStatus to OSM_MISSING");
					dbexpobd.setOnBoardingStatus(OnBoardingStatus.OSM_MISSING);
					dbexpobd = aMANOClient.updateExperimentOnBoardDescriptor(dbexpobd);
					logger.info("synchronizeNSDsOSM10 : ExperimentOnBoardedDescriptor " + dbexpobd.getId() + " updated OnboardingStatus to OSM_MISSING");
				}
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}			
	}

	private void synchronizeNSDsOSM11(List<ExperimentOnBoardDescriptor> experimentOnBoardDescriptors, ResponseEntity<String> nsds_list_entity, MANOprovider mp)
	{
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Parse the json list of objects
			org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[] nsd_array = (org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[]) mapper.readValue(nsds_list_entity.getBody(), org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[].class);
			// For each object
			for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd nsd : nsd_array)
			{
				String jsonInString=null;
				ObjectMapper mapper2 = new ObjectMapper();
				mapper2.setSerializationInclusion(Include.NON_NULL);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(nsd);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
										
				// Compare the received from the osm with the database entry and update the database.
				logger.debug("NSD to JSON:"+jsonInString);
				logger.info("NSD " + nsd.getInvariantId() + " added");						

				// Get the mapped ExperimentMetadata object
				ExperimentMetadata prod = mapOSM10NSD2ProductFromJSON(nsd,mp);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(prod);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				logger.debug("Prod created:"+jsonInString);	
				
				// Now that we have the product
				// Check if the ExperimentMetadata uuid already exists in the database
				
				boolean exists_in_db = false;
				for(ExperimentOnBoardDescriptor dbexpobd : experimentOnBoardDescriptors)
				{
					if(dbexpobd.getDeployId().equals(nsd.getInvariantId()))
					{
						logger.info("NSD " + nsd.getInvariantId() + " already exists");
						exists_in_db = true;
					}
				}						
				if(exists_in_db == false)
				{
					logger.info("NSD " + nsd.getInvariantId() + " does not exist. Adding to db.");
					exists_in_db = true;
					// Map osm vim to db vim object
					ExperimentMetadata newExperimentMetadata = new ExperimentMetadata();
					newExperimentMetadata.setUuid(nsd.getInvariantId());
					// Combine the vnfd name with the OSM name.
					newExperimentMetadata.setName(nsd.getName());
					newExperimentMetadata.setValidationStatus(ValidationStatus.COMPLETED);
					newExperimentMetadata.setDateCreated(new Date());
					newExperimentMetadata.setDateUpdated(new Date());
					newExperimentMetadata.setShortDescription(nsd.getName()+"@"+mp.getName());
					newExperimentMetadata.setPackagingFormat(PackagingFormat.OSMvELEVEN);
					// Get the manoServiceOwner to add it							
					newExperimentMetadata.setOwner(aMANOClient.getPortalUserByUsername("manoService"));
					
					// Get VNF Requirements from the vnfd
					OSM10NSRequirements nsdreq = new OSM10NSRequirements(nsd);
					// Store the requirements in HTML
					newExperimentMetadata.setDescriptorHTML(nsdreq.toHTML());
					// Store the YAML file
					newExperimentMetadata.setDescriptor(nsds_list_entity.getBody());
					
					for (Df v : nsd.getDf().values()) {
						for( VnfProfile q : v.getVnfProfile().values())
						{
							ConstituentVxF cvxf = new ConstituentVxF();
							try {
								cvxf.setMembervnfIndex(q.getId());
								
							} catch ( NumberFormatException e) {
								cvxf.setMembervnfIndex( "0" );
							}
							cvxf.setVnfdidRef((String) q.getVnfdId());
							String vxfuuid = aMANOClient.getVxFOnBoardedDescriptorByVxFAndMP(q.getVnfdId(), mp.getId());
							VxFMetadata vxf = (VxFMetadata) aMANOClient.getVxFByUUid(vxfuuid);
							cvxf.setVxfref(vxf);
							((ExperimentMetadata) newExperimentMetadata).getConstituentVxF().add(cvxf);					
						}
					}					
					// Add VxFMetadata object to db and get the generated object
					newExperimentMetadata = aMANOClient.addExperimentMetadata(newExperimentMetadata);
					logger.info("Experiment " + nsd.getId() + " added with ExperimentMetadata id="+newExperimentMetadata.getId());
					
					// Create the OnboardedDescriptor
					ExperimentOnBoardDescriptor newExperimentOnBoardedDescriptor = new ExperimentOnBoardDescriptor(newExperimentMetadata);
					newExperimentOnBoardedDescriptor.setDeployId(nsd.getInvariantId());
					newExperimentOnBoardedDescriptor.setFeedbackMessage("Automatically Retrieved from OSM");
					newExperimentOnBoardedDescriptor.setLastOnboarding(new Date());
					newExperimentOnBoardedDescriptor.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
					newExperimentOnBoardedDescriptor.setUuid(nsd.getInvariantId());
					newExperimentOnBoardedDescriptor.setExperimentMANOProviderID(nsd.getName());
					newExperimentOnBoardedDescriptor.setObMANOprovider(mp);
					newExperimentOnBoardedDescriptor.setExperiment(aMANOClient.getNSDById(newExperimentMetadata.getId()));					
					// Add VxFOnBoardedDescriptor object to db and get the generated object
					newExperimentOnBoardedDescriptor=aMANOClient.addExperimentOnBoardedDescriptor(newExperimentOnBoardedDescriptor);
					logger.info("ExperimentOnBoardedDescriptor " + newExperimentOnBoardedDescriptor.getId() + " added");
				}						
			}					
			// Check for orphaned 
			for(ExperimentOnBoardDescriptor dbexpobd : experimentOnBoardDescriptors)
			{
				boolean exists_in_osm = false;
				// For each object
				for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd nsd : nsd_array)
				{
					logger.debug("getDeployId()= " + dbexpobd.getDeployId() + "?=nsd.getInvariantId()" +nsd.getInvariantId());
					if(dbexpobd.getDeployId().equals(nsd.getInvariantId()))
					{
						logger.info("NSD " + dbexpobd.getDeployId() + " already exists");
						exists_in_osm = true;
					}
				}
				if(exists_in_osm == false && dbexpobd.getObMANOprovider().getName().equals(mp.getName()) && dbexpobd.getObMANOprovider().getProject().equals(mp.getProject()))
				{
					logger.debug(dbexpobd.getDeployId()+" does not exist and MP name '"+dbexpobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbexpobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'");
					logger.info("synchronizeNSDsOSM11 : ExperimentOnBoardedDescriptor " + dbexpobd.getId() + " not found. Updating OnboardingStatus to OSM_MISSING");
					dbexpobd.setOnBoardingStatus(OnBoardingStatus.OSM_MISSING);
					dbexpobd = aMANOClient.updateExperimentOnBoardDescriptor(dbexpobd);
					logger.info("synchronizeNSDsOSM11 : ExperimentOnBoardedDescriptor " + dbexpobd.getId() + " updated OnboardingStatus to OSM_MISSING");
				}
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}			
	}

	private void synchronizeNSDsOSM13(List<ExperimentOnBoardDescriptor> experimentOnBoardDescriptors, ResponseEntity<String> nsds_list_entity, MANOprovider mp)
	{
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Parse the json list of objects
			org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[] nsd_array = (org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[]) mapper.readValue(nsds_list_entity.getBody(), org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd[].class);
			// For each object
			for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd nsd : nsd_array)
			{
				String jsonInString=null;
				ObjectMapper mapper2 = new ObjectMapper();
				mapper2.setSerializationInclusion(Include.NON_NULL);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(nsd);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
										
				// Compare the received from the osm with the database entry and update the database.
				logger.debug("NSD to JSON:"+jsonInString);
				logger.info("NSD " + nsd.getInvariantId() + " added");						

				// Get the mapped ExperimentMetadata object
				ExperimentMetadata prod = mapOSM10NSD2ProductFromJSON(nsd,mp);
				try {
					jsonInString = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(prod);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				logger.debug("Prod created:"+jsonInString);	
				
				// Now that we have the product
				// Check if the ExperimentMetadata uuid already exists in the database
				
				boolean exists_in_db = false;
				for(ExperimentOnBoardDescriptor dbexpobd : experimentOnBoardDescriptors)
				{
					if(dbexpobd.getDeployId().equals(nsd.getInvariantId()))
					{
						logger.info("NSD " + nsd.getInvariantId() + " already exists");
						exists_in_db = true;
					}
				}						
				if(exists_in_db == false)
				{
					logger.info("NSD " + nsd.getInvariantId() + " does not exist. Adding to db.");
					exists_in_db = true;
					// Map osm vim to db vim object
					ExperimentMetadata newExperimentMetadata = new ExperimentMetadata();
					newExperimentMetadata.setUuid(nsd.getInvariantId());
					// Combine the vnfd name with the OSM name.
					newExperimentMetadata.setName(nsd.getName());
					newExperimentMetadata.setValidationStatus(ValidationStatus.COMPLETED);
					newExperimentMetadata.setDateCreated(new Date());
					newExperimentMetadata.setDateUpdated(new Date());
					newExperimentMetadata.setShortDescription(nsd.getName()+"@"+mp.getName());
					newExperimentMetadata.setPackagingFormat(PackagingFormat.OSMvTHIRTEEN);
					// Get the manoServiceOwner to add it							
					newExperimentMetadata.setOwner(aMANOClient.getPortalUserByUsername("manoService"));
					
					// Get VNF Requirements from the vnfd
					OSM10NSRequirements nsdreq = new OSM10NSRequirements(nsd);
					// Store the requirements in HTML
					newExperimentMetadata.setDescriptorHTML(nsdreq.toHTML());
					// Store the YAML file
					newExperimentMetadata.setDescriptor(nsds_list_entity.getBody());
					
					for (Df v : nsd.getDf().values()) {
						for( VnfProfile q : v.getVnfProfile().values())
						{
							ConstituentVxF cvxf = new ConstituentVxF();
							try {
								cvxf.setMembervnfIndex(q.getId());
								
							} catch ( NumberFormatException e) {
								cvxf.setMembervnfIndex( "0" );
							}
							cvxf.setVnfdidRef((String) q.getVnfdId());
							String vxfuuid = aMANOClient.getVxFOnBoardedDescriptorByVxFAndMP(q.getVnfdId(), mp.getId());
							VxFMetadata vxf = (VxFMetadata) aMANOClient.getVxFByUUid(vxfuuid);
							cvxf.setVxfref(vxf);
							((ExperimentMetadata) newExperimentMetadata).getConstituentVxF().add(cvxf);					
						}
					}					
					// Add VxFMetadata object to db and get the generated object
					newExperimentMetadata = aMANOClient.addExperimentMetadata(newExperimentMetadata);
					logger.info("Experiment " + nsd.getId() + " added with ExperimentMetadata id="+newExperimentMetadata.getId());
					
					// Create the OnboardedDescriptor
					ExperimentOnBoardDescriptor newExperimentOnBoardedDescriptor = new ExperimentOnBoardDescriptor(newExperimentMetadata);
					newExperimentOnBoardedDescriptor.setDeployId(nsd.getInvariantId());
					newExperimentOnBoardedDescriptor.setFeedbackMessage("Automatically Retrieved from OSM");
					newExperimentOnBoardedDescriptor.setLastOnboarding(new Date());
					newExperimentOnBoardedDescriptor.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
					newExperimentOnBoardedDescriptor.setUuid(nsd.getInvariantId());
					newExperimentOnBoardedDescriptor.setExperimentMANOProviderID(nsd.getName());
					newExperimentOnBoardedDescriptor.setObMANOprovider(mp);
					newExperimentOnBoardedDescriptor.setExperiment(aMANOClient.getNSDById(newExperimentMetadata.getId()));					
					// Add VxFOnBoardedDescriptor object to db and get the generated object
					newExperimentOnBoardedDescriptor=aMANOClient.addExperimentOnBoardedDescriptor(newExperimentOnBoardedDescriptor);
					logger.info("ExperimentOnBoardedDescriptor " + newExperimentOnBoardedDescriptor.getId() + " added");
				}						
			}					
			// Check for orphaned 
			for(ExperimentOnBoardDescriptor dbexpobd : experimentOnBoardDescriptors)
			{
				boolean exists_in_osm = false;
				// For each object
				for(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd nsd : nsd_array)
				{
					logger.debug("getDeployId()= " + dbexpobd.getDeployId() + "?=nsd.getInvariantId()" +nsd.getInvariantId());
					if(dbexpobd.getDeployId().equals(nsd.getInvariantId()))
					{
						logger.info("NSD " + dbexpobd.getDeployId() + " already exists");
						exists_in_osm = true;
					}
				}
				if(exists_in_osm == false && dbexpobd.getObMANOprovider().getName().equals(mp.getName()) && dbexpobd.getObMANOprovider().getProject().equals(mp.getProject()))
				{
					logger.debug(dbexpobd.getDeployId()+" does not exist and MP name '"+dbexpobd.getObMANOprovider().getName()+"'='"+mp.getName()+"' and project '"+dbexpobd.getObMANOprovider().getProject()+"'='"+mp.getProject()+"'");
					logger.info("synchronizeNSDsOSM11 : ExperimentOnBoardedDescriptor " + dbexpobd.getId() + " not found. Updating OnboardingStatus to OSM_MISSING");
					dbexpobd.setOnBoardingStatus(OnBoardingStatus.OSM_MISSING);
					dbexpobd = aMANOClient.updateExperimentOnBoardDescriptor(dbexpobd);
					logger.info("synchronizeNSDsOSM11 : ExperimentOnBoardedDescriptor " + dbexpobd.getId() + " updated OnboardingStatus to OSM_MISSING");
				}
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}			
	}
	
	public VxFMetadata mapOSM8VNFD2ProductFromJSON(Vnfd vnfd) {
		
		VxFMetadata prod = new VxFMetadata();
		// We need to provide different implementations for each OSM version as this
		// maps to a different version of NSD model.
		prod.setUuid(vnfd.getId());
		prod.setName(vnfd.getName());
		prod.setVersion(vnfd.getVersion());
		prod.setShortDescription(vnfd.getName());
		prod.setLongDescription(vnfd.getName());
		// Store the requirements in HTML
		prod.setDescriptorHTML("");
		// Store the YAML file
		prod.setDescriptor("Automatically loaded VNFD");
		prod.setIconsrc("");
		return prod;
		
	}
	
	public VxFMetadata mapOSM9VNFD2ProductFromJSON(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd) {
		
		VxFMetadata prod = new VxFMetadata();
		// We need to provide different implementations for each OSM version as this
		// maps to a different version of NSD model.
		prod.setUuid(vnfd.getId());
		prod.setName(vnfd.getProductName());
		prod.setVersion(vnfd.getVersion());
		prod.setShortDescription(vnfd.getProductName());
		prod.setLongDescription(vnfd.getProductName());
		// Store the requirements in HTML
		prod.setDescriptorHTML("");
		// Store the YAML file
		prod.setDescriptor("Automatically loaded VNFD");
		prod.setIconsrc("");
		return prod;
		
	}
	
	public VxFMetadata mapOSM10VNFD2ProductFromJSON(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd) {
		
		VxFMetadata prod = new VxFMetadata();
		// We need to provide different implementations for each OSM version as this
		// maps to a different version of NSD model.
		prod.setUuid(vnfd.getId());
		prod.setName(vnfd.getProductName());
		prod.setVersion(vnfd.getVersion());
		prod.setShortDescription(vnfd.getProductName());
		prod.setLongDescription(vnfd.getProductName());
		// Store the requirements in HTML
		prod.setDescriptorHTML("");
		// Store the YAML file
		prod.setDescriptor("Automatically loaded VNFD");
		prod.setIconsrc("");
		return prod;
		
	}



	public ExperimentMetadata mapOSM10NSD2ProductFromJSON(org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd nsd, MANOprovider mp) 
	{		
		try
		{
			ExperimentMetadata prod = new ExperimentMetadata();
			// We need to provide different implementations for each OSM version as this
			// maps to a different version of NSD model.
			prod.setUuid(nsd.getInvariantId());
			prod.setName(nsd.getName());
			prod.setVersion(nsd.getVersion());
			//prod.setVendor(ns.getDesigner());
			prod.setShortDescription(nsd.getName());
			prod.setLongDescription(nsd.getName());
			for (Df v : nsd.getDf().values()) {
				for( VnfProfile q : v.getVnfProfile().values())
				{
					ConstituentVxF cvxf = new ConstituentVxF();
					cvxf.setMembervnfIndex(q.getId());
					cvxf.setVnfdidRef((String) q.getVnfdId());
					String vxfuuid = aMANOClient.getVxFOnBoardedDescriptorByVxFAndMP(q.getVnfdId(), mp.getId());
					VxFMetadata vxf = (VxFMetadata) aMANOClient.getVxFByUUid(vxfuuid);
					cvxf.setVxfref(vxf);
					((ExperimentMetadata) prod).getConstituentVxF().add(cvxf);					
				}
			}
			// Get NS Requirements from the nsd
			OSM10NSRequirements vr = new OSM10NSRequirements(nsd);
			// Store the requirements in HTML
			prod.setDescriptorHTML(vr.toHTML());
			// Store the YAML file
			prod.setDescriptor("Automatically loaded NSD");
			prod.setIconsrc("");
			return prod;		
		}
		catch(Exception e)
		{
			centralLogger.log(CLevel.ERROR, "Failed to map NSD 2 Product for "+nsd.getName()+". Retuning null.", compname);
			return null;
		}
	}	
	
	@Transactional
	public void checkAndUpdateRunningDeploymentDescriptors() {	
		checkAndUpdateMANOProvidersResources();
		logger.info("Update Deployment Descriptors");
		// centralLogger.log( CLevel.INFO, "Update Deployment Descriptors!!!!",
		// compname);
		try {
			List<DeploymentDescriptor> runningDeploymentDescriptors = aMANOClient
					.getRunningInstantiatingAndTerminatingDeployments();
			for (DeploymentDescriptor nsd : runningDeploymentDescriptors) {
				logger.info("NSD name:" + nsd.getName());

			}
			OSMClient osmClient = null;			
			// For each deployment get the status info and the IPs
			for (int i = 0; i < runningDeploymentDescriptors.size(); i++) {
				DeploymentDescriptor deployment_tmp = aMANOClient
						.getDeploymentByIdEager(runningDeploymentDescriptors.get(i).getId());
				//deployment_tmp.getExperimentFullDetails();
				try {
					// Get the MANO Provider for each deployment
					logger.debug("MANOprovider sm deployment_tmp.getObddescriptor_uuid() = " + deployment_tmp.getObddescriptor_uuid() );
					logger.debug("MANOprovider sm deployment_tmp.getObddescriptor_uuid().toString() = " + deployment_tmp.getObddescriptor_uuid().toString() );
					MANOprovider sm = deployment_tmp.getObddescriptor_uuid().getObMANOprovider() ;

					logger.debug("manoVersion sm.getSupportedMANOplatform() = " + sm.getSupportedMANOplatform() );
					logger.debug("manoVersion sm.getSupportedMANOplatform().getVersion() = " + sm.getSupportedMANOplatform().getVersion() );
					String manoVersion = sm.getSupportedMANOplatform().getVersion();
					
					//if (osmClient == null || !osmClient.getMANOApiEndpoint().equals(sm.getApiEndpoint())) {
						try {
							osmClient = OSMClientFactory.getOSMClient(manoVersion, sm.getApiEndpoint(),
									sm.getUsername(), sm.getPassword(), sm.getProject());

							
							
							JSONObject ns_instance_info = osmClient.getNSInstanceInfo(deployment_tmp.getInstanceId());
							// JSONObject ns_instance_content_info =
							// osmClient.getNSInstanceContentInfo(deployment_tmp.getInstanceId());
							// If the no nsd with the specific id is found, mark the instance as faile to
							// delete.
							if (ns_instance_info == null) {
								deployment_tmp.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);
								centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deployment_tmp.getName()
										+ " to " + deployment_tmp.getStatus(), compname);
								logger.info("NS not found in OSM. Status change of deployment1 " + deployment_tmp.getName()
										+ " to " + deployment_tmp.getStatus());
								deployment_tmp.setFeedback("NS instance not present in OSM. Marking as FAILED_OSM_REMOVED");
								logger.info("Update DeploymentDescriptor Object in 363");
								DeploymentDescriptor deploymentdescriptor_final = aMANOClient
										.updateDeploymentDescriptor(deployment_tmp);
								logger.info("NS status change is now " + deploymentdescriptor_final.getStatus());
								aMANOClient.deleteInstanceFailed(deploymentdescriptor_final);
							} else {
								try {
									// String nsr_string = JSONObject.quote(ns_instance_info.toString());
									deployment_tmp.setNsr(ns_instance_info.toString());
									deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
									logger.info("Setting NSR Info:" + deployment_tmp.getNsr());
									
									
									
									if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.RUNNING) {

										updateDescriptorInRunningState( deployment_tmp, osmClient, ns_instance_info );
										
												
									}
									logger.info("Setting Operational Status");
									deployment_tmp.setOperationalStatus(ns_instance_info.getString("operational-status"));
									deployment_tmp.setConfigStatus(ns_instance_info.getString("config-status"));
									deployment_tmp.setDetailedStatus(ns_instance_info.getString("detailed-status")
											.replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
									logger.debug("deployment_tmp before update "+deployment_tmp.toJSON());
									deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
									logger.debug("deployment_tmp after update "+deployment_tmp.toJSON());

									
									// Depending on the current OSM status, change the portal status.
									if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.INSTANTIATING
											&& deployment_tmp.getOperationalStatus().toLowerCase().equals("running")) {
										JSONObject ns_nslcm_details = osmClient
												.getNSLCMDetails(deployment_tmp.getNsLcmOpOccId());
										deployment_tmp.setNs_nslcm_details(ns_nslcm_details.toString());
										deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
										deployment_tmp.setStatus(DeploymentDescriptorStatus.RUNNING);
										logger.info("Status change of deployment1 " + deployment_tmp.getName() + " to "
												+ deployment_tmp.getStatus());
										centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deployment_tmp.getName()
												+ " to " + deployment_tmp.getStatus(), compname);
										deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status")
												.replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));

										deployment_tmp.setConstituentVnfrIps( extractIPsFromNSR(ns_instance_info) );

										updateDescriptorInRunningState( deployment_tmp, osmClient, ns_instance_info );
										
//										deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
//										aMANOClient.deploymentInstantiationSucceded(deployment_tmp);
									}
									// deployment_tmp.getStatus() == DeploymentDescriptorStatus.TERMINATING &&
									if (deployment_tmp.getOperationalStatus().toLowerCase().equals("terminated")) {
										// This message changes in OSM5 from "terminating" to "terminated"
										// && deployment_tmp.getConfigStatus().toLowerCase().equals("terminated")
										// && deployment_tmp.getDetailedStatus().toLowerCase().equals("done")) {
										deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status")
												.replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
										deployment_tmp.setStatus(DeploymentDescriptorStatus.TERMINATED);
										centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deployment_tmp.getName()
												+ " to " + deployment_tmp.getStatus(), compname);
										logger.info("Status change of deployment1 " + deployment_tmp.getName() + " to "
												+ deployment_tmp.getStatus());
										deployment_tmp.setConstituentVnfrIps("N/A");
										deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
										aMANOClient.deploymentTerminationSucceded(deployment_tmp);
									}
									// if(deployment_tmp.getStatus() != DeploymentDescriptorStatus.FAILED &&
									// deployment_tmp.getOperationalStatus().equals("failed"))
									if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.INSTANTIATING
											&& deployment_tmp.getOperationalStatus().equals("failed")) {
										deployment_tmp.setStatus(DeploymentDescriptorStatus.FAILED);
										centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deployment_tmp.getName()
												+ " to " + deployment_tmp.getStatus(), compname);
										logger.info("Status change of deployment1 " + deployment_tmp.getName() + " to "
												+ deployment_tmp.getStatus());
										deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status")
												.replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
										deployment_tmp.setConstituentVnfrIps("N/A");
										deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
										aMANOClient.deploymentInstantiationFailed(deployment_tmp);
									}
									if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.TERMINATING
											&& deployment_tmp.getOperationalStatus().equals("failed")) {
										deployment_tmp.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);
										centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deployment_tmp.getName()
												+ " to " + deployment_tmp.getStatus(), compname);
										logger.info("Status change of deployment1 " + deployment_tmp.getName() + " to "
												+ deployment_tmp.getStatus());
										deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status")
												.replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
										deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
										aMANOClient.deploymentTerminationFailed(deployment_tmp);
									}
									logger.info("NS status change is now " + deployment_tmp.getStatus());
								} catch (JSONException e) {
									logger.error("Status update failed with error:" + e.getMessage());
								}
							}
							
						} catch (Exception e) {
							logger.error(manoVersion + " fails authentication");
							centralLogger.log(CLevel.ERROR, manoVersion + " fails authentication", compname);

							//return;
						}
					//}//end if
					
					
					
					
				} catch (Exception e) {
					logger.error("Check and update process failed with error:" + e.getMessage());
				}
			}
			checkAndDeployExperimentToMANOProvider();
			checkAndTerminateExperimentToMANOProvider();
			checkAndDeleteTerminatedOrFailedDeployments();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	private void updateDescriptorInRunningState(DeploymentDescriptor deployment_tmp, OSMClient osmClient, JSONObject ns_instance_info) {
		
		// JSONObject ns_nslcm_details =
		// osmClient.getNSLCMDetails(deployment_tmp.getNsLcmOpOccId());
		
		String previous_nslcm_details = deployment_tmp.getNs_nslcm_details(); //contains last performed action of array
		String current_nslcm_details = osmClient
				.getNSLCMDetailsListByNSID(deployment_tmp.getInstanceId());
		logger.info("Calling alert on scale");
		current_nslcm_details = aMANOClient.alertOnScaleOpsList(deployment_tmp, previous_nslcm_details, current_nslcm_details);

		deployment_tmp.setNs_nslcm_details(current_nslcm_details);
		logger.info("After Calling alert on scale");
//		deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);

		//ResponseEntity<String> response=this.performNSInstanceAction("{\"nsInstanceId\": \"1f12d5d7-2ffe-454b-95b5-a805b480303b\",\"member_vnf_index\" : \"1\",\"primitive\" : \"touch\", \"primitive_params\" : {\"filename\" : \"/home/ubuntu/osmclienttest2\"}}");
		//JSONObject obj = new JSONObject(response.getBody());
		//String action_id = obj.getString("id");
		//logger.info("Got action id:"+action_id);

		// *******************************************************************************************************************
		// Create the payload for
		//logger.info("Starting scaling");
		//ANSScaleRequestPayload nsscalerequestpayload = new ANSScaleRequestPayload();
		//nsscalerequestpayload.setScaleType("SCALE_VNF");
		//nsscalerequestpayload.setNsInstanceId(deployment_tmp.getInstanceId());
		//nsscalerequestpayload.getScaleVnfData().getScaleByStepData().setMember_vnf_index("1");
		//nsscalerequestpayload.getScaleVnfData().getScaleByStepData()
		//		.setScaling_group_descriptor("apache_vdu_autoscale");
		//if (Math.random() > 0.5) {
		//	nsscalerequestpayload.getScaleVnfData().setScaleVnfType("SCALE_IN");
		//} else {
		//	nsscalerequestpayload.getScaleVnfData().setScaleVnfType("SCALE_OUT");
		//}
		//ResponseEntity<String> response = this
		//		.performNSInstanceScale(nsscalerequestpayload.toJSON());
		//logger.info(nsscalerequestpayload.toJSON());
		//JSONObject obj = new JSONObject(response.getBody());
		//String action_id = obj.getString("id");
		//logger.info("Got action id:" + action_id);
		// *******************************************************************************************************************


		
		//Get the VNF instance ids
		logger.debug("checkAndUpdateRunningDeploymentDescriptors ns instance info "+ns_instance_info);									
		List<String> constituent_vnfr_refs = JsonPath.read(ns_instance_info.toString(), "$.constituent-vnfr-ref");									
		Integer q=0;
		deployment_tmp.getDeploymentDescriptorVxFInstanceInfo().clear();
		for(String constituent_vnfr_ref : constituent_vnfr_refs) 
		{
			logger.info("VNF with id "+constituent_vnfr_ref+" found in NS instance info.");									
			//get the info of the vnf instances
			JSONObject vnf_instance_info = osmClient.getVNFInstanceInfo(constituent_vnfr_ref);
			if (vnf_instance_info == null) {
				deployment_tmp.setFeedback("VNF instance not present in NS Instance Info.");
			} else {
				try {											
					logger.info("Updating vxfPlacementInfo");			
					logger.debug("VNF Instance information " +vnf_instance_info.toString());
					logger.debug("Initial "+deployment_tmp.getDeploymentDescriptorVxFInstanceInfo());
					DeploymentDescriptorVxFInstanceInfo tmp = new DeploymentDescriptorVxFInstanceInfo();
					String member_vnf_index_ref = JsonPath.read(vnf_instance_info.toString(), "$.member-vnf-index-ref");
					tmp.setMemberVnfIndexRef(member_vnf_index_ref);
					tmp.setVxfInstanceInfo(vnf_instance_info.toString());
					deployment_tmp.getDeploymentDescriptorVxFInstanceInfo().add(tmp);
				}
				catch (Exception e) {
					logger.error("Failed to load vxfplacements info with error " + e.getMessage());
				}
			}
			q++;
		}


	
		if (!deployment_tmp.getOperationalStatus()
				.equals(ns_instance_info.getString("operational-status"))
				|| !deployment_tmp.getConfigStatus()
						.equals(ns_instance_info.getString("config-status"))
				|| !deployment_tmp.getDetailedStatus()
						.equals(ns_instance_info.getString("detailed-status")
								.replaceAll("\\n", " ").replaceAll("\'", "'")
								.replaceAll("\\\\", ""))) {
			logger.info("Status change of deployment1 " + deployment_tmp.getName() + " to "
					+ deployment_tmp.getStatus());
			centralLogger.log(CLevel.INFO, "Status change of deployment1 "
					+ deployment_tmp.getName() + " to " + deployment_tmp.getStatus(), compname);
			deployment_tmp.setFeedback(ns_instance_info.getString("detailed-status")
					.replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
			if(deployment_tmp.getExperimentFullDetails().getPackagingFormat().ordinal()<=PackagingFormat.OSMvEIGHT.ordinal())
			{
				deployment_tmp.setConstituentVnfrIps( extractIPsFromNSR(ns_instance_info) );
			}
			else if(deployment_tmp.getExperimentFullDetails().getPackagingFormat().ordinal()>PackagingFormat.OSMvEIGHT.ordinal())
			{
				deployment_tmp.setConstituentVnfrIps("ToDo");										
			}
				
			deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);
			aMANOClient.deploymentInstantiationSucceded(deployment_tmp);
		} else {
			deployment_tmp = aMANOClient.updateDeploymentDescriptor(deployment_tmp);			
		}
		
		
		/**
		 * publish topic event that NSLCM changed
		 */
		if ( 
				!previous_nslcm_details.equals( deployment_tmp.getNs_nslcm_details() ) ) {
			logger.info("Calling notifyOnLCMChanged");
			aMANOClient.notifyOnLCMChanged( deployment_tmp );
		}
		
		
	}

	private String extractIPsFromNSR(JSONObject ns_instance_info) {
		try {
			JSONObject deploymentStatus = ns_instance_info.getJSONObject("deploymentStatus");
			StringBuffer IPinfo = new StringBuffer();
			JSONArray vnfs = deploymentStatus.getJSONArray("vnfs");
			for (int k = 0; k < vnfs.length(); k++) {
				JSONObject vnf = (JSONObject) vnfs.get(k);
				IPinfo.append(vnf.get("vnf_name") + ":");
				IPinfo.append(vnf.get("ip_address"));
				JSONArray vms = vnf.getJSONArray("vms");
				IPinfo.append("[");
				for (int l = 0; l < vms.length(); l++) {
					JSONObject vm = (JSONObject) vms.get(l);
					JSONArray interfaces = vm.getJSONArray("interfaces");
					// IPinfo.append( "ifs=" );
					for (int m = 0; m < interfaces.length(); m++) {
						JSONObject ipinterface = (JSONObject) interfaces.get(m);
						IPinfo.append(ipinterface.get("ip_address") + ",");
					}

				}
				IPinfo.append("]\n");
			}

			logger.debug(IPinfo);
			return IPinfo.toString();

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return "";
	}

	private ExperimentOnBoardDescriptor getExperimOBD(DeploymentDescriptor deployment_tmp) {
		logger.debug( "getExperimOBD start");
		for (ExperimentOnBoardDescriptor e : deployment_tmp.getExperimentFullDetails()
				.getExperimentOnBoardDescriptors()) {
			logger.debug( "getExperimOBD:"+ e.toString());
			return e; // return the first one found
		}
		return null;
	}

	public ResponseEntity<String> performNSInstanceAction(String nsactionrequestpayloadstring) {
		// Deserialize input string as a NSActionRequestPayload
		ΑNSActionRequestPayload nsactionrequestpayload;
		try {
			ObjectMapper mapper = new ObjectMapper();
			nsactionrequestpayload = mapper.readValue(nsactionrequestpayloadstring, ΑNSActionRequestPayload.class);

			DeploymentDescriptor deploymentdescriptor = aMANOClient
					.getDeploymentByInstanceIdEager(nsactionrequestpayload.getNsInstanceId());
			ExperimentOnBoardDescriptor tmp = deploymentdescriptor.getObddescriptor_uuid();
			// Connect to OSM
			OSMClient osmClient = null;
			try {
				logger.debug("Connecting to " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
						+ " MANO Client of version " + tmp.getObMANOprovider().getSupportedMANOplatform().getVersion()
						+ ".");
				osmClient = OSMClientFactory.getOSMClient(
						tmp.getObMANOprovider().getSupportedMANOplatform().getVersion(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getApiEndpoint(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getUsername(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getPassword(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getProject());
				// MANOStatus.setOsm5CommunicationStatusActive(null);
			} catch (Exception e) {
				logger.error("performNSInstanceAction, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
						+ " fails authentication! Aborting action on NS.");
				centralLogger.log(CLevel.ERROR,
						"performNSInstanceAction, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
								+ " fails authentication! Aborting action on NS.",
						compname);
				deploymentdescriptor
						.setFeedback((new Date()) + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
								+ "  communication failed. Aborting action on NS. ");
				deploymentdescriptor.setOperationalStatus((new Date()) + " communication-failure ");
				deploymentdescriptor = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
				// aMANOClient.deploymentInstantiationFailed(deploymentdescriptor);
				return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
			}

			// Create the payload
			// nsactionrequestpayload = new NSActionRequestPayload();
			// nsactionrequestpayload.setNsInstanceId(deploymentdescriptor.getInstanceId());
			// nsactionrequestpayload.setMember_vnf_index("1");
			// nsactionrequestpayload.setPrimitive("touch");
			// Map<String, Object> primitive_params = new LinkedHashMap<String,Object>();
			// primitive_params.put("filename", "/home/ubuntu/osmclienttest2");
			// nsactionrequestpayload.setPrimitive_params(primitive_params);

			// Apply the Action
			ResponseEntity<String> ns_action_entity = osmClient.actionNSInstance(deploymentdescriptor.getInstanceId(),
					nsactionrequestpayload.toJSON());
			if (ns_action_entity == null || ns_action_entity.getStatusCode().is4xxClientError()
					|| ns_action_entity.getStatusCode().is5xxServerError()) {
				logger.error("NS Action failed. Status Code:" + ns_action_entity.getStatusCode().toString()
						+ ", Payload:" + ns_action_entity.getBody().toString());
			} else {
				// NS action starts
				logger.info("NS action of NS with id" + deploymentdescriptor.getInstanceId() + " started.");
				// Save the changes to DeploymentDescriptor
				logger.debug("NS action Status Code:" + ns_action_entity.getStatusCode().toString() + ", Payload:"
						+ ns_action_entity.getBody().toString());
			}
			// Get the response id or failure
			return ns_action_entity;
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
		}
	}
	
	public ResponseEntity<String> performNSScale(ScaleDescriptor aScaleDescriptor) {

		DeploymentDescriptor deploymentdescriptor = aMANOClient
				.getDeploymentByInstanceIdEager(aScaleDescriptor.getNsInstanceId());

		ExperimentOnBoardDescriptor tmp = deploymentdescriptor.getObddescriptor_uuid();
		// Connect to OSM
		OSMClient osmClient = null;
		try {
			logger.debug("Connecting to " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
					+ " MANO Client of version " + tmp.getObMANOprovider().getSupportedMANOplatform().getVersion()
					+ ".");
			osmClient = OSMClientFactory.getOSMClient(tmp.getObMANOprovider().getSupportedMANOplatform().getVersion(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getApiEndpoint(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getUsername(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getPassword(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (Exception e) {
			logger.error("performNSScale, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
					+ " fails authentication! Aborting action on NS.");
			centralLogger.log(CLevel.ERROR,
					"performNSScale, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
							+ " fails authentication! Aborting action on NS.",
					compname);
			deploymentdescriptor.setFeedback((new Date()) + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
					+ "  communication failed. Aborting action on NS. ");
			deploymentdescriptor.setOperationalStatus((new Date()) + " communication-failure ");
			deploymentdescriptor = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
			// aMANOClient.deploymentInstantiationFailed(deploymentdescriptor);
			return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
		}

		ANSScaleRequestPayload nsscalerequestpayload = new ANSScaleRequestPayload();
		nsscalerequestpayload.setScaleType(aScaleDescriptor.getScaleType());
		nsscalerequestpayload.setNsInstanceId(aScaleDescriptor.getNsInstanceId());
		nsscalerequestpayload.getScaleVnfData().getScaleByStepData()
				.setMember_vnf_index(aScaleDescriptor.getMemberVnfIndex());
		nsscalerequestpayload.getScaleVnfData().getScaleByStepData()
				.setScaling_group_descriptor(aScaleDescriptor.getScalingGroupDescriptor());

		nsscalerequestpayload.getScaleVnfData().setScaleVnfType(aScaleDescriptor.getScaleVnfType());

		// Apply the Action
		ResponseEntity<String> ns_scale_entity = osmClient.scaleNSInstance(deploymentdescriptor.getInstanceId(),
				nsscalerequestpayload.toJSON());
		if (ns_scale_entity == null || ns_scale_entity.getStatusCode().is4xxClientError()
				|| ns_scale_entity.getStatusCode().is5xxServerError()) {
			logger.error("NS Scale failed. Status Code:" + ns_scale_entity.getStatusCode().toString() + ", Payload:"
					+ ns_scale_entity.getBody().toString());
		} else {
			// NS action starts
			logger.info("NS scale of NS with id" + deploymentdescriptor.getInstanceId() + " started.");
			// Save the changes to DeploymentDescriptor
			logger.debug("NS scale Status Code:" + ns_scale_entity.getStatusCode().toString() + ", Payload:"
					+ ns_scale_entity.getBody().toString());
		}
		// Get the response id or failure
		return ns_scale_entity;

	}

	public ResponseEntity<String> performNSInstanceScale(String nsscalerequestpayloadstring) {
		// Deserialize input string as a NSScaleRequestPayload
		ANSScaleRequestPayload nsscalerequestpayload;
		try {
			ObjectMapper mapper = new ObjectMapper();
			nsscalerequestpayload = mapper.readValue(nsscalerequestpayloadstring, ANSScaleRequestPayload.class);

			DeploymentDescriptor deploymentdescriptor = aMANOClient
					.getDeploymentByInstanceIdEager(nsscalerequestpayload.getNsInstanceId());
			ExperimentOnBoardDescriptor tmp = deploymentdescriptor.getObddescriptor_uuid();
			// Connect to OSM
			OSMClient osmClient = null;
			try {
				logger.debug("Connecting to " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
						+ " MANO Client of version " + tmp.getObMANOprovider().getSupportedMANOplatform().getVersion()
						+ ".");
				osmClient = OSMClientFactory.getOSMClient(
						tmp.getObMANOprovider().getSupportedMANOplatform().getVersion(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getApiEndpoint(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getUsername(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getPassword(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getProject());
				// MANOStatus.setOsm5CommunicationStatusActive(null);
			} catch (Exception e) {
				logger.error("performNSInstanceScale, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
						+ " fails authentication! Aborting action on NS.");
				centralLogger.log(CLevel.ERROR,
						"performNSInstanceScale, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
								+ " fails authentication! Aborting action on NS.",
						compname);
				deploymentdescriptor
						.setFeedback((new Date()) + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
								+ "  communication failed. Aborting action on NS. ");
				deploymentdescriptor.setOperationalStatus((new Date()) + " communication-failure ");
				deploymentdescriptor = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
				// aMANOClient.deploymentInstantiationFailed(deploymentdescriptor);
				return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
			}

			// Apply the Action
			ResponseEntity<String> ns_scale_entity = osmClient.scaleNSInstance(deploymentdescriptor.getInstanceId(),
					nsscalerequestpayload.toJSON());
			if (ns_scale_entity == null || ns_scale_entity.getStatusCode().is4xxClientError()
					|| ns_scale_entity.getStatusCode().is5xxServerError()) {
				logger.error("NS Scale failed. Status Code:" + ns_scale_entity.getStatusCode().toString() + ", Payload:"
						+ ns_scale_entity.getBody().toString());
			} else {
				// NS action starts
				logger.info("NS scale of NS with id" + deploymentdescriptor.getInstanceId() + " started.");
				// Save the changes to DeploymentDescriptor
				logger.debug("NS scale Status Code:" + ns_scale_entity.getStatusCode().toString() + ", Payload:"
						+ ns_scale_entity.getBody().toString());
			}
			// Get the response id or failure
			return ns_scale_entity;
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			return (ResponseEntity<String>) ResponseEntity.badRequest().body("{message:" + e.getMessage() + "}");
		}
	}

	public ResponseEntity<String> getNSLCMDetails(String nsactionid) {
		return null;
	}

	public void deployNSDToMANOProvider(long deploymentdescriptorid) {
		logger.debug("Starting deployNSDToMANOProvicer");
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);
		logger.debug("Starting getExperimOBD");
		ExperimentOnBoardDescriptor tmp = deploymentdescriptor.getObddescriptor_uuid();		
		logger.debug("The loaded obddescriptor contains:"+tmp.toJSON());
		logger.debug("Starting connection to osm");
		OSMClient osmClient = null;
		try {
			logger.debug("Connecting to " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
					+ " MANO Client of version " + tmp.getObMANOprovider().getSupportedMANOplatform().getVersion()
					+ ".");
			osmClient = OSMClientFactory.getOSMClient(tmp.getObMANOprovider().getSupportedMANOplatform().getVersion(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getApiEndpoint(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getUsername(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getPassword(),
					deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getProject());
			// MANOStatus.setOsm5CommunicationStatusActive(null);
		} catch (Exception e) {
			logger.error("deployNSDToMANOProvider, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
					+ " fails authentication! Aborting deployment of NSD.");
			centralLogger.log(CLevel.ERROR,
					"deployNSDToMANOProvider, " + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
							+ " fails authentication! Aborting deployment of NSD.",
					compname);
			// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting deployment of NSD.");
			// NS instance creation failed
			// deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
			deploymentdescriptor.setFeedback((new Date()) + tmp.getObMANOprovider().getSupportedMANOplatform().getName()
					+ "  communication failed. Aborting NSD deployment action. ");
			deploymentdescriptor.setOperationalStatus((new Date()) + " communication-failure ");
			deploymentdescriptor = aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
			// aMANOClient.deploymentInstantiationFailed(deploymentdescriptor);
			return;
		}
		logger.debug("Connected to OSM");
		NSCreateInstanceRequestPayload nscreateinstancerequestpayload = null;
		String nscreateinstancerequestpayload_json = null;
		if (deploymentdescriptor.getInstantiationconfig() != null) {
			nscreateinstancerequestpayload_json = deploymentdescriptor.getInstantiationconfig();
			logger.debug("Found and parsed instantiation configuration " + nscreateinstancerequestpayload_json);
		} else {
			logger.info("Could not find or parse instantiation configuration from user. Getting default configuration");
			nscreateinstancerequestpayload = new NSCreateInstanceRequestPayload(deploymentdescriptor);
			nscreateinstancerequestpayload_json = nscreateinstancerequestpayload.toJSON();
		}
		// Get Experiment ID and VIM ID and create NS Instance.
		logger.info("NS Instance creation payload : " + nscreateinstancerequestpayload_json);
		ResponseEntity<String> ns_instance_creation_entity = osmClient
				.createNSInstance(nscreateinstancerequestpayload_json);
		// The NS Instance ID is set

		// NS instance creation
		if (ns_instance_creation_entity == null || ns_instance_creation_entity.getStatusCode().is4xxClientError()
				|| ns_instance_creation_entity.getStatusCode().is5xxServerError()) {
			// NS instance creation failed
			deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
			centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deploymentdescriptor.getName() + " to "
					+ deploymentdescriptor.getStatus(), compname);
			logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
					+ deploymentdescriptor.getStatus());
			deploymentdescriptor.setFeedback(ns_instance_creation_entity.getBody().toString());
			DeploymentDescriptor deploymentdescriptor_final = aMANOClient
					.updateDeploymentDescriptor(deploymentdescriptor);
			aMANOClient.deploymentInstantiationFailed(deploymentdescriptor_final);
			logger.error(
					"NS Instance creation failed with response: " + ns_instance_creation_entity.getBody().toString());
		} else {
			// String nsr_id =
			// osm5Client.instantiateNSInstance(nsd_instance_id,deploymentdescriptor.getName(),deploymentdescriptor.getInfrastructureForAll().getVIMid(),
			// deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getDeployId());
			JSONObject ns_instance_creation_entity_json_obj = new JSONObject(ns_instance_creation_entity.getBody());
			String nsd_instance_id = ns_instance_creation_entity_json_obj.getString("id");
			deploymentdescriptor.setInstanceId(nsd_instance_id);
			// Instantiate NS Instance
			// NSInstantiateInstanceRequestPayload nsrequestpayload = new
			// NSInstantiateInstanceRequestPayload(deploymentdescriptor);
			// logger.debug("NS Instantiation payload : " + nsrequestpayload.toJSON());

			NSInstantiateInstanceRequestPayload nsrequestpayload = null;
			String nsrequestpayload_json = null;
			if (deploymentdescriptor.getInstantiationconfig() != null) {
				nsrequestpayload_json = deploymentdescriptor.getInstantiationconfig();
				logger.debug("Found and parsed instantiation configuration " + nsrequestpayload_json);
			} else {
				logger.info(
						"Could not find or parse instantiation configuration from user. Getting default configuration");
				nsrequestpayload = new NSInstantiateInstanceRequestPayload(deploymentdescriptor);
				nsrequestpayload_json = nscreateinstancerequestpayload.toJSON();
			}
			// Get Experiment ID and VIM ID and create NS Instance.
			logger.debug("NS Instance creation payload : " + nsrequestpayload_json);

			// Here we need the feedback
			// String nsr_id = osm5Client.instantiateNSInstance(nsd_instance_id,
			// nsrequestpayload.toJSON());
			ResponseEntity<String> instantiate_ns_instance_entity = osmClient.instantiateNSInstance(nsd_instance_id,
					nsrequestpayload_json);
			if (instantiate_ns_instance_entity == null
					|| instantiate_ns_instance_entity.getStatusCode().is4xxClientError()
					|| instantiate_ns_instance_entity.getStatusCode().is5xxServerError()) {
				// NS Instantiation failed
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
				centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deploymentdescriptor.getName() + " to "
						+ deploymentdescriptor.getStatus(), compname);
				logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
						+ deploymentdescriptor.getStatus());
				deploymentdescriptor.setFeedback(instantiate_ns_instance_entity.getBody().toString());
				logger.error("NS Instantiation failed. Status Code:"
						+ instantiate_ns_instance_entity.getStatusCode().toString() + ", Payload:"
						+ ns_instance_creation_entity.getBody().toString());
				// Save the changes to DeploymentDescriptor
				DeploymentDescriptor deploymentdescriptor_final = aMANOClient
						.updateDeploymentDescriptor(deploymentdescriptor);
				aMANOClient.deploymentInstantiationFailed(deploymentdescriptor_final);
			} else {
				// NS Instantiation starts
				JSONObject instantiate_ns_instance_entity_json_obj = new JSONObject(
						instantiate_ns_instance_entity.getBody());
				deploymentdescriptor.setNsLcmOpOccId(instantiate_ns_instance_entity_json_obj.getString("id"));
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.INSTANTIATING);
				centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deploymentdescriptor.getName() + " to "
						+ deploymentdescriptor.getStatus(), compname);
				logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
						+ deploymentdescriptor.getStatus());
				deploymentdescriptor.setFeedback(instantiate_ns_instance_entity.getBody().toString());
				logger.info("NS Instantiation of NS with id" + nsd_instance_id + " started.");
				// Save the changes to DeploymentDescriptor
				aMANOClient.updateDeploymentDescriptor(deploymentdescriptor);
				// aMANOClient.deploymentInstantiationSucceded(deploymentdescriptor_final );
			}
		}
		return;
	}

	public void terminateNSFromMANOProvider(long deploymentdescriptorid) {
		logger.info("Starting terminateNSFromMANOProvicer");
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);

		logger.info("Current status change before termination is :" + deploymentdescriptor.getStatus());
		if (deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.INSTANTIATING
				|| deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.RUNNING
				|| deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED) {
			try {
				MANOprovider tmpMANOProvider = deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider();
				OSMClient osmClient = OSMClientFactory.getOSMClient(
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getSupportedMANOplatform().getVersion(),
						tmpMANOProvider.getApiEndpoint(), tmpMANOProvider.getUsername(), tmpMANOProvider.getPassword(),
						tmpMANOProvider.getProject());

				ResponseEntity<String> response = osmClient
						.terminateNSInstanceNew(deploymentdescriptor.getInstanceId());
				if (response == null || response.getStatusCode().is4xxClientError()
						|| response.getStatusCode().is5xxServerError()) {
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);
					centralLogger.log(CLevel.ERROR, "Status change of deployment1 " + deploymentdescriptor.getName()
							+ " to " + deploymentdescriptor.getStatus(), compname);
					logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
							+ deploymentdescriptor.getStatus());
					deploymentdescriptor.setFeedback(response.getBody().toString());
					logger.error("Termination of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient
							.updateDeploymentDescriptor(deploymentdescriptor);
					logger.info("NS status change is now " + deploymentdescriptor_final.getStatus());
					aMANOClient.terminateInstanceFailed(deploymentdescriptor_final);
				} else {
					// NS Termination succeeded
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.TERMINATING);
					centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deploymentdescriptor.getName()
							+ " to " + deploymentdescriptor.getStatus(), compname);
					logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
							+ deploymentdescriptor.getStatus());
					deploymentdescriptor.setConstituentVnfrIps("N/A");
					logger.info("Termination of NS " + deploymentdescriptor.getInstanceId() + " with name "
							+ deploymentdescriptor.getName() + " succeded");
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient
							.updateDeploymentDescriptor(deploymentdescriptor);
					logger.info("NS status change is now " + deploymentdescriptor_final.getStatus());
					aMANOClient.terminateInstanceSucceded(deploymentdescriptor_final);
				}
			} catch (Exception e) {
				centralLogger.log(CLevel.ERROR,
						"terminateNSFromMANOProvider, " + deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider()
								.getSupportedMANOplatform().getName() + " fails authentication. Aborting action.",
						compname);
			}
		}
	}

	public void deleteNSFromMANOProvider(long deploymentdescriptorid) {
		DeploymentDescriptor deploymentdescriptor = aMANOClient.getDeploymentByIdEager(deploymentdescriptorid);

		logger.info("Will delete with deploymentdescriptorid : " + deploymentdescriptorid);
		String aMANOplatform = "";
		try {
			logger.debug("MANOplatform: " + aMANOplatform);
			aMANOplatform = deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getSupportedMANOplatform().getVersion();
		} catch (Exception e) {
			aMANOplatform = "UNKNOWN";
		}
		if (OSMClientFactory.isOSMVersionSupported(aMANOplatform)) {
			logger.info(
					"Descriptor targets an " + aMANOplatform + " deploymentdescriptorid: " + deploymentdescriptorid);
			// There can be multiple MANOs for the Experiment. We need to handle that also.
			// After TERMINATION
			boolean force = false;
	
			if ( deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATED
					|| deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED
					|| deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATION_FAILED ) // for FAILED
																											// OR
																											// TERMINATION_FAILED
																											// instances
			{
				centralLogger.log(CLevel.INFO, "Following forcefull deletion. Status of " + deploymentdescriptor.getId()
						+ " is " + deploymentdescriptor.getStatus(), compname);
				logger.info("Following forcefull deletion. Status of " + deploymentdescriptor.getId() + " is "
						+ deploymentdescriptor.getStatus());
				force = false;
				/**
				 * ctranoris: changed  on 20210324 making force always false.
				 */
			} else {
				logger.info("Skipping deletion. Status of " + deploymentdescriptor.getId() + " is "
						+ deploymentdescriptor.getStatus());
				return;
			}
			OSMClient osmClient = null;
			try {
				osmClient = OSMClientFactory.getOSMClient(aMANOplatform,
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getApiEndpoint(),						
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getUsername(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getPassword(),
						deploymentdescriptor.getObddescriptor_uuid().getObMANOprovider().getProject());
				// MANOStatus.setOsm5CommunicationStatusActive(null);
			} catch (Exception e) {
				logger.error(aMANOplatform + " fails authentication");
				// MANOStatus.setOsm5CommunicationStatusFailed(" Aborting NS deletion action.");
				centralLogger.log(CLevel.ERROR, aMANOplatform + " fails authentication", compname);
				deploymentdescriptor.setFeedback(aMANOplatform + " communication failed. Aborting NS deletion action.");
				logger.error("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
				aMANOClient.deleteInstanceFailed(deploymentdescriptor);
				return;
			}
			ResponseEntity<String> deletion_response = osmClient
					.deleteNSInstanceNew(deploymentdescriptor.getInstanceId(), force);
			if (deletion_response.getStatusCode().is4xxClientError()
					|| deletion_response.getStatusCode().is5xxServerError()) {
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.DELETION_FAILED);
				centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deploymentdescriptor.getName() + " to "
						+ deploymentdescriptor.getStatus(), compname);
				logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
						+ deploymentdescriptor.getStatus());
				deploymentdescriptor.setFeedback(deletion_response.getBody().toString());
				logger.error("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
				DeploymentDescriptor deploymentdescriptor_final = aMANOClient
						.updateDeploymentDescriptor(deploymentdescriptor);
				logger.info("NS status change is now " + deploymentdescriptor_final.getStatus());
				aMANOClient.deleteInstanceFailed(deploymentdescriptor_final);
			} else if (deletion_response.getStatusCode().is2xxSuccessful()) {
				if (deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATED) {
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.COMPLETED);
					centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deploymentdescriptor.getName()
							+ " to " + deploymentdescriptor.getStatus(), compname);
					logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
							+ deploymentdescriptor.getStatus());
					logger.info("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " succeded");
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient
							.updateDeploymentDescriptor(deploymentdescriptor);
					logger.info("NS status change is now " + deploymentdescriptor_final.getStatus());
					aMANOClient.deleteInstanceSucceded(deploymentdescriptor_final);
				}
				if (deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.FAILED
						|| deploymentdescriptor.getStatus() == DeploymentDescriptorStatus.TERMINATION_FAILED) {
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);
					centralLogger.log(CLevel.INFO, "Status change of deployment1 " + deploymentdescriptor.getName()
							+ " to " + deploymentdescriptor.getStatus(), compname);
					logger.info("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
							+ deploymentdescriptor.getStatus());
					logger.info("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " succeeded");
					DeploymentDescriptor deploymentdescriptor_final = aMANOClient
							.updateDeploymentDescriptor(deploymentdescriptor);
					logger.info("NS status change is now " + deploymentdescriptor_final.getStatus());
					aMANOClient.deleteInstanceSucceded(deploymentdescriptor_final);
				}
			} else {
				try {
					centralLogger.log(CLevel.ERROR,
							"Status change of deployment1 " + deploymentdescriptor.getName() + " to "
									+ deploymentdescriptor.getStatus() + " replied with false code "
									+ deletion_response.getStatusCodeValue() + "and body" + deletion_response.getBody(),
							compname);
					logger.error("Status change of deployment1 " + deploymentdescriptor.getName() + " to "
							+ deploymentdescriptor.getStatus() + " replied with false code "
							+ deletion_response.getStatusCodeValue() + "and body" + deletion_response.getBody());
				} catch (Exception e) {
					centralLogger.log(CLevel.ERROR, "Deletion failed with message" + e.getMessage(), compname);
					logger.error("Deletion failed with message" + e.getMessage());
				}
			}
		} else {
			// if this is not a supported OSM then just complete
			logger.info(
					"Descriptor targets an older not supported OSM deploymentdescriptorid: " + deploymentdescriptorid);
			deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED_OSM_REMOVED);
			logger.info("Status change of deployment1 " + deploymentdescriptor.getId() + ", "
					+ deploymentdescriptor.getName() + " to " + deploymentdescriptor.getStatus());
			DeploymentDescriptor deploymentdescriptor_final = aMANOClient
					.updateDeploymentDescriptor(deploymentdescriptor);
			logger.info("NS status changed is now :" + deploymentdescriptor_final.getStatus());
		}
	}

	


	public String mapOSM10VNFD2ProductEagerDataJson(String yamlFile) throws JsonProcessingException {
		VxFMetadata vxfMetadata = this.mapOSM10VNFD2Product(yamlFile);
		ObjectMapper mapper = new ObjectMapper();
		String res = mapper.writeValueAsString(vxfMetadata);

		return res;
	}


	
	public String mapOSM10NSD2ProductEagerDataJson(String yamlFile) throws JsonProcessingException {
		ExperimentMetadata vxfMetadata = this.mapOSM10NSD2Product(yamlFile);
		ObjectMapper mapper = new ObjectMapper();
		String res = mapper.writeValueAsString(vxfMetadata);

		return res;
	}	


	
	public ExperimentMetadata mapOSM10NSD2Product(String yamlFile) {
		ExperimentMetadata prod = new ExperimentMetadata();

		// Get the nsd object out of the file info
		org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Nsd ns;
		try {
			// We need to provide different implementations for each OSM version as this
			// maps to a different version of NSD model.
			ns = OSM10NSExtractor.extractNsdDescriptorFromYAMLFile(yamlFile);

			prod.setName(ns.getName());
			prod.setVersion(ns.getVersion());
			prod.setVendor(ns.getDesigner());
			prod.setShortDescription(ns.getName());
			prod.setLongDescription(ns.getName());

			for (Df v : ns.getDf().values()) {
				for( VnfProfile q : v.getVnfProfile().values())
				{
					ConstituentVxF cvxf = new ConstituentVxF();
					// Here we try to convert the id to int.
					cvxf.setMembervnfIndex(q.getId());
					cvxf.setVnfdidRef((String) q.getVnfdId());
					VxFMetadata vxf = (VxFMetadata) aMANOClient.getVxFByName((String) q.getVnfdId());
					cvxf.setVxfref(vxf);
					((ExperimentMetadata) prod).getConstituentVxF().add(cvxf);					
				}
			}

			// Get NS Requirements from the nsd
			OSM10NSRequirements vr = new OSM10NSRequirements(ns);
			// Store the requirements in HTML
			prod.setDescriptorHTML(vr.toHTML());
			// Store the YAML file
			prod.setDescriptor(yamlFile);
			prod.setIconsrc("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prod;
	}
	

	
		
	public VxFMetadata mapOSM10VNFD2Product(String yamlFile) {
		VxFMetadata prod = new VxFMetadata();
		// Get the vnfd object out of the file info
		org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.Vnfd vnfd;
		try {
			// We need to provide different implementations for each OSM version as this
			// maps to a different version of VNFD model.
			vnfd = OSM10VNFDExtractor.extractVnfdDescriptorFromYAMLFile(yamlFile);
			if (vnfd == null) {
				logger.error("Cannot read Descriptor from YAML file:" + yamlFile);
				return null;
			}
			// Get the name for the db
			prod.setName(vnfd.getProductName());
			prod.setVersion(vnfd.getVersion());
			prod.setVendor(vnfd.getProvider());
			prod.setShortDescription(vnfd.getProductName());
			prod.setLongDescription(vnfd.getProductInfoDescription());

			((VxFMetadata) prod).setValidationStatus(ValidationStatus.UNDER_REVIEW);
			((VxFMetadata) prod).getVfimagesVDU().clear();// clear previous referenced images
			if(vnfd.getVdu() != null)
			{
				for (org.opendaylight.yang.gen.v1.urn.etsi.nfv.yang.etsi.nfv.descriptors.rev190425.vnfd.Vdu vdu : vnfd.getVdu().values()) {
					String imageName = vdu.getSwImageDesc();
					if ((imageName != null) && (!imageName.equals(""))) {
						VFImage sm = new VFImage();
						sm.setName(imageName);
						((VxFMetadata) prod).getVfimagesVDU().add(sm);
					}
				}
			}
			// Get VNF Requirements from the vnfd
			OSM10VNFRequirements vr = new OSM10VNFRequirements(vnfd);
			// Store the requirements in HTML
			prod.setDescriptorHTML(vr.toHTML());
			// Store the YAML file
			prod.setDescriptor(yamlFile);

			prod.setIconsrc("");
		} catch (IOException e) {
			logger.error("Cannot read Descriptor from YAML file:" + yamlFile);
			e.printStackTrace();
			return null;
		}
		return prod;
	}
		
	public void getScaleAlert(String body)
	{
		logger.info("Scaling message received with body");
	}
	

	
}
