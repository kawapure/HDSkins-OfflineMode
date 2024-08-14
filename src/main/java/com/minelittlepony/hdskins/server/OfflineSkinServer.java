package com.minelittlepony.hdskins.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import com.google.common.collect.Sets;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinUpload.Session;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.util.Util;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.StandardWatchEventKinds;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Offline skin server.
 */
@ServerType("offline")
public class OfflineSkinServer implements SkinServer
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.DOWNLOAD_USER_SKIN,
            Feature.UPLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES
    );

    private transient static final String OFFLINE_STORAGE_FILE_NAME = "hdskins_offline.json";

    private transient final Gson gson;

    private transient JsonObject skinsProvider;
    
    private transient Thread observerThread;

    public OfflineSkinServer()
    {
        GsonBuilder gsonBuilder = new GsonBuilder();

        this.gson = gsonBuilder
            .setLenient()
            .setPrettyPrinting()
            .create();

        reloadServerFiles();

        if (skinsProvider == null)
        {
            skinsProvider = new JsonObject();
        }
        
        // Set up the observer thread:
        Path storageFilePath = getOfflineStorageFilePath();
        
        this.observerThread = new Thread(new Runnable()
        {
            public void run()
            {
                LOGGER.info("OfflineSkinServer: Starting observer thread...");
                
                try
                {
                    WatchService ws = FileSystems.getDefault().newWatchService();

                    storageFilePath.getParent().register(
                        ws,
                        StandardWatchEventKinds.ENTRY_MODIFY
                    );
                    
                    try
                    {
                        WatchKey key;
                        while ((key = ws.take()) != null)
                        {
                            for (WatchEvent<?> event : key.pollEvents())
                            {
                                final Path changed = (Path)event.context();
                                
                                if (changed.endsWith(OFFLINE_STORAGE_FILE_NAME))
                                {
                                    LOGGER.info("OfflineSkinServer: Reloading skins because they were updated elsewhere.");
                                    reloadServerFiles();
                                    
                                    // Exile local cache:
                                    HDSkins.getInstance().getProfileRepository().clear();
                                }
                            }
                            
                            key.reset();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.error("OfflineSkinServer: Observer thread was interrupted; bailing...", e);
                    }
                }
                catch (IOException e)
                {
                    LOGGER.error("OfflineSkinServer: IO exception during registration; bailing...", e);
                }
            }
        });
        
        this.observerThread.setPriority(1);
        this.observerThread.start();
    }

    /**
     * Get the file path used for offline storage.
     */
    private Path getOfflineStorageFilePath()
    {
        String userFolder = System.getProperty("user.home");

        //return GamePaths.getConfigDirectory().resolve(OFFLINE_STORAGE_FILE_NAME);
        return Paths.get(userFolder).resolve(OFFLINE_STORAGE_FILE_NAME);
    }
    
    /**
     * Reload files from the server.
     */
    private void reloadServerFiles()
    {
        Path filePath = getOfflineStorageFilePath();
        
        try
        {
            if (Files.isReadable(filePath))
            {
                try (BufferedReader s = Files.newBufferedReader(filePath))
                {
                    this.skinsProvider = gson.fromJson(s, JsonObject.class);
                }
                catch (JsonParseException e)
                {
                    LOGGER.warn("Failed to read or parse offline skins JSON store.");
                }
            }
        }
        catch (IOException | JsonParseException e)
        {
            LOGGER.warn("Failed to read offline skins JSON store.");
        }
    }

    /**
     * Commit skin changes to the JSON store.
     */
    private void saveServerFiles()
    {
        Path filePath = getOfflineStorageFilePath();

        try (BufferedWriter buffer = Files.newBufferedWriter(filePath))
        {
            String str = skinsProvider.toString();
            buffer.write(str);
        }
        catch (IOException e)
        {
            LOGGER.warn("Error while committing changes to the offline skin store", e);
        }
    }
    
    private Optional<String> getCurrentPlayerSkinPath()
    {
        net.minecraft.client.session.Session session = MinecraftClient.getInstance().getSession();
        String profileName = session.getUsername();
        
        return getPlayerSkinPath(profileName);
    }
    
    private Optional<String> getPlayerSkinPath(String profileName)
    {
        if (skinsProvider.has(profileName))
        {
            JsonObject obj = skinsProvider.getAsJsonObject(profileName);

            if (obj.has("skinFilePath"))
            {
                String texturePath = obj.getAsJsonPrimitive("skinFilePath").getAsString();
                
                return Optional.of(texturePath);
            }
        }

        // null result
        return Optional.empty();
    }

    @Override
    public Set<Feature> getFeatures()
    {
        return FEATURES;
    }

    @Override
    public boolean supportsSkinType(SkinType skinType)
    {
        // TODO: add cape support
        return skinType.isKnown() && skinType != SkinType.CAPE;
    }

    @Override
    public boolean ownsUrl(String url)
    {
        // We don't own any URLs for the offline server.
        return false;
    }

    @Override
    public TexturePayload loadSkins(GameProfile profile) throws IOException
    {
        String profileName = profile.getName();
        
        Optional<String> wrappedTexturePath = getPlayerSkinPath(profileName);

        if (wrappedTexturePath.isPresent())
        {
            String texturePath = wrappedTexturePath.get();
            MinecraftProfileTexture texture = new MinecraftProfileTexture(
                "file:///" + texturePath.replace("\\", "/"),
                null
            );

            Map<SkinType, MinecraftProfileTexture> map = new HashMap<>();
            map.put(SkinType.SKIN, texture);

            TexturePayload texturePayload = new TexturePayload(
                profile,
                map
            );

            LOGGER.info("OfflineSkinServer: Loading texture " + texturePath + " for user " + profileName);

            return texturePayload;
        }

        LOGGER.info("OfflineSkinServer: Loading default texture for user " + profileName);

        // null result
        return new TexturePayload(profile, new HashMap<SkinType, MinecraftProfileTexture>());
    }

    @Override
    public void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException
    {
        // Commit the skin changes to the JSON store.
        net.minecraft.client.session.Session session = MinecraftClient.getInstance().getSession();
        String profileName = session.getUsername();

        if (upload instanceof SkinUpload.UriUpload)
        {
            String errorMsg = "URI upload not supported by offline mode.";
            LOGGER.warn(errorMsg);
            throw new IOException(errorMsg);
        }
        else if (upload instanceof SkinUpload.FileUpload fileUpload)
        {
            Path filePath = fileUpload.file();

            JsonObject skinProperties = new JsonObject();
            skinProperties.addProperty("skinFilePath", filePath.toString());

            if (skinsProvider.has(profileName))
            {
                skinsProvider.remove(profileName);
            }

            skinsProvider.add(profileName, skinProperties);
        }
        else if (upload instanceof SkinUpload.Delete deleteCmd)
        {
            if (skinsProvider.has(profileName))
            {
                skinsProvider.remove(profileName);
            }
        }

        saveServerFiles();
    }

    @Override
    public void authorize(Session session) throws IOException, AuthenticationException
    {
        // We don't need to authenticate a logged-out session, of course, so
        // this function just returns immediately.
        return;
    }

    @Override
    public Optional<SkinServerProfile<?>> loadProfile(Session session) throws IOException, AuthenticationException
    {
        SkinServerProfile<RemotePreviewTexture> result = new SkinServerProfile<RemotePreviewTexture>()
        {
            @Override
            public List<RemotePreviewTexture> getSkins(SkinType type)
            {
                Optional<String> texturePath = getCurrentPlayerSkinPath();
                
                if (texturePath.isPresent())
                {
                    RemotePreviewTexture texture = new RemotePreviewTexture(texturePath.get());
                    
                    List<RemotePreviewTexture> texturesList = new ArrayList<RemotePreviewTexture>();
                    texturesList.add(texture);
                    
                    return texturesList;
                }
                
                return List.of();
            }
            
            @Override
            public void setActive(SkinType type, RemotePreviewTexture texture) throws IOException, AuthenticationException
            {
                // Do nothing
            }
        };
        
        return Optional.of(result);
    }

    @Override
    public String toString()
    {
        return new IndentedToStringStyle.Builder(this)
                .append("address", "127.0.0.1")
                .toString();
    }
    
    /*
     * Remote preview texture.
     * 
     * This is required to update the image on the right side of the upload GUI.
     */
    private record RemotePreviewTexture (String url) implements SkinServerProfile.Skin {
        @Override
        public String getModel() {
            return "";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public String getUri() {
            return url;
        }
    }
}
