/* (C)2020 */
package saps.scheduler.core;

import static org.junit.Assert.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import saps.scheduler.interfaces.*;
import saps.scheduler.core.arrebol.Arrebol;
import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;
import saps.scheduler.core.selector.DefaultRoundRobin;
import saps.scheduler.core.selector.Selector;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CatalogUtils.class})
public class SchedulerTest {

    Properties properties;
    Scheduler scheduler;

    @Mock
    Catalog catalog = mock(Catalog.class);

    @Mock
    ScheduledExecutorService sapsExecutor = mock(ScheduledExecutorService.class);

    @Mock
    Selector selector = mock(Selector.class);

    @Mock
    JobResponseDTO jobResponseDTO = mock(JobResponseDTO.class);

    @Mock
    Arrebol arrebol = mock(Arrebol.class);

    @Mock
    SapsImage sapsImage1 = mock(SapsImage.class);

    @Mock
    SapsImage sapsImage2 = mock(SapsImage.class);

    @Mock 
    SapsImage sapsImage3 = mock(SapsImage.class);

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        properties = new Properties();
    }

    @Test
    public void testRecovery() throws SapsException {

        scheduler = new DefaultScheduler(properties, catalog, sapsExecutor, arrebol, selector);
        
        List<SapsImage> tasksInProcessingState = new ArrayList<>();
        List<JobResponseDTO> jobResponseDTOs = new ArrayList<>();

        tasksInProcessingState.add(sapsImage1);
        tasksInProcessingState.add(sapsImage2);
        jobResponseDTOs.add(jobResponseDTO);

        sapsImage1.setState(ImageTaskState.CREATED);

        when (sapsImage1.getArrebolJobId().equals(SapsImage.NONE_ARREBOL_JOB_ID)).thenReturn(true);
        when (sapsImage1.getState().getValue()).thenReturn("1");
        when(sapsImage1.getTaskId()).thenReturn("1");

        assertTrue(sapsImage1.getState().equals(ImageTaskState.CREATED));
        scheduler.recovery();
        assertTrue(sapsImage1.getState().equals(ImageTaskState.DOWNLOADING));

    }

  
}
