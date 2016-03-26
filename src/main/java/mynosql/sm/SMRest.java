package mynosql.sm;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.ws.rs.GET;
import java.lang.Exception;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
//import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

@Path("/restapi")
public class SMRest{

	public SM storage;
	private static Boolean DEBUG = true;
	private String ip1, //in case of slave master ip, else slave 1 ip
					ip2, //other slave 2 ip
					ip3; //self ip
	private Boolean partitioned = false;
	private Boolean autoResolve = false;
	private Boolean isMaster = false;
	File log1, log2;
	File config = new File("smconfig.txt");
	int version = 0;
	public SMRest(){
		storage = SMFactory.getSM();
		
		try {
			log1 = new File("Partition1.log");
			log1.createNewFile();
			log2 = new File("Partition2.log");
			log2.createNewFile();
			
			//BufferedReader br = new BufferedReader(new FileReader("smconfig.txt"));
			String configStr = new String(Files.readAllBytes(Paths.get("smconfig.txt")));
			JSONObject js = new JSONObject(configStr);
//			if(br.readLine().equals("slave"))
//			{
//				isMaster = false;
//				ip1 = br.readLine(); //master
//				ip2 = br.readLine(); //slave 2
//				ip3 = br.readLine(); //self
//			}
//			else
//			{
//				isMaster = true;
//				ip1 = br.readLine(); //slave 1
//				ip2 = br.readLine(); //slave 2
//				ip3 = br.readLine(); //self
//			}
			partitioned = js.getBoolean("partition");
			autoResolve = js.getBoolean("autoResolve");
			
			if(js.getString("type").equals("slave"))
			{				
				isMaster = false;
				ip1 = js.getString("master");
				ip2 = js.getString("slave2");
				ip3 = js.getString("slave1");
				version = js.getInt("version");
			}
			else
			{
				isMaster = true;
				ip3 = js.getString("master");
				ip2 = js.getString("slave2");
				ip1 = js.getString("slave1");
				version = js.getInt("version");
			}
			
		} catch (Exception e) {
			System.out.println("Config file not found");
			e.printStackTrace();
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/heartbeat")
	public String ping() { 
		String responseString = "";
		if(DEBUG)
			System.out.println("Heartbeat request received");
		
		responseString =  "{\"Reply\": \"heartbeating\"}";		
		JSONObject js = new JSONObject(responseString);
		js.append("ip", ip3);
		js.append("DBVersion", version);
		return js.toString();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/read/{key}")
	public String read(@PathParam("key") String key) { 
		
		String responseString = "";
		String ip = "";
		WebTarget wt;
		int status = 0;
		
		if(DEBUG)
			System.out.println("Read request received for key: "+key);
		try
		{
			//check heart-beat
			wt = ClientBuilder.newClient().target(ip3).path("sm/restapi/heartbeat");
			status  = wt.request().get().getStatus();
			if(status == 200)
			{
				if(partitioned && autoResolve)
				{
					resolve();
				}
				partitioned = false;
			}
			else
				partitioned = true;
		}
		catch(WebApplicationException|ProcessingException e)
		{
			if(DEBUG)
				System.out.println("Partition at: " + ip);
			partitioned = true;
		}
		
		JSONObject js = new JSONObject(responseString);
		if(!partitioned)
		{
			try{
				String value = storage.fetch(key);
				responseString = value;
			}
			catch(Exception e){
				responseString =  "{\"Reply\": \"Fail\"}";
			}
		}
		else
		{
			js.append("Reply", "In Partition. No read and writes allowed. Data may not be consisten.");
			if(!autoResolve)
				js.append("Info", "After resolving partition go to ../resolve because autoResolve if off.");
		}
		
		js.append("ip", ip3);
		js.append("DBVersion", version);
		return js.toString();
	}
//	@GET
//	@Produces(MediaType.APPLICATION_JSON)
//	@Path("/dumpdata")
//	public String dump() { 
//		String responseString = "";
//		if(DEBUG)
//			System.out.println("Dump request received");
//		try{
//			String value = storage.fetch(key);
//			responseString = value;
//		}
//		catch(Exception e){
//			responseString =  "{\"Reply\": \"Fail\"}";
//		}
//		JSONObject js = new JSONObject(responseString);
//		js.append("ip", ip3);
//		return js.toString();
//	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/write/{key}/{value}")
	public String insert(@PathParam("key")String key, @PathParam("value") String value) {
		KeyValue kv = new KeyValue(key,value);
		if(DEBUG)
			System.out.println("Insert request received for key: "+key+ " value: "+value);
		String responseEntity;
		if(!isMaster)
		{			
			responseEntity = ClientBuilder.newClient()
	            .target(ip1).path("sm/restapi/insertm").path(key).path(kv.ToJSONString())
	                        .request().get(String.class);			
		}
		else
		{
			responseEntity = insertM(key, kv.ToJSONString());
		}
		
		return responseEntity;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/resolve")
	public String resolve() {
		if(DEBUG)
			System.out.println("Resolve request received");
		String responseEntity;
		if(!isMaster)
		{			
			responseEntity = ClientBuilder.newClient().target(ip1).path("sm/restapi/resolvem").request().get(String.class);			
		}
		else
		{
			responseEntity = resolveM();
		}

		return responseEntity;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/delete/{key}")
	public String delete(@PathParam("key") String key) {
		if(DEBUG)
			System.out.println("Delete request received for key: "+key);
		String responseEntity;
		if(!isMaster)
		{
			responseEntity = ClientBuilder.newClient()
	            .target(ip1).path("sm/restapi/deletem").path(key)
	                        .request().get(String.class);			
		}
		else
		{
			responseEntity = deleteM(key);
		}
		
		return responseEntity;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/update/{key}/{newValue}")
	public String update(@PathParam("key") String key, @PathParam("newValue") String newValue) {
		KeyValue kv = new KeyValue(key,newValue);
		if(DEBUG)
			System.out.println("Update request received for key: "+key+" new value: "+newValue);
		String responseEntity;
		if(!isMaster)
		{
			responseEntity = ClientBuilder.newClient()
	            .target(ip1).path("sm/restapi/updatem").path(key).path(kv.ToJSONString())
	                        .request().get(String.class);
		}
		else
		{
			responseEntity = updateM(key, kv.ToJSONString());
		}
		
		return responseEntity;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/resolvem")
	public String resolveM() {
		String ip = "";
		WebTarget wt;
		BufferedReader br;
		int status = 0;
		JSONObject js = new JSONObject();
		try
		{
			
			br = new BufferedReader(new FileReader(log1));
			while(br.ready())
			{
				String url = br.readLine();
				wt = ClientBuilder.newClient().target(url);
				status  = wt.request().get().getStatus();
			}
			br.close();
			log1.delete();
			log1.createNewFile();
			js.append("Node1", "Synced");
		}
		catch(Exception e)
		{
			if(DEBUG)
				System.out.println("Partition at: " + ip);
			js.append("Node1", "Not Synced");
		}
		
		try
		{
			
			br = new BufferedReader(new FileReader(log2));
			while(br.ready())
			{
				String url = br.readLine();
				wt = ClientBuilder.newClient().target(url);
				status  = wt.request().get().getStatus();
			}
			br.close();
			log2.delete();
			log2.createNewFile();
			js.append("Node2", "Synced");
		}
		catch(Exception e)
		{
			if(DEBUG)
				System.out.println("Partition at: " + ip);
			js.append("Node2", "Not Synced");
		}

		return js.toString();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/writem/{key}/{value}")
	public String insertM(@PathParam("key")String key, @PathParam("value") String value) {

		String ip = "";
		WebTarget wt;
		int status = 0;
		if(DEBUG)
			System.out.println("Insert request received by master for key: "+key+ " value: "+value);
		//KeyValue data;
		try{
			try
			{
				//update on slave nodes
				wt = ClientBuilder.newClient().target(ip1).path("sm/restapi/inserts").path(key).path(value);
				ip = wt.getUri().toString();
				status  = wt.request().get().getStatus();
				System.out.println(status);
			}
			catch(WebApplicationException|ProcessingException e)
			{
				if(DEBUG)
					System.out.println("Partition at: " + ip);
				BufferedWriter wr = new BufferedWriter(new FileWriter(log1, true));
				wr.write(ip);
				wr.newLine();
				wr.close();
			}
			
			try
			{
				//update on slave nodes				
				wt = ClientBuilder.newClient().target(ip2).path("sm/restapi/inserts").path(key).path(value);
				ip = wt.getUri().toString();
				status = wt.request().get().getStatus();
				System.out.println(status);
			}
			catch(WebApplicationException|ProcessingException e)
			{
				if(DEBUG)
					System.out.println("Partition at: " + ip);
				BufferedWriter wr = new BufferedWriter(new FileWriter(log2, true));
				wr.write(ip);
				wr.newLine();
				wr.close();
			}
			
			storage.store(key, value);
			version++;
			updateConfig();
			//data = new KeyValue(key, value);
			return "{\"Reply\": \"Success\"}";
		}
		catch(Exception e){
			return "{\"Reply\": \"Fail\"}";
		}
	}
	
	private void updateConfig()
	{
		//++version;
		BufferedWriter wr;
		try {
			wr = new BufferedWriter(new FileWriter(config));		
			String configStr = new String(Files.readAllBytes(Paths.get("smconfig.txt")));
			JSONObject js = new JSONObject(configStr);
			
			js.put("partition", partitioned);
			js.put("autoResolve", autoResolve);
			js.put("version", version);
			
			wr.write(js.toString());
			wr.newLine();
			wr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/deletem/{key}")
	public String deleteM(@PathParam("key") String key) {
		String ip = "";
		WebTarget wt;
		int status = 0;
		if(DEBUG)
			System.out.println("Delete request received by master for key: "+key);
		try{
			try
			{
				//update on slave nodes
				wt = ClientBuilder.newClient().target(ip1).path("sm/restapi/deletes").path(key);
				ip = wt.getUri().toString();
				status  = wt.request().get().getStatus();
				System.out.println(status);
			}
			catch(WebApplicationException|ProcessingException e)
			{
				if(DEBUG)
					System.out.println("Partition at: " + ip);
				BufferedWriter wr = new BufferedWriter(new FileWriter(log1, true));
				wr.write(ip);
				wr.newLine();
				wr.close();
			}
			
			try
			{
				//update on slave nodes				
				wt = ClientBuilder.newClient().target(ip2).path("sm/restapi/deletes").path(key);
				ip = wt.getUri().toString();
				status = wt.request().get().getStatus();
				System.out.println(status);
			}
			catch(WebApplicationException|ProcessingException e)
			{
				if(DEBUG)
					System.out.println("Partition at: " + ip);
				BufferedWriter wr = new BufferedWriter(new FileWriter(log2, true));
				wr.write(ip);
				wr.newLine();
				wr.close();
			}
			
			storage.delete(key);
			version++;
			updateConfig();
			return "{\"Reply\": \"Success\"}";
		}
		catch(Exception e){
			return "{\"Reply\": \"Fail\"}";
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/updatem/{key}/{newValue}")
	public String updateM(@PathParam("key") String key, @PathParam("newValue") String newValue) {
		String ip = "";
		WebTarget wt;
		int status = 0;
		if(DEBUG)
			System.out.println("Update request received by master for key: "+key+ " new value: "+newValue);
		try{
			try
			{
				//update on slave nodes
				wt = ClientBuilder.newClient().target(ip1).path(key).path(newValue);
				ip = wt.getUri().toString();
				status  = wt.request().get().getStatus();
				System.out.println(status);
			}
			catch(WebApplicationException|ProcessingException e)
			{
				if(DEBUG)
					System.out.println("Partition at: " + ip);
				BufferedWriter wr = new BufferedWriter(new FileWriter(log1, true));
				wr.write(ip);
				wr.newLine();
				wr.close();
			}
			
			try
			{
				//update on slave nodes				
				wt = ClientBuilder.newClient().target(ip2).path(key).path(newValue);
				ip = wt.getUri().toString();
				status = wt.request().get().getStatus();
				System.out.println(status);
			}
			catch(WebApplicationException|ProcessingException e)
			{
				if(DEBUG)
					System.out.println("Partition at: " + ip);
				BufferedWriter wr = new BufferedWriter(new FileWriter(log2, true));
				wr.write(ip);
				wr.newLine();
				wr.close();
			}
			storage.update(key, newValue);
			version++;
			updateConfig();
			return "{\"Reply\": \"Success\"}";
		}
		catch(Exception e){
			return "{\"Reply\": \"Fail\"}";
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/writes/{key}/{value}")
	public String insertS(@PathParam("key")String key, @PathParam("value") String value) {

		if(DEBUG)
			System.out.println("Insert request received by slave for key: "+key+ " value: "+value);
		//KeyValue data;
		try{
			storage.store(key, value);
			version++;
			updateConfig();
			//data = new KeyValue(key, value);
			return "{\"Reply\": \"Success\"}";
		}
		catch(Exception e){
			return "{\"Reply\": \"Fail\"}";
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/deletes/{key}")
	public String deleteS(@PathParam("key") String key) {
		if(DEBUG)
			System.out.println("Delete request received by slave for key: "+key);
		try{
			storage.delete(key);
			version++;
			updateConfig();
			return "{\"Reply\": \"Success\"}";
		}
		catch(Exception e){
			return "{\"Reply\": \"Fail\"}";
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/updates/{key}/{newValue}")
	public String updateS(@PathParam("key") String key, @PathParam("newValue") String newValue) {
		if(DEBUG)
			System.out.println("Update request received by slave for key: "+key+ " new value: "+newValue);
		try{
			storage.update(key, newValue);
			version++;
			updateConfig();
			return "{\"Reply\": \"Success\"}";
		}
		catch(Exception e){
			return "{\"Reply\": \"Fail\"}";
		}
	}
}
