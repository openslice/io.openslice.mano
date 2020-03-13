package io.openslice.mano;

import org.springframework.web.client.HttpStatusCodeException;

import OSM5NBIClient.OSM5Client;
import OSM7NBIClient.OSM7Client;
import io.openslice.sol005nbi.OSMClient;

public class OSMClientFactory {

	public static OSMClient getOSMClient(String type,String apiEndpoint, String username, String password, String project_id) throws HttpStatusCodeException
	{
		switch(type)
		{
			case "OSM FIVE":
				return new OSM5Client(apiEndpoint,username,password,project_id);
			case "OSM SEVEN":
				return new OSM7Client(apiEndpoint,username,password,project_id);
		}
		return null;
	}
	
	public static Boolean isOSMVersionSupported(String type)
	{
		switch(type)
		{
			case "OSM FIVE":
				return true;
			case "OSM SEVEN":
				return true;				
		}
		return false;
	}
}
