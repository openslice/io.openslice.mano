package io.openslice.mano;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VIMCreateRequestPayload {
	private String _id;
	private String schema_version;
	private String schema_type;
	private String name;
	private String description;
	private String vim;
	private String datacenter;
	private String vim_type;
	private String vim_url;
	private String vim_tenant_name;
	private String vim_user;
	private String vim_password;
	//private String config;
	
	public String getSchema_version() {
		return schema_version;
	}
	public void setSchema_version(String schema_version) {
		this.schema_version = schema_version;
	}
	public String getSchema_type() {
		return schema_type;
	}
	public void setSchema_type(String schema_type) {
		this.schema_type = schema_type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getVim() {
		return vim;
	}
	public void setVim(String vim) {
		this.vim = vim;
	}
	public String getDatacenter() {
		return datacenter;
	}
	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
	}
	public String getVim_type() {
		return vim_type;
	}
	public void setVim_type(String vim_type) {
		this.vim_type = vim_type;
	}
	public String getVim_url() {
		return vim_url;
	}
	public void setVim_url(String vim_url) {
		this.vim_url = vim_url;
	}
	public String getVim_tenant_name() {
		return vim_tenant_name;
	}
	public void setVim_tenant_name(String vim_tenant_name) {
		this.vim_tenant_name = vim_tenant_name;
	}
	public String getVim_user() {
		return vim_user;
	}
	public void setVim_user(String vim_user) {
		this.vim_user = vim_user;
	}
	public String getVim_password() {
		return vim_password;
	}
	public void setVim_password(String vim_password) {
		this.vim_password = vim_password;
	}
//	public String getConfig() {
//		return config;
//	}
//	public void setConfig(String config) {
//		this.config = config;
//	}

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
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	
	
}
