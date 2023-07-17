/* (C)2020 */
package saps.scheduler.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import saps.scheduler.interfaces.*;
import saps.scheduler.core.arrebol.Arrebol;
import saps.scheduler.core.arrebol.ArrebolUtils;
import saps.scheduler.core.arrebol.JobSubmitted;
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
    JobSubmitted JobSubmitted = mock(JobSubmitted.class);

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
    public void testChecker() {

        ImageTaskState nextState = ImageTaskState.FINISHED;


        // Crie os mocks
        Arrebol mockArrebol = mock(Arrebol.class);
        JobSubmitted mockJob = mock(JobSubmitted.class);
        SapsImage mockTask = mock(SapsImage.class);
        JobResponseDTO mockJobResponse = mock(JobResponseDTO.class);
        
        // Crie a lista de jobs
        List<JobSubmitted> submittedJobs = Arrays.asList(mockJob);
        
        // Configure os mocks para retornar os valores esperados
        when(mockJob.getJobId()).thenReturn("Mock Job ID");
        when(mockJob.getImageTask()).thenReturn(mockTask);
        when(mockArrebol.returnAllJobsSubmitted()).thenReturn(submittedJobs);
        
        // Crie a instância da classe que contém o método que você deseja testar
        DefaultScheduler defaultScheduler = new DefaultScheduler(properties, catalog, mockArrebol, selector);
        
        // Use PowerMockito para fazer o mock dos métodos internos
        DefaultScheduler spyDefaultScheduler = spy(defaultScheduler);
        
        doReturn(mockJobResponse).when(spyDefaultScheduler).getJobByIdInArrebol(anyString(), anyString());
        doReturn(true).when(spyDefaultScheduler).checkJobWasFinish(mockJobResponse);
        doReturn(true).when(spyDefaultScheduler).checkJobFinishedWithSucess(mockJobResponse);

        doReturn(nextState).when(spyDefaultScheduler).getNextState(any(ImageTaskState.class));

        
        when(mockTask.getTaskId()).thenReturn("123");

        when(mockTask.getState()).thenReturn(ImageTaskState.CREATED);

        doReturn(ImageTaskState.FINISHED).when(spyDefaultScheduler).getNextState(any(ImageTaskState.class));

        
        // Chame o método
        spyDefaultScheduler.checker();
        
        
        // Verifique se os métodos foram chamados com os argumentos corretos
        verify(spyDefaultScheduler, times(1)).getJobByIdInArrebol(anyString(), anyString());
        verify(spyDefaultScheduler, times(1)).checkJobWasFinish(mockJobResponse);
        verify(spyDefaultScheduler, times(1)).checkJobFinishedWithSucess(mockJobResponse);
        verify(mockArrebol, times(1)).returnAllJobsSubmitted();
        verify(mockArrebol, times(1)).removeJob(mockJob);
    }

}   
