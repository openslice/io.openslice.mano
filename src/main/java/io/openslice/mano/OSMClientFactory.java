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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.project.nsd.rev170228.nsd.constituent.vnfd.ConstituentVnfd;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.project.nsd.rev170228.project.nsd.catalog.Nsd;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.vnfd.base.rev170228.vnfd.descriptor.Vdu;
import org.opendaylight.yang.gen.v1.urn.etsi.osm.yang.vnfd.rev170228.vnfd.catalog.Vnfd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpStatusCodeException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import OSM5NBIClient.OSM5Client;
import OSM5Util.OSM5ArchiveExtractor.OSM5NSExtractor;
import OSM5Util.OSM5ArchiveExtractor.OSM5VNFDExtractor;
import OSM5Util.OSM5NSReq.OSM5NSRequirements;
import OSM5Util.OSM5VNFReq.OSM5VNFRequirements;
import OSM7NBIClient.OSM7Client;
import OSM7Util.OSM7ArchiveExtractor.OSM7NSExtractor;
import OSM7Util.OSM7ArchiveExtractor.OSM7VNFDExtractor;
import OSM7Util.OSM7NSReq.OSM7NSRequirements;
import OSM7Util.OSM7VNFReq.OSM7VNFRequirements;
import io.openslice.model.ConstituentVxF;
import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.ExperimentMetadata;
import io.openslice.model.PortalUser;
import io.openslice.model.Product;
import io.openslice.model.VFImage;
import io.openslice.model.ValidationStatus;
import io.openslice.model.VxFMetadata;
import io.openslice.sol005nbi.OSMClient;
import io.openslice.sol005nbi.OSMUtil.OSMNSExtractor;
import io.openslice.sol005nbi.OSMUtil.OSMVNFDExtractor;
import io.openslice.sol005nbi.etsi.GenericSOL005Client;

@Configuration
public class OSMClientFactory {
	
	public static OSMClient getOSMClient(String type,String apiEndpoint, String username, String password, String project_id) throws HttpStatusCodeException
	{
		String tokenEndpoint = "https://10.10.10.37:9999/osm/admin/v1/tokens/";
		String basePath = "/vnfpkgm/v1";
		
		switch(type)
		{
			case "OSMvFIVE":
				return new OSM5Client(apiEndpoint,username,password,project_id);
			case "OSMvSEVEN":
				return new OSM7Client(apiEndpoint,username,password,project_id);
			case "GenericSOL005":
			return new GenericSOL005Client(apiEndpoint,username,password,project_id, tokenEndpoint, basePath);
		}
		return null;
	}
	
	public static OSMNSExtractor getOSMNSExtractor(String type,File NSDescriptorFile)
	{
		switch(type)
		{
			case "OSMvFIVE":
				return new OSM5NSExtractor(NSDescriptorFile);
			case "OSMvSEVEN":
				return new OSM7NSExtractor(NSDescriptorFile);
		}
		return null;
	}
		
	public static Boolean isOSMVersionSupported(String type)
	{
		switch(type)
		{
			case "OSMvFIVE":
				return true;
			case "OSMvSEVEN":
				return true;				
		}
		return false;
	}	
}
