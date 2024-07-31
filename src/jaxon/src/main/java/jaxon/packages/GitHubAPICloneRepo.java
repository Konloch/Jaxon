package jaxon.packages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Konloch
 * @since 7/30/2024
 */
public class GitHubAPICloneRepo
{
	public static boolean cloneRepo(File repoDir, String repoURL) throws IOException
	{
		String[] parts = repoURL.split("/");
		String owner = parts[3];
		String repo = parts[4];
		String commitHash = parts[6];
		String path = String.join("/", Arrays.copyOfRange(parts, 7, parts.length));
		
		repoDir.mkdirs();
		
		return downloadContents(repoDir, owner, repo, path, commitHash);
	}
	
	private static boolean downloadContents(File currentDir, String owner, String repo, String path, String ref) throws IOException
	{
		URL url = new URL("https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path + "?ref=" + ref);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		
		int responseCode = connection.getResponseCode();
		if (responseCode == 200)
		{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(connection.getInputStream());
			
			if (jsonNode.isArray())
			{
				for (JsonNode node : jsonNode)
				{
					String name = node.get("name").asText();
					String type = node.get("type").asText();
					String downloadUrl = node.has("download_url") ? node.get("download_url").asText() : null;
					String dirPath = node.get("path").asText();
					
					if ("file".equals(type))
						downloadFile(currentDir, name, downloadUrl);
					else if ("dir".equals(type))
					{
						File newDir = new File(currentDir, name);
						newDir.mkdir();
						//String treeUrl = node.get("_links").get("self").asText();
						downloadContents(newDir, owner, repo, dirPath, ref);
					}
				}
				
				return true;
			}
			
			System.err.println("Failed to parse contents: " + jsonNode.asText());
			return false;
		}
		else
		{
			System.err.println("Failed to fetch contents: " + connection.getResponseMessage());
			return false;
		}
	}
	
	private static void downloadFile(File currentDir, String name, String downloadUrl) throws IOException
	{
		File file = new File(currentDir, name);
		System.out.print("Downloading: " + file.getPath() + "...");
		
		URL url = new URL(downloadUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		
		int responseCode = connection.getResponseCode();
		if (responseCode == 200)
		{
			try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(file))
			{
				
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1)
				{
					out.write(buffer, 0, bytesRead);
				}
			}
			
			System.out.println("Finished");
		}
		else
		{
			System.err.println("Failed to download file: " + connection.getResponseMessage());
		}
	}
}
