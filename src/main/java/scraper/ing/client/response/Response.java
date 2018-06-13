package scraper.ing.client.response;

import org.apache.http.Header;
import org.json.JSONObject;

public class Response {

  final JSONObject jsonBody;
  final Header[] headers;

  public Response(JSONObject jsonBody, Header[] headers) {
    this.jsonBody = jsonBody;
    this.headers = headers;
  }
}