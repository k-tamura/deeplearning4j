package org.deeplearning4j.rl4j.learning.async;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.deeplearning4j.rl4j.learning.IHistoryProcessor;
import org.deeplearning4j.rl4j.learning.listener.TrainingListenerList;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.network.NeuralNet;
import org.deeplearning4j.rl4j.policy.Policy;
import org.deeplearning4j.rl4j.space.Encodable;
import org.deeplearning4j.rl4j.support.*;
import org.deeplearning4j.rl4j.util.IDataManager;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AsyncThreadTest {

    @Test
    public void when_newEpochStarted_expect_neuralNetworkReset() {
        // Arrange
        TestContext context = new TestContext();
        context.listener.setRemainingOnNewEpochCallCount(5);

        // Act
        context.sut.run();

        // Assert
        assertEquals(6, context.neuralNet.resetCallCount);
    }

    @Test
    public void when_onNewEpochReturnsStop_expect_threadStopped() {
        // Arrange
        TestContext context = new TestContext();
        context.listener.setRemainingOnNewEpochCallCount(1);

        // Act
        context.sut.run();

        // Assert
        assertEquals(2, context.listener.onNewEpochCallCount);
        assertEquals(1, context.listener.onEpochTrainingResultCallCount);
    }

    @Test
    public void when_epochTrainingResultReturnsStop_expect_threadStopped() {
        // Arrange
        TestContext context = new TestContext();
        context.listener.setRemainingOnEpochTrainingResult(1);

        // Act
        context.sut.run();

        // Assert
        assertEquals(2, context.listener.onNewEpochCallCount);
        assertEquals(2, context.listener.onEpochTrainingResultCallCount);
    }

    @Test
    public void when_run_expect_preAndPostEpochCalled() {
        // Arrange
        TestContext context = new TestContext();

        // Act
        context.sut.run();

        // Assert
        assertEquals(6, context.sut.preEpochCallCount);
        assertEquals(6, context.sut.postEpochCallCount);
    }

    @Test
    public void when_run_expect_trainSubEpochCalledAndResultPassedToListeners() {
        // Arrange
        TestContext context = new TestContext();

        // Act
        context.sut.run();

        // Assert
        assertEquals(5, context.listener.statEntries.size());
        int[] expectedStepCounter = new int[] { 2, 4, 6, 8, 10 };
        for(int i = 0; i < 5; ++i) {
            IDataManager.StatEntry statEntry = context.listener.statEntries.get(i);
            assertEquals(expectedStepCounter[i], statEntry.getStepCounter());
            assertEquals(i, statEntry.getEpochCounter());
            assertEquals(38.0, statEntry.getReward(), 0.0001);
        }
    }

    @Test
    public void when_run_expect_NeuralNetIsResetAtInitAndEveryEpoch() {
        // Arrange
        TestContext context = new TestContext();

        // Act
        context.sut.run();

        // Assert
        assertEquals(6, context.neuralNet.resetCallCount);
    }

    @Test
    public void when_run_expect_trainSubEpochCalled() {
        // Arrange
        TestContext context = new TestContext();

        // Act
        context.sut.run();

        // Assert
        assertEquals(10, context.sut.trainSubEpochParams.size());
        for(int i = 0; i < 10; ++i) {
            MockAsyncThread.TrainSubEpochParams params = context.sut.trainSubEpochParams.get(i);
            if(i % 2 == 0) {
                assertEquals(2, params.nstep);
                assertEquals(8.0, params.obs.toArray()[0], 0.00001);
            }
            else {
                assertEquals(1, params.nstep);
                assertNull(params.obs);
            }
        }
    }

    private static class TestContext {
        public final MockAsyncGlobal asyncGlobal = new MockAsyncGlobal();
        public final MockNeuralNet neuralNet = new MockNeuralNet();
        public final MockObservationSpace observationSpace = new MockObservationSpace();
        public final MockMDP mdp = new MockMDP(observationSpace);
        public final MockAsyncConfiguration config = new MockAsyncConfiguration(5, 10, 0, 0, 10, 0, 0, 0, 0, 0);
        public final TrainingListenerList listeners = new TrainingListenerList();
        public final MockTrainingListener listener = new MockTrainingListener();
        private final IHistoryProcessor.Configuration hpConf = new IHistoryProcessor.Configuration(5, 4, 4, 4, 4, 0, 0, 2);
        public final MockHistoryProcessor historyProcessor = new MockHistoryProcessor(hpConf);

        public final MockAsyncThread sut = new MockAsyncThread(asyncGlobal, 0, neuralNet, mdp, config, listeners);

        public TestContext() {
            asyncGlobal.setMaxLoops(10);
            listeners.add(listener);
            sut.setHistoryProcessor(historyProcessor);
        }
    }

    public static class MockAsyncThread extends AsyncThread {

        public int preEpochCallCount = 0;
        public int postEpochCallCount = 0;

        private final IAsyncGlobal asyncGlobal;
        private final MockNeuralNet neuralNet;
        private final AsyncConfiguration conf;

        private final List<TrainSubEpochParams> trainSubEpochParams = new ArrayList<TrainSubEpochParams>();

        public MockAsyncThread(IAsyncGlobal asyncGlobal, int threadNumber, MockNeuralNet neuralNet, MDP mdp, AsyncConfiguration conf, TrainingListenerList listeners) {
            super(asyncGlobal, mdp, listeners, threadNumber, 0);

            this.asyncGlobal = asyncGlobal;
            this.neuralNet = neuralNet;
            this.conf = conf;
        }

        @Override
        protected void preEpoch() {
            ++preEpochCallCount;
            super.preEpoch();
        }

        @Override
        protected void postEpoch() {
            ++postEpochCallCount;
            super.postEpoch();
        }

        @Override
        protected NeuralNet getCurrent() {
            return neuralNet;
        }

        @Override
        protected IAsyncGlobal getAsyncGlobal() {
            return asyncGlobal;
        }

        @Override
        protected AsyncConfiguration getConf() {
            return conf;
        }

        @Override
        protected Policy getPolicy(NeuralNet net) {
            return null;
        }

        @Override
        protected SubEpochReturn trainSubEpoch(Encodable obs, int nstep) {
            trainSubEpochParams.add(new TrainSubEpochParams(obs, nstep));
            return new SubEpochReturn(1, null, 1.0, 1.0);
        }

        @AllArgsConstructor
        @Getter
        public static class TrainSubEpochParams {
            Encodable obs;
            int nstep;
        }
    }



}
