package twitter4jads.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import twitter4jads.*;
import twitter4jads.api.TwitterAdsTargetingApi;
import twitter4jads.internal.http.HttpParameter;
import twitter4jads.internal.http.HttpResponse;
import twitter4jads.internal.models4j.TwitterException;
import twitter4jads.models.LocationType;
import twitter4jads.models.ads.*;
import twitter4jads.models.ads.tags.TwitterApplicationList;
import twitter4jads.models.ads.targeting.TargetingParamRequest;
import twitter4jads.models.ads.targeting.TargetingParamResponse;
import twitter4jads.models.ads.targeting.TwitterTargetingOperator;
import twitter4jads.util.TwitterAdUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import static twitter4jads.TwitterAdsConstants.*;
import static twitter4jads.models.ads.TargetingType.*;
import static twitter4jads.util.TwitterAdUtil.constructBaseAdsListResponse;
import static twitter4jads.util.TwitterAdUtil.constructBaseAdsResponse;

/**
 * User: abhay
 * Date: 4/4/16
 * Time: 7:16 PM
 */
public class TwitterAdsTargetingApiImpl implements TwitterAdsTargetingApi {

    private static final Integer MAX_REQUEST_PARAMETER_SIZE = 50;
    private static final Set<TargetingType> TARGETING_TYPES_WITH_MODIFIER_PREFIX_OR_SUFFIX =
            Sets.newHashSet(EXCLUDE_APP_LIST, NEGATIVE_BEHAVIOR, NEGATIVE_EXACT_KEYWORD, NEGATIVE_PHRASE_KEYWORD, NEGATIVE_UNORDERED_KEYWORD,
                    NETWORK_ACTIVATION_DURATION_GTE, NETWORK_ACTIVATION_DURATION_LT);

    private static final Gson GSON = new Gson();
    private final TwitterAdsClient twitterAdsClient;

    public TwitterAdsTargetingApiImpl(TwitterAdsClient twitterAdsClient) {
        this.twitterAdsClient = twitterAdsClient;
    }

    @Deprecated
    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getTargetingCriterias(String accountId, String lineItemId, boolean withDeleted)
            throws TwitterException {
        TwitterAdUtil.ensureNotNull(lineItemId, "lineItemId");

        return getTargetingCriterias(accountId, Collections.singleton(lineItemId), withDeleted, null, null, null);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getTargetingCriterias(String accountId, Collection<String> lineItemIds, boolean withDeleted,
                                                                                Integer count, String cursor, String sortBy)
            throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, ACCOUNT_ID);
        TwitterAdUtil.ensureNotEmpty(lineItemIds, "lineItemIds");

        final List<HttpParameter> params = new ArrayList<>();
        TwitterAdUtil.ensureMaxSize(lineItemIds, MAX_LINE_ITEM_IDS_REQUEST_SIZE);
        params.add(new HttpParameter(PARAM_LINE_ITEM_IDS, TwitterAdUtil.getCsv(lineItemIds)));
        params.add(new HttpParameter(PARAM_WITH_DELETED, withDeleted));
        if (count != null) {
            params.add(new HttpParameter(PARAM_COUNT, count));
        }
        if (TwitterAdUtil.isNotNullOrEmpty(cursor)) {
            params.add(new HttpParameter(PARAM_CURSOR, cursor));
        }
        if (TwitterAdUtil.isNotNullOrEmpty(sortBy)) {
            params.add(new HttpParameter(PARAM_SORT_BY, sortBy));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_TARGETING_CRITERIA;
        final Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsResponse<TargetingCriteria> getTargetingCriteriaById(String accountId, String targetingId, boolean withDeleted)
            throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, ACCOUNT_ID);
        TwitterAdUtil.ensureNotNull(targetingId, "Targeting Id");

