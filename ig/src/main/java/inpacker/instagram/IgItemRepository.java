package inpacker.instagram;

import com.google.gson.JsonElement;
import inpacker.core.ItemRepository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class IgItemRepository implements ItemRepository<IgPackConfig, IgPackItem> {

    private AsyncHttpClient asyncHttpClient;
    private JsonParser      jsonParser;

    public IgItemRepository() {
        asyncHttpClient = new DefaultAsyncHttpClient();
        jsonParser = new JsonParser();
    }

    @Override
    public void getPackItems(IgPackConfig conf, Collection<IgPackItem> items) {
        JsonObject respJson;
        boolean moreAvailable;
        String query = "";
        int packedItemsAmount = 0;
        do {
            final ListenableFuture<Response> f =
                    asyncHttpClient.prepareGet(getUrl(conf.user.username, query)).execute();
            final Response response;
            try {
                response = f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
            respJson = jsonParser.parse(response.getResponseBody()).getAsJsonObject();
            moreAvailable = respJson.get("more_available").getAsBoolean();
            final JsonArray itemsJsonArray = respJson.getAsJsonArray("items");
            final List<IgPost> posts = parseItems(itemsJsonArray);
            for (IgPost post: posts) {
                final IgPackItem item = new IgPackItem(post, packedItemsAmount+1, conf.getFileNameCreator());
                if (conf.test(item)) {
                    items.add(item);
                    if (++packedItemsAmount >= conf.nItems) break;
                }
            }
            query = "?max_id=" + posts.get(posts.size()-1).id;
        } while (moreAvailable && packedItemsAmount < conf.nItems);
        final IgPost last = new IgPost("end", "end", 0, "end", "end");
        items.add(new IgPackItem(last, packedItemsAmount, (i, p) -> "end"));
    }

    private List<IgPost> parseItems(JsonArray itemsJsonArr) {
        List<IgPost> items = new ArrayList<>(itemsJsonArr.size());
        for (int i = 0; i < itemsJsonArr.size(); i++) {
            try {
                items.add(parseItem(itemsJsonArr.get(i).getAsJsonObject()));
            } catch (RuntimeException parseItemException) {} // fixme
        }
        return items;
    }

    private IgPost parseItem(JsonObject itemJson) {
        final String username = itemJson.get("user").getAsJsonObject().get("username").getAsString();
        final long createdTime = itemJson.get("created_time").getAsLong();
        final String id = itemJson.get("id").getAsString();
        String type = itemJson.get("type").getAsString();
        String urlsObj = "image".equals(type) ? "images" : "videos";
        if (urlsObj.equals("videos") && itemJson.get(urlsObj) == null) {
            urlsObj = "images";
            type = "image";
        }
        final String url = itemJson.get(urlsObj).getAsJsonObject()
                                   .get("standard_resolution").getAsJsonObject()
                                   .get("url").getAsString();
        return new IgPost(username, url, createdTime, type, id);
    }

    private String getUrl(String username, String queryString) {
        return String.format("https://www.instagram.com/%s/media/%s", username, queryString);
    }

    public IgUser getInstagramUser(String username) {
        Objects.requireNonNull(username, "username is null");
        final Response response;
        final String url = String.format("https://www.instagram.com/%s/?__a=1", username);
        final ListenableFuture<Response> f = asyncHttpClient.prepareGet(url).execute();
        try {
            response = f.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
        if (response.getStatusCode() != 200)
            return null;
        else
            return parseUser(response.getResponseBody());
    }

    private IgUser parseUser(String json) {
        final JsonObject uj = jsonParser.parse(json).getAsJsonObject().get("user").getAsJsonObject();
        String id = uj.get("id").getAsString();
        String username = uj.get("username").getAsString();
        boolean isPrivate = uj.get("is_private").getAsBoolean();
        final JsonElement fullNameEl = uj.get("full_name");
        String fullName = fullNameEl.isJsonNull() ? username : fullNameEl.getAsString();
        final JsonElement biographyEl = uj.get("biography");
        String biography = biographyEl.isJsonNull() ? "" : biographyEl.getAsString();
        String profilePic = uj.get("profile_pic_url_hd").getAsString();
        int count = uj.get("media").getAsJsonObject().get("count").getAsInt();
        boolean isVerified = uj.get("is_verified").getAsBoolean();
        return new IgUser(id, username, isPrivate, fullName, biography, profilePic, count, isVerified);
    }

}
