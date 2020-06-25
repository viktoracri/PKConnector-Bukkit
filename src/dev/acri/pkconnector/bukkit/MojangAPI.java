package dev.acri.pkconnector.bukkit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MojangAPI {

	  public static UUID getUUID(String player)
	  {
	    try
	    {
	      URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + player);
	      HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
	      BufferedReader in = new BufferedReader(
	      new InputStreamReader(con.getInputStream()));
	      
	      String inputLine;
	      if ((inputLine = in.readLine()) != null) {
	        JsonObject json = new JsonParser().parse(inputLine).getAsJsonObject();
	        String raw = json.get("id").toString().replace("\"", "");
	        raw = new StringBuilder(raw).insert(8, "-").toString();
	        raw = new StringBuilder(raw).insert(13, "-").toString();
	        raw = new StringBuilder(raw).insert(18, "-").toString();
	        raw = new StringBuilder(raw).insert(23, "-").toString();
	        return UUID.fromString(raw);
	      }
	      

	      in.close();
	    } catch (Exception e) {
	      return null; }
	    return null;
	  }
	  
	  public static String getFixedName(String player)
	  {
	    try
	    {
	      URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + player);
	      HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
	      BufferedReader in = new BufferedReader(
	      new InputStreamReader(con.getInputStream()));
	      
	      String inputLine;
	      if ((inputLine = in.readLine()) != null) {
	        JsonObject json = new JsonParser().parse(inputLine).getAsJsonObject();
	        String raw = json.get("name").toString().replace("\"", "");
	        return raw;
	      }
	      

	      in.close();
	    } catch (Exception e) {
	      return null; }
	    return null;
	  }
	  
	  public static JsonObject getLocation(String host) {
	    try {
	      URL url = new URL("https://ipapi.co/"+host+ "/json");
	      
	      URLConnection request = url.openConnection();
	      request.connect();

	      JsonElement root = new JsonParser().parse(new InputStreamReader((InputStream) request.getContent(), "utf-8")); //Convert the input stream to a json element
	      return root.getAsJsonObject();
	      
	    } catch (Exception e) {
	      return null; }
	    
	  }
	  
	  public static JsonArray getPreviousNames(UUID uuid) {
	    try {
	      URL url = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names");
	      HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
	      BufferedReader in = new BufferedReader(
	        new InputStreamReader(con.getInputStream()));
	      
	      String inputLine;
	      if ((inputLine = in.readLine()) != null) {
	        return new JsonParser().parse(inputLine).getAsJsonArray();
	      }
	      

	      in.close();
	    } catch (Exception e) {
	      return null; }
	    return null;
	  }
	  
	  public static String getCurrentName(UUID uuid) {
		  JsonArray array = getPreviousNames(uuid);
		  if(array != null) {
			  return array.get(array.size()-1).getAsJsonObject().get("name").getAsString();
		  }
		  return null;
	  }
	  
	  public static List<String> getChangedNamesAtTime(JsonArray json) { List<String> list = new ArrayList<String>();
	    for (JsonElement jo : json) {
	      JsonObject job = jo.getAsJsonObject();
	      if (job.has("changedToAt")) {
	        Date date = new Date(job.get("changedToAt").getAsLong());
	        String newstring = new SimpleDateFormat("yyyy-MM-dd").format(date);
	        list.add(job.get("name").getAsString() + "§7: changed at: §b" + newstring);
	      } else {
	        list.add(job.get("name").getAsString() + "§7: §bfirst name");
	      }
	    }
	    return list;
	  }


	  public static HypixelStatus getHypixelStatus(UUID uuid){
		try{
			URL url = new URL("https://api.hypixel.net/status?key=3dc9004e-2d93-4541-bb19-623721a4899f&uuid=" + uuid.toString());
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			String inputLine;
			if ((inputLine = in.readLine()) != null) {
				JsonObject jsonRaw = new JsonParser().parse(inputLine).getAsJsonObject();
				if(jsonRaw.has("session")){
					JsonObject json = jsonRaw.getAsJsonObject("session");
					boolean online = json.get("online").getAsBoolean();
					String gameType = "";
					String mode = "";
					if(json.has("gameType"))gameType = json.get("gameType").getAsString();
					if(json.has("mode"))mode = json.get("mode").getAsString();

					return new HypixelStatus(online, gameType, mode);
				}
			}

		}catch(Exception e){
		}
		return null;
	  }

	  public static class HypixelStatus{

	private boolean online;
	private String gameType;
	private String mode;

		  public HypixelStatus(boolean online, String gameType, String mode) {
			  this.online = online;
			  this.gameType = gameType;
			  this.mode = mode;
		  }

		  public boolean isOnline() {
		  return online;
		}

		public String getGameType() {
		  return gameType;
		}

		public String getMode() {
		  return mode;
		}

		public String getStatus(){
		  	if(!online)return "Offline";

		  	if(mode.equals("LOBBY")){
		  		return "In " + gameType + " lobby";
			}else if(gameType.equals("SKYBLOCK")){
		  		return "Playing Skyblock";
			}else if(gameType.equals("HOUSING")){
				return "Playing Housing";
			}else{
		  		return "Playing " + mode + " on " + gameType;
			}


		}
  }
	  
	 
	
}