        String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_TARGETING_CRITERIA +
                targetingId;
        HttpParameter[] params = new HttpParameter[]{new HttpParameter(PARAM_WITH_DELETED, withDeleted)};
        Type type = new TypeToken<BaseAdsResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpRequest(baseUrl, params, type, HttpVerb.GET);
    }

    @Override
    public BaseAdsResponse<TargetingCriteria> deleteTargetingCriteria(String accountId, String targetingCriteriaId) throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, ACCOUNT_ID);
        TwitterAdUtil.ensureNotNull(targetingCriteriaId, "Targeting Id");
        String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_TARGETING_CRITERIA +
                targetingCriteriaId;
        Type type = new TypeToken<BaseAdsResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpRequest(baseUrl, null, type, HttpVerb.DELETE);
    }

    @Override
    public BaseAdsListResponseIterable<twitter4jads.models.ads.TargetingLocation> getAllTargetingLocations(Optional<LocationType> locationType, String q,
                                                                                                           String countryCode, Optional<Integer> count)
            throws TwitterException {
        List<HttpParameter> params = validateTargetingLocationParameters(locationType, q, countryCode, count);
        String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TARGETING_CRITERIA_LOCATION;

        Type type = new TypeToken<BaseAdsListResponse<twitter4jads.models.ads.TargetingLocation>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTargetingEvents(String q) throws TwitterException {
        return hitQueryForPath(q, PATH_TARGETING_CRITERIA_EVENT);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTargetingInterests(String q) throws TwitterException {
        return hitQueryForPath(q, PATH_TARGETING_CRITERIA_INTERESTS);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTargetingPlatforms(String q) throws TwitterException {
        return hitQueryForPath(q, PATH_TARGETING_CRITERIA_PLATFORMS);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTargetingNetworkOperators(String q) throws TwitterException {
        return hitQueryForPath(q, PATH_TARGETING_CRITERIA_NETWORK_OPERATORS);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTargetingLocales(String q) throws TwitterException {
        return hitQueryForPath(q, PATH_TARGETING_LANGUAGES);
    }

    @Override
    public List<TargetingCriteria> createTargetingCriterias(String accountId, String lineItemId, List<TargetingCriteria> targetingCriteriaValues)
            throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, ACCOUNT_ID);
        TwitterAdUtil.ensureNotNull(lineItemId, "Line Item");

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_TARGETING_CRITERIA;
        final List<HttpParameter> params = validateAndCreateTargetingParameters(targetingCriteriaValues);
        params.add(new HttpParameter(PARAM_LINE_ITEM_ID, lineItemId));

        final HttpResponse httpResponse = twitterAdsClient.putRequest(baseUrl, params.toArray(new HttpParameter[params.size()]));
        try {
            final Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
            }.getType();

            final String asString = httpResponse.asString();
            final BaseAdsListResponse<TargetingCriteria> baseAdsListResponse = constructBaseAdsListResponse(httpResponse, asString, type);
            return baseAdsListResponse == null ? Collections.<TargetingCriteria>emptyList() : baseAdsListResponse.getData();
        } catch (IOException e) {
            throw new TwitterException("Failed to parse targeting criterias.");
        }
    }

    @Override
    public BaseAdsResponse<TargetingCriteria> createTargetingCriteria(String accountId, String lineItemId, TargetingType targetingType,
                                                                      String targetingValue, TwitterTargetingOperator targetingOperator) throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, "AccountId");
        TwitterAdUtil.ensureNotNull(lineItemId, "Line Item Id");
        TwitterAdUtil.ensureNotNull(targetingType, "Targeting Type");
        if (TARGETING_TYPES_WITH_MODIFIER_PREFIX_OR_SUFFIX.contains(targetingType)) {
            throw new IllegalArgumentException("Support for targeting types with modifier prefix or suffix is removed since API v6. "
                    + "Please use corresponding unmodified targeting types with correct operator type.\n"
                    + "e.g. targeting_type=NEGATIVE_BEHAVIOR => targeting_type=BEHAVIOR&operator_type=NE");
        }
        TwitterAdUtil.ensureNotNull(targetingValue, "Targeting Value");
        TwitterAdUtil.ensureNotNull(targetingOperator, "Targeting Operator Type");

        final List<HttpParameter> params = new ArrayList<>();
        params.add(new HttpParameter(PARAM_LINE_ITEM_ID, lineItemId));
        params.add(new HttpParameter(PARAM_TARGETING_TYPE, targetingType.name()));
        params.add(new HttpParameter(PARAM_OPERATOR_TYPE, targetingOperator.name()));
        params.add(new HttpParameter(PARAM_TARGETING_VALUE, targetingValue));

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_TARGETING_CRITERIA;
        final Type type = new TypeToken<BaseAdsResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpRequest(baseUrl, params.toArray(new HttpParameter[params.size()]), type, HttpVerb.POST);
    }

    @Override
    public BaseAdsListResponseIterable<PlatformVersions> getAllTargetingPlatformVersions() throws TwitterException {
        return getAllTargetingPlatformVersions(null, null);
    }

    @Override
    public BaseAdsListResponseIterable<PlatformVersions> getAllTargetingPlatformVersions(String q, TwitterOSType osType) throws TwitterException {
        return hitQueryForPath(q, PATH_TARGETING_CRITERIA_PLATFORM_VERSIONS);
    }

    @Override
    public BaseAdsListResponseIterable<Devices> getAllTargetingDevices(String q) throws TwitterException {
        return hitQueryForPath(q, PATH_TARGETING_CRITERIA_DEVICES);
    }

    private void validateTargetingBatch(List<TargetingParamRequest> targetingParamRequests) throws
            TwitterException {
        if (!TwitterAdUtil.isNotEmpty(targetingParamRequests)) {
            throw new TwitterException("Targeting Params size is 0");
        }

        if (targetingParamRequests.size() > 500) {
            throw new TwitterException("Targeting Params size cannot be more than 500 as per the API");
            //https://dev.twitter.com/ads/reference/post/batch/accounts/%3Aaccount_id/targeting_criteria
        }
    }

    @Override
    public BaseAdsResponse<NewTwitterReachEstimate> getReachEstimate(String accountId, ProductType productType,
                                                                     List<TargetingCriteria> targetingCriterias, String objective,
                                                                     Long bidAmountLocalMicro, String currency,
                                                                     Long campaignDailyBudgetAmountLocalMicro, String bidType)
            throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, ACCOUNT_ID);
        TwitterAdUtil.ensureNotNull(productType, "Product Type");
        TwitterAdUtil.ensureNotNull(objective, "Objective");

        TwitterAdUtil.ensureNotNull(currency, "Currency");
        TwitterAdUtil.ensureNotNull(campaignDailyBudgetAmountLocalMicro, "Campaign Daily Budget Amount Local Micro");
        if (StringUtils.isBlank(bidType)) {
            if (bidAmountLocalMicro != null) {
                throw new IllegalArgumentException("bidAmountLocalMicro has to be null when the BidType is null as default bidType is AUTO");
            }
            bidType = BidType.AUTO.name();
        }

        final List<HttpParameter> params = validateAndCreateTargetingParameters(targetingCriterias);
        params.add(new HttpParameter(PARAM_PRODUCT_TYPE, productType.name()));
        params.add(new HttpParameter(PARAM_OBJECTIVE, objective));
        params.add(new HttpParameter(CURRENCY, currency));
        params.add(new HttpParameter(CAMPAIGN_DAILY_BUDGET_AMOUNT_LOCAL_MICRO, campaignDailyBudgetAmountLocalMicro));
        params.add(new HttpParameter(BID_TYPE, bidType));

        if (!BidType.AUTO.name().equals(bidType)) {
            TwitterAdUtil.ensureNotNull(bidAmountLocalMicro, "Bid Amount Local Micro");
            params.add(new HttpParameter(BID_AMOUNT_LOCAL_MICRO, bidAmountLocalMicro));
        }

        final String url = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_REACH_ESTIMATE;
        final Type type = new TypeToken<BaseAdsResponse<NewTwitterReachEstimate>>() {
        }.getType();

        return twitterAdsClient.executeHttpRequest(url, params.toArray(new HttpParameter[params.size()]), type, HttpVerb.GET);
    }

    @Override
    public BaseAdsResponse<AudienceSummary> getAudienceSummary(String accountId, List<TargetingCriteria> targetingCriteriaList)
            throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, ACCOUNT_ID);
        TwitterAdUtil.ensureNotEmpty(targetingCriteriaList, "Targeting Criteria");

        JsonObject params = new JsonObject();
        params.add("targeting_criteria", new Gson().toJsonTree(targetingCriteriaList, new TypeToken<List<AudienceSummary>>() {}.getType()).getAsJsonArray());

        final String url = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_AUDIENCE_SUMMARY;
        final Type type = new TypeToken<BaseAdsResponse<AudienceSummary>>() {
        }.getType();


        try {
            HttpResponse httpResponse = twitterAdsClient.postRequest(url, GSON.toJson(params));
            String stringResponse = httpResponse.asString();
            return constructBaseAdsResponse(httpResponse, stringResponse, type);
        } catch (IOException e) {
            throw new TwitterException("Failed to parse response.", e);
        }
    }

    @Override
    public BaseAdsListResponseIterable<IabCategory> fetchIabCategories(String q) throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        if (TwitterAdUtil.isNotNullOrEmpty(q)) {
            params.add(new HttpParameter(PARAM_Q, q));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_IAB_CATEGORIES;
        final Type type = new TypeToken<BaseAdsListResponse<IabCategory>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public TargetingParamResponse createTargetingBatchRequest(String
                                                                      accountId, List<TargetingParamRequest> targetingParamRequests) throws TwitterException {
        validateTargetingBatch(targetingParamRequests);

        String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_BATCH_ACCOUNTS_URI + accountId + PATH_TARGETING_CRITERIA;
        HttpResponse httpResponse = twitterAdsClient.postRequest(baseUrl, GSON.toJson(targetingParamRequests));
        Type typeToken = new TypeToken<TargetingParamResponse>() {
        }.getType();

        return GSON.fromJson(httpResponse.asString(), typeToken);
    }


    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTargetingTVChannels(String
                                                                                            tvMarketLocale, Optional<Integer> count, Optional<String> cursor)
            throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        if (TwitterAdUtil.isNotNullOrEmpty(tvMarketLocale)) {
            params.add(new HttpParameter(PARAM_TV_MARKET_LOCALE, tvMarketLocale));
        }
        if (count != null && count.isPresent()) {
            params.add(new HttpParameter(PARAM_COUNT, count.get()));
        }
        if (cursor != null && cursor.isPresent()) {
            params.add(new HttpParameter(PARAM_CURSOR, cursor.get()));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TV_CHANNELS;
        Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingLocation> getTargetingLocations(String query, LocationType
            locationType) throws TwitterException {

        List<HttpParameter> params = new ArrayList<>(2);
        if (StringUtils.isNotBlank(query)) {
            params.add(new HttpParameter(PARAM_Q, query.trim()));
        }
        params.add(new HttpParameter(PARAM_LOCATION_TYPE, locationType.name()));
        String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TARGETING_CRITERIA_LOCATION;
        Type type = new TypeToken<BaseAdsListResponse<TargetingLocation>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<Conversations> getAllTargetingConversations() throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TARGETING_CRITERIA_CONVERSATIONS;
        final Type type = new TypeToken<BaseAdsListResponse<Conversations>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<IabCategory> getAllIabCategories(String q) throws TwitterException {
        return hitQueryForPath(q, PATH_IAB_CATEGORIES);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTVShows(String tvMarket, String q, Optional<Integer> count, Optional<String> cursor) throws TwitterException {
        TwitterAdUtil.ensureNotNull(tvMarket, "tvMarketLocale");

        List<HttpParameter> params = validateTvShowsParameters(tvMarket, q, count, cursor);
        String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TV_SHOWS;

        final Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllEvents() throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TARGETING_CRITERIA_EVENT;
        final Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTVMarkets() throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TV_MARKETS;
        final Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<TargetingCriteria> getAllTargetingTVGenres() throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TV_GENRES;
        final Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public List<TargetingSuggestion> getTargetingSuggestion(String accountId, SuggestionType suggestionType, List<String> targetingValues, Optional<Integer> count, List<String> ignoredValues) throws TwitterException {
        TwitterAdUtil.ensureNotNull(accountId, ACCOUNT_ID);
        TwitterAdUtil.ensureNotNull(suggestionType, "Suggestion Type");
        TwitterAdUtil.ensureNotEmpty(targetingValues, "Targeting Values");

        final List<HttpParameter> params = new ArrayList<>();
        params.add(new HttpParameter(PARAM_SUGGESTION_TYPE, suggestionType.name()));
        params.add(new HttpParameter(PARAM_TARGETING_VALUES, TwitterAdUtil.getCsv(targetingValues)));
        if (TwitterAdUtil.isNotEmpty(ignoredValues)) {
            params.add(new HttpParameter(PARAM_IGNORED_VALUES, TwitterAdUtil.getCsv(ignoredValues)));
        }
        if (count != null && count.isPresent()) {
            params.add(new HttpParameter(PARAM_COUNT, count.get() > MAX_REQUEST_PARAMETER_SIZE ? MAX_REQUEST_PARAMETER_SIZE : count.get()));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_TARGETING_SUGGESTIONS;
        final HttpResponse httpResponse = twitterAdsClient.getRequest(baseUrl, params.toArray(new HttpParameter[params.size()]));
        try {
            final Type type = new TypeToken<BaseAdsListResponse<TargetingSuggestion>>() {
            }.getType();

            final BaseAdsListResponse<TargetingSuggestion> baseAdsListResponse =
                    constructBaseAdsListResponse(httpResponse, httpResponse.asString(), type);
            return baseAdsListResponse == null ? Collections.<TargetingSuggestion>emptyList() : baseAdsListResponse.getData();
        } catch (IOException e) {
            throw new TwitterException("Failed to parse promoted tweets.");
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public List<TwitterAppStore> searchAppStoreCategories(String q, Optional<TwitterOSType> osType) throws TwitterException {
        List<HttpParameter> params = new ArrayList<>();
        if (TwitterAdUtil.isNotNullOrEmpty(q)) {
            params.add(new HttpParameter(PARAM_Q, q));
        }
        if (osType != null && osType.isPresent()) {
            params.add(new HttpParameter(PARAM_OS_TYPE, osType.get().name()));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_TARGETING_CRITERIA_APP_STORE_CATEGORIES;
        final HttpResponse httpResponse = twitterAdsClient.getRequest(baseUrl, params.toArray(new HttpParameter[params.size()]));
        try {
            final Type type = new TypeToken<BaseAdsListResponse<TwitterAppStore>>() {
            }.getType();

            final BaseAdsListResponse<TwitterAppStore> baseAdsListResponse =
                    constructBaseAdsListResponse(httpResponse, httpResponse.asString(), type);
            return baseAdsListResponse == null ? Collections.<TwitterAppStore>emptyList() : baseAdsListResponse.getData();
        } catch (IOException e) {
            throw new TwitterException("Failed to parse response for app store categories");
        }
    }

    @Override
    public BaseAdsListResponseIterable<TwitterBehavior> getBehaviors(Optional<Integer> count, Optional<String> cursor, List<String> behaviorIds, Optional<String> countryCode) throws TwitterException {
        List<HttpParameter> params = new ArrayList<>();
        if (count != null && count.isPresent()) {
            params.add(new HttpParameter("count", count.get()));
        }
        if (cursor != null && cursor.isPresent()) {
            params.add(new HttpParameter("cursor", cursor.get()));
        }
        if (TwitterAdUtil.isNotEmpty(behaviorIds)) {
            params.add(new HttpParameter("behavior_ids", TwitterAdUtil.getCsv(behaviorIds)));
        }
        if (countryCode != null && countryCode.isPresent()) {
            params.add(new HttpParameter("country_code", countryCode.get()));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_BEHAVIORS;
        final Type type = new TypeToken<BaseAdsListResponse<TwitterBehavior>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<TwitterBehaviorTaxonomy> getBehaviorTaxonomy(List<String> behaviorTaxonomyIds, List<String> parentBehaviorTaxonomyIds, Optional<Integer> count, Optional<String> cursor) throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        if (TwitterAdUtil.isNotEmpty(parentBehaviorTaxonomyIds)) {
            params.add(new HttpParameter(PARAM_PARENT_BEHAVIOR_TAXONOMY_IDS, TwitterAdUtil.getCsv(parentBehaviorTaxonomyIds)));
        }

        if (count != null && count.isPresent()) {
            params.add(new HttpParameter("count", count.get()));
        }

        if (cursor != null && cursor.isPresent()) {
            params.add(new HttpParameter("cursor", cursor.get()));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PATH_BEHAVIORS_TAXONOMY;
        final Type type = new TypeToken<BaseAdsListResponse<TwitterBehaviorTaxonomy>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsListResponseIterable<TwitterApplicationList> getAllAppLists(String accountId) throws
            TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_APP_LIST;
        final Type type = new TypeToken<BaseAdsListResponse<TwitterApplicationList>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    @Override
    public BaseAdsResponse<TwitterApplicationList> getAllAppsListsById(String accountId, String listId) throws TwitterException {
        //noinspection MismatchedQueryAndUpdateOfCollection
        final List<HttpParameter> params = new ArrayList<>();
        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_APP_LIST + listId;
        final Type typeToken = new TypeToken<BaseAdsResponse<TwitterApplicationList>>() {
        }.getType();

        return twitterAdsClient.executeHttpRequest(baseUrl, params.toArray(new HttpParameter[params.size()]), typeToken, HttpVerb.GET);
    }

    @Override
    public BaseAdsResponse<TwitterApplicationList> createNewApplicationList(String accountId, TwitterApplicationList twitterApplicationList) throws TwitterException {
        final List<HttpParameter> params = validateAndCreateApplicationListParameters(twitterApplicationList);
        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + PREFIX_ACCOUNTS_URI + accountId + PATH_APP_LIST;
        final Type typeToken = new TypeToken<BaseAdsResponse<TwitterApplicationList>>() {
        }.getType();

        return twitterAdsClient.executeHttpRequest(baseUrl, params.toArray(new HttpParameter[params.size()]), typeToken, HttpVerb.POST);
    }

    // ------------------------------------------------------------------- PRIVATE METHODS ---------------------------------------------------------

    @SuppressWarnings("Duplicates")
    private <T> BaseAdsListResponseIterable<T> hitQueryForPath(String q, String queryPath) throws
            TwitterException {
        return hitQueryForPath(q, queryPath, null);
    }

    private <T> BaseAdsListResponseIterable<T> hitQueryForPath(String q, String queryPath, TwitterOSType osType)
            throws TwitterException {
        final List<HttpParameter> params = new ArrayList<>();
        if (TwitterAdUtil.isNotNullOrEmpty(q)) {
            params.add(new HttpParameter(PARAM_Q, q));
        }
        if (TwitterAdUtil.isNotNull(osType)) {
            params.add(new HttpParameter(PARAM_OS_TYPE, osType.name()));
        }

        final String baseUrl = twitterAdsClient.getBaseAdsAPIUrl() + queryPath;
        final Type type = new TypeToken<BaseAdsListResponse<TargetingCriteria>>() {
        }.getType();

        return twitterAdsClient.executeHttpListRequest(baseUrl, params, type);
    }

    private List<HttpParameter> validateAndCreateTargetingParameters(List<TargetingCriteria> targetingValues) {
        final List<HttpParameter> params = new ArrayList<>();
        final Map<TargetingType, List<String>> targeting = new HashMap<>();
        for (TargetingCriteria targetingCriteria : targetingValues) {
            if (targeting.containsKey(targetingCriteria.getTargetingType())) {
                targeting.get(targetingCriteria.getTargetingType()).add(targetingCriteria.getTargetingValue());
            } else {
                targeting.put(targetingCriteria.getTargetingType(), Lists.newArrayList(targetingCriteria.getTargetingValue()));
            }
        }

        for (Map.Entry<TargetingType, List<String>> entry : targeting.entrySet()) {
            switch (entry.getKey()) {
                case LOCATION:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 250);
                    params.add(new HttpParameter(PARAM_LOCATIONS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case FOLLOWERS_OF_USER:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 100);
                    params.add(new HttpParameter(PARAM_FOLLOWERS_OF_USERS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case SIMILAR_TO_FOLLOWERS_OF_USER:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 100);
                    params.add(new HttpParameter(PARAM_SIMILAR_TO_FOLLOWERS_OF_USERS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case INTEREST:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1000);
                    params.add(new HttpParameter(PARAM_INTERESTS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case PLATFORM:
                    params.add(new HttpParameter(PARAM_PLATFORMS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case PLATFORM_VERSION:
                    params.add(new HttpParameter(PARAM_PLATFORM_VERSIONS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case CONVERSATION:
                    params.add(new HttpParameter(PARAM_CONVERSATIONS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case DEVICE:
                    params.add(new HttpParameter(PARAM_DEVICES, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case WIFI_ONLY:
                    params.add(new HttpParameter(PARAM_WIFI_ONLY, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case GENDER:
                    params.add(new HttpParameter(PARAM_GENDER, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case TV_SHOW:
                    params.add(new HttpParameter(PARAM_TV_SHOWS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case TV_CHANNEL:
                    params.add(new HttpParameter(PARAM_TV_CHANNEL, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case TV_GENRE:
                    params.add(new HttpParameter(PARAM_TV_GENRE, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case TV_MARKET:
                    break;
                case NETWORK_OPERATOR:
                    params.add(new HttpParameter(PARAM_NETWORK_OPERATOR, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case BROAD_KEYWORD:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1000);
                    params.add(new HttpParameter(PARAM_BROAD_KEYWORDS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case UNORDERED_KEYWORD:
                    params.add(new HttpParameter(PARAM_UNORDERED_KEYWORDS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case PHRASE_KEYWORD:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1000);
                    params.add(new HttpParameter(PARAM_PHRASE_KEYWORDS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case EXACT_KEYWORD:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1000);
                    params.add(new HttpParameter(PARAM_EXACT_KEYWORDS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case NEGATIVE_PHRASE_KEYWORD:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1000);
                    params.add(new HttpParameter(PARAM_NEGATIVE_PHRASE_KEYWORDS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case NEGATIVE_UNORDERED_KEYWORD:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1000);
                    params.add(new HttpParameter(PARAM_NEGATIVE_UNORDERED_KEYWORDS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case NEGATIVE_EXACT_KEYWORD:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1000);
                    params.add(new HttpParameter(PARAM_NEGATIVE_EXACT_KEYWORDS, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case TAILORED_AUDIENCE:
                    params.add(new HttpParameter(PARAM_TAILORED_AUDIENCES, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case TAILORED_AUDIENCES_EXCLUDED:
                    params.add(new HttpParameter(PARAM_TAILORED_AUDIENCES_EXCLUDED, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case TAILORED_AUDIENCES_EXPANDED:
                    params.add(new HttpParameter(PARAM_TAILORED_AUDIENCES_EXPANDED, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case LANGUAGE:
                    params.add(new HttpParameter(PARAM_LANGUAGES, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case RTB_APP_CATEGORY:
                    break;
                case NETWORK_ACTIVATION_DURATION_LT:
                    params.add(new HttpParameter(PARAM_NETWORK_ACTIVATION_DURATION_LT, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case NETWORK_ACTIVATION_DURATION_GTE:
                    params.add(new HttpParameter(PARAM_NETWORK_ACTIVATION_DURATION_GTE, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case AGE:
                    params.add(new HttpParameter(PARAM_AGE_RANGE, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case BEHAVIOR:
                    params.add(new HttpParameter(PARAM_BEHAVIOR, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case NEGATIVE_BEHAVIOR:
                    params.add(new HttpParameter(PARAM_NEGATIVE_BEHAVIOR, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case BEHAVIOR_EXPANDED:
                    params.add(new HttpParameter(PARAM_BEHAVIOR_EXPANDED, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case ENGAGEMENT_TYPE:
                    params.add(new HttpParameter(PARAM_ENGAGEMENT_TYPE, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case USER_ENGAGEMENT:
                    params.add(new HttpParameter(PARAM_USER_ENGAGEMENT, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case CAMPAIGN_ENGAGEMENT:
                    params.add(new HttpParameter(PARAM_CAMPAIGN_ENGAGEMENT, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case EXCLUDE_APP_LIST:
                    params.add(new HttpParameter(PARAM_EXCLUDE_APP_LIST_IDENTIFIER, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case EVENT:
                    TwitterAdUtil.ensureMaxSize(entry.getValue(), 1);
                    params.add(new HttpParameter(PARAM_EVENT, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
                case APP_STORE_CATEGORY:
                    params.add(new HttpParameter(PARAM_APP_STORE_CATEGORY, TwitterAdUtil.getCsv(entry.getValue())));
                    break;
            }
        }
        return params;
    }

    private List<HttpParameter> validateTargetingLocationParameters(final Optional<LocationType> locationType,
                                                                    final String q, final String countryCode,
                                                                    final Optional<Integer> count) {

        List<HttpParameter> params = new ArrayList<>();
        if (locationType != null && locationType.isPresent()) {
            params.add(new HttpParameter(PARAM_LOCATION_TYPE, locationType.get().name()));
        }
        if (TwitterAdUtil.isNotNullOrEmpty(q)) {
            params.add(new HttpParameter(PARAM_Q, q));
        }
        if (TwitterAdUtil.isNotNullOrEmpty(countryCode)) {
            params.add(new HttpParameter(PARAM_COUNTRY_CODE, countryCode));
        }
        if (count != null && count.isPresent()) {
            params.add(new HttpParameter(PARAM_COUNT, count.get()));
        }
        return params;
    }

    private List<HttpParameter> validateTvShowsParameters(String tvMarket, String
            q, Optional<Integer> count, Optional<String> cursor) {
        TwitterAdUtil.ensureNotNull(tvMarket, "tvMarket");
        List<HttpParameter> params = new ArrayList<>();
        if (StringUtils.isNotBlank(tvMarket)) {
            params.add(new HttpParameter(TwitterAdsConstants.PARAM_LOCALE, tvMarket));
        }
        if (StringUtils.isNotBlank(q)) {
            params.add(new HttpParameter(PARAM_Q, q));
        }
        if (count != null && count.isPresent()) {
            params.add(new HttpParameter(PARAM_COUNT, count.get()));
        }
        if (cursor != null && cursor.isPresent()) {
            params.add(new HttpParameter(PARAM_CURSOR, cursor.get()));
        }
        return params;
    }

    private List<HttpParameter> validateAndCreateApplicationListParameters(TwitterApplicationList twitterApplicationList) {
        List<HttpParameter> params = new ArrayList<>();
        if (TwitterAdUtil.isNotNullOrEmpty(twitterApplicationList.getName())) {
            params.add(new HttpParameter(PARAM_NAME, twitterApplicationList.getName()));
        }
        if (TwitterAdUtil.isNotEmpty(twitterApplicationList.getApps())) {
            List<TwitterApplicationDetails> apps = twitterApplicationList.getApps();
            List<String> appIdentifiers = Lists.newArrayList();
            for (TwitterApplicationDetails twitterApplicationDetails : apps) {
                if (TwitterAdUtil.isNotNullOrEmpty(twitterApplicationDetails.getAppIdentifier())) {
                    appIdentifiers.add(twitterApplicationDetails.getAppIdentifier());
                }
            }
            if (TwitterAdUtil.isNotEmpty(appIdentifiers)) {
                params.add(new HttpParameter(PARAM_APP_STORE_IDENTIFIERS, TwitterAdUtil.getCsv(appIdentifiers)));
            }
        }
        return params;
    }
}
