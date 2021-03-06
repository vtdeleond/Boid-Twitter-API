package com.teamboid.twitterapi.media;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.scribe.builder.api.ImgUrApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.teamboid.twitterapi.client.RequestHandler;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.client.TwitterException;
import org.json.JSONObject;
import com.teamboid.twitterapi.status.StatusUpdate;
import com.teamboid.twitterapi.status.entity.media.MediaEntity;
import com.teamboid.twitterapi.status.entity.url.UrlEntity;

public class ImgurMediaService extends ExternalMediaService {

	@Override
	public String getServiceName() {
		return "imgur";
	}

	@Override
	public String[] getSupportedUrls() {
		return new String[]{
				"http://imgur.com/*",
				"http://www.imgur.com/*"
		};
	}

	@Override
	public MediaEntity uploadFile(StatusUpdate tweet, Twitter tw,
			RequestHandler client, InputStream file) throws TwitterException {
		try{
			if(this.authToken == null) throw new TwitterException("This requires an OAuth Token");
			if(this.authorizedService == null) throw new TwitterException("This requires an Authorized Service");
			
			OAuthRequest ar = new OAuthRequest(Verb.POST, "http://api.imgur.com/2/account/images.json");
			
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("caption", new StringBody(attribution));
			entity.addPart("title", new StringBody( tweet.getStatus() ));
			entity.addPart("type", new StringBody("file"));
			entity.addPart("image", new InputStreamBody(file, "BOIDUPLOAD.jpg"));
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
	        entity.writeTo(out);
	        ar.addPayload(out.toByteArray());
	        ar.addHeader(entity.getContentType().getName(), entity.getContentType().getValue());
	        
			this.authorizedService.signRequest(authToken, ar);
			Response r = ar.send();
			if(!r.isSuccessful()){ System.err.println(r.getBody()); return null; }
			
			JSONObject jo = new JSONObject(r.getBody());
            String url = jo.getJSONObject("images").getJSONObject("links").getString("imgur_page");
            
            ExternalMediaEntity ema = new ExternalMediaEntity(url);
            setUrls(ema, url);
			return ema;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public String getUserName() throws TwitterException{
		if(this.authToken == null) throw new TwitterException("This requires an OAuth Token");
		if(this.authorizedService == null) throw new TwitterException("This requires an Authorized Service");
		
		OAuthRequest ar = new OAuthRequest(Verb.GET, "http://api.imgur.com/2/account.json");
		authorizedService.signRequest(authToken, ar);
		
		Response r = ar.send();
		if(!r.isSuccessful()) throw new TwitterException("Could not get imgur user");
		
		try{
			JSONObject ob = new JSONObject(r.getBody());
			String url = ob.getJSONObject("account").getString("url");
			return url;
		} catch(Exception e){
			throw new TwitterException("Error with JSON: " + e.getMessage());
		}
	}
	
	void setUrls(ExternalMediaEntity ema, String url){
		String[] parts = url.split("imgur.com/");
		if(parts.length < 2) return;
		
		url = "http://i.imgur.com/" + parts[1] + ".jpg";
		ema.setUrls(url, url, url, url);
	}

	@Override
	public MediaEntity getEntity(UrlEntity in) {
		ExternalMediaEntity ema = new ExternalMediaEntity(in);
		setUrls(ema, in.getExpandedUrl());
		return ema;
	}
	
	public Class<?> getOAuthService(){
		return ImgUrApi.class;
	}
	
	public AuthorizationNeeded getNeededAuthorization(){
		return AuthorizationNeeded.OAUTH;
	}

}
