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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.DeploymentDescriptorVxFPlacement;
import io.openslice.model.ExperimentOnBoardDescriptor;

/**
 * @author ctranoris
 *
 */
public class NSInstantiateInstanceRequestPayload {
	public String nsName;
	public String vimAccountId;
	public String nsdId;
	public List<VnF> vnf = new ArrayList<>();	
	//private List<Vld> vld = null; //new ArrayList<>();	

	class VnF {
		@JsonProperty("member-vnf-index")
		public String memberVnFIndex;
		@JsonProperty("vimAccountId")
		public String vimAccount;
	}

	class Vld
	{
		@JsonProperty("name")
		private String name;
		@JsonProperty("vim-network-name")
		private LinkedHashMap<String,String> vimNetworkName = new LinkedHashMap<>();

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public LinkedHashMap<String, String> getVimNetworkName() {
			return vimNetworkName;
		}
		public void setVimNetworkName(LinkedHashMap<String, String> vimNetworkName) {
			this.vimNetworkName = vimNetworkName;
		}
	}

//	public List<Vld> getVld() {
//		return vld;
//	}
//
//	public void setVld(List<Vld> vld) {
//		this.vld = vld;
//	}
	
	public NSInstantiateInstanceRequestPayload(DeploymentDescriptor deploymentdescriptor) {
		this.nsName = deploymentdescriptor.getName();
		this.vimAccountId = deploymentdescriptor.getInfrastructureForAll().getVIMid();
		// Here we need to get the ExperimentOnBoardDescriptor based on the Experiment.
		// An Experiment might have multiple OnBoardDescriptors if it is OnBoarded to
		// multiple OSM MANOs.
		// We temporarily select the first (and most probably the only one).
		// Otherwise the user needs to define the OSM MANO where the Experiment is
		// OnBoarded in order to instantiate.
		this.nsdId = deploymentdescriptor.getObddescriptor_uuid().getDeployId();
		
		Integer count = 1;
		for (DeploymentDescriptorVxFPlacement tmp : deploymentdescriptor.getVxfPlacements()) {
			VnF vnf_tmp = new VnF();
			vnf_tmp.memberVnFIndex = tmp.getConstituentVxF().getMembervnfIndex();
			vnf_tmp.vimAccount = tmp.getInfrastructure().getVIMid();
			this.vnf.add(vnf_tmp);			
			count++;
		}	
	}
	
	private ExperimentOnBoardDescriptor getExperimOBD(DeploymentDescriptor deployment_tmp) {

		for (ExperimentOnBoardDescriptor e : deployment_tmp.getExperimentFullDetails()
				.getExperimentOnBoardDescriptors()) {

			return e; // return the first one found
		}
		return null;
	}

	public String toJSON()
	{
		String jsonInString=null;
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		try {
			jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return jsonInString;
	}
	
}
