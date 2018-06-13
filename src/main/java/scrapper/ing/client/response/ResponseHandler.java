package scrapper.ing.client.response;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import scrapper.account.Account;
import scrapper.account.Money;
import scrapper.ing.security.AuthenticatedSession;
import scrapper.ing.security.UnauthenticatedSession;

import java.util.*;

public class ResponseHandler {

    private static final String TOKEN = "token";
    private static final String DATA_FIELD_KEY = "data";
    private static final String SAV_FIELD_KEY = "sav";
    private static final String CUR_FIELD_KEY = "cur";
    private static final String ACCOUNT_KEY = "acct";
    private static final String AVAILABLE_BALANCE_KEY = "avbal";
    private static final String CURRENCY_KEY = "curr";
    private static final String NAME_KEY = "name";
    private static final String SALT = "salt";
    private static final String MASK = "mask";
    private static final String KEY = "key";

    public Optional<UnauthenticatedSession> extractUnauthenticatedSession(Response response) {
        try {
            JSONObject jsonBody = response.jsonBody;
            if (!jsonBody.has(DATA_FIELD_KEY)) {
                return Optional.empty();
            }
            JSONObject data = response.jsonBody.getJSONObject(DATA_FIELD_KEY);
            if (data.has(SALT) && data.has(MASK) && data.has(KEY)) {
                return Optional.of(new UnauthenticatedSession(data.getString(SALT), data.getString(MASK), data
                        .getString(KEY), extractSessionId(response)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private String extractSessionToken(Response response) {
        try {
            JSONObject jsonBody = response.jsonBody;
            if (!jsonBody.has(DATA_FIELD_KEY)) {
                return "";
            }
            JSONObject data = jsonBody.getJSONObject(DATA_FIELD_KEY);
            if (data.has(TOKEN)) {
                return data.getString(TOKEN);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public Optional<AuthenticatedSession> extractAuthenticatedSession(Response authenticationResponse) {
        String token = extractSessionToken(authenticationResponse);
        String sessionId = extractSessionId(authenticationResponse);

        if (token.isEmpty() || sessionId.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AuthenticatedSession(token, sessionId));
    }

    private String extractSessionId(Response response) {
        Optional<Header> sessionHeader = Arrays.stream(response.headers).filter(header -> header.getName().equals
                ("Set-Cookie")).filter(cookieHeader -> cookieHeader.getValue().contains("JSESSIONID")).findFirst();

        if (!sessionHeader.isPresent()) {
            return "";
        }

        String header = sessionHeader.get().getValue();
        int i = header.indexOf('=') + 1;
        int j = header.indexOf(';');
        return header.substring(i, j);
    }

    public List<Account> extractAccountsInfo(Response response) {
        try {
            JSONObject jsonBody = response.jsonBody;
            if (!jsonBody.has(DATA_FIELD_KEY)) {
                return Collections.emptyList();
            }

            JSONObject accounts = jsonBody.getJSONObject(DATA_FIELD_KEY);
            if (!accounts.has(SAV_FIELD_KEY) || !accounts.has(CUR_FIELD_KEY)) {
                return Collections.emptyList();
            }

            JSONArray savingAccounts = accounts.getJSONArray(SAV_FIELD_KEY);
            JSONArray currentAccounts = accounts.getJSONArray(CUR_FIELD_KEY);

            List<Account> result = new ArrayList<>();
            addAccounts(result, savingAccounts);
            addAccounts(result, currentAccounts);

            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void addAccounts(List<Account> aggregator, JSONArray savingAccounts) throws JSONException {
        for (int i = 0; i < savingAccounts.length(); i++) {
            JSONObject current = savingAccounts.getJSONObject(i);

            if (current.has(ACCOUNT_KEY) && current.has(AVAILABLE_BALANCE_KEY) && current.has(CURRENCY_KEY) &&
                    current.has(NAME_KEY)) {
                Account account = new Account(current.getString(ACCOUNT_KEY), new Money(current.getString
                        (AVAILABLE_BALANCE_KEY), current.getString(CURRENCY_KEY)), current.getString(NAME_KEY));
                aggregator.add(account);
            }

        }
    }
}