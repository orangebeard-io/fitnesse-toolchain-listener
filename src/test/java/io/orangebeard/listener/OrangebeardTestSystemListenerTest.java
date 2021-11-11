package io.orangebeard.listener;

import fitnesse.testsystems.TestSystem;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;

@RunWith(MockitoJUnitRunner.class)
public class OrangebeardTestSystemListenerTest {

    @Mock
    private TestSystem testSystem;

    @Test
    public void test() throws Exception {
        withEnvironmentVariable("orangebeard.endpoint", "http://google.com")
                .and("orangebeard.testset", "blabla")
                .and("orangebeard.project", "blablba")
                .and("orangebeard.accessToken", "e0b677ed-af98-4da8-a111-49f3af19299b")
                .execute(() -> {
                    OrangebeardTestSystemListener orangebeardTestSystemListener = new OrangebeardTestSystemListener(null, true);
                    orangebeardTestSystemListener.testSystemStarted(testSystem);
                });
    }
}
