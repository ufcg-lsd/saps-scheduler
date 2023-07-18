/* (C)2020 */
package saps.scheduler.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.mockito.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

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
    
    @Mock 
    SapsImage sapsImage4 = mock(SapsImage.class);

    @Mock 
    SapsImage sapsImage5 = mock(SapsImage.class);

    @Mock 
    SapsImage sapsImage6 = mock(SapsImage.class);

    @Mock
    SapsUser sapsUser = mock(SapsUser.class);

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
   
    @Test
    public void testRecovery() {
        DefaultScheduler scheduler = new DefaultScheduler(properties, catalog, arrebol, selector);
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

    @Test
    public void testSchedule() {
        DefaultScheduler dScheduler = new DefaultScheduler(properties, catalog, arrebol, selector);
        DefaultScheduler spyScheduler = spy(dScheduler);
        PowerMockito.mockStatic(CatalogUtils.class);
        PowerMockito.mockStatic(ArrebolUtils.class);
        SapsUser sapsUser1 = mock(SapsUser.class);

        List<SapsImage> selectedTasks = new ArrayList<>();
        Map<String, List<SapsImage>> mapUsers2Tasks = new TreeMap<String, List<SapsImage>>();

        List<SapsImage> createdTasks = new ArrayList<>();
        List<SapsImage> readyTasks = new ArrayList<>();
        List<SapsImage> downloadedTasks = new ArrayList<>();

        sapsImage1.setState(ImageTaskState.CREATED);
        createdTasks.add(sapsImage1);

        sapsImage3.setState(ImageTaskState.DOWNLOADED);
        downloadedTasks.add(sapsImage3);

        sapsImage2.setState(ImageTaskState.READY);
        readyTasks.add(sapsImage2);
        sapsImage4.setState(ImageTaskState.READY);
        readyTasks.add(sapsImage4);

        sapsImage5.setState(ImageTaskState.RUNNING);
        sapsImage6.setState(ImageTaskState.PREPROCESSING);

        selectedTasks.add(sapsImage1);
        selectedTasks.add(sapsImage2);
        selectedTasks.add(sapsImage3);
        selectedTasks.add(sapsImage4);

        when(sapsUser1.getId()).thenReturn("user1");

        when(sapsImage1.getUser()).thenReturn("user1");
        when(sapsImage2.getUser()).thenReturn("user1");
        when(sapsImage3.getUser()).thenReturn("user1");
        when(sapsImage4.getUser()).thenReturn("user1");

        when(sapsImage1.getCreationTime()).thenReturn(Timestamp.valueOf("2022-01-01 12:00:00"));
        when(sapsImage2.getCreationTime()).thenReturn(Timestamp.valueOf("2022-02-15 09:30:00"));
        when(sapsImage3.getCreationTime()).thenReturn(Timestamp.valueOf("2022-06-30 18:45:00"));
        when(sapsImage4.getCreationTime()).thenReturn(Timestamp.valueOf("2022-12-25 00:00:00"));
        
        mapUsers2Tasks.put("user1", selectedTasks);

        when(ArrebolUtils.getCountSlots(eq(arrebol), anyString())).thenReturn(10);
        when(CatalogUtils.getTasks(catalog, ImageTaskState.CREATED)).thenReturn(createdTasks);
        when(CatalogUtils.getTasks(catalog, ImageTaskState.READY)).thenReturn(readyTasks);
        when(CatalogUtils.getTasks(catalog, ImageTaskState.DOWNLOADED)).thenReturn(downloadedTasks);
        
        doReturn(mapUsers2Tasks).when(spyScheduler).mapUsers2Tasks(selectedTasks);
        
        spyScheduler.schedule();

        verify(spyScheduler, times(1)).selectTasks();
        verify(spyScheduler, times(1)).submitTasks(any());
    }
  
}
