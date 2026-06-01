package com.gridscape.wiki;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

/** OSRS Wiki API client for equip-task icon lookup. */
@Slf4j
@Singleton
public class OsrsWikiApiService
{
	private static final String API_BASE = "https://oldschool.runescape.wiki/api.php";
	private static final String USER_AGENT = "GridScape/1.0 (OSRS Wiki API; RuneLite plugin)";
	private static final int REQUEST_TIMEOUT_MS = 15_000;
	private static final Pattern INFOBOX_IMAGE = Pattern.compile("\\|\\s*image\\s*=\\s*\\[\\[File:([^\\]|]+)\\]\\]", Pattern.CASE_INSENSITIVE);

	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "GridScape-WikiApi");
		t.setDaemon(true);
		return t;
	});

	@Inject
	public OsrsWikiApiService()
	{
	}

	public String getImageUrl(String imageFileName)
	{
		String title = imageFileName.startsWith("File:") ? imageFileName : "File:" + imageFileName;
		String query = "?action=query&titles=" + urlEncode(title) + "&prop=imageinfo&iiprop=url&format=json";
		try
		{
			String json = httpGet(API_BASE + query);
			if (json == null) return null;
			@SuppressWarnings("deprecation") JsonObject root = new JsonParser().parse(json).getAsJsonObject();
			JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
			for (String id : pages.keySet())
			{
				if ("-1".equals(id)) continue;
				JsonElement info = pages.getAsJsonObject(id).getAsJsonArray("imageinfo");
				if (info != null && info.isJsonArray() && info.getAsJsonArray().size() > 0)
					return info.getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
			}
		}
		catch (Exception e)
		{
			log.warn("Wiki API getImageUrl failed for {}: {}", imageFileName, e.getMessage());
		}
		return null;
	}

	public BufferedImage fetchImage(String imageFileName)
	{
		String url = getImageUrl(imageFileName);
		if (url == null) return null;
		try (InputStream in = httpGetStream(url))
		{
			return in != null ? ImageIO.read(in) : null;
		}
		catch (IOException e)
		{
			log.warn("Wiki image fetch failed for {}: {}", imageFileName, e.getMessage());
			return null;
		}
	}

	public List<String> search(String search, int limit)
	{
		if (search == null || search.isEmpty()) return new ArrayList<>();
		int clamped = Math.max(1, Math.min(500, limit));
		String query = "?action=opensearch&search=" + urlEncode(search) + "&limit=" + clamped + "&format=json";
		try
		{
			String json = httpGet(API_BASE + query);
			if (json == null) return new ArrayList<>();
			@SuppressWarnings("deprecation")
			JsonArray root = new JsonParser().parse(json).getAsJsonArray();
			if (root.size() < 2) return new ArrayList<>();
			JsonArray titles = root.get(1).getAsJsonArray();
			List<String> out = new ArrayList<>(titles.size());
			for (JsonElement e : titles) out.add(e.getAsString());
			return out;
		}
		catch (Exception e)
		{
			log.warn("Wiki API search failed for {}: {}", search, e.getMessage());
			return new ArrayList<>();
		}
	}

	public String getPageContent(String pageTitle)
	{
		String query = "?action=query&prop=revisions&rvprop=content&rvslots=main&titles=" + urlEncode(pageTitle) + "&format=json";
		try
		{
			String json = httpGet(API_BASE + query);
			if (json == null) return null;
			@SuppressWarnings("deprecation") JsonObject root = new JsonParser().parse(json).getAsJsonObject();
			JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
			for (String id : pages.keySet())
			{
				if ("-1".equals(id)) continue;
				JsonElement revs = pages.getAsJsonObject(id).get("revisions");
				if (revs == null || !revs.isJsonArray() || revs.getAsJsonArray().size() == 0) continue;
				JsonObject rev = revs.getAsJsonArray().get(0).getAsJsonObject();
				JsonElement slots = rev.get("slots");
				if (slots != null && slots.isJsonObject())
				{
					JsonElement main = slots.getAsJsonObject().get("main");
					if (main != null && main.isJsonObject())
					{
						JsonElement content = main.getAsJsonObject().get("*");
						if (content != null) return content.getAsString();
					}
				}
				JsonElement content = rev.get("*");
				if (content != null) return content.getAsString();
			}
		}
		catch (Exception e)
		{
			log.warn("Wiki API getPageContent failed for {}: {}", pageTitle, e.getMessage());
		}
		return null;
	}

	public String getFirstInfoboxImageFileName(String pageTitle)
	{
		String content = getPageContent(pageTitle);
		if (content == null) return null;
		Matcher m = INFOBOX_IMAGE.matcher(content);
		return m.find() ? m.group(1).trim() : null;
	}

	public void fetchItemIconAsync(String displayName, Consumer<BufferedImage> callback)
	{
		if (callback == null) return;
		executor.execute(() -> {
			List<String> titles = search(displayName, 1);
			if (titles.isEmpty()) { callback.accept(null); return; }
			String fileName = getFirstInfoboxImageFileName(titles.get(0));
			if (fileName == null) { callback.accept(null); return; }
			callback.accept(fetchImage(fileName));
		});
	}

	private static String urlEncode(String s)
	{
		return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private String httpGet(String urlString)
	{
		try (InputStream in = httpGetStream(urlString))
		{
			if (in == null) return null;
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			log.debug("Wiki HTTP GET failed: {}", e.getMessage());
			return null;
		}
	}

	private InputStream httpGetStream(String urlString)
	{
		try
		{
			URI uri = URI.create(urlString);
			java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
				.connectTimeout(java.time.Duration.ofMillis(REQUEST_TIMEOUT_MS))
				.followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
				.build();
			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(uri)
				.timeout(java.time.Duration.ofMillis(REQUEST_TIMEOUT_MS))
				.header("User-Agent", USER_AGENT)
				.GET()
				.build();
			java.net.http.HttpResponse<InputStream> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) return null;
			return response.body();
		}
		catch (Exception e)
		{
			log.debug("Wiki HTTP request failed: {}", e.getMessage());
			return null;
		}
	}
}
