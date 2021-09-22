// SPDX-License-Identifier: MIT
package com.daimler.sechub.pds.job;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PDSStreamContentUpdateCheckerTest {

    private static final String RUNNING = "RUNNING";
    private PDSStreamContentUpdateChecker checkerToTest;

    @BeforeEach
    void beforeEach() {
        checkerToTest = new PDSStreamContentUpdateChecker();
    }

    @ParameterizedTest(name = "{index} For PDS job status state {0} a refresh check IS necessary")
    @EnumSource(value = PDSJobStatusState.class, names = { RUNNING }, mode = EnumSource.Mode.INCLUDE)
    void check_necessary_refresh_on_job_state_when_never_last_update_done(PDSJobStatusState state) {

        /* prepare */
        PDSJob job = createCompleteJobWithoutLastStreamUpdateSet(state);

        /* execute */
        boolean necessary = checkerToTest.isUpdateNecessaryWhenRefreshRequestedNow(job);

        /* test */
        assertTrue(necessary);
    }

    @ParameterizedTest(name = "{index} For PDS job status state {0} a refresh check is NOT necessary - even when stream data was never updated before")
    @EnumSource(value = PDSJobStatusState.class, names = { RUNNING }, mode = EnumSource.Mode.EXCLUDE)
    void check_unnecessary_refresh_on_state_when_never_last_update_done(PDSJobStatusState state) {

        /* prepare */
        PDSJob job = createCompleteJobWithoutLastStreamUpdateSet(state);

        /* execute */
        boolean necessary = checkerToTest.isUpdateNecessaryWhenRefreshRequestedNow(job);

        /* test */
        assertFalse(necessary);
    }

    @ParameterizedTest(name = "{index} For PDS job where last stream update was {0} milliseconds before, a refresh check IS always necessary when no stream data has been updated before")
    @ValueSource(longs = { 2100, 10000 })
    void check_RUNNING_state_and_update_time_gap_too_big_a_refresh_IS_necessary(long milliseconds) {
        /* prepare */
        PDSJob job = createCompleteJobWithoutLastStreamUpdateSet(PDSJobStatusState.RUNNING);
        job.lastStreamTxtUpdate = LocalDateTime.now().minusNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds));

        /* execute */
        boolean necessary = checkerToTest.isUpdateNecessaryWhenRefreshRequestedNow(job);

        /* test */
        assertTrue(necessary);
    }

    @ParameterizedTest(name = "{index} For PDS job where last stream update was {0} milliseconds before, a refresh check IS NOT necessary")
    @ValueSource(longs = { 300, 1000, 1500, 1900 })
    void check_RUNNING_state_and_update_time_gap_too_big_a_refresh_IS_NOT_necessary(long milliseconds) {
        /* prepare */
        PDSJob job = createCompleteJobWithoutLastStreamUpdateSet(PDSJobStatusState.RUNNING);
        job.lastStreamTxtUpdate = LocalDateTime.now().minusNanos(TimeUnit.NANOSECONDS.toMillis(milliseconds));

        /* execute */
        boolean necessary = checkerToTest.isUpdateNecessaryWhenRefreshRequestedNow(job);

        /* test */
        assertFalse(necessary);
    }

    PDSJob createCompleteJobWithoutLastStreamUpdateSet(PDSJobStatusState state) {

        PDSJob job = new PDSJob();
        job.setStarted(LocalDateTime.now());
        job.setOwner("owner");
        job.setResult("result");
        job.setState(state);
        job.lastStreamTxtUpdate = null;
        job.lastStreamTxtRefreshRequest = null;

        return job;
    }
}
