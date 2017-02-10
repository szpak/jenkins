package jenkins.widgets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import hudson.model.AllView;
import hudson.model.MockItem;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.View;
import hudson.search.UserSearchProperty;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import jenkins.model.Jenkins;

public class HistoryPageFilterUserTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test   //TODO: Fails with: IllegalArgumentException: Argument for @Nonnull parameter 'primaryView' of hudson/model/AllView.migrateLegacyPrimaryAllViewLocalizedName must not be null
    public void test_search_runs_by_build_result() throws IOException {

//        FullControlOnceLoggedInAuthorizationStrategy strategy = new FullControlOnceLoggedInAuthorizationStrategy();
//        strategy.setAllowAnonymousRead(false);
        AuthorizationStrategy.Unsecured strategy = new AuthorizationStrategy.Unsecured();
        Jenkins.getInstance().setAuthorizationStrategy(strategy);

        Jenkins.getInstance().setSecurityRealm(j.createDummySecurityRealm());

        j.createFreeStyleProject();
        View primaryView = new AllView("all");
        j.jenkins.addView(primaryView);
        j.jenkins.setPrimaryView(primaryView);

        ACL.impersonate(new UsernamePasswordAuthenticationToken("testUser", "any"), new Runnable() {
//        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                try {
                    User.get("testUser").addProperty(new UserSearchProperty(true));
//                    User.get("anonymous").addProperty(new UserSearchProperty(true));
                    List<ModelObject> runs = ImmutableList.<ModelObject>of(new MockRun(2, Result.FAILURE), new MockRun(1, Result.SUCCESS));
                    assertOneMatchingBuildForGivenSearchStringAndRunItems("failure", runs);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void assertOneMatchingBuildForGivenSearchStringAndRunItems(String searchString, List<ModelObject> runs) {
        //given
        HistoryPageFilter<ModelObject> historyPageFilter = newPage(5, null, null);
        //and
        historyPageFilter.setSearchString(searchString);
        //and
        List<Queue.Item> queueItems = newQueueItems(3, 4);

        //when
        historyPageFilter.add(runs, queueItems);

        //then
        Assert.assertEquals(1, historyPageFilter.runs.size());
        Assert.assertEquals(HistoryPageEntry.getEntryId(2), historyPageFilter.runs.get(0).getEntryId());
    }

    @SuppressWarnings("unchecked")
    private static class MockRun extends Run {
        private final long queueId;

        public MockRun(long queueId) throws IOException {
            super(Mockito.mock(Job.class));
            this.queueId = queueId;
        }

        public MockRun(long queueId, Result result) throws IOException {
            this(queueId);
            this.result = result;
        }

        @Override
        public int compareTo(Run o) {
            return 0;
        }

        @Override
        public Result getResult() {
            return result;
        }

        @Override
        public boolean isBuilding() {
            return false;
        }

        @Override
        public long getQueueId() {
            return queueId;
        }

        @Override
        public int getNumber() {
            return (int) queueId;
        }
    }

    private HistoryPageFilter<ModelObject> newPage(int maxEntries, Long newerThan, Long olderThan) {
        HistoryPageFilter<ModelObject> pageFilter = new HistoryPageFilter<>(maxEntries);
        if (newerThan != null) {
            pageFilter.setNewerThan(HistoryPageEntry.getEntryId(newerThan));
        } else if (olderThan != null) {
            pageFilter.setOlderThan(HistoryPageEntry.getEntryId(olderThan));
        }
        return pageFilter;
    }

    private List<Queue.Item> newQueueItems(long startId, long endId) {
        List<Queue.Item> items = new ArrayList<>();

        for (long queueId = startId; queueId <= endId; queueId++) {
            items.add(new MockItem(queueId));
        }
        return items;
    }
}
