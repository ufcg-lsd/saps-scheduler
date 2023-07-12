/* (C)2020 */
package saps.scheduler.core;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import saps.scheduler.core.arrebol.ArrebolUtils;
import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;
import saps.scheduler.core.selector.DefaultRoundRobin;
import saps.scheduler.core.selector.Selector;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CatalogUtils.class, ArrebolUtils.class})
public class SchedulerTest {

    Properties properties;
    Scheduler scheduler;

    @Mock
    Catalog catalog = mock(Catalog.class);

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
    public void testRecovery() {
        scheduler = new DefaultScheduler(properties, catalog, arrebol, selector);
        PowerMockito.mockStatic(CatalogUtils.class);
        PowerMockito.mockStatic(ArrebolUtils.class);

        List<SapsImage> tasksInProcessingState = new ArrayList<>();
        List<JobResponseDTO> jobResponseDTOs = new ArrayList<>();

        tasksInProcessingState.add(sapsImage1);
        jobResponseDTOs.add(jobResponseDTO);

        when(CatalogUtils.getProcessingTasks(catalog, "gets tasks in processing state")).thenReturn(tasksInProcessingState);
        when(ArrebolUtils.getJobByName(eq(arrebol), anyString(), anyString())).thenReturn(jobResponseDTOs);

        when(sapsImage1.getArrebolJobId()).thenReturn(SapsImage.NONE_ARREBOL_JOB_ID);
        when(sapsImage1.getState()).thenReturn(ImageTaskState.CREATED);
        when(sapsImage1.getTaskId()).thenReturn("1");

        when(jobResponseDTO.getId()).thenReturn("1-1");

        System.out.println(sapsImage1.getState());
        assertEquals(tasksInProcessingState, scheduler.recovery());
    }
  
}
