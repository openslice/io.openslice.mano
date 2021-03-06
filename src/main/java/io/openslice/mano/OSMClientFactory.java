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
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpStatusCodeException;

import OSM7NBIClient.OSM7Client;
import OSM7Util.OSM7ArchiveExtractor.OSM7NSExtractor;
import OSM8NBIClient.OSM8Client;
import OSM8Util.OSM8ArchiveExtractor.OSM8NSExtractor;
import io.openslice.sol005nbi.OSMClient;
import io.openslice.sol005nbi.OSMUtil.OSMNSExtractor;
import io.openslice.sol005nbi.etsi.GenericSOL005Client;

@Configuration
public class OSMClientFactory {
	
	public static OSMClient getOSMClient(String type,String apiEndpoint, String username, String password, String project_id) throws HttpStatusCodeException
	{
		String tokenEndpoint = "https://10.10.10.37:9999/osm/admin/v1/tokens/";
		String basePath = "/vnfpkgm/v1";
		
		switch(type)
		{
			case "OSMvSEVEN":
				return new OSM7Client(apiEndpoint,username,password,project_id);
			case "OSMvEIGHT":
				return new OSM8Client(apiEndpoint,username,password,project_id);
			case "GenericSOL005":
				return new GenericSOL005Client(apiEndpoint,username,password,project_id, tokenEndpoint, basePath);
		}
		return new OSM8Client(apiEndpoint,username,password,project_id);
	}
	
	public static OSMNSExtractor getOSMNSExtractor(String type,File NSDescriptorFile)
	{
		switch(type)
		{
		case "OSMvSEVEN":
			return new OSM7NSExtractor(NSDescriptorFile);
		case "OSMvEIGHT":
			return new OSM8NSExtractor(NSDescriptorFile);
		}
		return new OSM8NSExtractor(NSDescriptorFile);
	}
		
	public static Boolean isOSMVersionSupported(String type)
	{
		switch(type)
		{
			case "OSMvSEVEN":
				return true;	
			case "OSMvEIGHT":
				return true;				
		}
		return false;
	}	
}
