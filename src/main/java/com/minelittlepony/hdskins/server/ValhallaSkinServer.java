package com.minelittlepony.hdskins.server;

import com.google.common.collect.Sets;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinUpload.Session;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.function.Function;

@ServerType("valhalla")
public class ValhallaSkinServer implements SkinServer {

    private static final String API_PREFIX = "/api/v1";

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.DOWNLOAD_USER_SKIN,
            Feature.UPLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES
    );

    private final String address;

    private transient String accessToken;

    public ValhallaSkinServer(String address) {
        this.address = address;
    }

    private URI buildBackendUri(String path) {
        return URI.create(String.format("%s%s/%s", address, API_PREFIX, path));
    }

    private URI buildBackendUserUri(UUID uuid) {
        return buildBackendUri(String.format("user/%s", uuid.toString()));
    }

    private URI buildBackendHistoryUri(UUID uuid) {
        return buildBackendUri(String.format("history/%s", uuid.toString()));
    }

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public boolean supportsSkinType(SkinType skinType) {
        return skinType.isKnown() && skinType != SkinType.CAPE;
    }

    @Override
    public boolean ownsUrl(String url) {
        try {
            url = new URI(url).getHost();
            String domain = new URI(address).getHost();

            return domain.contentEquals(url) || (url.startsWith("textures") && domain.contentEquals(url.replace("textures", "skins")));
        } catch (final URISyntaxException ignored) { }

        return false;
    }

    @Override
    public TexturePayload loadSkins(GameProfile profile) throws IOException {
        var path = buildBackendUserUri(profile.getId());
        return MoreHttpResponses.execute(HttpRequest.newBuilder(path)
                    .GET()
                    .build())
                .requireOk()
                .json(TexturePayload.class, "Invalid texture payload");
    }

    @Override
    public List<TexturePayload> loadSkins(Collection<GameProfile> profiles) throws IOException {
        var data = new BulkTextures(profiles.stream().map(GameProfile::getId).toList());
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("bulk_textures"))
                        .POST(FileTypes.json(data))
                        .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.APPLICATION_JSON)
                        .build())
                .requireOk()
                .json(BulkTexturesResponse.class, "Invalid texture payload")
                .users;
    }

    @Override
    public TexturePayload loadSkins(Session session) throws IOException, AuthenticationException {
        authorize(session);
        return doAuthorizedRequest(session, (accessToken) -> new TexturePayload(
                session.profile(),
                MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("textures"))
                        .GET()
                        .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                        .build())
                    .requireOk()
                    .json(TexturePayload.Textures.class, "Invalid texture payload")
        ));
    }

    @Override
    public void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException {
        doAuthorizedRequest(upload.session(), (accessToken) -> switch (upload) {
            case SkinUpload.Delete ignored -> MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("texture"))
                            .method("DELETE", FileTypes.json(Map.of("type", upload.type().getParameterizedName())))
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
            case SkinUpload.FileUpload fileUpload ->
                    MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("textures"))
                            .PUT(FileTypes.multiPart(fileUpload.metadata())
                                    .field("type", fileUpload.type().getParameterizedName())
                                    .field("file", fileUpload.file())
                                    .build())
                            .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                            .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
            case SkinUpload.UriUpload uriUpload -> MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("textures"))
                            .POST(FileTypes.json(Util.make(new HashMap<>(uriUpload.metadata()), metadata -> {
                                metadata.put("type", uriUpload.type().getParameterizedName());
                                metadata.put("file", uriUpload.uri().toString());
                            })))
                            .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
        });
    }

    @Override
    public void authorize(Session session) throws IOException, AuthenticationException {
        if (accessToken != null) {
            return;
        }
        GameProfile profile = session.profile();
        AuthHandshake handshake = authHandshake(profile.getName());

        if (handshake.offline) {
            return;
        }

        session.validate(handshake.serverId);

        AuthResponse response = authResponse(profile.getName(), handshake.verifyToken);
        if (!response.userId.equals(profile.getId())) {
            throw new IOException("UUID mismatch!"); // probably won't ever throw
        }
        accessToken = response.accessToken;
    }

    @Override
    public Optional<SkinServerProfile<?>> loadProfile(Session session) throws IOException, AuthenticationException {
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendHistoryUri(session.profile().getId()))
                .GET()
                .build()).accept(r -> r.json(Textures.class, "Server sent invalid profile response")).map(p -> {
                    // TODO: Remove duplicates and sort by upload time
            Function<SkinType, List<Texture>> textures = Util.memoize(type -> {
                Set<String> visited = new HashSet<>();
                return p.textures().getOrDefault(type, List.of())
                        .stream()
                        .filter(texture -> visited.add(texture.url + texture.getModel()))
                        .sorted(Comparator.comparing(t -> -t.startTime))
                        .toList();
            });
            return new SkinServerProfile<Texture>() {
                @Override
                public List<Texture> getSkins(SkinType type) {
                    return textures.apply(type);
                }

                @Override
                public void setActive(SkinType type, Texture texture) throws IOException, AuthenticationException {
                    // TODO: Swap active skins rather than upload a copy
                    uploadSkin(new SkinUpload.UriUpload(session, type, URI.create(texture.getUri()), texture.metadata));
                }
            };
        });
    }

    private interface AuthorizedRequest<T> {
        T doRequest(String accessToken) throws IOException, AuthenticationException;
    }

    private <T> T doAuthorizedRequest(Session session, AuthorizedRequest<T> requester) throws IOException, AuthenticationException {
        //authorize(session);
        try {
            return requester.doRequest(accessToken);
        } catch (HttpException e) {
            if (e.getStatusCode() != 401) {
                throw e;
            }

            accessToken = null;
            //authorize(session);
            return requester.doRequest(accessToken);
        } catch (IOException e) {
            if (e.getMessage().equals("Authorization failed")) {
                accessToken = null;
                authorize(session);
                return requester.doRequest(accessToken);
            }

            throw e;
        }
    }

    private AuthHandshake authHandshake(String name) throws IOException {
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("auth/minecraft"))
                .POST(FileTypes.multiPart()
                        .field("name", name)
                        .build())
                .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                .build())
                .requireOk()
                .json(AuthHandshake.class, "Invalid handshake response");
    }

    private AuthResponse authResponse(String name, long verifyToken) throws IOException {
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("auth/minecraft/callback"))
                .POST(FileTypes.multiPart()
                        .field("name", name)
                        .field("verifyToken", verifyToken)
                        .build())
                .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                .build())
                .requireOk()
                .json(AuthResponse.class, "Invalid auth response");
    }

    @Override
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .toString();
    }

    private record AuthHandshake(boolean offline, String serverId, long verifyToken) {}
    private record AuthResponse(String accessToken, UUID userId) {}

    private record BulkTextures(List<UUID> uuids) {}
    private record BulkTexturesResponse(List<TexturePayload> users) {}

    // TODO: Response does not match the documentation
    private record Textures (String profileId, String profilename, Map<SkinType, List<Texture>> textures) {}
    private record Texture (long startTime, String endTime, Map<String, String> metadata, String url) implements SkinServerProfile.Skin {
        @Override
        public String getModel() {
            return metadata.get("model");
        }

        @Override
        public boolean isActive() {
            return endTime == null || "null".equalsIgnoreCase(endTime);
        }

        @Override
        public String getUri() {
            return url;
        }
    }
}
